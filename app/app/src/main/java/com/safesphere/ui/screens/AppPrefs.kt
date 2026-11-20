package com.safesphere.ui.screens

import android.content.Context

/** Small on-device memory: setup finished? is this phone the teen's or the parent's? */
object AppPrefs {
    const val ROLE_TEEN = "teen"
    const val ROLE_PARENT = "parent"

    // Same file as Person 4's TokenStore, but different keys, so nothing clashes.
    private const val FILE = "safesphere_prefs"

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isSetupDone(ctx: Context): Boolean = prefs(ctx).getBoolean("setup_done", false)

    fun setSetupDone(ctx: Context, done: Boolean) {
        prefs(ctx).edit().putBoolean("setup_done", done).apply()
    }

    fun getRole(ctx: Context): String? = prefs(ctx).getString("role", null)

    fun setRole(ctx: Context, role: String) {
        prefs(ctx).edit().putString("role", role).apply()
    }
}