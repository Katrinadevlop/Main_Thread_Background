package ru.netology.nmedia.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.min
import kotlin.properties.Delegates

class StatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var data: List<Float> by Delegates.observable(emptyList()) { _, _, _ ->
        update()
    }
    var maxValue: Float? by Delegates.observable(null) { _, _, _ ->
        update()
    }

    private var progress = 0f
    private var valueAnimator: ValueAnimator? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 16f.dp
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 20f.dp
    }

    private val colors = intArrayOf(
        Color.parseColor("#FF007A"),
        Color.parseColor("#6C2BFF"),
        Color.parseColor("#00C2FF"),
        Color.parseColor("#FFC400"),
        Color.parseColor("#FF8A00"),
        Color.parseColor("#00D084"),
    )
    private val emptyColor = Color.parseColor("#E6E6E6")
    private val oval = RectF()

    private fun update() {
        valueAnimator?.let {
            it.removeAllListeners()
            it.cancel()
        }
        progress = 0f
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1200
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { anim ->
                progress = anim.animatedValue as Float
                invalidate()
            }
        }.also {
            it.start()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val contentW = w - paddingLeft - paddingRight
        val contentH = h - paddingTop - paddingBottom
        val size = min(contentW, contentH).toFloat()
        val strokePadding = paint.strokeWidth / 2
        val left = paddingLeft + (contentW - size) / 2 + strokePadding
        val top = paddingTop + (contentH - size) / 2 + strokePadding
        oval.set(left, top, left + size - 2 * strokePadding, top + size - 2 * strokePadding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (data.isEmpty()) return
        val (segments, percent) = buildSegments(data, maxValue)
        val stage = stageParams(progress)
        val gapAngle = lerp(26f, 6f, stage.gapT)
        val totalGap = gapAngle * segments.size
        val sweepBase = (360f - totalGap).coerceAtLeast(0f)

        var startFrom = -90f + stage.rotation
        segments.forEach { segment ->
            val allocated = sweepBase * segment.fraction
            val sweep = allocated * stage.lengthT
            paint.color = segment.color
            canvas.drawArc(oval, startFrom, sweep, false, paint)
            startFrom += allocated + gapAngle
        }

        val text = String.format("%.2f%%", percent)
        val x = width / 2f
        val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(text, x, y, textPaint)
    }
    private data class Segment(val fraction: Float, val color: Int)
    private data class StageParams(val lengthT: Float, val gapT: Float, val rotation: Float)

    private fun buildSegments(values: List<Float>, max: Float?): Pair<List<Segment>, Float> {
        val positive = values.map { if (it > 0f) it else 0f }
        val sum = positive.sum()
        if (sum <= 0f) return emptyList<Segment>() to 0f

        val effectiveMax = if (max != null && max > sum) max else sum
        val segments = ArrayList<Segment>(positive.size + 1)
        positive.forEachIndexed { index, value ->
            if (value > 0f) {
                segments += Segment(value / effectiveMax, colors.getOrNull(index) ?: randomColor())
            }
        }
        if (max != null) {
            val remainder = effectiveMax - sum
            if (remainder > 0f) {
                segments += Segment(remainder / effectiveMax, emptyColor)
            }
        }
        val percent = (sum / effectiveMax) * 100f
        return segments to percent
    }

    private fun stageParams(p: Float): StageParams {
        val clamped = p.coerceIn(0f, 1f)
        return when {
            clamped < 0.33f -> {
                val t = ease(clamped / 0.33f)
                StageParams(
                    lengthT = lerp(0.12f, 0.35f, t),
                    gapT = lerp(0f, 0.6f, t),
                    rotation = 0f,
                )
            }
            clamped < 0.66f -> {
                val t = ease((clamped - 0.33f) / 0.33f)
                StageParams(
                    lengthT = lerp(0.35f, 0.8f, t),
                    gapT = lerp(0.6f, 0.85f, t),
                    rotation = lerp(0f, 14f, t),
                )
            }
            else -> {
                val t = ease((clamped - 0.66f) / 0.34f)
                StageParams(
                    lengthT = lerp(0.8f, 1f, t),
                    gapT = lerp(0.85f, 1f, t),
                    rotation = 14f,
                )
            }
        }
    }

    private fun lerp(from: Float, to: Float, t: Float): Float = from + (to - from) * t
    private fun ease(t: Float): Float = t * t * (3 - 2 * t)

    private fun randomColor(): Int {
        val hue = (0..359).random().toFloat()
        return Color.HSVToColor(floatArrayOf(hue, 0.65f, 0.95f))
    }

    private val Float.dp: Float
        get() = this * resources.displayMetrics.density

    override fun onDetachedFromWindow() {
        valueAnimator?.cancel()
        valueAnimator = null
        super.onDetachedFromWindow()
    }
}
