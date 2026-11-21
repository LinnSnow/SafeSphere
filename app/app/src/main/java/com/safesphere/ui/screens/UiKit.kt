package com.safesphere.ui.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/** Shared look for every screen: colours, cards, buttons, top bar. Colours are from the Stitch designs. */
object UiKit {
    const val BLUE = "#3F6FE0"
    const val BLUE_DARK = "#1D55C6"
    const val BLUE_SOFT = "#E7EEFF"
    const val INK = "#141F2F"
    const val MUTED = "#434653"
    const val BG = "#F9F9FF"
    const val GREEN = "#2E9E6B"
    const val GREEN_DARK = "#006C44"
    const val GREEN_SOFT = "#C6FBDE"
    const val RED = "#BA1A1A"
    const val RED_SOFT = "#FFDAD6"

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

    /** Small rounded label such as "31 total blocked". */
    fun pill(ctx: Context, label: String, textColorHex: String, bgHex: String, sizeSp: Float = 12f): TextView =
        TextView(ctx).apply {
            text = label
            textSize = sizeSp
            setTextColor(Color.parseColor(textColorHex))
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(ctx, 10), dp(ctx, 4), dp(ctx, 10), dp(ctx, 4))
            background = rounded(Color.parseColor(bgHex), dp(ctx, 20).toFloat())
        }

    /**
     * Turns the old emoji strings into icon names, so older screens get real icons without edits.
     * You can also pass "i:name" (for example "i:shield") to pick an icon directly.
     */
    private fun iconFor(glyph: String): String? {
        val g = glyph.replace("\uFE0F", "").trim()
        if (g.startsWith("i:")) return g.substring(2)
        return when (g) {
            "←" -> "arrow-left"
            "⚙" -> "settings"
            "👁" -> "eye"
            "👤", "🙂" -> "user"
            "🔑" -> "key"
            "🤝" -> "users"
            "📱" -> "smartphone"
            "🛡" -> "shield"
            "🚫" -> "x"
            "🔒" -> "lock"
            "ℹ" -> "info"
            "⏸" -> "clock"
            "↗" -> "trend"
            "🔋" -> "battery"
            "✈" -> "wifi-off"
            "💬" -> "message"
            "⚠" -> "alert"
            "🔔" -> "bell"
            else -> null
        }
    }

    /** Round icon button, e.g. the settings gear. Emoji glyphs are drawn as icons. */
    fun roundIcon(
        ctx: Context, glyph: String, sizeDp: Int, bgHex: String, glyphHex: String, glyphSp: Float = 16f
    ): View {
        val name = iconFor(glyph)
        if (name != null) return Icons.circle(ctx, name, sizeDp, bgHex, glyphHex)
        return TextView(ctx).apply {
            text = glyph
            textSize = glyphSp
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(glyphHex))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor(bgHex))
            }
            layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp), dp(ctx, sizeDp))
        }
    }

    /** Soft box with a small icon on the left and any view as its body. */
    fun infoBox(ctx: Context, glyph: String, body: View, bgHex: String = "#F0F3FF"): LinearLayout =
        LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = rounded(Color.parseColor(bgHex), dp(ctx, 12).toFloat())
            setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12))
            val name = iconFor(glyph)
            if (name != null) {
                addView(Icons.icon(ctx, name, 20, BLUE_DARK))
            } else {
                addView(text(ctx, glyph, 18f, BLUE_DARK))
            }
            body.layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(ctx, 12) }
            addView(body)
        }

    /** The two small labels under the status: "On-device loop active" and "Zero-cloud snooping". */
    fun statusPills(ctx: Context): LinearLayout {
        fun weighted(p: LinearLayout, last: Boolean) = p.apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                if (!last) rightMargin = dp(ctx, 6)
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(weighted(Icons.pill(ctx, "", "● On-device loop active", GREEN_DARK, GREEN_SOFT, 11f), false))
            addView(weighted(Icons.pill(ctx, "lock", "Zero-cloud snooping", MUTED, "#E7EEFF", 11f), true))
        }
    }

    /** Top bar: logo, "SafeSphere" + subtitle, bell, profile. Optional long-press on the title (demo switch). */
    fun topBar(ctx: Context, subtitle: String, onTitleLongPress: (() -> Unit)? = null): LinearLayout {
        val logo = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36))
            background = rounded(Color.parseColor(BLUE_SOFT), dp(ctx, 10).toFloat())
            addView(IconView(ctx, "shield-check", BLUE_DARK).apply {
                layoutParams = FrameLayout.LayoutParams(dp(ctx, 20), dp(ctx, 20), Gravity.CENTER)
            })
        }
        val titles = vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f).apply { leftMargin = dp(ctx, 10) }
            addView(text(ctx, "SafeSphere", 18f, INK, bold = true))
            addView(text(ctx, subtitle, 12f, MUTED))
            if (onTitleLongPress != null) {
                setOnLongClickListener { onTitleLongPress(); true }
            }
        }
        val bell = Icons.icon(ctx, "bell", 22, "#737685").apply {
            layoutParams = LinearLayout.LayoutParams(dp(ctx, 22), dp(ctx, 22)).apply {
                leftMargin = dp(ctx, 8)
                rightMargin = dp(ctx, 14)
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(ctx, 16), dp(ctx, 8), dp(ctx, 16), dp(ctx, 8))
            addView(logo)
            addView(titles)
            addView(bell)
            addView(Icons.circle(ctx, "user", 36, "#6056F0", "#FFFFFF"))
        }
    }

    /** Keeps the content clear of the status bar and the phone's gesture bar. */
    @Suppress("DEPRECATION")
    fun padForSystemBars(root: View) {
        root.setOnApplyWindowInsetsListener { v, insets ->
            v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            insets
        }
        root.requestApplyInsets()
    }
}