package com.hairgrowth.os.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import androidx.core.content.res.ResourcesCompat
import com.hairgrowth.os.R

/**
 * Colour tokens. Every value is resolved for the current day/night mode, so the
 * whole UI follows the system theme (or the user's manual choice) — set
 * [dark] once per activity before building views.
 */
object Palette {

    var dark: Boolean = true

    fun applyConfiguration(config: Configuration) {
        dark = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
    }

    // --- surfaces ---
    val VOID get() = if (dark) 0xFF07080A.toInt() else 0xFFF7F4EE.toInt()
    val INK get() = if (dark) 0xFF0C0E12.toInt() else 0xFFFFFFFF.toInt()
    val PANEL get() = if (dark) 0xC014161C.toInt() else 0xF5FFFFFF.toInt()
    val PANEL_TOP get() = if (dark) 0xD81B1E26.toInt() else 0xFFFFFFFF.toInt()
    val PANEL_BOTTOM get() = if (dark) 0xB0111319.toInt() else 0xEDF0EBE1.toInt()
    val DOCK_BG get() = if (dark) 0xE0121419.toInt() else 0xF5FFFFFF.toInt()
    val TOAST_BG get() = if (dark) 0xF0181A20.toInt() else 0xF5FFFFFF.toInt()
    val CHIP_BG get() = if (dark) 0x0DFFFFFF else 0x0D000000
    val NOTE_BG get() = if (dark) 0x08FFFFFF else 0x0A000000

    // --- lines & effects ---
    val LINE get() = if (dark) 0x14FFFFFF else 0x16000000
    val LINE_STRONG get() = if (dark) 0x24FFFFFF else 0x22000000
    val TRACK get() = if (dark) 0x14FFFFFF else 0x16000000
    val RIPPLE get() = if (dark) 0x22FFFFFF else 0x1A000000
    val GLOW get() = if (dark) 0x2AFFFFFF else 0x14000000

    // --- text ---
    val TEXT get() = if (dark) 0xFFF5F1E8.toInt() else 0xFF15171D.toInt()
    val MUTED get() = if (dark) 0xFF8F8A80.toInt() else 0xFF6E6A62.toInt()
    val MUTED2 get() = if (dark) 0xFF625E57.toInt() else 0xFFA39E94.toInt()

    // --- semantic accents (contrast-corrected for light mode) ---
    val GOLD get() = if (dark) 0xFFD4AF7A.toInt() else 0xFFA9791F.toInt()
    val GOLD_SOFT get() = if (dark) 0xFFE7CFA4.toInt() else 0xFF8A6116.toInt()
    val MINT get() = if (dark) 0xFF7EE0C3.toInt() else 0xFF1B8A69.toInt()
    val ROSE get() = if (dark) 0xFFE8A0B4.toInt() else 0xFFB85275.toInt()
    val ICE get() = if (dark) 0xFF9EC5FF.toInt() else 0xFF3C6BAE.toInt()
    val DANGER get() = if (dark) 0xFFFF6B6B.toInt() else 0xFFC0392B.toInt()

    /** Resolves one of the four accent choices for the current mode. */
    fun accent(hex: String): Int = when (hex.lowercase()) {
        "#7ee0c3" -> MINT
        "#e8a0b4" -> ROSE
        "#9ec5ff" -> ICE
        else -> GOLD
    }

    fun accentSoft(hex: String): Int = when (hex.lowercase()) {
        "#7ee0c3" -> if (dark) 0xFFA8F0D8.toInt() else 0xFF0F6B4F.toInt()
        "#e8a0b4" -> if (dark) 0xFFF6C4D1.toInt() else 0xFF9A3A5A.toInt()
        "#9ec5ff" -> if (dark) 0xFFC4DBFF.toInt() else 0xFF28548F.toInt()
        else -> GOLD_SOFT
    }

    /** Soft tinted fill used behind icons and chips. */
    fun tint(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}

/** Bundled typefaces (Bodoni Moda / Plus Jakarta Sans / Hind Siliguri). */
object Fonts {
    private var display: Typeface? = null
    private var sans: Typeface? = null
    private var sansSemi: Typeface? = null
    private var sansX: Typeface? = null
    private var bn: Typeface? = null
    private var bnMed: Typeface? = null
    private var bnSemi: Typeface? = null
    private var bnBold: Typeface? = null

    fun display(c: Context): Typeface? =
        display ?: ResourcesCompat.getFont(c, R.font.bodoni_moda_bold).also { display = it }

