package com.safesphere.ui.shield

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/** The three states the shield can show. */
enum class ShieldState { ON, OFF, ALERT }

/**
 * Shield drawn by hand with Bezier curves (Path.cubicTo), animated with
 * canvas.translate / rotate / scale.
 *
 *  ON    green, gentle breathing pulse, check mark
 *  OFF   grey, slightly shrunk and tilted, crossed out
 *  ALERT red, pulsing and shaking, exclamation mark
 *
 * Usage:  shield.setState(ShieldState.ALERT)
 */
class AnimatedShieldView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dp = resources.displayMetrics.density

    // ---- state + animation values ----
    var state: ShieldState = ShieldState.ON
        private set
    private var fromState = ShieldState.ON
    private var t = 1f          // 0 = still the old state, 1 = fully the new state
    private var phase = 0f      // 0..1, loops forever (drives pulse and shake)
    private var transition: ValueAnimator? = null
    private var idle: ValueAnimator? = null
    private val colorMixer = ArgbEvaluator()

    // ---- paints ----
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val symbolPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 0.16f           // in "shield units" because the canvas is scaled
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    // ---- shapes, drawn in a unit space centred on (0, 0) ----
    private val shieldPath = Path()
    private val checkPath = Path()

    init {
        // The shield outline: 4 cubic Bezier curves.
        shieldPath.moveTo(0f, -1.1f)                                  // top middle
        shieldPath.cubicTo(0.30f, -0.90f, 0.65f, -0.80f, 1.0f, -0.80f) // top-right shoulder
        shieldPath.cubicTo(1.0f, 0.10f, 0.80f, 0.70f, 0f, 1.15f)       // right side down to the tip
        shieldPath.cubicTo(-0.80f, 0.70f, -1.0f, 0.10f, -1.0f, -0.80f) // left side back up
        shieldPath.cubicTo(-0.65f, -0.80f, -0.30f, -0.90f, 0f, -1.1f)  // top-left shoulder
        shieldPath.close()

        // Check mark for the ON state
        checkPath.moveTo(-0.40f, 0.02f)
        checkPath.lineTo(-0.10f, 0.32f)
        checkPath.lineTo(0.42f, -0.28f)
    }

    // ---- per-state look ----
    private fun colorOf(s: ShieldState) = when (s) {
        ShieldState.ON -> Color.parseColor("#2E9E6B")
        ShieldState.OFF -> Color.parseColor("#90A4AE")
        ShieldState.ALERT -> Color.parseColor("#D32F2F")
    }
    private fun baseScaleOf(s: ShieldState) = when (s) {
        ShieldState.ON -> 1.0f
        ShieldState.OFF -> 0.88f
        ShieldState.ALERT -> 1.0f
    }
    private fun baseTiltOf(s: ShieldState) = when (s) {        // degrees
        ShieldState.ON -> 0f
        ShieldState.OFF -> -6f
        ShieldState.ALERT -> 0f
    }
    private fun pulseOf(s: ShieldState) = when (s) {           // how much it "breathes"
        ShieldState.ON -> 0.035f
        ShieldState.OFF -> 0f
        ShieldState.ALERT -> 0.06f
    }
    private fun shakeOf(s: ShieldState) = when (s) {           // degrees of side-to-side shake
        ShieldState.ON -> 0f
        ShieldState.OFF -> 0f
        ShieldState.ALERT -> 7f
    }
    private fun lerp(a: Float, b: Float, k: Float) = a + (b - a) * k

    /** Switch to a new state. The change animates (colour, size, tilt, symbol). */
    fun setState(newState: ShieldState, animate: Boolean = true) {
        if (newState == state) return
        fromState = state
        state = newState
        transition?.cancel()
        if (!animate) {
            t = 1f
            invalidate()
            return
        }
        t = 0f
        transition = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 500
            interpolator = DecelerateInterpolator()
            addUpdateListener { t = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        idle = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { phase = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        idle?.cancel()
        transition?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (160 * dp).toInt()
        setMeasuredDimension(resolveSize(size, widthMeasureSpec), resolveSize(size, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Mix the old and new state according to t
        val color = colorMixer.evaluate(t, colorOf(fromState), colorOf(state)) as Int
        val baseScale = lerp(baseScaleOf(fromState), baseScaleOf(state), t)
        val baseTilt = lerp(baseTiltOf(fromState), baseTiltOf(state), t)
        val pulseAmp = lerp(pulseOf(fromState), pulseOf(state), t)
        val shakeAmp = lerp(shakeOf(fromState), shakeOf(state), t)

        val wave = sin(phase * 2f * PI.toFloat())
        val pulse = 1f + pulseAmp * wave
        val shake = shakeAmp * sin(phase * 2f * PI.toFloat() * 4f)
        val pop = 0.12f * sin(t * PI.toFloat())          // a small "pop" while switching state

        val cx = width / 2f
        val cy = height / 2f
        val unit = min(width, height) * 0.36f             // half the shield's width, in pixels

        // Soft halo behind the shield
        haloPaint.color = color
        haloPaint.alpha = 40
        canvas.drawCircle(cx, cy, unit * 1.45f * pulse, haloPaint)

        // translate -> rotate -> scale, then draw in unit space
        canvas.save()
        canvas.translate(cx, cy)
        canvas.rotate(baseTilt + shake)
        val s = unit * baseScale * pulse * (1f + pop)
        canvas.scale(s, s)

        fillPaint.color = color
        canvas.drawPath(shieldPath, fillPaint)

        // Symbols cross-fade between the old and the new state
        if (fromState == state) {
            drawSymbol(canvas, state, 1f)
        } else {
            drawSymbol(canvas, fromState, 1f - t)
            drawSymbol(canvas, state, t)
        }
        canvas.restore()
    }

    private fun drawSymbol(canvas: Canvas, s: ShieldState, alpha: Float) {
        if (alpha <= 0f) return
        val a = (255 * alpha).toInt().coerceIn(0, 255)
        symbolPaint.alpha = a
        dotPaint.alpha = a
        when (s) {
            ShieldState.ON -> canvas.drawPath(checkPath, symbolPaint)
            ShieldState.OFF -> canvas.drawLine(-0.38f, -0.34f, 0.38f, 0.34f, symbolPaint)
            ShieldState.ALERT -> {
                canvas.drawLine(0f, -0.50f, 0f, 0.12f, symbolPaint)
                canvas.drawCircle(0f, 0.42f, 0.10f, dotPaint)
            }
        }
    }
}