package com.safesphere.ui.screens

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

/** Reusable pieces: feature rows, number keypad, code boxes. */
object Widgets {

    /** Soft row with a round mark (✓ or ✕), a bold title and a small description. */
    fun featureRow(
        ctx: Context, title: String, desc: String,
        mark: String = "✓", markBgHex: String = UiKit.GREEN_SOFT, markFgHex: String = UiKit.GREEN_DARK
    ): LinearLayout {
        val texts = UiKit.vertical(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(0, UiKit.WRAP, 1f).apply { leftMargin = UiKit.dp(ctx, 12) }
            addView(UiKit.text(ctx, title, 14f, UiKit.INK, bold = true))
            if (desc.isNotEmpty()) {
                addView(UiKit.text(ctx, desc, 12f, UiKit.MUTED).apply {
                    setPadding(0, UiKit.dp(ctx, 2), 0, 0)
                })
            }
        }
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = UiKit.rounded(Color.parseColor("#F1F4FD"), UiKit.dp(ctx, 12).toFloat())
            setPadding(UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12), UiKit.dp(ctx, 12))
            addView(UiKit.roundIcon(ctx, mark, 28, markBgHex, markFgHex, 14f))
            addView(texts)
        }
    }

    /** 3 x 4 number pad: 1-9, blank, 0, delete. */
    fun keypad(ctx: Context, onDigit: (Char) -> Unit, onDelete: () -> Unit): LinearLayout {
        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "⌫")
        )
        return UiKit.vertical(ctx).apply {
            rows.forEach { row ->
                addView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    row.forEach { key ->
                        addView(TextView(ctx).apply {
                            text = key
                            textSize = 22f
                            gravity = Gravity.CENTER
                            setTextColor(Color.parseColor(UiKit.INK))
                            layoutParams = LinearLayout.LayoutParams(0, UiKit.dp(ctx, 52), 1f).apply {
                                val m = UiKit.dp(ctx, 6)
                                setMargins(m, m, m, m)
                            }
                            if (key.isNotEmpty()) {
                                background = UiKit.rounded(Color.parseColor("#E8EEFD"), UiKit.dp(ctx, 26).toFloat())
                                setOnClickListener { if (key == "⌫") onDelete() else onDigit(key[0]) }
                            }
                        })
                    }
                })
            }
        }
    }
}

/**
 * A row of code boxes. Filled boxes show the digit (or a dot when masked),
 * the next box is highlighted, the rest show a small dot. Red outline on error.
 */
class CodeBoxes(private val ctx: Context, private val length: Int, private val masked: Boolean) {

    val view: LinearLayout = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
    }

    private val cells: List<TextView> = List(length) { i ->
        TextView(ctx).apply {
            gravity = Gravity.CENTER
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(UiKit.dp(ctx, 44), UiKit.dp(ctx, 52)).apply {
                if (i > 0) leftMargin = UiKit.dp(ctx, 8)
            }
        }
    }

    private var value = ""
    private var error = false

    init {
        cells.forEach { view.addView(it) }
        refresh()
    }

    fun setValue(v: String) {
        value = v
        refresh()
    }

    fun setError(e: Boolean) {
        error = e
        refresh()
    }

    private fun refresh() {
        cells.forEachIndexed { i, cell ->
            val filled = i < value.length
            val current = i == value.length
            cell.text = when {
                filled -> if (masked) "●" else value[i].toString()
                current -> "–"
                else -> "•"
            }
            cell.setTextColor(Color.parseColor(if (filled) UiKit.INK else "#9AA7BD"))
            val fill = if (current) Color.WHITE else Color.parseColor("#EEF1FB")
            val stroke: Int? = when {
                error -> Color.parseColor(UiKit.RED)
                current -> Color.parseColor(UiKit.BLUE)
                else -> null
            }
            cell.background = UiKit.rounded(fill, UiKit.dp(ctx, 10).toFloat(), stroke, UiKit.dp(ctx, 2))
        }
    }
}