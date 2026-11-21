package com.safesphere.ui.screens

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

/**
 * Steps 1 and 2 of 2 on the teen's phone.
 *  Step 1: Allow Local Shielding -> the real Android VPN permission dialog.
 *  Step 2: Notification Safety   -> asks to show warnings, then opens the system "notification access" page.
 */
class PermissionStepActivity : Activity() {

    companion object {
        const val EXTRA_STEP = "step"
        private const val REQ_VPN = 101
        private const val REQ_POST_NOTIFICATIONS = 7
    }

    private var step = 1
    private var openedListenerSettings = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        step = intent.getIntExtra(EXTRA_STEP, 1)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }

        val stepName = if (step == 1) "Web Protection" else "Notification Safety"
        val stepRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.pill(ctx, "Step $step of 2", UiKit.BLUE_DARK, UiKit.BLUE_SOFT, 11f))
            addView(UiKit.text(ctx, stepName, 12f, UiKit.MUTED, bold = true).apply {
                setPadding(dp(10), 0, 0, 0)
            })
        }
        UiKit.topMargin(ctx, stepRow, 16)

        val bars = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            for (i in 1..2) {
                addView(View(ctx).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(6), 1f).apply {
                        if (i == 1) rightMargin = dp(6)
                    }
                    background = UiKit.rounded(
                        Color.parseColor(if (i <= step) UiKit.BLUE else "#D6DEEF"), dp(3).toFloat()
                    )
                })
            }
        }
        UiKit.topMargin(ctx, bars, 10)

        val title = UiKit.text(
            ctx, if (step == 1) "Allow Local Shielding" else "Notification Safety",
            24f, UiKit.INK, bold = true
        )
        UiKit.topMargin(ctx, title, 16)

        val card = if (step == 1) stepOneCard() else stepTwoCard()
        UiKit.topMargin(ctx, card, 16)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(16))
            addView(back)
            addView(stepRow)
            addView(bars)
            addView(title)
            addView(card)
        }

        // ---- bottom: main button + small links ----
        val mainButton = UiKit.button(
            this, if (step == 1) "Allow VPN access" else "Allow notification access", filled = true
        ) { if (step == 1) askVpn() else openNotificationSettings() }

        val whyLink = UiKit.text(
            ctx,
            if (step == 1) "Why do we use on-device filtering?" else "Learn how notifications stay private",
            13f, UiKit.BLUE_DARK, bold = true
        ).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(6))
            setOnClickListener { if (step == 1) showWhyFiltering() else showZeroSurveillance() }
        }

        // Demo helper only: lets you click through when the permission can't be granted.
        // TODO: remove before the final version.
        val skip = UiKit.text(ctx, "Skip for now", 12f, UiKit.MUTED).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, dp(4))
            setOnClickListener { goNext() }
        }

        val bottom = UiKit.vertical(this).apply {
            setBackgroundColor(Color.parseColor(UiKit.BG))
            setPadding(dp(16), dp(4), dp(16), dp(10))
            addView(mainButton)
            addView(whyLink)
            addView(skip)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(UiKit.topBar(ctx, "Permission Limits"))
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
            addView(bottom)
        }
        UiKit.padForSystemBars(root)
        setContentView(root)

        // Android 13+: the app must ask before it can show its warning notification.
        if (step == 2 && Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                REQ_POST_NOTIFICATIONS
            )
        }
    }

    // ---------- Step 1 ----------

    private fun stepOneCard(): LinearLayout {
        val ctx = this
        val dp = { v: Int -> UiKit.dp(ctx, v) }

        val flow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(flowCell("📱", "This Phone", UiKit.BLUE_SOFT, UiKit.BLUE_DARK))
            addView(UiKit.text(ctx, "→", 18f, UiKit.MUTED))
            addView(flowCell("🛡", "Local Filter", UiKit.GREEN_SOFT, UiKit.GREEN_DARK))
            addView(UiKit.text(ctx, "→", 18f, UiKit.MUTED))
            addView(flowCell("🚫", "No Cloud Log", UiKit.RED_SOFT, UiKit.RED))
        }
        val diagram = UiKit.vertical(ctx).apply {
            background = UiKit.rounded(Color.parseColor("#EAF8F1"), dp(14).toFloat())
            setPadding(dp(12), dp(14), dp(12), dp(14))
            addView(flow)
            addView(UiKit.pill(ctx, "🔒 Private on-device VPN loopback", UiKit.MUTED, "#FFFFFF", 11f).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.WRAP, UiKit.WRAP).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    topMargin = dp(12)
                }
            })
        }

        return UiKit.card(ctx).apply {
            addView(UiKit.text(
                ctx,
                "SafeSphere creates an on-device local filter to block harmful websites before they load. " +
                        "Your web browsing data never leaves this phone and is never uploaded.",
                14f, UiKit.INK
            ))
            addView(diagram.also { UiKit.topMargin(ctx, it, 14) })
            listOf(
                "Blocks malicious links and harmful domains silently",
                "Zero battery drain compared to remote proxy tunnels",
                "Parents see protection status, not your personal search history"
            ).forEach { addView(Widgets.featureRow(ctx, it, "").also { r -> UiKit.topMargin(ctx, r, 10) }) }
            addView(
                UiKit.infoBox(
                    ctx, "ℹ",
                    UiKit.text(
                        ctx,
                        "Next, your system will ask to confirm the \"SafeSphere Connection Request.\" Tap OK to continue.",
                        12f, UiKit.INK
                    )
                ).also { UiKit.topMargin(ctx, it, 12) }
            )
        }
    }

    private fun flowCell(icon: String, label: String, bgHex: String, fgHex: String): LinearLayout {
        val ctx = this
        return UiKit.vertical(ctx).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            addView(UiKit.roundIcon(ctx, icon, 52, bgHex, fgHex, 22f).apply {
                (layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL
            })
            addView(UiKit.text(ctx, label, 11f, UiKit.INK, bold = true).apply {
                gravity = Gravity.CENTER
                setPadding(0, UiKit.dp(ctx, 6), 0, 0)
            })
        }
    }

    private fun askVpn() {
        // Shows Android's own "Connection request" dialog. Returns null if already allowed.
        val intent = VpnService.prepare(this)
        if (intent != null) startActivityForResult(intent, REQ_VPN) else goNext()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_VPN) {
            if (resultCode == RESULT_OK) goNext()
            else Toast.makeText(this, "The VPN permission is needed to block harmful sites.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showWhyFiltering() {
        AlertDialog.Builder(this)
            .setTitle("Why on-device filtering?")
            .setMessage(
                "The filter runs on this phone and only compares each website's name with a block list. " +
                        "The list of sites you visit is never sent anywhere, and only the number of blocked " +
                        "sites is counted."
            )
            .setPositiveButton("Got it", null)
            .show()
    }

    // ---------- Step 2 ----------

    private fun stepTwoCard(): LinearLayout {
        val ctx = this
        val dp = { v: Int -> UiKit.dp(ctx, v) }

        val head = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.text(ctx, "SafeSphere Intercept", 12f, UiKit.INK, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            })
            addView(UiKit.text(ctx, "Just now", 11f, UiKit.MUTED))
        }
        fun bar(widthPercent: Float) = View(ctx).apply {
            background = UiKit.rounded(Color.parseColor("#E2E8F0"), dp(4).toFloat())
            layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, dp(8)).apply { topMargin = dp(8) }
            alpha = widthPercent
        }
        val mock = UiKit.vertical(ctx).apply {
            background = UiKit.rounded(Color.parseColor("#F0F3FF"), dp(14).toFloat())
            setPadding(dp(12), dp(12), dp(12), dp(12))
            addView(head)
            addView(UiKit.pill(ctx, "⚠ Sensitive text warning", UiKit.RED, UiKit.RED_SOFT, 11f).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.WRAP, UiKit.WRAP).apply { topMargin = dp(10) }
            })
            addView(bar(1f))
            addView(bar(0.7f))
            addView(UiKit.text(
                ctx, "Only you can see this alert. Parents are never sent your texts.", 11f, UiKit.GREEN_DARK
            ).apply { setPadding(0, dp(10), 0, 0) })
        }

        return UiKit.card(ctx).apply {
            addView(UiKit.text(ctx, "We need this to warn you about risky messages", 18f, UiKit.INK, bold = true))
            addView(UiKit.text(
                ctx,
                "We scan incoming notification previews on your phone to give you an early heads-up about " +
                        "adult or coercive language. We never read full chats, store conversations, or alert " +
                        "parents to message text.",
                13f, UiKit.MUTED
            ).also { UiKit.topMargin(ctx, it, 8) })
            addView(mock.also { UiKit.topMargin(ctx, it, 14) })
            listOf(
                "On-device processing only — zero cloud chat logs",
                "Mutual transparency: you always see what triggers an alert",
                "Easily pause or adjust sensitivity anytime"
            ).forEach { addView(Widgets.featureRow(ctx, it, "").also { r -> UiKit.topMargin(ctx, r, 10) }) }
        }
    }

    private fun openNotificationSettings() {
        openedListenerSettings = true
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun listenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: return false
        return flat.contains(packageName)
    }

    override fun onResume() {
        super.onResume()
        if (step == 2 && openedListenerSettings) {
            openedListenerSettings = false
            if (listenerEnabled()) {
                goNext()
            } else {
                Toast.makeText(this, "Notification access isn't turned on yet.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showZeroSurveillance() {
        AlertDialog.Builder(this)
            .setTitle("Zero-Surveillance Guarantee")
            .setMessage(
                "SafeSphere works as a protective layer directly inside your device. " +
                        "Text analysis runs in temporary memory.\n\n" +
                        "Local Ephemeral AI: keywords and phrasing patterns never touch external internet servers.\n\n" +
                        "No Guardian Chat Spy: parents only receive aggregate wellness metrics, never conversation transcripts."
            )
            .setPositiveButton("Understood", null)
            .show()
    }

    // ---------- next ----------

    private fun goNext() {
        if (step == 1) {
            startActivity(
                Intent(this, PermissionStepActivity::class.java).putExtra(EXTRA_STEP, 2)
            )
        } else {
            startActivity(Intent(this, SetPinActivity::class.java))
        }
    }
}