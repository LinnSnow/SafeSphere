package com.safesphere.ui.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.ceil
import kotlin.math.max

/**
 * Custom bar chart: 7 bars (Mon..Sun), drawn by hand on a Canvas.
 * Usage: chart.setData(listOf(3, 5, 0, 2, 8, 12, 1))
 */
class WeeklyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density
    private val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private var data: List<Int> = List(7) { 0 }
    private var progress = 1f            // 0 = bars have no height, 1 = full height
    private var animator: ValueAnimator? = null

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E")
        strokeWidth = 2 * dp
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0")
        strokeWidth = 1 * dp
    }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F6FE0")
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#424242")
        textSize = 12 * dp
        textAlign = Paint.Align.CENTER
    }
    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575")
        textSize = 11 * dp
        textAlign = Paint.Align.RIGHT
    }

    /** Give the chart 7 numbers (Monday to Sunday). Bars grow up from zero. */
    fun setData(dailyCounts: List<Int>) {
        require(dailyCounts.size == 7) { "Need exactly 7 values (Mon..Sun)" }
        data = dailyCounts
        startGrowAnimation()
    }

    private fun startGrowAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()   // ask Android to call onDraw again
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // The rectangle where the chart lives (leave room for labels)
        val left = 40 * dp
        val right = width - 16 * dp
        val top = 28 * dp
        val bottom = height - 32 * dp
        val chartW = right - left
        val chartH = bottom - top

        // Round the max up to a multiple of 4 so the grid numbers look clean
        val maxValue = max(data.maxOrNull() ?: 0, 1)
        val niceMax = (ceil(maxValue / 4.0) * 4).toInt()

        // Horizontal grid lines + y-axis numbers
        for (i in 0..4) {
            val y = bottom - chartH * i / 4f
            canvas.drawLine(left, y, right, y, if (i == 0) axisPaint else gridPaint)
            canvas.drawText((niceMax * i / 4).toString(), left - 6 * dp, y + 4 * dp, axisTextPaint)
        }

        // Bars
        val slot = chartW / 7f
        val barW = slot * 0.6f
        data.forEachIndexed { i, value ->
            val cx = left + slot * (i + 0.5f)
            val barH = value.toFloat() / niceMax * chartH * progress
            canvas.drawRect(cx - barW / 2, bottom - barH, cx + barW / 2, bottom, barPaint)
            if (value > 0) {
                canvas.drawText(value.toString(), cx, bottom - barH - 6 * dp, textPaint)
            }
            canvas.drawText(labels[i], cx, bottom + 20 * dp, textPaint)
        }
    }
}