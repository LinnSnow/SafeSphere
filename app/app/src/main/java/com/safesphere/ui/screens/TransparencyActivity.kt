package com.safesphere.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * "What does this app see?" in the Stitch design.
 * Opened from the Shared tab: shows the bottom bar.
 * Opened during first-time setup (EXTRA_ONBOARDING): shows a Continue button to the Pair screen.
 */
class TransparencyActivity : Activity() {

    companion object {
        const val EXTRA_ONBOARDING = "onboarding"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        val onboarding = intent.getBooleanExtra(EXTRA_ONBOARDING, false)
        fun dp(v: Int) = UiKit.dp(ctx, v)

        // ---- page header ----
        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }
        val titles = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(UiKit.text(ctx, "Transparency & Privacy", 19f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, "Shared Trust Overview", 12f, UiKit.MUTED))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(back)
            addView(titles)
            addView(UiKit.pill(ctx, "● Active Shield", UiKit.GREEN_DARK, UiKit.GREEN_SOFT, 11f))
        }

        // ---- Teen View / Guardian View (visual toggle) ----
        val teen = UiKit.text(ctx, "Teen View", 13f, UiKit.BLUE_DARK, bold = true)
        val guardian = UiKit.text(ctx, "Guardian View", 13f, UiKit.MUTED, bold = true)
        fun style(tv: TextView, on: Boolean) {
            tv.gravity = Gravity.CENTER
            tv.setTextColor(Color.parseColor(if (on) UiKit.BLUE_DARK else UiKit.MUTED))
            tv.background = if (on) UiKit.rounded(Color.WHITE, dp(18).toFloat()) else null
            tv.layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
        }
        style(teen, true)
        style(guardian, false)
        teen.setOnClickListener { style(teen, true); style(guardian, false) }
        guardian.setOnClickListener { style(teen, false); style(guardian, true) }
        val toggle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = UiKit.rounded(Color.parseColor("#E8EEFD"), dp(22).toFloat())
            setPadding(dp(4), dp(4), dp(4), dp(4))
            addView(teen)
            addView(guardian)
        }
        UiKit.topMargin(this, toggle, 16)

        // ---- intro card ----
        val pledge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.text(ctx, "🤝 Shared accountability pledge", 12f, UiKit.INK, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            })
            addView(UiKit.text(ctx, "100% Auditable", 12f, UiKit.BLUE_DARK, bold = true))
        }
        val introCard = UiKit.card(this).apply {
            addView(UiKit.roundIcon(ctx, "👁", 40, UiKit.BLUE_SOFT, UiKit.BLUE_DARK, 18f))
            addView(UiKit.text(ctx, "Protection they can see through", 20f, UiKit.INK, bold = true)
                .also { UiKit.topMargin(ctx, it, 12) })
            addView(UiKit.text(
                ctx, "Complete mutual clarity. Zero stealth tracking, secret keystrokes, or hidden monitors.",
                14f, UiKit.MUTED
            ).also { UiKit.topMargin(ctx, it, 6) })
            addView(pledge.also { UiKit.topMargin(ctx, it, 14) })
        }
        UiKit.topMargin(this, introCard, 16)

        // ---- what we check ----
        val checkCard = UiKit.card(this).apply {
            addView(sectionHeader("What we check", "Safe Boundary"))
            val items = listOf(
                "Website names" to "To filter explicit or dangerous domains automatically.",
                "Message previews" to "On-device algorithm triggers gentle alerts for harsh language.",
                "New incoming photos" to "Private local scan flags adult imagery without transmitting files."
            )
            items.forEach { (t, d) ->
                addView(Widgets.featureRow(ctx, t, d).also { UiKit.topMargin(ctx, it, 10) })
            }
        }
        UiKit.topMargin(this, checkCard, 16)

        // ---- zero content storage ----
        val zeroTexts = UiKit.vertical(this).apply {
            addView(UiKit.text(ctx, "ZERO CONTENT STORAGE", 11f, UiKit.BLUE_DARK, bold = true))
            addView(UiKit.text(
                ctx, "We never save your messages, photos, or browsing history. Only count tallies for safety summaries.",
                14f, UiKit.INK, bold = true
            ).apply { setPadding(0, dp(4), 0, 0) })
        }
        val zeroBox = UiKit.infoBox(ctx, "🔒", zeroTexts, "#E4EBFC")
        UiKit.topMargin(this, zeroBox, 16)

        // ---- live audit example ----
        val bad = UiKit.vertical(this).apply {
            background = UiKit.rounded(Color.parseColor("#FDECEA"), dp(12).toFloat())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { rightMargin = dp(8) }
            addView(UiKit.text(ctx, "Unencrypted Data", 11f, UiKit.RED, bold = true))
            addView(UiKit.text(ctx, "⊗ Actual Text", 13f, UiKit.INK, bold = true).apply {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setPadding(0, dp(4), 0, dp(2))
            })
            addView(UiKit.text(ctx, "Discarded instantly", 11f, UiKit.MUTED))
        }
        val good = UiKit.vertical(this).apply {
            background = UiKit.rounded(Color.parseColor("#E3F8EC"), dp(12).toFloat())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            addView(UiKit.text(ctx, "Safe Metric Logged", 11f, UiKit.GREEN_DARK, bold = true))
            addView(UiKit.text(ctx, "# 1 Alert Prevented", 13f, UiKit.INK, bold = true).apply {
                setPadding(0, dp(4), 0, dp(2))
            })
            addView(UiKit.text(ctx, "Aggregated count", 11f, UiKit.MUTED))
        }
        val auditCard = UiKit.card(this).apply {
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(UiKit.text(ctx, "Live Audit Example", 13f, UiKit.INK, bold = true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
                })
                addView(UiKit.text(ctx, "On-device only", 11f, UiKit.BLUE_DARK, bold = true))
            })
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(bad)
                addView(good)
            }.also { UiKit.topMargin(ctx, it, 12) })
        }
        UiKit.topMargin(this, auditCard, 16)

        // ---- what we never see ----
        val neverCard = UiKit.card(this).apply {
            addView(sectionHeader("What we never see", "Off-limits"))
            listOf(
                "Your passwords & logins",
                "What you type in other apps",
                "Your call logs & voice memos",
                "Your real-time GPS location"
            ).forEach { t ->
                addView(
                    Widgets.featureRow(ctx, t, "", "✕", UiKit.RED_SOFT, UiKit.RED)
                        .also { UiKit.topMargin(ctx, it, 10) }
                )
            }
        }
        UiKit.topMargin(this, neverCard, 16)

        // ---- one screen for both ----
        val both = UiKit.vertical(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            background = UiKit.rounded(Color.parseColor("#EEF2FE"), dp(16).toFloat())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            addView(UiKit.text(ctx, "⇄", 22f, UiKit.BLUE_DARK).apply { gravity = Gravity.CENTER })
            addView(UiKit.text(ctx, "One Screen for Both of You", 15f, UiKit.INK, bold = true)
                .apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) })
            addView(UiKit.text(
                ctx, "Teens and parents share this exact same transparency console in real time. No secret tiers, no surprise filters.",
                12f, UiKit.MUTED
            ).apply { gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) })
        }
        UiKit.topMargin(this, both, 16)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(header)
            addView(toggle)
            addView(introCard)
            addView(checkCard)
            addView(zeroBox)
            addView(auditCard)
            addView(neverCard)
            addView(both)
        }

        val footer = if (onboarding) {
            UiKit.vertical(this).apply {
                setBackgroundColor(Color.parseColor(UiKit.BG))
                setPadding(dp(16), dp(4), dp(16), dp(12))
                addView(UiKit.button(ctx, "Continue  →", filled = true) {
                    startActivity(Intent(ctx, PairActivity::class.java))
                })
            }
        } else {
            Nav.bar(this, Tab.SHARED)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(UiKit.topBar(ctx, "Shared View"))
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
            addView(footer)
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
    }

    private fun sectionHeader(title: String, tag: String): LinearLayout {
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.text(ctx, title, 18f, UiKit.INK, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            })
            addView(UiKit.pill(ctx, tag, UiKit.MUTED, "#EEF1F6", 11f))
        }
    }
}