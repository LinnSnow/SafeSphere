package com.safesphere.ui.screens

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.Toast

/** The four bottom-bar tabs. */
enum class Tab(val icon: String, val label: String) {
    SHIELD("🛡", "Shield"),
    SHARED("👁", "Shared"),
    LIMITS("☑", "Limits"),
    FAMILY("👥", "Family")
}

/** Bottom bar + opening the right screen for each tab. */
object Nav {

    fun bar(activity: Activity, selected: Tab): LinearLayout {
        val ctx = activity
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.WHITE)
            elevation = UiKit.dp(ctx, 8).toFloat()
            setPadding(0, UiKit.dp(ctx, 8), 0, UiKit.dp(ctx, 8))
            Tab.values().forEach { tab ->
                val active = tab == selected
                val color = if (active) UiKit.BLUE else UiKit.MUTED
                addView(UiKit.vertical(ctx).apply {
                    gravity = Gravity.CENTER_HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f)
                    setPadding(0, UiKit.dp(ctx, 4), 0, UiKit.dp(ctx, 4))
                    addView(UiKit.text(ctx, tab.icon, 20f, color).apply {
                        gravity = Gravity.CENTER
                        alpha = if (active) 1f else 0.5f
                    })
                    addView(UiKit.text(ctx, tab.label, 12f, color, bold = active).apply {
                        gravity = Gravity.CENTER
                    })
                    setOnClickListener { open(activity, tab) }
                })
            }
        }
    }

    fun open(from: Activity, tab: Tab) {
        val target: Class<*>? = when (tab) {
            Tab.SHIELD -> HomeActivity::class.java
            Tab.SHARED -> TransparencyActivity::class.java
            Tab.FAMILY -> FamilyActivity::class.java
            Tab.LIMITS -> null   // TODO: Limits / permissions screen
        }
        if (target == null) {
            Toast.makeText(from, "Limits: coming soon", Toast.LENGTH_SHORT).show()
            return
        }
        if (from.javaClass == target) return
        from.startActivity(
            Intent(from, target).addFlags(
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NO_ANIMATION
            )
        )
    }
}