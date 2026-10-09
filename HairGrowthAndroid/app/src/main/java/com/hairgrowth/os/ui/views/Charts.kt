package com.hairgrowth.os.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.hairgrowth.os.ui.Palette
import kotlin.math.min

/** 7–day daily-completion bar chart (with day labels). */
class BarsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val barRect = RectF()
    private var labels: List<String> = emptyList()
    private var values: List<Int> = emptyList()
    private var todayIndex = -1
    private var accent = Palette.GOLD
    private var shown = 0f

    init {
        paint.textAlign = Paint.Align.CENTER
    }

    fun setAccent(hex: String) {
        accent = Palette.accent(hex)
        invalidate()
    }

    fun setData(labels: List<String>, values: List<Int>, todayIndex: Int) {
        this.labels = labels
        this.values = values
        this.todayIndex = todayIndex
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                shown = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (values.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val labelH = h * 0.16f
        val chartH = h - labelH
        val gap = w * 0.028f
        val barW = (w - gap * (values.size - 1)) / values.size
        val radius = barW * 0.32f

        paint.textSize = labelH * 0.62f
        paint.typeface = null

        for (i in values.indices) {
            val left = i * (barW + gap)
            val ratio = (values[i] / 100f).coerceIn(0f, 1f) * shown
            val barH = (chartH * 0.86f * ratio).coerceAtLeast(barW * 0.10f)
            val top = chartH - barH
            barRect.set(left, top, left + barW, chartH)

            val base = if (i == todayIndex) Palette.ROSE else accent
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                0f, top, 0f, chartH,
                intArrayOf(base, (base and 0x00FFFFFF) or (0x2A shl 24)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(barRect, radius, radius, paint)
            paint.shader = null

            if (i < labels.size) {
                paint.color = if (i == todayIndex) Palette.TEXT else Palette.MUTED
                canvas.drawText(labels[i], left + barW / 2f, h - labelH * 0.15f, paint)
            }
        }
    }
}

/** Row of mini progress rings, one per day. */
class RingsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val path = Path()
    private var labels: List<String> = emptyList()
    private var values: List<Int> = emptyList()
    private var accent = Palette.GOLD
    private var shown = 0f

    init {
        paint.textAlign = Paint.Align.CENTER
    }

    fun setAccent(hex: String) {
        accent = Palette.accent(hex)
        invalidate()
    }

    fun setData(labels: List<String>, values: List<Int>) {
        this.labels = labels
        this.values = values
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 950L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                shown = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (values.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val cellW = w / values.size
        val labelH = h * 0.30f
        val ringSpace = h - labelH
        val radius = min(cellW * 0.40f, ringSpace * 0.44f)
        val stroke = radius * 0.34f
        val cy = ringSpace * 0.5f

        paint.textSize = labelH * 0.42f
        paint.typeface = null

        for (i in values.indices) {
            val cx = i * cellW + cellW / 2f
            rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.color = 0x14FFFFFF
            canvas.drawCircle(cx, cy, radius, paint)

            val sweep = 360f * (values[i] / 100f) * shown
            if (sweep > 0.4f) {
                paint.strokeCap = Paint.Cap.ROUND
                paint.color = accent
                canvas.drawArc(rect, -90f, sweep, false, paint)
            }
            paint.color = Palette.MUTED
            val cl = labels.getOrElse(i) { "" }
            canvas.drawText(cl, cx, ringSpace + labelH * 0.42f, paint)
            paint.color = Palette.TEXT
            paint.textSize = labelH * 0.40f
            canvas.drawText("${values[i]}%", cx, ringSpace + labelH * 0.86f, paint)
            paint.textSize = labelH * 0.42f
        }
    }
}

/** Compact sparkline bars used inside the bento cell. */
class SparkView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var values: List<Int> = emptyList()
    private var accent = Palette.GOLD
    private var shown = 0f

    fun setAccent(hex: String) {
        accent = Palette.accent(hex)
        invalidate()
    }

    fun setData(values: List<Int>) {
        this.values = values
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 800L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                shown = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (values.isEmpty()) return
        val w = width.toFloat()
        val h = height.toFloat()
        val gap = w * 0.03f
        val barW = (w - gap * (values.size - 1)) / values.size
        val radius = barW * 0.35f
        for (i in values.indices) {
            val left = i * (barW + gap)
            val ratio = (values[i].coerceAtLeast(10) / 100f) * shown
            val barH = (h * ratio).coerceAtLeast(barW * 0.18f)
            rect.set(left, h - barH, left + barW, h)
            paint.shader = LinearGradient(
                0f, h - barH, 0f, h,
                intArrayOf(accent, (accent and 0x00FFFFFF) or (0x1E shl 24)),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.style = Paint.Style.FILL
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
        paint.shader = null
    }
}
