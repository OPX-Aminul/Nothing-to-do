package com.hairgrowth.os.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.hairgrowth.os.ui.Palette
import kotlin.math.cos
import kotlin.math.sin

/** Lightweight confetti burst (reference used canvas-confetti). */
class ConfettiView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private class P {
        var x = 0f
        var y = 0f
        var vx = 0f
        var vy = 0f
        var rot = 0f
        var vr = 0f
        var w = 0f
        var h = 0f
        var color = 0
        var life = 1f
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val parts = ArrayList<P>(200)
    private val colors = intArrayOf(
        Palette.GOLD_SOFT, Palette.GOLD, Palette.MINT, Palette.ROSE, Palette.ICE
    )
    private var running = false
    private val dpr: Float = resources.displayMetrics.density

    init {
        isClickable = false
        isFocusable = false
    }

    fun burst() {
        parts.clear()
        val w = if (width > 0) width else resources.displayMetrics.widthPixels
        val h = if (height > 0) height else resources.displayMetrics.heightPixels
        repeat(160) {
            val p = P()
            p.x = w * (0.15f + Math.random().toFloat() * 0.7f)
            p.y = h * (0.30f + Math.random().toFloat() * 0.12f)
            val angle = (-110f + Math.random().toFloat() * 40f) * Math.PI.toFloat() / 180f
            val speed = (7f + Math.random().toFloat() * 10f) * dpr
            p.vx = cos(angle) * speed
            p.vy = sin(angle) * speed
            p.rot = Math.random().toFloat() * 360f
            p.vr = (Math.random().toFloat() - 0.5f) * 22f
            p.w = (5f + Math.random().toFloat() * 6f) * dpr
            p.h = (8f + Math.random().toFloat() * 8f) * dpr
            p.color = colors[(Math.random() * colors.size).toInt().coerceIn(0, colors.size - 1)]
            p.life = 1f
            parts.add(p)
        }
        if (!running) {
            running = true
            postOnAnimation(frame)
        }
    }

    private val frame = object : Runnable {
        override fun run() {
            var alive = false
            for (p in parts) {
                if (p.life <= 0f) continue
                p.vy += 0.34f * dpr
                p.vx *= 0.992f
                p.vy *= 0.995f
                p.x += p.vx
                p.y += p.vy
                p.rot += p.vr
                p.life -= 0.0075f
                if (p.life > 0f && p.y < height + 60f) alive = true
            }
            invalidate()
            if (alive) postOnAnimation(this) else running = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        for (p in parts) {
            if (p.life <= 0f) continue
            canvas.save()
            canvas.rotate(p.rot, p.x, p.y)
            rect.set(p.x - p.w / 2f, p.y - p.h / 2f, p.x + p.w / 2f, p.y + p.h / 2f)
            paint.color = p.color
            paint.alpha = (255 * p.life.coerceIn(0f, 1f)).toInt()
            canvas.drawRoundRect(rect, p.w * 0.25f, p.w * 0.25f, paint)
            canvas.restore()
        }
        canvas.restore()
    }
}
