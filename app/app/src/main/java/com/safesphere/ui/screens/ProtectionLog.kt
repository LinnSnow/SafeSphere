package com.safesphere.ui.screens

import android.content.Context

/** Last 30 protection changes (what + when). No content. Shown on the Alerts screen. */
object ProtectionLog {
    const val LOCAL = "protection_log"        // changes made on THIS phone (teen)
    const val WATCH = "parent_watch_log"      // changes the PARENT's phone noticed on the teen's phone

    data class Entry(val kind: String, val at: Long, val until: Long)

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("safesphere_prefs", Context.MODE_PRIVATE)

    /** kind = "off", "on", "paused" or "resumed". */
    fun add(ctx: Context, kind: String, until: Long = 0L, key: String = LOCAL) {
        val old = prefs(ctx).getString(key, "") ?: ""
        val lines = old.split("\n").filter { it.isNotBlank() }.takeLast(29) +
                "$kind|${System.currentTimeMillis()}|$until"
        prefs(ctx).edit().putString(key, lines.joinToString("\n")).apply()
    }

    /** Newest first. */
    fun recent(ctx: Context, n: Int = 10, key: String = LOCAL): List<Entry> {
        val text = prefs(ctx).getString(key, "") ?: ""
        return text.split("\n").filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("|")
            if (p.size < 3) null else Entry(p[0], p[1].toLongOrNull() ?: 0L, p[2].toLongOrNull() ?: 0L)
        }.reversed().take(n)
    }
}