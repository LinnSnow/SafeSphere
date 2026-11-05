package com.safesphere.ui.charts

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.ceil
import kotlin.math.max

/**
 * Trend line chart: same 7 numbers as the bar chart (Mon..Sun), drawn as a smooth curve.
 * The curve is built from cubic Bezier segments (Path.cubicTo) and "draws itself"
 * from left to right when the data is set.
 *
 * Usage: chart.setData(listOf(3, 5, 0, 2, 8, 12, 1))
 */
class TrendLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density
    private val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    private var data: List<Int> = List(7) { 0 }
    private var progress = 1f                 // 0 = nothing drawn, 1 = whole curve drawn
    private var animator: ValueAnimator? = null

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9E9E9E"); strokeWidth = 2 * dp
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E0E0E0"); strokeWidth = 1 * dp
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F6FE0")
        style = Paint.Style.STROKE
        strokeWidth = 3 * dp
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#223F6FE0")   // blue at ~13% opacity
        style = Paint.Style.FILL
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3F6FE0"); style = Paint.Style.FILL
    }
    private val dotRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#424242"); textSize = 12 * dp; textAlign = Paint.Align.CENTER
    }
    private val axisTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#757575"); textSize = 11 * dp; textAlign = Paint.Align.RIGHT
    }

    private val curve = Path()
    private val drawnPart = Path()
    private val fillPath = Path()
    private val pos = FloatArray(2)

    /** Give the chart 7 numbers (Monday to Sunday). The curve draws itself. */
    fun setData(dailyCounts: List<Int>) {
        require(dailyCounts.size == 7) { "Need exactly 7 values (Mon..Sun)" }
        data = dailyCounts
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1100
            interpolator = DecelerateInterpolator()
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val left = 40 * dp
        val right = width - 16 * dp
        val top = 28 * dp
        val bottom = height - 32 * dp
        val chartW = right - left
        val chartH = bottom - top

        val maxValue = max(data.maxOrNull() ?: 0, 1)
        val niceMax = (ceil(maxValue / 4.0) * 4).toInt()

        // Grid + y-axis numbers
        for (i in 0..4) {
            val y = bottom - chartH * i / 4f
            canvas.drawLine(left, y, right, y, if (i == 0) axisPaint else gridPaint)
            canvas.drawText((niceMax * i / 4).toString(), left - 6 * dp, y + 4 * dp, axisTextPaint)
        }

        // Point positions (one per day, centred in its slot, same as the bar chart)
        val slot = chartW / 7f
        val xs = FloatArray(7) { left + slot * (it + 0.5f) }
        val ys = FloatArray(7) { bottom - data[it].toFloat() / niceMax * chartH }

        // Day labels
        for (i in 0 until 7) canvas.drawText(labels[i], xs[i], bottom + 20 * dp, textPaint)

        // Smooth curve: between two points use a cubic Bezier whose control points
        // sit at the horizontal midpoint. This gives a soft S-shape and never dips
        // below a point's own height (no fake "overshoot").
        curve.reset()
        curve.moveTo(xs[0], ys[0])
        for (i in 0 until 6) {
            val midX = (xs[i] + xs[i + 1]) / 2f
            curve.cubicTo(midX, ys[i], midX, ys[i + 1], xs[i + 1], ys[i + 1])
        }

        // Draw only the first part of the curve, so it looks like it is being drawn
        val measure = PathMeasure(curve, false)
        val visibleLength = measure.length * progress
        if (visibleLength > 0f) {
            drawnPart.reset()
            measure.getSegment(0f, visibleLength, drawnPart, true)

            // Soft fill under the curve
            measure.getPosTan(visibleLength, pos, null)
            fillPath.reset()
            fillPath.addPath(drawnPart)
            fillPath.lineTo(pos[0], bottom)
            fillPath.lineTo(xs[0], bottom)
            fillPath.close()
            canvas.drawPath(fillPath, fillPaint)

            canvas.drawPath(drawnPart, linePaint)
        }

        // Dots + values appear as the line reaches them
        for (i in 0 until 7) {
            if (i / 6f <= progress) {
                canvas.drawCircle(xs[i], ys[i], 6 * dp, dotRingPaint)
                canvas.drawCircle(xs[i], ys[i], 4 * dp, dotPaint)
                canvas.drawText(data[i].toString(), xs[i], ys[i] - 12 * dp, textPaint)
            }
        }
    }
}