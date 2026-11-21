package com.safesphere.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.graphics.PathParser
import kotlin.math.cos
import kotlin.math.sin

/** Exact colours sampled from the Stitch designs. */
object Pal {
    const val BG = "#F9F9FF"
    const val SURFACE = "#F0F3FF"
    const val CONTAINER = "#E7EEFF"
    const val HIGH = "#DEE8FF"
    const val PRIMARY = "#3F6FE0"
    const val PRIMARY_DARK = "#1D55C6"
    const val ACCENT = "#6056F0"
    const val INK = "#141F2F"
    const val MUTED = "#434653"
    const val OUTLINE = "#737685"
    const val GREEN_DARK = "#006C44"
    const val GREEN_SOFT = "#C6FBDE"
    const val GREEN_TINT = "#DDFDEB"
    const val RED = "#BA1A1A"
    const val RED_SOFT = "#FFDAD6"
    const val RED_TINT = "#FBF0F3"
    const val BORDER = "#E3E7F0"
}

/** One line icon drawn on a 24x24 grid. Use Icons.icon(...) to create one. */
class IconView(
    ctx: Context,
    name: String,
    colorHex: String,
    slash: Boolean = false
) : View(ctx) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor(colorHex)
    }
    private val path: Path? = Icons.path(name)
    private val slashPath: Path? = if (slash) Icons.path("slash") else null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val s = minOf(width, height) / 24f
        canvas.save()
        canvas.translate((width - 24f * s) / 2f, (height - 24f * s) / 2f)
        canvas.scale(s, s)
        path?.let { canvas.drawPath(it, paint) }
        slashPath?.let { canvas.drawPath(it, paint) }
        canvas.restore()
    }
}

object Icons {

    private val data = mapOf(
        "shield" to "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z",
        "shield-check" to "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z M9 12l2 2 4-4",
        "check" to "M20 6L9 17l-5-5",
        "x" to "M18 6L6 18M6 6l12 12",
        "lock" to "M5 11h14a2 2 0 0 1 2 2v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-7a2 2 0 0 1 2-2z M7 11V7a5 5 0 0 1 10 0v4",
        "eye" to "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z",
        "eye-off" to "M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94 M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19 M14.12 14.12a3 3 0 1 1-4.24-4.24 M1 1l22 22",
        "bell" to "M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9 M13.73 21a2 2 0 0 1-3.46 0",
        "user" to "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2 M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0z",
        "users" to "M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M9 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8z M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75",
        "arrow-left" to "M19 12H5 M12 19l-7-7 7-7",
        "arrow-right" to "M5 12h14 M12 5l7 7-7 7",
        "check-square" to "M9 11l3 3L22 4 M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11",
        "phone" to "M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z",
        "smartphone" to "M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z M12 18h.01",
        "message" to "M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z",
        "image" to "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z M10.5 8.5a1.5 1.5 0 1 1-3 0 1.5 1.5 0 0 1 3 0z M21 15l-5-5L5 21",
        "globe" to "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M2 12h20 M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z",
        "wifi-off" to "M1 1l22 22 M16.72 11.06A10.94 10.94 0 0 1 19 12.55 M5 12.55a10.94 10.94 0 0 1 5.17-2.39 M10.71 5.05A16 16 0 0 1 22.58 9 M1.42 9a15.91 15.91 0 0 1 4.7-2.88 M8.53 16.11a6 6 0 0 1 6.95 0 M12 20h.01",
        "battery" to "M3 7h15a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2z M23 11v2 M5 11v2",
        "clock" to "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M12 6v6l4 2",
        "hourglass" to "M6 2h12 M6 22h12 M7 2v4.5l5 5.5-5 5.5V22 M17 2v4.5L12 12l5 5.5V22",
        "info" to "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M12 16v-4 M12 8h.01",
        "alert" to "M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h.01",
        "heart" to "M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z",
        "swap" to "M17 1l4 4-4 4 M3 11V9a4 4 0 0 1 4-4h14 M7 23l-4-4 4-4 M21 13v2a4 4 0 0 1-4 4H3",
        "key" to "M21 2l-2 2 M15.5 7.5l3 3L22 7l-3-3 M11.39 11.61a5.5 5.5 0 1 1-7.78 7.78 5.5 5.5 0 0 1 7.78-7.78z M11.39 11.61L15.5 7.5",
        "life-buoy" to "M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8z M4.93 4.93l4.24 4.24 M14.83 9.17l4.24-4.24 M14.83 14.83l4.24 4.24 M9.17 14.83l-4.24 4.24",
        "trend" to "M23 6l-9.5 9.5-5-5L1 18 M17 6h6v6",
        "map-pin" to "M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z M15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0z",
        "keyboard" to "M4 5h16a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z M6 9h.01 M10 9h.01 M14 9h.01 M18 9h.01 M7 15h10",
        "mail" to "M4 4h16a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z M22 6l-10 7L2 6",
        "power" to "M18.36 6.64a9 9 0 1 1-12.73 0 M12 2v10",
        "slash" to "M3 3l18 18",
        "delete" to "M21 4H8l-7 8 7 8h13a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z M18 9l-6 6 M12 9l6 6",
        "chevron-right" to "M9 18l6-6-6-6"
    )

