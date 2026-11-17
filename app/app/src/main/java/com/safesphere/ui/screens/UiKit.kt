package com.safesphere.ui.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

/** Small helpers so every screen looks the same (colours, cards, buttons). */
object UiKit {
    const val BLUE = "#3F6FE0"
    const val INK = "#212121"
    const val MUTED = "#757575"
    const val BG = "#F4F6FB"
    const val GREEN = "#2E9E6B"
    const val RED = "#C62828"

    const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun rounded(fill: Int, radiusPx: Float, strokeColor: Int? = null, strokePx: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radiusPx
            if (strokeColor != null) setStroke(strokePx, strokeColor)
        }

    fun text(ctx: Context, s: String, sizeSp: Float, colorHex: String = INK, bold: Boolean = false): TextView =
        TextView(ctx).apply {
            text = s
            textSize = sizeSp
            setTextColor(Color.parseColor(colorHex))
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

    fun vertical(ctx: Context): LinearLayout =
        LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }

    /** White rounded card with a thin border. */
    fun card(ctx: Context): LinearLayout = vertical(ctx).apply {
        background = rounded(Color.WHITE, dp(ctx, 16).toFloat(), Color.parseColor("#E3E7F0"), dp(ctx, 1))
        setPadding(dp(ctx, 16), dp(ctx, 16), dp(ctx, 16), dp(ctx, 16))
        elevation = dp(ctx, 2).toFloat()
    }

    /** Full-width pill button. filled = solid colour, otherwise outlined. */
    fun button(
        ctx: Context,
        label: String,
        filled: Boolean,
        colorHex: String = BLUE,
        onClick: () -> Unit
    ): TextView = TextView(ctx).apply {
        text = label
        textSize = 16f
        gravity = Gravity.CENTER
        setTypeface(typeface, Typeface.BOLD)
        val c = Color.parseColor(colorHex)
        if (filled) {
            setTextColor(Color.WHITE)
            background = rounded(c, dp(ctx, 26).toFloat())
        } else {
            setTextColor(c)
            background = rounded(Color.WHITE, dp(ctx, 26).toFloat(), c, dp(ctx, 2))
        }
        layoutParams = LinearLayout.LayoutParams(MATCH, dp(ctx, 52)).apply { topMargin = dp(ctx, 12) }
        setOnClickListener { onClick() }
    }

    /** One line like "✔  We block adult websites". */
    fun checkLine(ctx: Context, mark: String, markColorHex: String, label: String): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(ctx, 10) }
            addView(text(ctx, mark, 16f, markColorHex, bold = true).apply {
                layoutParams = LinearLayout.LayoutParams(dp(ctx, 28), WRAP)
            })
            addView(text(ctx, label, 15f).apply {
                layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
            })
        }

    /** Gives a view a top margin when it is added to a vertical LinearLayout. */
    fun topMargin(ctx: Context, view: View, marginDp: Int, heightPx: Int = WRAP): View {
        view.layoutParams = LinearLayout.LayoutParams(MATCH, heightPx).apply { topMargin = dp(ctx, marginDp) }
        return view
    }
}