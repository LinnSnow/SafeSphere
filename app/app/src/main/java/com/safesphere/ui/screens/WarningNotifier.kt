package com.safesphere.ui.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * The two notifications SafeSphere shows. Neither ever contains the message or the website.
 *  - Text warning: from the ML notification service. Buttons: Review, Get help (Childline 1098).
 *  - Site blocked: from the VPN service (at most one per minute). Opens the "site blocked" screen.
 */
object WarningNotifier {
    const val TEXT_ID = 4201            // TextWarningActivity cancels this id on "Don't open"
    private const val SITE_ID = 4202
    private const val CH_TEXT = "safesphere_text_warnings"
    private const val CH_SITE = "safesphere_site_blocks"

    @Volatile
    private var lastSiteShown = 0L

    private fun manager(ctx: Context) =
        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val m = manager(ctx)
        m.createNotificationChannel(
            NotificationChannel(CH_TEXT, "Content warnings", NotificationManager.IMPORTANCE_HIGH)
        )
        m.createNotificationChannel(
            NotificationChannel(CH_SITE, "Blocked sites", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
                ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** [message] is handed to the warning screen in memory only. Pass null for a demo/sample. */
    fun showTextWarning(ctx: Context, message: String?) {
        val app = ctx.applicationContext
        if (!canNotify(app)) return
        ensureChannels(app)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val open = Intent(app, TextWarningActivity::class.java).apply {
            if (message != null) putExtra(TextWarningActivity.EXTRA_MESSAGE, message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val review = PendingIntent.getActivity(app, 1, open, flags)
        val help = PendingIntent.getActivity(
            app, 2, Intent(Intent.ACTION_DIAL, Uri.parse("tel:1098")), flags
        )

        val n = NotificationCompat.Builder(app, CH_TEXT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("SafeSphere warning")
            .setContentText("A message you just received may be inappropriate. Tap to review.")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "A message you just received may contain inappropriate content. " +
                            "It is hidden here. Tap Review to look at it safely, or get help any time."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(review)
            .addAction(android.R.drawable.ic_menu_view, "Review", review)
            .addAction(android.R.drawable.ic_menu_call, "Get help (1098)", help)
            .setAutoCancel(true)
            .build()
        manager(app).notify(TEXT_ID, n)
    }

    /** Called when the VPN blocks a site. No website name is passed. Rate-limited to once a minute. */
    fun showSiteBlocked(ctx: Context, force: Boolean = false) {
        val app = ctx.applicationContext
        val now = System.currentTimeMillis()
        if (!force && now - lastSiteShown < 60_000L) return
        if (!canNotify(app)) return
        lastSiteShown = now
        ensureChannels(app)

        val open = Intent(app, SiteBlockedActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            app, 3, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(app, CH_SITE)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("SafeSphere blocked a site")
            .setContentText("A harmful site was stopped before it loaded. Tap to see why.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager(app).notify(SITE_ID, n)
    }
}