package com.safesphere.ui.screens

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Limits tab: what SafeSphere is allowed to do on this phone, and whether each permission is on.
 * Tap a row that needs attention to fix it.
 */
class LimitsActivity : Activity() {

    companion object {
        private const val REQ_VPN = 201
        private const val REQ_NOTIFICATIONS = 202
    }

    private lateinit var host: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        host = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Pal.BG))
            addView(UiKit.topBar(ctx, "Permissions & Limits"))
            addView(host)
            addView(Nav.bar(ctx, Tab.LIMITS))
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
        render()
    }

    override fun onResume() {
        super.onResume()
        render() // refresh after coming back from a system dialog or settings page
    }

    private fun render() {
        host.removeAllViews()
        host.addView(buildColumn())
    }

    // ---------- real status checks ----------

    private fun vpnAllowed(): Boolean = try {
        VpnService.prepare(this) == null
    } catch (e: Exception) {
        false
    }

    private fun listenerAllowed(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName)
    }

    private fun postNotificationsAllowed(): Boolean =
        if (Build.VERSION.SDK_INT >= 33) {
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

    // ---------- screen ----------

    private fun buildColumn(): LinearLayout {
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val title = UiKit.text(ctx, "Permissions & Limits", 24f, Pal.INK, bold = true)
        val sub = UiKit.text(
            ctx, "Everything SafeSphere is allowed to do on this phone. Nothing else.", 14f, Pal.MUTED
        )
        UiKit.topMargin(ctx, sub, 4)

        val card = UiKit.card(ctx).apply {
            addView(permissionRow(
                "globe", "Web protection", "Blocks adult and harmful sites on this phone.",
                vpnAllowed()
            ) { VpnService.prepare(ctx)?.let { startActivityForResult(it, REQ_VPN) } })

            addView(permissionRow(
                "message", "Message safety", "Reads notification previews on-device to warn you.",
                listenerAllowed()
            ) { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })

            addView(permissionRow(
                "bell", "Warning notifications", "Lets SafeSphere show you a warning.",
                postNotificationsAllowed()
            ) {
                if (Build.VERSION.SDK_INT >= 33) {
                    requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATIONS)
                }
            })

            addView(permissionRow(
                "key", "Parent PIN", "Needed to turn protection off or pause it.",
                AppPrefs.hasPin(ctx)
            ) { startActivity(Intent(ctx, SetPinActivity::class.java)) })

            val active = AppPrefs.isProtectionActive(ctx)
            addView(permissionRow(
                "shield", "Protection status",
                if (active) "Filtering is running." else "Protection is turned off or paused.",
                active, okLabel = "On", badLabel = "Off"
            ) { startActivity(Intent(ctx, ProtectionSettingsActivity::class.java)) })
        }
        UiKit.topMargin(ctx, card, 16)

        // Not built yet: shown honestly as "Coming soon".
        val soon = UiKit.card(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.circle(ctx, "clock", 40, Pal.CONTAINER, Pal.MUTED))
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
                addView(UiKit.text(ctx, "Screen-time limits", 14f, Pal.INK, bold = true))
                addView(UiKit.text(ctx, "Daily limits you and your parent agree on.", 12f, Pal.MUTED))
            })
            addView(UiKit.pill(ctx, "Coming soon", Pal.MUTED, Pal.CONTAINER, 11f))
        }
        UiKit.topMargin(ctx, soon, 12)

        return UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(title)
            addView(sub)
            addView(card)
            addView(soon)
        }
    }

    /** One row: icon, title, description, status tag. Tapping a row that needs attention runs [onFix]. */
    private fun permissionRow(
        iconName: String, title: String, desc: String, ok: Boolean,
        okLabel: String = "Allowed", badLabel: String = "Needs attention",
        onFix: () -> Unit
    ): LinearLayout {
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)
        val texts = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(UiKit.text(ctx, title, 14f, Pal.INK, bold = true))
            addView(UiKit.text(ctx, desc, 12f, Pal.MUTED).apply { setPadding(0, dp(2), 0, 0) })
        }
        val tag = if (ok) {
            Icons.pill(ctx, "check", okLabel, Pal.GREEN_DARK, Pal.GREEN_SOFT, 11f)
        } else {
            Icons.pill(ctx, "alert", badLabel, Pal.RED, Pal.RED_SOFT, 11f)
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, UiKit.WRAP).apply { topMargin = dp(6) }
            setPadding(0, dp(8), 0, dp(8))
            addView(Icons.circle(ctx, iconName, 40, if (ok) Pal.GREEN_SOFT else Pal.RED_SOFT, if (ok) Pal.GREEN_DARK else Pal.RED))
            addView(texts)
            addView(tag)
            setOnClickListener { onFix() }
        }
    }
}