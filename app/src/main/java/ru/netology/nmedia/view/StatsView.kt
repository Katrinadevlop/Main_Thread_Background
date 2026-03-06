package ru.netology.nmedia.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.min
import kotlin.properties.Delegates

class StatsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    var data: List<Float> by Delegates.observable(emptyList()) { _, _, _ ->
        invalidate()
    }
    var maxValue: Float? by Delegates.observable(null) { _, _, _ ->
        invalidate()
    }

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
        val segments = buildSegments(data)
        var startAngle = -90f
        val radius = oval.width() / 2f
        val circumference = 2f * Math.PI.toFloat() * radius
        val overlapAngle = if (circumference > 0f) 360f * paint.strokeWidth / circumference else 0f
        val capAngle = overlapAngle / 2f
        val emptySegment = segments.firstOrNull { it.isEmpty }
        if (emptySegment != null) {
            val sweep = emptySegment.fraction * 360f
            if (sweep > 0f) {
                paint.color = emptySegment.color
                canvas.drawArc(oval, startAngle - capAngle, sweep + 2 * capAngle, false, paint)
                startAngle += sweep
            }
        }
        val colored = segments.filter { !it.isEmpty }
        colored.forEachIndexed { index, segment ->
            var sweep = segment.fraction * 360f
            if (sweep <= 0f) return@forEachIndexed
            paint.color = segment.color
            canvas.drawArc(oval, startAngle - capAngle, sweep + 2 * capAngle, false, paint)
            startAngle += sweep
        }
        // Redraw the start of the first segment so its cap overlaps the last segment
        if (emptySegment == null && colored.size > 1) {
            paint.color = colored.first().color
            canvas.drawArc(oval, -90f - capAngle, 2 * capAngle, false, paint)
        }
        if (segments.isNotEmpty()) {
            val percent = currentPercent(data)
            val text = String.format("%.2f%%", percent)
            val x = width / 2f
            val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
            canvas.drawText(text, x, y, textPaint)
        }
    }
    private data class Segment(val fraction: Float, val color: Int, val isEmpty: Boolean)

    private fun buildSegments(values: List<Float>): List<Segment> {
        val positive = values.map { if (it > 0f) it else 0f }
        val sum = positive.sum()
        if (sum <= 0f) return emptyList()

        val max = maxValue
        val effectiveMax = if (max != null && max > sum) max else sum
        val segments = ArrayList<Segment>(positive.size + 1)
        positive.forEachIndexed { index, value ->
            if (value > 0f) {
                segments += Segment(value / effectiveMax, colors[index % colors.size], false)
            }
        }
        val remainder = effectiveMax - sum
        if (remainder > 0f) {
            segments += Segment(remainder / effectiveMax, emptyColor, true)
        }
        return segments
    }

    private fun currentPercent(values: List<Float>): Float {
        val sum = values.filter { it > 0f }.sum()
        if (sum <= 0f) return 0f
        val max = maxValue
        val effectiveMax = if (max != null && max > sum) max else sum
        return (sum / effectiveMax) * 100f
    }

    private val Float.dp: Float
        get() = this * resources.displayMetrics.density
}
