package com.safesphere.ui.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

/**
 * Heatmap: 7 days x 4 time blocks = 28 cells.
 * Index formula (from the team data contract): values[day * 4 + timeBlock]
 * Time blocks: 0 = 00-06, 1 = 06-12, 2 = 12-18, 3 = 18-24
 * Colour goes from light yellow (few events) to dark red (many events).
 */
class HeatmapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density
    private val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val blocks = listOf("Night", "Morning", "Afternoon", "Evening")

    private var values: List<Int> = List(28) { 0 }

    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#424242")
        textSize = 12 * dp
    }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14 * dp
        textAlign = Paint.Align.CENTER
    }
    private val rect = RectF()

    // Light yellow -> dark red, as RGB numbers
    private val lowColor = intArrayOf(255, 255, 204)
    private val highColor = intArrayOf(128, 0, 38)

    /** Give the heatmap exactly 28 numbers. */
    fun setData(heatmapValues: List<Int>) {
        require(heatmapValues.size == 28) { "Need exactly 28 values (7 days x 4 blocks)" }
        values = heatmapValues
        invalidate()
    }

    /** ratio 0.0 -> light yellow, 1.0 -> dark red */
    private fun colorFor(ratio: Float): Int {
        val r = (lowColor[0] + (highColor[0] - lowColor[0]) * ratio).toInt()
        val g = (lowColor[1] + (highColor[1] - lowColor[1]) * ratio).toInt()
        val b = (lowColor[2] + (highColor[2] - lowColor[2]) * ratio).toInt()
        return Color.rgb(r, g, b)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val leftLabelW = 44 * dp
        val topLabelH = 28 * dp
        val pad = 8 * dp
        val gap = 4 * dp

        val cellW = (width - leftLabelW - pad - gap * 3) / 4f
        val cellH = (height - topLabelH - pad - gap * 6) / 7f
        val maxValue = max(values.maxOrNull() ?: 0, 1)

        // Column titles (time blocks)
        labelPaint.textAlign = Paint.Align.CENTER
        for (t in 0 until 4) {
            val cx = leftLabelW + t * (cellW + gap) + cellW / 2
            canvas.drawText(blocks[t], cx, topLabelH - 8 * dp, labelPaint)
        }

        // Row titles (days) + the coloured cells
        labelPaint.textAlign = Paint.Align.LEFT
        for (d in 0 until 7) {
            val top = topLabelH + d * (cellH + gap)
            canvas.drawText(days[d], 4 * dp, top + cellH / 2 + 4 * dp, labelPaint)

            for (t in 0 until 4) {
                val value = values[d * 4 + t]          // <- the index formula
                val ratio = value.toFloat() / maxValue
                val left = leftLabelW + t * (cellW + gap)

                cellPaint.color = colorFor(ratio)
                rect.set(left, top, left + cellW, top + cellH)
                canvas.drawRoundRect(rect, 6 * dp, 6 * dp, cellPaint)

                // White text on dark cells, dark text on light cells
                numberPaint.color = if (ratio > 0.6f) Color.WHITE else Color.parseColor("#424242")
                canvas.drawText(value.toString(), rect.centerX(), rect.centerY() + 5 * dp, numberPaint)
            }
        }
    }
}