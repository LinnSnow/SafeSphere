package com.safesphere.vpn

import android.content.Context

class BlocklistManager(context: Context) {

    private val blocked = HashSet<String>()

    init {
        context.assets.open("blocklist.txt").bufferedReader().useLines { lines ->
            lines.forEach { raw ->
                val line = raw.substringBefore('#').trim().lowercase()
                if (line.isEmpty()) return@forEach
                val parts = line.split(Regex("\\s+"))
                val domain = if (parts.size >= 2) parts[1] else parts[0]   // hosts format or plain
                if (domain.contains('.') && domain != "0.0.0.0") blocked.add(domain)
            }
        }
    }

    /** True if the domain, or any parent domain, is on the list (so sub.example.com is caught by example.com). */
    fun isBlocked(domain: String): Boolean {
        var d = domain.lowercase().trimEnd('.')
        while (d.contains('.')) {
            if (d in blocked) return true
            d = d.substringAfter('.')
        }
        return false
    }
}