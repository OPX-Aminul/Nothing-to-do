package com.hairgrowth.os.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import com.hairgrowth.os.ui.Palette

/** The three drifting glow blobs from the reference background. */
class BlobView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var t = 0f
    private var accent = Palette.GOLD
    private var animator: ValueAnimator? = null

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    fun setAccent(hex: String) {
        accent = Palette.accent(hex)
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animator == null) {
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 24000L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = LinearInterpolator()
                addUpdateListener {
                    t = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    private fun blob(x: Float, y: Float, r: Float, color: Int, alpha: Int) {
        if (r <= 1f) return
        paint.shader = RadialGradient(
            x, y, r,
            intArrayOf((color and 0x00FFFFFF) or (alpha shl 24), (color and 0x00FFFFFF)),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        drawCircleSafely(x, y, r)
    }

    private fun drawCircleSafely(x: Float, y: Float, r: Float) {
        canvasRef?.drawCircle(x, y, r, paint)
    }

    private var canvasRef: Canvas? = null

    override fun onDraw(canvas: Canvas) {
        canvasRef = canvas
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val drift = (t - 0.5f) * 2f
        // blob 1 — accent, top-left
        blob(
            w * 0.10f + drift * 30f,
            h * 0.06f + drift * 24f,
            w * 0.62f,
            accent,
            92
        )
        // blob 2 — mint, bottom-right
        blob(
            w * 0.92f - drift * 34f,
            h * 0.94f - drift * 20f,
            w * 0.55f,
            Palette.MINT,
            58
        )
        // blob 3 — rose, centre
        blob(
            w * 0.58f + drift * 16f,
            h * 0.46f + drift * 26f,
            w * 0.45f,
            Palette.ROSE,
            44
        )
        canvasRef = null
    }
}
