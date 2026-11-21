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
 * Opened during first-time setup (EXTRA_ONBOARDING): shows a Continue button to the role screen.
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
        val back = Icons.circle(ctx, "arrow-left", 40, Pal.CONTAINER, Pal.INK).apply {
            setOnClickListener { finish() }
        }
        val titles = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(12) }
            addView(UiKit.text(ctx, "Transparency & Privacy", 19f, Pal.INK, bold = true))
            addView(UiKit.text(ctx, "Shared Trust Overview", 12f, Pal.MUTED))
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(back)
            addView(titles)
            addView(Icons.pill(ctx, "shield-check", "Active Shield", Pal.GREEN_DARK, Pal.GREEN_SOFT))
        }

        // ---- Teen View / Guardian View (visual toggle) ----
        val teen = UiKit.text(ctx, "Teen View", 13f, Pal.PRIMARY_DARK, bold = true)
        val guardian = UiKit.text(ctx, "Guardian View", 13f, Pal.MUTED, bold = true)
        fun style(tv: TextView, on: Boolean) {
            tv.gravity = Gravity.CENTER
            tv.setTextColor(Color.parseColor(if (on) Pal.PRIMARY_DARK else Pal.MUTED))
            tv.background = if (on) UiKit.rounded(Color.WHITE, dp(18).toFloat()) else null
            tv.layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
        }
        style(teen, true)
        style(guardian, false)
        teen.setOnClickListener { style(teen, true); style(guardian, false) }
        guardian.setOnClickListener { style(teen, false); style(guardian, true) }
        val toggle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = UiKit.rounded(Color.parseColor(Pal.CONTAINER), dp(22).toFloat())
            setPadding(dp(4), dp(4), dp(4), dp(4))
            addView(teen)
            addView(guardian)
        }
        UiKit.topMargin(this, toggle, 16)

        // ---- intro card ----
        val pledge = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(Icons.icon(ctx, "shield-check", 16, Pal.GREEN_DARK).apply {
                (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(6)
            })
            addView(UiKit.text(ctx, "Shared accountability pledge", 12f, Pal.INK, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            })
            addView(UiKit.text(ctx, "100% Auditable", 12f, Pal.PRIMARY_DARK, bold = true))
        }
        val introCard = UiKit.card(this).apply {
            addView(Icons.circle(ctx, "eye", 44, Pal.CONTAINER, Pal.PRIMARY_DARK))
            addView(UiKit.text(ctx, "Protection they can see through", 20f, Pal.INK, bold = true)
                .also { UiKit.topMargin(ctx, it, 12) })
            addView(UiKit.text(
                ctx, "Complete mutual clarity. Zero stealth tracking, secret keystrokes, or hidden monitors.",
                14f, Pal.MUTED
            ).also { UiKit.topMargin(ctx, it, 6) })
            addView(pledge.also { UiKit.topMargin(ctx, it, 14) })
        }
        UiKit.topMargin(this, introCard, 16)

        // ---- what we check ----
        val checkCard = UiKit.card(this).apply {
            addView(sectionHeader("What we check", "Safe Boundary"))
            listOf(
                "Website names" to "To filter explicit or dangerous domains automatically.",
                "Message previews" to "On-device algorithm triggers gentle alerts for harsh language.",
                "New incoming photos" to "Private local scan flags adult imagery without transmitting files."
            ).forEach { (t, d) ->
                addView(row("check", Pal.GREEN_SOFT, Pal.GREEN_DARK, t, d).also { UiKit.topMargin(ctx, it, 10) })
            }
        }
        UiKit.topMargin(this, checkCard, 16)

        // ---- zero content storage ----
        val zeroTexts = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = dp(14) }
            addView(UiKit.text(ctx, "ZERO CONTENT STORAGE", 11f, Pal.PRIMARY_DARK, bold = true))
            addView(UiKit.text(
                ctx, "We never save your messages, photos, or browsing history. Only count tallies for safety summaries.",
                14f, Pal.INK, bold = true
            ).apply { setPadding(0, dp(4), 0, 0) })
        }
        val zeroBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.parseColor(Pal.CONTAINER), dp(16).toFloat())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            addView(Icons.circle(ctx, "lock", 44, "#FFFFFF", Pal.PRIMARY_DARK))
            addView(zeroTexts)
        }
        UiKit.topMargin(this, zeroBox, 16)

        // ---- live audit example ----
        val bad = UiKit.vertical(this).apply {
            background = UiKit.rounded(Color.parseColor("#FFEDEB"), dp(12).toFloat())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { rightMargin = dp(8) }
            addView(UiKit.text(ctx, "Unencrypted Data", 11f, Pal.RED, bold = true))
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(4))
                addView(Icons.icon(ctx, "x", 14, Pal.RED))
                addView(UiKit.text(ctx, "Actual Text", 13f, Pal.INK, bold = true).apply {
                    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    setPadding(dp(6), 0, 0, 0)
                })
            })
            addView(UiKit.text(ctx, "Discarded instantly", 11f, Pal.MUTED))
        }
        val good = UiKit.vertical(this).apply {
            background = UiKit.rounded(Color.parseColor(Pal.GREEN_TINT), dp(12).toFloat())
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            addView(UiKit.text(ctx, "Safe Metric Logged", 11f, Pal.GREEN_DARK, bold = true))
            addView(UiKit.text(ctx, "#  1 Alert Prevented", 13f, Pal.INK, bold = true).apply {
                setPadding(0, dp(6), 0, dp(4))
            })
            addView(UiKit.text(ctx, "Aggregated count", 11f, Pal.MUTED))
        }
        val auditCard = UiKit.card(this).apply {
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(UiKit.text(ctx, "Live Audit Example", 13f, Pal.INK, bold = true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
                })
                addView(Icons.icon(ctx, "settings", 14, Pal.PRIMARY_DARK).apply {
                    (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(4)
                })
                addView(UiKit.text(ctx, "On-device only", 11f, Pal.PRIMARY_DARK, bold = true))
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
                Triple("Your passwords & logins", "eye-off", false),
                Triple("What you type in other apps", "keyboard", false),
                Triple("Your call logs & voice memos", "phone", true),
                Triple("Your real-time GPS location", "map-pin", true)
            ).forEach { (t, rightIcon, slash) ->
                addView(
                    row("x", Pal.RED_SOFT, Pal.RED, t, "", rightIcon, slash)
                        .also { UiKit.topMargin(ctx, it, 10) }
                )
            }
        }
        UiKit.topMargin(this, neverCard, 16)

        // ---- one screen for both ----
        val swapCircle = Icons.circle(ctx, "swap", 40, "#FFFFFF", Pal.PRIMARY_DARK).apply {
            (layoutParams as LinearLayout.LayoutParams).gravity = Gravity.CENTER_HORIZONTAL
        }
        val both = UiKit.vertical(this).apply {
            gravity = Gravity.CENTER_HORIZONTAL
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), dp(16).toFloat())
            setPadding(dp(16), dp(16), dp(16), dp(16))
            addView(swapCircle)
            addView(UiKit.text(ctx, "One Screen for Both of You", 15f, Pal.INK, bold = true)
                .apply { gravity = Gravity.CENTER; setPadding(0, dp(8), 0, 0) })
            addView(UiKit.text(
                ctx, "Teens and parents share this exact same transparency console in real time. No secret tiers, no surprise filters.",
                12f, Pal.MUTED
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
                setBackgroundColor(Color.parseColor(Pal.BG))
                setPadding(dp(16), dp(4), dp(16), dp(12))
                addView(UiKit.button(ctx, "Continue  →", filled = true) {
                    startActivity(Intent(ctx, RoleActivity::class.java))
                })
            }
        } else {
            Nav.bar(this, Tab.SHARED)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Pal.BG))
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

    /** A light rounded row: coloured icon badge, title, optional description, optional icon on the right. */
    private fun row(
        iconName: String, bgHex: String, fgHex: String, title: String, desc: String,
        rightIcon: String? = null, rightSlash: Boolean = false
    ): LinearLayout {
        val ctx = this
        val texts = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = UiKit.dp(ctx, 12) }
            addView(UiKit.text(ctx, title, 14f, Pal.INK, bold = true))
            if (desc.isNotEmpty()) {
                addView(UiKit.text(ctx, desc, 12f, Pal.MUTED).apply { setPadding(0, UiKit.dp(ctx, 2), 0, 0) })
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.parseColor(Pal.SURFACE), UiKit.dp(ctx, 14).toFloat())
            setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12))
            addView(Icons.circle(ctx, iconName, 28, bgHex, fgHex))
            addView(texts)
            if (rightIcon != null) {
                addView(Icons.icon(ctx, rightIcon, 18, Pal.OUTLINE, rightSlash))
            }
        }
    }

    private fun sectionHeader(title: String, tag: String): LinearLayout {
        val ctx = this
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.text(ctx, title, 18f, Pal.INK, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            })
            addView(UiKit.pill(ctx, tag, Pal.MUTED, Pal.CONTAINER, 11f))
        }
    }
}