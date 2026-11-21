package com.safesphere.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

/**
 * Heatmap: 7 days (columns) x 4 time blocks (rows) = 28 cells.
 * Index formula (from the team data contract): values[day * 4 + timeBlock]
 * Time blocks: 0 = 00-06 (Night), 1 = 06-12 (Morn), 2 = 12-18 (Aftn), 3 = 18-24 (Eve)
 * Empty cells are light blue-grey; busier cells go from light yellow to dark red.
 */
class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density
    private val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
    private val blocks = listOf("Night", "Morn", "Aftn", "Eve")

    private var values: List<Int> = List(28) { 0 }

    private val emptyColor = Color.parseColor("#E7EEFF")
    private val ramp = intArrayOf(
        Color.parseColor("#FEF5AD"),
        Color.parseColor("#FDE047"),
        Color.parseColor("#FB923C"),
        Color.parseColor("#EA580C"),
        Color.parseColor("#991B1B"),
        Color.parseColor("#7F1D1D")
    )

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#434653")
        textSize = 11 * dp
    }
    private val rect = RectF()

    /** Give the heatmap exactly 28 numbers. */
    fun setData(heatmapValues: List<Int>) {
        require(heatmapValues.size == 28) { "Need exactly 28 values (7 days x 4 blocks)" }
        values = heatmapValues
        invalidate()
    }

    private fun colorFor(value: Int, maxValue: Int): Int {
        if (value <= 0) return emptyColor
        val ratio = value.toFloat() / maxValue
        return ramp[min(ramp.size - 1, (ratio * ramp.size).toInt())]
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val leftLabelW = 40 * dp
        val topLabelH = 24 * dp
        val gap = 5 * dp

        val cellW = (width - leftLabelW - gap * 6) / 7f
        val cellH = (height - topLabelH - gap * 3) / 4f
        val maxValue = max(values.maxOrNull() ?: 0, 1)

        // Column titles (days)
        labelPaint.textAlign = Paint.Align.CENTER
        for (d in 0 until 7) {
            val cx = leftLabelW + d * (cellW + gap) + cellW / 2
            canvas.drawText(dayLetters[d], cx, topLabelH - 8 * dp, labelPaint)
        }

        // Row titles (time blocks) + the coloured cells
        for (t in 0 until 4) {
            val top = topLabelH + t * (cellH + gap)
            labelPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(blocks[t], 0f, top + cellH / 2 + 4 * dp, labelPaint)

            for (d in 0 until 7) {
                val value = values[d * 4 + t]          // <- the index formula
                val left = leftLabelW + d * (cellW + gap)
                cellPaint.color = colorFor(value, maxValue)
                rect.set(left, top, left + cellW, top + cellH)
                canvas.drawRoundRect(rect, 6 * dp, 6 * dp, cellPaint)
            }
        }
    }
}