    fun sans(c: Context): Typeface? =
        sans ?: ResourcesCompat.getFont(c, R.font.plus_jakarta_regular).also { sans = it }

    fun sansSemi(c: Context): Typeface? =
        sansSemi ?: ResourcesCompat.getFont(c, R.font.plus_jakarta_semibold).also { sansSemi = it }

    fun sansX(c: Context): Typeface? =
        sansX ?: ResourcesCompat.getFont(c, R.font.plus_jakarta_extrabold).also { sansX = it }

    fun bn(c: Context): Typeface? =
        bn ?: ResourcesCompat.getFont(c, R.font.hind_siliguri_regular).also { bn = it }

    fun bnMed(c: Context): Typeface? =
        bnMed ?: ResourcesCompat.getFont(c, R.font.hind_siliguri_medium).also { bnMed = it }

    fun bnSemi(c: Context): Typeface? =
        bnSemi ?: ResourcesCompat.getFont(c, R.font.hind_siliguri_semibold).also { bnSemi = it }

    fun bnBold(c: Context): Typeface? =
        bnBold ?: ResourcesCompat.getFont(c, R.font.hind_siliguri_bold).also { bnBold = it }
}

object Ui {

    fun dp(c: Context, v: Float): Int = (v * c.resources.displayMetrics.density).toInt()
    fun fdp(c: Context, v: Float): Float = v * c.resources.displayMetrics.density

    fun round(
        fill: Int,
        radiusPx: Float,
        strokeWidth: Int = 0,
        strokeColor: Int = 0
    ): GradientDrawable = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radiusPx
        setColor(fill)
        if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
    }

    /**
     * Premium card surface: a soft vertical gradient instead of a flat fill,
     * with a hairline border so panels read as raised, layered glass.
     */
    fun card(c: Context, radiusDp: Float, strokeColor: Int = Palette.LINE): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(
            Palette.PANEL_TOP, Palette.PANEL_BOTTOM
        )).apply {
            cornerRadius = fdp(c, radiusDp)
            gradientType = GradientDrawable.LINEAR_GRADIENT
            setStroke(dp(c, 1f), strokeColor)
        }

    fun circle(fill: Int, strokeWidth: Int = 0, strokeColor: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fill)
            if (strokeWidth > 0) setStroke(strokeWidth, strokeColor)
        }

    fun linear(colors: IntArray, radiusPx: Float): GradientDrawable =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, colors).apply {
            cornerRadius = radiusPx
            gradientType = GradientDrawable.LINEAR_GRADIENT
        }

    /** Border-only rounded surface (used for "+ Add routine"). */
    fun outline(c: Context, radiusDp: Float, color: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = fdp(c, radiusDp)
            setColor(Color.TRANSPARENT)
            setStroke(dp(c, 1f), color)
        }

    /** Springy press-down feedback for tappable surfaces. */
    fun pressScale(vararg views: View) {
        views.forEach { v ->
            v.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(view: View, event: MotionEvent): Boolean {
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN ->
                            view.animate().scaleX(0.972f).scaleY(0.972f)
                                .setDuration(90).start()
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                            view.animate().scaleX(1f).scaleY(1f).setDuration(230)
                                .setInterpolator(OvershootInterpolator(2.2f)).start()
                    }
                    return false
                }
            })
        }
    }

    /** Fade + rise a view in, used when swapping screen content. */
    fun animIn(view: View, dy: Float = 14f) {
        view.alpha = 0f
        view.translationY = view.resources.displayMetrics.density * dy
        view.animate().alpha(1f).translationY(0f).setDuration(360)
            .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
    }

    /** Soft card shadow (Android 5+ uses elevation with the background outline). */
    fun elevate(view: View, dpValue: Float = 5f) {
        view.elevation = dpValue * view.resources.displayMetrics.density
    }

    /** Animated accent underline that grows in from the left. */
    fun underline(c: Context, color: Int, widthDp: Float = 34f): View =
        View(c).apply {
            background = round(color, fdp(c, 2f))
            pivotX = 0f
            scaleX = 0f
            post {
                animate().scaleX(1f).setDuration(620)
                    .setInterpolator(android.view.animation.DecelerateInterpolator()).start()
            }
            layoutParams = android.widget.LinearLayout.LayoutParams(
                dp(c, widthDp), dp(c, 2.5f)
            )
        }
}
