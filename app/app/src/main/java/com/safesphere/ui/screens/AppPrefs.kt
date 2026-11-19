package com.safesphere.ui.screens

import android.content.Context

/** Tiny on-device memory: has the teen/parent finished the first-time setup? */
object AppPrefs {
    private const val FILE = "safesphere_prefs"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isSetupDone(ctx: Context): Boolean = prefs(ctx).getBoolean("setup_done", false)

    fun setSetupDone(ctx: Context, done: Boolean) {
        prefs(ctx).edit().putBoolean("setup_done", done).apply()
    }
}