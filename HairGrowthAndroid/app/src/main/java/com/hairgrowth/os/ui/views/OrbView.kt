package com.hairgrowth.os.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import com.hairgrowth.os.ui.Palette
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/** Weekly progress orb — gradient arc, layered glow and orbiting satellite. */
class OrbView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val orbitPath = Path()

    private var shown = 0f
    private var target = 0f
    private var rotation = 0f
    private var breath = 0f

    private var accent = Palette.GOLD
    private var accentSoft = Palette.GOLD_SOFT

    private var orbitAnimator: ValueAnimator? = null
    private var breathAnimator: ValueAnimator? = null
    private var progressAnimator: ValueAnimator? = null

    fun setAccent(hex: String) {
        accent = Palette.accent(hex)
        accentSoft = Palette.accentSoft(hex)
        invalidate()
    }

    /** @param fraction 0f..1f */
    fun setProgress(fraction: Float) {
        target = fraction.coerceIn(0f, 1f)
        progressAnimator?.cancel()
        progressAnimator = ValueAnimator.ofFloat(shown, target).apply {
            duration = 1100L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                shown = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (orbitAnimator == null) {
            orbitAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
                duration = 26000L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    rotation = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
        if (breathAnimator == null) {
            breathAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 5200L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    breath = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        orbitAnimator?.cancel(); orbitAnimator = null
        breathAnimator?.cancel(); breathAnimator = null
        progressAnimator?.cancel(); progressAnimator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f
        val stroke = h * 0.045f
        val radius = min(w, h) / 2f - stroke * 1.7f
        if (radius <= 0f) return

        // breathing halo
        paint.style = Paint.Style.FILL
        paint.shader = RadialGradient(
            cx, cy, radius * (1.15f + 0.16f * breath),
            intArrayOf((accent and 0x00FFFFFF) or (0x2A shl 24), (accent and 0x00FFFFFF)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * (1.15f + 0.16f * breath), paint)

        // track
        paint.shader = null
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = stroke
        paint.color = 0x14FFFFFF
        canvas.drawCircle(cx, cy, radius, paint)

        // layered glow arcs (blur-free glow for all API levels)
        val sweep = 360f * shown
        rect.set(cx - radius, cy - radius, cx + radius, cy + radius)
        if (sweep > 0.5f) {
            val layers = 3
            for (i in layers downTo 1) {
                paint.strokeWidth = stroke + stroke * (0.55f * i)
                paint.color = (accent and 0x00FFFFFF) or ((14 / i) shl 24)
                canvas.drawArc(rect, -90f, sweep, false, paint)
            }
            paint.strokeWidth = stroke
            paint.shader = LinearGradient(
                cx - radius, cy - radius, cx + radius, cy + radius,
                intArrayOf(accentSoft, accent, Palette.MINT),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawArc(rect, -90f, sweep, false, paint)
            paint.shader = null
        }

        // dashed orbit
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = h * 0.006f
        paint.color = (accent and 0x00FFFFFF) or (0x3A shl 24)
        orbitPath.reset()
        orbitPath.addCircle(cx, cy, radius + stroke * 1.05f, Path.Direction.CW)
        paint.pathEffect = android.graphics.DashPathEffect(
            floatArrayOf(h * 0.028f, h * 0.024f), 0f
        )
        canvas.drawPath(orbitPath, paint)
        paint.pathEffect = null

        // satellite
        val rad = Math.toRadians((rotation - 90f).toDouble())
        val sx = cx + (radius + stroke * 1.05f) * cos(rad).toFloat()
        val sy = cy + (radius + stroke * 1.05f) * sin(rad).toFloat()
        paint.style = Paint.Style.FILL
        paint.color = (accent and 0x00FFFFFF) or (0x44 shl 24)
        canvas.drawCircle(sx, sy, h * 0.035f, paint)
        paint.color = accent
        canvas.drawCircle(sx, sy, h * 0.017f, paint)
    }
}
