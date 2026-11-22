package com.safesphere.ui.screens

import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView

/**
 * Text Warning: opened when the on-device model flags an incoming message.
 * The message text arrives only as an extra from the notification, is shown on this screen only
 * if the teen taps "View anyway", and is never saved or sent anywhere.
 */
class TextWarningActivity : Activity() {

    companion object {
        const val EXTRA_MESSAGE = "message"
        private const val WARNING_NOTIFICATION_ID = 4201
    }

    private var revealed = false
    private var helpShown = false
    private var message = ""

    private lateinit var contentBox: LinearLayout
    private lateinit var buttons: LinearLayout
    private lateinit var helpHolder: LinearLayout
    private lateinit var scroll: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        message = intent.getStringExtra(EXTRA_MESSAGE)
            ?: "Hey! This is a sample message so you can preview how this screen works."

        // ---- header ----
        val back = Icons.circle(ctx, "arrow-left", 40, Pal.CONTAINER, Pal.INK).apply {
            setOnClickListener { finish() }
        }
        val title = UiKit.text(ctx, "Text Warning", 19f, Pal.INK, bold = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(back)
            addView(title)
            addView(Icons.circle(ctx, "user", 36, Pal.ACCENT, "#FFFFFF"))
        }

        // ---- status row ----
        val statusRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.pill(ctx, "", "● Protection Active • On-Device AI", Pal.GREEN_DARK, Pal.GREEN_SOFT, 11f))
            addView(View(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, 1, 1f) })
            addView(Icons.circle(ctx, "shield-check", 32, Pal.CONTAINER, Pal.PRIMARY_DARK))
        }
        UiKit.topMargin(ctx, statusRow, 14)

        // ---- main card ----
        val source = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.circle(ctx, "shield", 36, Pal.PRIMARY_DARK, "#FFFFFF"))
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(10) }
                addView(UiKit.text(ctx, "SafeSphere Intercept", 14f, Pal.INK, bold = true))
                addView(UiKit.text(ctx, "Incoming text • Just now", 11f, Pal.MUTED))
            })
            addView(UiKit.pill(ctx, "Message Preview", Pal.MUTED, Pal.CONTAINER, 11f))
        }

        contentBox = UiKit.vertical(ctx).apply {
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), dp(14).toFloat())
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }

        val privacy = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), dp(14).toFloat())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            addView(Icons.icon(ctx, "lock", 20, Pal.PRIMARY_DARK).apply {
                (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(12)
            })
            addView(UiKit.vertical(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
                addView(UiKit.text(ctx, "Your Privacy Guarantee", 13f, Pal.INK, bold = true))
                addView(UiKit.text(
                    ctx,
                    "Only you can see this warning. The actual text or media is never uploaded to external " +
                            "servers, and is not shared with your guardians.",
                    12f, Pal.MUTED
                ).apply { setPadding(0, dp(2), 0, 0) })
            })
        }

        val mainCard = UiKit.card(ctx).apply {
            addView(source)
            addView(Icons.pill(ctx, "alert", "Sensitive Content Detected", Pal.RED, Pal.RED_SOFT, 11f)
                .also { (it.layoutParams as LinearLayout.LayoutParams).topMargin = dp(14) })
            addView(UiKit.text(ctx, "SafeSphere warning: adult content flag", 20f, Pal.INK, bold = true)
                .also { UiKit.topMargin(ctx, it, 12) })
            addView(UiKit.text(
                ctx,
                "We paused this preview to give you control before it opens on your screen. " +
                        "You get to decide how to handle it.",
                14f, Pal.MUTED
            ).also { UiKit.topMargin(ctx, it, 6) })
            addView(contentBox.also { UiKit.topMargin(ctx, it, 14) })
            addView(privacy.also { UiKit.topMargin(ctx, it, 12) })
        }
        UiKit.topMargin(ctx, mainCard, 14)

        buttons = UiKit.vertical(ctx)
        helpHolder = UiKit.vertical(ctx)

        // ---- how mutual shield works ----
        val how = UiKit.vertical(ctx).apply {
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), dp(16).toFloat())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(Icons.icon(ctx, "shield-check", 20, Pal.PRIMARY_DARK).apply {
                    (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(8)
                })
                addView(UiKit.text(ctx, "How Mutual Shield Works", 14f, Pal.INK, bold = true))
            })
            addView(UiKit.text(
                ctx,
                "SafeSphere protects your screen in real time using local AI processing. Your parent's " +
                        "dashboard only sees that \"Shield filters were active today\", keeping your personal " +
                        "conversations completely confidential.",
                12f, Pal.MUTED
            ).also { UiKit.topMargin(ctx, it, 8) })
        }
        UiKit.topMargin(ctx, how, 16)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(header)
            addView(statusRow)
            addView(mainCard)
            addView(buttons)
            addView(helpHolder)
            addView(how)
        }

        scroll = ScrollView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
            addView(column)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Pal.BG))
            addView(scroll)
        }
        UiKit.padForSystemBars(root)
        setContentView(root)

        renderContent()
        renderButtons()
    }

    /** The grey box: hidden message, or the message itself after "View anyway". */
    private fun renderContent() {
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)
        contentBox.removeAllViews()
        contentBox.addView(LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.circle(ctx, "user", 28, Pal.HIGH, Pal.MUTED))
            addView(UiKit.text(ctx, "Sender hidden", 13f, Pal.INK, bold = true).apply {
                setPadding(dp(10), 0, 0, 0)
            })
        })
        if (revealed) {
            contentBox.addView(UiKit.text(ctx, message, 15f, Pal.INK).apply {
                background = UiKit.rounded(Color.WHITE, dp(12).toFloat())
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }.also { UiKit.topMargin(ctx, it, 12) })
        } else {
            contentBox.addView(UiKit.vertical(ctx).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                background = UiKit.rounded(Color.WHITE, dp(12).toFloat())
                setPadding(dp(12), dp(18), dp(12), dp(18))
                addView(Icons.icon(ctx, "eye-off", 28, Pal.OUTLINE))
                addView(UiKit.text(ctx, "Content hidden for your safety", 13f, Pal.INK, bold = true).apply {
                    gravity = Gravity.CENTER
                    setPadding(0, dp(8), 0, 0)
                })
                addView(UiKit.text(
                    ctx, "Tap \"View anyway\" below only if you feel safe", 12f, Pal.MUTED
                ).apply { gravity = Gravity.CENTER })
            }.also { UiKit.topMargin(ctx, it, 12) })
        }
    }

    private fun renderButtons() {
        val ctx = this
        buttons.removeAllViews()

        buttons.addView(Icons.softButton(ctx, "shield-check", "Don't open", Pal.PRIMARY, "#FFFFFF") {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(WARNING_NOTIFICATION_ID)
            finish()
        })

        val toggle = Icons.softButton(
            ctx,
            if (revealed) "eye-off" else "eye",
            if (revealed) "Hide message" else "View anyway",
            "#FFFFFF", Pal.INK
        ) {
            revealed = !revealed
            renderContent()
            renderButtons()
        }
        toggle.elevation = UiKit.dp(ctx, 2).toFloat()
        buttons.addView(toggle)

        buttons.addView(Icons.softButton(ctx, "life-buoy", "I need help with this", Pal.CONTAINER, Pal.PRIMARY_DARK) {
            helpShown = true
            renderHelp()
            scroll.post { scroll.smoothScrollBy(0, UiKit.dp(ctx, 320)) }
        })
    }

    /** The "You're in safe hands" card. */
    private fun renderHelp() {
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)
        helpHolder.removeAllViews()
        if (!helpShown) return

        fun line(text: String) = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Icons.circle(ctx, "check", 22, Pal.GREEN_SOFT, Pal.GREEN_DARK))
            addView(UiKit.text(ctx, text, 13f, Pal.INK).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(10) }
            })
        }

        val card = UiKit.card(ctx).apply {
            background = UiKit.rounded(
                Color.parseColor(Pal.GREEN_TINT), dp(16).toFloat(), Color.parseColor("#B7EBCB"), dp(1)
            )
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(Icons.circle(ctx, "heart", 44, "#FFFFFF", Pal.GREEN_DARK))
                addView(UiKit.text(ctx, "You're in safe hands", 18f, Pal.INK, bold = true).apply {
                    setPadding(dp(12), 0, 0, 0)
                })
            })
            addView(UiKit.text(
                ctx,
                "It's okay to feel uncomfortable. You haven't done anything wrong, and you don't have to deal with this alone.",
                13f, Pal.MUTED
            ).also { UiKit.topMargin(ctx, it, 10) })
            addView(line("Talk to a trusted adult: a parent, relative, teacher or school counsellor.")
                .also { UiKit.topMargin(ctx, it, 12) })
            addView(line("Don't reply or forward it. You can block and report the sender in the app it came from.")
                .also { UiKit.topMargin(ctx, it, 10) })
            addView(line("SafeSphere has not shared this message with anyone, including your parent.")
                .also { UiKit.topMargin(ctx, it, 10) })
            // CHILDLINE India is free and open 24x7. Change this number if your region is different.
            addView(Icons.softButton(ctx, "phone", "Call CHILDLINE 1098 (free, 24x7)", Pal.GREEN_SOFT, Pal.GREEN_DARK) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:1098")))
            })
        }
        UiKit.topMargin(ctx, card, 16)
        helpHolder.addView(card)
    }
}