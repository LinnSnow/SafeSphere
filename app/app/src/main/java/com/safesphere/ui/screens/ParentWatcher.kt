package com.safesphere.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Parent phone: notices when the teen's protection changes (on <-> off/paused), by comparing each
 * status() answer with the last one. Logs it, and shows a notification when the app is closed.
 * (Exact from/to times need Person 4's protection-log endpoint. This records when WE noticed.)
 */
object ParentWatcher {
    private const val WORK = "parent_watch"
    private const val KEY_LAST = "watch_last_active"
    private const val CHANNEL = "safesphere_parent_alerts"
    private const val NOTIF_ID = 4203

    private fun prefs(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("safesphere_prefs", Context.MODE_PRIVATE)

    /** Background check about every 15 minutes (Android's minimum). Safe to call again and again. */
    fun schedule(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<ParentWatchWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(ctx.applicationContext)
            .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx.applicationContext).cancelUniqueWork(WORK)
    }

    fun reset(ctx: Context) {
        prefs(ctx).edit().remove(KEY_LAST).apply()
    }

    /** Returns "off" or "on" if the state CHANGED since the last check, otherwise null. */
    fun apply(ctx: Context, active: Boolean): String? {
        val p = prefs(ctx)
        val last: Boolean? = if (p.contains(KEY_LAST)) p.getBoolean(KEY_LAST, true) else null
        p.edit().putBoolean(KEY_LAST, active).apply()
        if (last == null || last == active) return null
        val kind = if (active) "on" else "off"
        ProtectionLog.add(ctx, kind, key = ProtectionLog.WATCH)
        return kind
    }

    /** Notification on the parent's phone. No content, only the state. */
    fun notifyChange(ctx: Context, kind: String, detail: String? = null) {
        val app = ctx.applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Teen protection alerts", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val open = Intent(app, FamilyActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            app, 4, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val off = kind == "off"
        val n = NotificationCompat.Builder(app, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(if (off) "Protection turned off" else "Protection is back on")
            .setContentText(
                detail ?: if (off) "SafeSphere is off or paused on your teen's phone."
                else "SafeSphere protection is active again on your teen's phone."
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIF_ID, n)
    }
}

/** Runs in the background on the parent's phone. */
class ParentWatchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (AppPrefs.getRole(ctx) != AppPrefs.ROLE_PARENT) return Result.success()
        when (val r = Session.loadParent(ctx)) {
            is ParentResult.Ok -> {
                val status = r.status
                if (status != null) {
                    val changed = ParentWatcher.apply(ctx, status.protectionActive)
                    if (changed != null) ParentWatcher.notifyChange(ctx, changed)
                }
            }
            ParentResult.Unpaired -> ParentWatcher.cancel(ctx)
            ParentResult.Offline -> {}
        }
        return Result.success()
    }
}