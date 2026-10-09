package com.hairgrowth.os.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.hairgrowth.os.ui.Palette

/** Draws the three gold strands progressively, like the reference splash. */
class SplashMarkView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paths = ArrayList<Path>()
    private val lengths = ArrayList<Float>()
    private var t = 0f
    private var animator: ValueAnimator? = null

    init {
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 5f * resources.displayMetrics.density
        glow.style = Paint.Style.STROKE
        glow.strokeCap = Paint.Cap.ROUND
        glow.strokeWidth = 14f * resources.displayMetrics.density
        val raw = listOf(
            floatArrayOf(0.28f, 0.78f, 0.42f, 0.58f, 0.58f, 0.42f, 0.72f, 0.22f),
            floatArrayOf(0.40f, 0.82f, 0.54f, 0.62f, 0.68f, 0.46f, 0.84f, 0.26f),
            floatArrayOf(0.16f, 0.72f, 0.30f, 0.52f, 0.46f, 0.36f, 0.60f, 0.16f)
        )
        for (r in raw) {
            val p = Path()
            p.moveTo(r[0], r[1])
            p.cubicTo(r[2], r[3], r[4], r[5], r[6], r[7])
            paths.add(p)
            lengths.add(PathMeasure(p, false).length)
        }
    }

    fun start() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1500L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                t = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (t == 0f) start()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val minSide = if (w < h) w else h
        val scale = minSide * 0.92f
        val dx = (w - scale) / 2f - minSide * 0.12f
        val dy = (h - scale) / 2f - minSide * 0.12f

        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(scale, scale)

        paint.shader = LinearGradient(
            0.2f, 0.2f, 0.9f, 0.9f,
            intArrayOf(Palette.GOLD_SOFT, Palette.GOLD, Palette.ROSE),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        glow.color = (Palette.GOLD and 0x00FFFFFF) or (0x22 shl 24)

        for (i in paths.indices) {
            val delay = i * 0.12f
            val local = ((t - delay) / (1f - delay)).coerceIn(0f, 1f)
            if (local <= 0f) continue
            val pm = PathMeasure(paths[i], false)
            val seg = Path()
            pm.getSegment(0f, pm.length * local, seg, true)
            canvas.drawPath(seg, glow)
            canvas.drawPath(seg, paint)
        }

        // dashed ring
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 0.03f
            color = (Palette.MINT and 0x00FFFFFF) or (0x66 shl 24)
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(0.05f, 0.06f), 0f)
        }
        canvas.drawCircle(0.5f, 0.5f, 0.44f, ring)

        canvas.restore()
    }
}
