package com.safesphere.ui.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.max

/**
 * Custom bar chart: 7 bars (Mon..Sun), drawn by hand on a Canvas.
 * Light bars for low days, medium for busy days, dark blue + "Peak" badge for the busiest day.
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
    private var progress = 1f
    private var animator: ValueAnimator? = null

    private val lowColor = Color.parseColor("#C5D4F6")
    private val midColor = Color.parseColor("#9FB7EF")
    private val peakColor = Color.parseColor("#3F6FE0")

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#141F2F")
        textSize = 12 * dp
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val dayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#737685")
        textSize = 11 * dp
        textAlign = Paint.Align.CENTER
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = peakColor
        style = Paint.Style.FILL
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 10 * dp
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val path = Path()
    private val rect = RectF()

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
                invalidate()
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

        val left = 8 * dp
        val right = width - 8 * dp
        val top = 46 * dp          // room for the value label and the Peak badge
        val bottom = height - 28 * dp
        val chartH = bottom - top

        val maxValue = max(data.maxOrNull() ?: 0, 1)
        val peakIndex = if ((data.maxOrNull() ?: 0) > 0) data.indexOf(data.max()) else -1

        val slot = (right - left) / 7f
        val barW = slot * 0.62f
        val radius = 8 * dp

        data.forEachIndexed { i, value ->
            val cx = left + slot * (i + 0.5f)
            val ratio = value.toFloat() / maxValue
            val barH = ratio * chartH * progress
            val barTop = bottom - barH

            if (value > 0) {
                barPaint.color = when {
                    i == peakIndex -> peakColor
                    ratio >= 0.5f -> midColor
                    else -> lowColor
                }
                rect.set(cx - barW / 2, barTop, cx + barW / 2, bottom)
                path.reset()
                path.addRoundRect(rect, floatArrayOf(radius, radius, radius, radius, 0f, 0f, 0f, 0f), Path.Direction.CW)
                canvas.drawPath(path, barPaint)
            }

            // value label above the bar
            val valueY = barTop - 6 * dp
            valuePaint.color = if (i == peakIndex) peakColor else Color.parseColor("#141F2F")
            canvas.drawText(value.toString(), cx, valueY, valuePaint)

            // "Peak" badge above the busiest day
            if (i == peakIndex) {
                val bw = 34 * dp
                val bh = 16 * dp
                val by = valueY - 16 * dp - bh
                rect.set(cx - bw / 2, by, cx + bw / 2, by + bh)
                canvas.drawRoundRect(rect, bh / 2, bh / 2, badgePaint)
                canvas.drawText("Peak", cx, by + bh / 2 + 3.5f * dp, badgeTextPaint)
            }

            // day label under the bar
            dayPaint.color = if (i == peakIndex) peakColor else Color.parseColor("#737685")
            dayPaint.typeface = Typeface.create(
                Typeface.DEFAULT, if (i == peakIndex) Typeface.BOLD else Typeface.NORMAL
            )
            canvas.drawText(labels[i], cx, bottom + 20 * dp, dayPaint)
        }
    }
}