    fun path(name: String): Path? {
        if (name == "settings") return gear()
        val d = data[name] ?: return null
        return try {
            PathParser.createPathFromPathData(d)
        } catch (e: Exception) {
            null
        }
    }

    /** A simple gear: centre circle, outer ring and 8 teeth. */
    private fun gear(): Path {
        val p = Path()
        p.addCircle(12f, 12f, 3.2f, Path.Direction.CW)
        p.addCircle(12f, 12f, 7.2f, Path.Direction.CW)
        for (i in 0 until 8) {
            val a = Math.toRadians(i * 45.0)
            val c = cos(a).toFloat()
            val s = sin(a).toFloat()
            p.moveTo(12f + 7.2f * c, 12f + 7.2f * s)
            p.lineTo(12f + 10.2f * c, 12f + 10.2f * s)
        }
        return p
    }

    private fun dp(ctx: Context, v: Int) = UiKit.dp(ctx, v)

    /** Just the icon, sizeDp x sizeDp. Add it to a LinearLayout. */
    fun icon(ctx: Context, name: String, sizeDp: Int, colorHex: String, slash: Boolean = false): IconView =
        IconView(ctx, name, colorHex, slash).apply {
            layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp), dp(ctx, sizeDp))
        }

    /** Round coloured badge with an icon in the middle. Add it to a LinearLayout. */
    fun circle(
        ctx: Context, name: String, sizeDp: Int, bgHex: String, fgHex: String, slash: Boolean = false
    ): FrameLayout {
        val box = FrameLayout(ctx)
        box.layoutParams = LinearLayout.LayoutParams(dp(ctx, sizeDp), dp(ctx, sizeDp))
        box.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor(bgHex))
        }
        val inner = dp(ctx, (sizeDp * 0.52f).toInt())
        box.addView(IconView(ctx, name, fgHex, slash).apply {
            layoutParams = FrameLayout.LayoutParams(inner, inner, Gravity.CENTER)
        })
        return box
    }

    /** Small rounded tag with an optional icon, e.g. "Active Shield". */
    fun pill(
        ctx: Context, iconName: String, label: String, fgHex: String, bgHex: String, sizeSp: Float = 11f
    ): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = UiKit.rounded(Color.parseColor(bgHex), dp(ctx, 20).toFloat())
        setPadding(dp(ctx, 10), dp(ctx, 5), dp(ctx, 12), dp(ctx, 5))
        layoutParams = LinearLayout.LayoutParams(UiKit.WRAP, UiKit.WRAP)
        if (iconName.isNotEmpty()) {
            addView(icon(ctx, iconName, 13, fgHex).apply {
                (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(ctx, 5)
            })
        }
        addView(UiKit.text(ctx, label, sizeSp, fgHex, bold = true))
    }

    /** Full-width soft button with an icon, e.g. "Pause for 1 hour". */
    fun softButton(
        ctx: Context, iconName: String, label: String, bgHex: String, fgHex: String, onClick: () -> Unit
    ): LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        background = UiKit.rounded(Color.parseColor(bgHex), dp(ctx, 26).toFloat())
        layoutParams = LinearLayout.LayoutParams(UiKit.MATCH, dp(ctx, 52)).apply { topMargin = dp(ctx, 12) }
        if (iconName.isNotEmpty()) {
            addView(icon(ctx, iconName, 18, fgHex).apply {
                (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(ctx, 8)
            })
        }
        addView(UiKit.text(ctx, label, 15f, fgHex, bold = true))
        setOnClickListener { onClick() }
    }
}