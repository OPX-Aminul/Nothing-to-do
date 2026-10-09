package com.hairgrowth.os.ui.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import com.hairgrowth.os.ui.Palette

/** Pill switch (44x26dp in the reference). */
class SwitchView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(ctx, attrs, defStyle) {

    private val track = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumb = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private var pos = 0f
    private var accent = Palette.GOLD

    var isOn: Boolean = false
        private set

    private var listener: ((Boolean) -> Unit)? = null

    init {
        setOnClickListener { setChecked(!isOn, animate = true, notify = true) }
        isClickable = true
    }

    fun setAccent(hex: String) {
        accent = Palette.accent(hex)
        invalidate()
    }

    fun setOnChange(l: (Boolean) -> Unit) {
        listener = l
    }

    fun setChecked(on: Boolean, animate: Boolean = false, notify: Boolean = false) {
        isOn = on
        val to = if (on) 1f else 0f
        if (animate && isAttachedToWindow) {
            ValueAnimator.ofFloat(pos, to).apply {
                duration = 280L
                interpolator = OvershootInterpolator(1.6f)
                addUpdateListener {
                    pos = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        } else {
            pos = to
            invalidate()
        }
        if (notify) listener?.invoke(on)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        val w = (46 * d).toInt()
        val h = (27 * d).toInt()
        setMeasuredDimension(
            resolveSize(w, widthMeasureSpec),
            resolveSize(h, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val w = width.toFloat()
        val h = height.toFloat()
        val radius = h / 2f

        val off = 0x1AFFFFFF
        track.color = blend(off, accent, pos)
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, radius, radius, track)

        val pad = 3f * d
        val thumbR = radius - pad
        val cx = pad + thumbR + (w - 2f * (pad + thumbR)) * pos
        val cy = h / 2f
        if (isOn) {
            thumb.color = (accent and 0x00FFFFFF) or (0x55 shl 24)
            canvas.drawCircle(cx, cy, thumbR * 1.35f, thumb)
        }
        thumb.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(cx, cy, thumbR, thumb)
    }

    private fun blend(from: Int, to: Int, f: Float): Int {
        val a = ((from ushr 24 and 0xFF) + ((to ushr 24 and 0xFF) - (from ushr 24 and 0xFF)) * f).toInt()
        val r = ((from ushr 16 and 0xFF) + ((to ushr 16 and 0xFF) - (from ushr 16 and 0xFF)) * f).toInt()
        val g = ((from ushr 8 and 0xFF) + ((to ushr 8 and 0xFF) - (from ushr 8 and 0xFF)) * f).toInt()
        val b = ((from and 0xFF) + ((to and 0xFF) - (from and 0xFF)) * f).toInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
