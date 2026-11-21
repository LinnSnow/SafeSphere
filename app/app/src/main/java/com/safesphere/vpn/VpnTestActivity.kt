package com.safesphere.vpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout

/**
 * Temporary test screen for Person 1's VPN. Not shown to users.
 * The prepare() then start logic here is what the real permission step will need to call.
 */
class VpnTestActivity : Activity() {

    private val reqVpn = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val start = Button(this).apply { text = "Start VPN" }
        val stop = Button(this).apply { text = "Stop VPN" }

        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 96, 48, 48)
            addView(start)
            addView(stop)
        })

        start.setOnClickListener {
            val intent = VpnService.prepare(this)          // null means already permitted
            if (intent != null) {
                startActivityForResult(intent, reqVpn)     // shows the system dialog
            } else {
                sendAction(SafeSphereVpnService.ACTION_START)
            }
        }
        stop.setOnClickListener { sendAction(SafeSphereVpnService.ACTION_STOP) }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == reqVpn && resultCode == RESULT_OK) {
            sendAction(SafeSphereVpnService.ACTION_START)
        }
    }

    private fun sendAction(action: String) {
        startService(Intent(this, SafeSphereVpnService::class.java).setAction(action))
    }
}