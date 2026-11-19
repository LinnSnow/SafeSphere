package com.safesphere.data

import android.util.Log

/**
 * Hand-off interface agreed across the SafeSphere team (Person 1 & Person 2 call it,
 * Person 4 implements it via Room database).
 *
 * Stores blocked count events: timestamp + category ("website" or "text") ONLY.
 * No domain name, URL, or message text is ever stored to preserve teen privacy.
 */
interface BlockEventLogger {
    suspend fun logBlockedEvent(category: String = "text")
}

/**
 * Default fallback implementation used when testing Person 2's ML module
 * before Person 4 finishes the Room database.
 */
class LogcatBlockEventLogger : BlockEventLogger {
    override suspend fun logBlockedEvent(category: String) {
        val timestamp = System.currentTimeMillis()
        Log.i(TAG, "Blocked event logged [category=$category, timestamp=$timestamp (UTC millis)]")
    }

    companion object {
        private const val TAG = "SafeSphereLogger"
    }
}
