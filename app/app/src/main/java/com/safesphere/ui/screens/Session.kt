package com.safesphere.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.work.WorkManager
import com.safesphere.data.ApiClient
import com.safesphere.data.StatusResponse
import com.safesphere.data.TokenStore
import com.safesphere.data.WeeklyResponse
import com.safesphere.vpn.SafeSphereVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.TimeZone

sealed class ParentResult {
    data class Ok(val status: StatusResponse?, val weekly: WeeklyResponse?) : ParentResult()
    object Unpaired : ParentResult()
    object Offline : ParentResult()
}

/** Small helpers shared by Home, Family and Parent settings. Person 4's rule: any 401 = unpaired. */
object Session {

    fun tzOffsetMinutes(): Int = TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

    /** Parent phone: real status + weekly numbers for the paired teen. */
    suspend fun loadParent(ctx: Context): ParentResult = withContext(Dispatchers.IO) {
        val store = TokenStore(ctx.applicationContext)
        val token = store.getParentToken()
        val id = store.getPairedDeviceId()
        if (token == null || id == null) return@withContext ParentResult.Unpaired
        val bearer = store.bearer(token)
        try {
            val status = ApiClient.apiService.status(bearer, id)
            val weekly = ApiClient.apiService.weekly(bearer, id, tzOffsetMinutes())
            ParentResult.Ok(status, weekly)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                resetParent(ctx)
                ParentResult.Unpaired
            } else ParentResult.Offline
        } catch (e: IOException) {
            ParentResult.Offline
        }
    }

    /** Teen phone: still paired? Returns false (after resetting) if the server says 401. */
    suspend fun checkTeen(ctx: Context): Boolean = withContext(Dispatchers.IO) {
        val store = TokenStore(ctx.applicationContext)
        val token = store.getTeenToken() ?: return@withContext true
        try {
            ApiClient.apiService.pairState(store.bearer(token), store.getOrCreateDeviceId())
            true
        } catch (e: HttpException) {
            if (e.code() == 401) {
                resetTeen(ctx)
                false
            } else true
        } catch (e: IOException) {
            true
        }
    }

    fun resetParent(ctx: Context) {
        TokenStore(ctx.applicationContext).resetParentState()
        ParentWatcher.cancel(ctx)
        ParentWatcher.reset(ctx)
        AppPrefs.setSetupDone(ctx, false)
    }

    fun resetTeen(ctx: Context) {
        TokenStore(ctx.applicationContext).resetTeenState()   // also makes a NEW device id
        WorkManager.getInstance(ctx.applicationContext).cancelAllWork()
        AppPrefs.setSetupDone(ctx, false)
    }

    fun leaveToWelcome(activity: Activity) {
        activity.startActivity(
            Intent(activity, WelcomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    /** Teen phone: if the VPN permission was given but the service is not running (app was killed), start it. */
    fun ensureVpn(ctx: Context) {
        if (AppPrefs.getRole(ctx) != AppPrefs.ROLE_TEEN) return
        if (SafeSphereVpnService.isActive()) return
        if (VpnService.prepare(ctx) != null) return           // permission not granted yet
        ctx.startService(
            Intent(ctx, SafeSphereVpnService::class.java).setAction(SafeSphereVpnService.ACTION_START)
        )
    }
}