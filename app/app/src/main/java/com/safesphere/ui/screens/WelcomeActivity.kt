package com.safesphere.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.safesphere.ui.shield.AnimatedShieldView
import com.safesphere.ui.shield.ShieldState

/**
 * First screen: how SafeSphere works + Mutual Agreement.
 * "Continue" unlocks only when both the parent and the teen have ticked their box.
 * If setup was already finished, this screen jumps straight to Home.
 */
class WelcomeActivity : Activity() {

    private var parentOk = false
    private var teenOk = false
    private lateinit var counter: TextView
    private lateinit var continueButton: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AppPrefs.isSetupDone(this)) {
            val home = if (AppPrefs.getRole(this) == AppPrefs.ROLE_PARENT) {
                FamilyActivity::class.java
            } else {
                HomeActivity::class.java
            }
            startActivity(Intent(this, home))
            finish()
            return
        }
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        // ---- hero ----
        val shield = AnimatedShieldView(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72)).apply { gravity = Gravity.CENTER_HORIZONTAL }
            setState(ShieldState.ON)
        }
        val name = UiKit.text(ctx, "SafeSphere", 28f, UiKit.INK, bold = true).apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, name, 12)
        val tagline = UiKit.text(ctx, "Protection they can see through.", 14f, UiKit.MUTED)
            .apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, tagline, 4)
        val badge = UiKit.pill(ctx, "● OPEN & TRANSPARENT SAFETY", UiKit.BLUE_DARK, UiKit.BLUE_SOFT, 11f).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.WRAP, UiKit.WRAP).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(12)
            }
        }

        // ---- how it works ----
        val howTitles = UiKit.vertical(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            addView(UiKit.text(ctx, "How it works", 18f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, "Designed for peace of mind, built on privacy", 12f, UiKit.MUTED))
        }
        val howCard = UiKit.card(this).apply {
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(howTitles)
                addView(Icons.circle(ctx, "eye", 32, UiKit.BLUE_SOFT, UiKit.BLUE_DARK))
            })
            val rows = listOf(
                "We block adult websites" to "Filters out harmful content seamlessly across every browser.",
                "We never read or store your messages" to "Direct chats, photos, and personal texts stay strictly private.",
                "Your parent only sees weekly counts, not content" to "High-level activity metrics foster trust without micro-monitoring."
            )
            rows.forEach { (t, d) ->
                addView(featureRow(t, d).also { UiKit.topMargin(ctx, it, 10) })
            }
            addView(
                UiKit.infoBox(
                    ctx, "🤝",
                    UiKit.text(ctx, "Both parties share identical dashboard visibility.", 12f, UiKit.INK)
                ).also { UiKit.topMargin(ctx, it, 10) }
            )
        }
        UiKit.topMargin(this, howCard, 20)

        // ---- mutual agreement ----
        counter = UiKit.pill(ctx, "0 of 2 confirmed", UiKit.BLUE_DARK, UiKit.BLUE_SOFT, 11f)
        val agreeHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.text(ctx, "MUTUAL AGREEMENT", 11f, UiKit.MUTED, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
            })
            addView(counter)
        }
        val agreeCard = UiKit.card(this).apply {
            addView(agreeHeader)
            addView(
                agreeRow("I'm the parent and I agree", "I support transparent safety boundaries.") {
                    parentOk = it
                    updateState()
                }.also { UiKit.topMargin(ctx, it, 12) }
            )
            addView(
                agreeRow("I'm the teen and I agree", "I understand my digital space remains respected.") {
                    teenOk = it
                    updateState()
                }.also { UiKit.topMargin(ctx, it, 10) }
            )
        }
        UiKit.topMargin(this, agreeCard, 16)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(24), dp(16), dp(16))
            addView(shield)
            addView(name)
            addView(tagline)
            addView(badge)
            addView(howCard)
            addView(agreeCard)
        }

        // ---- bottom: Continue ----
        continueButton = UiKit.button(this, "Continue  →", filled = true) {
            if (parentOk && teenOk) {
                startActivity(
                    Intent(ctx, TransparencyActivity::class.java)
                        .putExtra(TransparencyActivity.EXTRA_ONBOARDING, true)
                )
            } else {
                Toast.makeText(ctx, "Please tick both boxes to continue", Toast.LENGTH_SHORT).show()
            }
        }
        val caption = UiKit.text(
            ctx, "You can review these consent settings anytime in Preferences.", 11f, UiKit.MUTED
        ).apply { gravity = Gravity.CENTER }
        UiKit.topMargin(ctx, caption, 8)
        val bottom = UiKit.vertical(this).apply {
            setBackgroundColor(Color.parseColor(UiKit.BG))
            setPadding(dp(16), dp(4), dp(16), dp(12))
            addView(continueButton)
            addView(caption)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
            addView(bottom)
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
        updateState()
    }

    private fun updateState() {
        val n = (if (parentOk) 1 else 0) + (if (teenOk) 1 else 0)
        counter.text = "$n of 2 confirmed"
        continueButton.alpha = if (n == 2) 1f else 0.45f
    }

    /** A light row with a green tick badge, a title and a description. */
    private fun featureRow(title: String, desc: String): LinearLayout {
        val ctx = this
        val texts = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = UiKit.dp(ctx, 12) }
            addView(UiKit.text(ctx, title, 14f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, desc, 12f, UiKit.MUTED).apply { setPadding(0, UiKit.dp(ctx, 2), 0, 0) })
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.parseColor("#F0F3FF"), UiKit.dp(ctx, 14).toFloat())
            setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12))
            addView(Icons.circle(ctx, "check", 28, UiKit.GREEN_SOFT, UiKit.GREEN_DARK))
            addView(texts)
        }
    }

    /** A tick-box row. Tapping anywhere on the row toggles it. */
    private fun agreeRow(title: String, sub: String, onChange: (Boolean) -> Unit): LinearLayout {
        val ctx = this
        var checked = false
        val tick = IconView(ctx, "check", "#FFFFFF").apply {
            layoutParams = FrameLayout.LayoutParams(UiKit.dp(ctx, 16), UiKit.dp(ctx, 16), Gravity.CENTER)
        }
        val box = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 26), UiKit.dp(ctx, 26))
            addView(tick)
        }
        fun paint() {
            if (checked) {
                tick.visibility = View.VISIBLE
                box.background = UiKit.rounded(Color.parseColor(UiKit.BLUE), UiKit.dp(ctx, 8).toFloat())
            } else {
                tick.visibility = View.INVISIBLE
                box.background = UiKit.rounded(
                    Color.WHITE, UiKit.dp(ctx, 8).toFloat(), Color.parseColor("#B6C2DA"), UiKit.dp(ctx, 2)
                )
            }
        }
        paint()
        val texts = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = UiKit.dp(ctx, 12) }
            addView(UiKit.text(ctx, title, 14f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, sub, 12f, UiKit.MUTED))
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.parseColor("#F0F3FF"), UiKit.dp(ctx, 12).toFloat())
            setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12))
            addView(box)
            addView(texts)
            setOnClickListener {
                checked = !checked
                paint()
                onChange(checked)
            }
        }
    }
}