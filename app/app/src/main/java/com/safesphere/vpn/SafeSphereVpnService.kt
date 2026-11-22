package com.safesphere.vpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.safesphere.data.AppDatabase
import com.safesphere.data.BlockEventLogger
import com.safesphere.data.BlockEventLoggerImpl
import com.safesphere.data.HeartbeatWorker
import com.safesphere.data.ProtectionState
import com.safesphere.ui.screens.AppPrefs
import com.safesphere.ui.screens.WarningNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class SafeSphereVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.safesphere.START"
        const val ACTION_STOP = "com.safesphere.STOP"

        @Volatile
        private var running = false

        /** Lets other code (e.g. the heartbeat) check VPN status without holding a service reference. */
        fun isActive(): Boolean = running
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var blocklist: BlocklistManager? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val blockLogger: BlockEventLogger by lazy {
        BlockEventLoggerImpl(AppDatabase.getInstance(applicationContext).blockedEventDao())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopVpn()
            return START_NOT_STICKY
        }
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (running) return
        vpnInterface = Builder()
            .setSession("SafeSphere")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("10.0.0.1")      // fake DNS server: all lookups are sent here
            .addRoute("10.0.0.1", 32)      // only DNS traffic enters the tunnel
            .establish() ?: return
        running = true
        ProtectionState.setVpnActive(this, true)
        Log.d("DNS", "VPN started")
        scope.launch {
            blocklist = BlocklistManager(applicationContext)   // load before reading packets
            readLoop()
        }
    }

    private suspend fun readLoop() {
        val fd = vpnInterface?.fileDescriptor ?: return
        val input = FileInputStream(fd)
        val output = FileOutputStream(fd)
        val buf = ByteArray(32767)
        while (running) {
            val len = try {
                input.read(buf)
            } catch (e: Exception) {
                break
            }
            if (len <= 0) continue
            val packet = buf.copyOf(len)
            scope.launch { handlePacket(packet, output) }
        }
    }

    private suspend fun handlePacket(packet: ByteArray, output: FileOutputStream) {
        try {
            val ihl = DnsPacketParser.dnsIpHeaderLength(packet)
            if (ihl < 0) return
            val domain = DnsPacketParser.parse(packet) ?: return
            val query = packet.copyOfRange(ihl + 8, packet.size)

            // Only block while protection is ON (not turned off, not paused from Protection settings).
            if (AppPrefs.isProtectionActive(applicationContext) && blocklist?.isBlocked(domain) == true) {
                Log.d("DNS", "BLOCKED: $domain")   // debug only; remove domain logging for release
                val reply = DnsPacketParser.buildResponse(
                    packet, ihl, DnsPacketParser.buildNxDomain(query)
                )
                synchronized(output) { output.write(reply) }
                blockLogger.logBlock("website")    // timestamp + category only, never the domain
                WarningNotifier.showSiteBlocked(applicationContext)   // at most one per minute, no domain
                return
            }

            Log.d("DNS", "Queried: $domain")
            DatagramSocket().use { socket ->
                protect(socket)                    // keep this socket out of our own VPN
                socket.soTimeout = 5000
                socket.send(
                    DatagramPacket(query, query.size, InetAddress.getByName("8.8.8.8"), 53)
                )
                val buf = ByteArray(4096)
                val rp = DatagramPacket(buf, buf.size)
                socket.receive(rp)
                val response = DnsPacketParser.buildResponse(packet, ihl, buf.copyOf(rp.length))
                synchronized(output) { output.write(response) }
            }
        } catch (e: Exception) {
            Log.w("DNS", "Packet handling failed: ${e.message}")
        }
    }

    private fun stopVpn() {
        running = false
        vpnInterface?.close()
        vpnInterface = null
        scope.coroutineContext.cancelChildren()
        ProtectionState.setVpnActive(this, false)
        stopSelf()
        Log.d("DNS", "VPN stopped")
    }

    /** Called when the user switches the VPN off from system settings. */
    override fun onRevoke() {
        stopVpn()                                  // also sets ProtectionState to false
        // Tell the backend right away instead of waiting for the next scheduled heartbeat.
        WorkManager.getInstance(applicationContext).enqueue(
            OneTimeWorkRequestBuilder<HeartbeatWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
        )
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}