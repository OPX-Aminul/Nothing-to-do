package com.hairgrowth.os.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import androidx.core.content.res.ResourcesCompat
import com.hairgrowth.os.R

/** Colour tokens matching the reference design. */
object Palette {
    val VOID = 0xFF07080A.toInt()
    val INK = 0xFF0C0E12.toInt()
    val TEXT = 0xFFF5F1E8.toInt()
    val MUTED = 0xFF8F8A80.toInt()
    val MUTED2 = 0xFF625E57.toInt()
    val GOLD = 0xFFD4AF7A.toInt()
    val GOLD_SOFT = 0xFFE7CFA4.toInt()
    val MINT = 0xFF7EE0C3.toInt()
    val ROSE = 0xFFE8A0B4.toInt()
    val ICE = 0xFF9EC5FF.toInt()
    val DANGER = 0xFFFF6B6B.toInt()
    val LINE = 0x14FFFFFF
    val LINE_STRONG = 0x24FFFFFF
    val PANEL = 0xB014161C.toInt()

    fun accent(hex: String): Int = try {
        Color.parseColor(hex)
    } catch (_: Throwable) {
        GOLD
    }

    fun accentSoft(hex: String): Int = when (hex.lowercase()) {
        "#7ee0c3" -> 0xFFA8F0D8.toInt()
        "#e8a0b4" -> 0xFFF6C4D1.toInt()
        "#9ec5ff" -> 0xFFC4DBFF.toInt()
        else -> GOLD_SOFT
    }
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
}
