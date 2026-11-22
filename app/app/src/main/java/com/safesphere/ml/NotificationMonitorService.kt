package com.safesphere.ml

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.safesphere.data.AppDatabase
import com.safesphere.data.BlockEventLogger
import com.safesphere.data.BlockEventLoggerImpl
import com.safesphere.ui.screens.AppPrefs
import com.safesphere.ui.screens.WarningNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Listens to notifications from OTHER apps (WhatsApp, SMS, Instagram...), scores the text with
 * the on-device ML model, and if it looks adult-toned:
 *   1. logs a block event (timestamp + category "text" ONLY - never the message), and
 *   2. shows a warning notification to the teen (WarningNotifier). Tapping it opens the Text Warning screen.
 *
 * The message text is only handed to the Text Warning screen inside that notification's intent
 * (in memory). It is never written to disk, the database, the logs or the network.
 */
class NotificationMonitorService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var classifier: TextClassifier? = null
    private lateinit var logger: BlockEventLogger

    // Notification keys we have already scored. Chat apps re-post the same notification
    // every time the text is updated, and we don't want to warn twice.
    private val seen = object : LinkedHashMap<String, Int>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>?) = size > 200
    }

    override fun onCreate() {
        super.onCreate()
        logger = createLogger()
    }

    /** Person 4's Room-backed logger: stores only a timestamp and the category, never the message. */
    private fun createLogger(): BlockEventLogger =
        BlockEventLoggerImpl(AppDatabase.getInstance(applicationContext).blockedEventDao())

    override fun onListenerConnected() {
        super.onListenerConnected()
        // Load the model once, off the main thread (it reads a large file).
        scope.launch {
            try {
                classifier = TextClassifier(applicationContext)
                Log.i(TAG, "Classifier ready (threshold=${classifier?.threshold})")
            } catch (e: Exception) {
                Log.e(TAG, "Could not load classifier", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName) return // never scan our own warnings (infinite loop!)
        if (sbn.isOngoing) return                    // music players, downloads, etc.
        if (!AppPrefs.isProtectionActive(applicationContext)) return // protection is off or paused

        val notification = sbn.notification ?: return
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return // "3 new messages" bundles

        val text = extractText(notification) ?: return
        val hash = text.hashCode() // we only keep a hash in memory to skip repeats; never the text itself
        if (seen[sbn.key] == hash) return
        seen[sbn.key] = hash

        val model = classifier ?: return // not loaded yet; skip this one
        scope.launch {
            try {
                val score = model.classify(text)
                if (score > model.threshold) {
                    logger.logBlock("text")
                    WarningNotifier.showTextWarning(applicationContext, text)
                }
            } catch (e: Exception) {
                Log.e(TAG, "classification failed", e) // do NOT log the message text
            }
        }
    }

    /** Title is skipped (it's the sender's name). Text, big text and inbox lines are joined. */
    private fun extractText(notification: Notification): String? {
        val extras = notification.extras ?: return null
        val parts = ArrayList<String>()
        extras.getCharSequence(Notification.EXTRA_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.let { parts.add(it.toString()) }
        extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { parts.add(it.toString()) }
        val joined = parts.distinct().joinToString(". ").trim()
        return joined.ifEmpty { null }
    }

    override fun onDestroy() {
        scope.cancel()
        classifier?.close()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "NotifMonitor"
    }
}