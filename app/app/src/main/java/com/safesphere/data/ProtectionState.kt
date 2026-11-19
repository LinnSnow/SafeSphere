package com.safesphere.data

import android.content.Context

// VPN ka status yahan save hota hai. Person 1 ki service start/stop par
// setVpnActive() call karegi, aur HeartbeatWorker isActive padhega.
object ProtectionState {

    private const val PREFS = "safesphere_protection"
    private const val KEY_VPN_ACTIVE = "vpn_active"

    fun setVpnActive(context: Context, active: Boolean) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_VPN_ACTIVE, active)
            .apply()
    }

    fun isVpnActive(context: Context): Boolean =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_VPN_ACTIVE, false)
}