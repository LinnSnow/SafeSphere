package com.safesphere.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView

/** "Who is using this phone?"  Teen -> shows the code.  Parent -> enters the code. */
class RoleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ctx = this
        fun dp(v: Int) = UiKit.dp(ctx, v)

        val back = UiKit.roundIcon(ctx, "←", 40, "#E8EEFD", UiKit.INK, 18f).apply {
            setOnClickListener { finish() }
        }
        val title = UiKit.text(ctx, "Who is using this phone?", 24f, UiKit.INK, bold = true)
        UiKit.topMargin(ctx, title, 20)
        val subtitle = UiKit.text(
            ctx,
            "SafeSphere runs on two phones. The teen's phone shows a 6-digit code, and the parent types it in on theirs.",
            14f, UiKit.MUTED
        )
        UiKit.topMargin(ctx, subtitle, 8)

        val teenCard = roleCard("🙂", "I'm the teen", "This phone will show a pairing code") {
            AppPrefs.setRole(ctx, AppPrefs.ROLE_TEEN)
            startActivity(Intent(ctx, TeenCodeActivity::class.java))
        }
        UiKit.topMargin(ctx, teenCard, 24)
        val parentCard = roleCard("👤", "I'm the parent", "Enter the teen's code to link the phones") {
            AppPrefs.setRole(ctx, AppPrefs.ROLE_PARENT)
            startActivity(Intent(ctx, PairActivity::class.java))
        }
        UiKit.topMargin(ctx, parentCard, 14)

        val column = UiKit.vertical(this).apply {
            setPadding(dp(16), dp(8), dp(16), dp(24))
            addView(back)
            addView(title)
            addView(subtitle)
            addView(teenCard)
            addView(parentCard)
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(UiKit.BG))
            addView(ScrollView(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, 0, 1f)
                addView(column)
            })
        }
        UiKit.padForSystemBars(root)
        setContentView(root)
    }

    private fun roleCard(icon: String, title: String, sub: String, onClick: () -> Unit): LinearLayout {
        val ctx = this
        val texts = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = UiKit.dp(ctx, 14) }
            addView(UiKit.text(ctx, title, 18f, UiKit.INK, bold = true))
            addView(UiKit.text(ctx, sub, 13f, UiKit.MUTED))
        }
        return UiKit.card(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(UiKit.roundIcon(ctx, icon, 52, UiKit.BLUE_SOFT, UiKit.BLUE_DARK, 22f))
            addView(texts)
            addView(UiKit.text(ctx, "›", 26f, UiKit.MUTED))
            setOnClickListener { onClick() }
        }
    }
}