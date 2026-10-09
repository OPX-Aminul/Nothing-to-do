package com.hairgrowth.os.ui.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.hairgrowth.os.R
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.data.ScheduleStore
import com.hairgrowth.os.reminder.ReminderScheduler
import com.hairgrowth.os.ui.Fonts
import com.hairgrowth.os.ui.Palette
import com.hairgrowth.os.ui.Ui
import com.hairgrowth.os.ui.views.SwitchView
import com.hairgrowth.os.util.Fx
import com.hairgrowth.os.util.TonePlayer

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private var accent = Palette.GOLD
    private var lastScrollY = 0

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        prefs = Prefs(ctx)
        accent = Palette.accent(prefs.accent)

        val sv = ScrollView(ctx).apply {
            setPadding(0, 0, 0, Ui.dp(ctx, 120f))
            setOnScrollChangeListener { _, _, y, _, _ ->
                (activity as? Host)?.dockOnScroll(y - lastScrollY)
                lastScrollY = y
            }
        }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 18f), 0, Ui.dp(ctx, 18f), Ui.dp(ctx, 24f))
        }
        sv.addView(root)

        root.addView(buildHead())
        root.addView(setRow(R.drawable.ic_mobile, getString(R.string.set_haptic), getString(R.string.set_haptic_sub)) {
            SwitchView(ctx).apply {
                setAccent(prefs.accent)
                setChecked(prefs.haptic)
                setOnChange { v ->
                    prefs.haptic = v
                    Fx.tap(ctx, 20)
                }
            }
        })
        val tones = TonePlayer(ctx)
        root.addView(setRow(R.drawable.ic_volume, getString(R.string.set_sound), getString(R.string.set_sound_sub)) {
            SwitchView(ctx).apply {
                setAccent(prefs.accent)
                setChecked(prefs.sound)
                setOnChange { v ->
                    prefs.sound = v
                    if (v) tones.play("complete")
                }
            }
        })
        root.addView(languageRow())
        root.addView(plainRow(R.drawable.ic_palette, getString(R.string.set_accent), getString(R.string.set_accent_sub)))
        root.addView(accentPicker())
        root.addView(customizeRow())
        root.addView(resetWeekRow())
        root.addView(resetProtocolRow())
        root.addView(plainRow(R.drawable.ic_shield, getString(R.string.set_privacy), getString(R.string.set_privacy_sub)))
        return sv
    }

    private fun buildHead(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Ui.dp(ctx, 6f)
                bottomMargin = Ui.dp(ctx, 14f)
            }
        }
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_sliders)
            imageTintList = ColorStateList.valueOf(accent)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f)))
        row.addView(TextView(ctx).apply {
            text = "  " + getString(R.string.set_title)
            typeface = Fonts.bnSemi(ctx)
            textSize = 18f
            setTextColor(Palette.TEXT)
        })
        return row
    }

    private fun rowShell(icon: Int, title: String, sub: String): LinearLayout {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 15f), Ui.dp(ctx, 15f), Ui.dp(ctx, 15f), Ui.dp(ctx, 15f))
            background = Ui.round(Palette.PANEL, Ui.fdp(ctx, 16f), 1, Palette.LINE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = Ui.dp(ctx, 10f) }
        }
        row.addView(AppCompatImageView(ctx).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(Palette.GOLD_SOFT)
            background = Ui.round(withAlpha(accent, 0x1A), Ui.fdp(ctx, 11f), 1, withAlpha(accent, 0x38))
            setPadding(Ui.dp(ctx, 9f), Ui.dp(ctx, 9f), Ui.dp(ctx, 9f), Ui.dp(ctx, 9f))
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 38f), Ui.dp(ctx, 38f)))
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 13f), 0, Ui.dp(ctx, 8f), 0)
        }
        col.addView(TextView(ctx).apply {
            text = title
            typeface = Fonts.bnSemi(ctx)
            textSize = 14f
            setTextColor(Palette.TEXT)
        })
        col.addView(TextView(ctx).apply {
            text = sub
            typeface = Fonts.bn(ctx)
            textSize = 11.5f
            setTextColor(Palette.MUTED)
            setPadding(0, Ui.dp(ctx, 2f), 0, 0)
        })
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        return row
    }

    private fun setRow(icon: Int, title: String, sub: String, switchFactory: () -> View): View {
        val row = rowShell(icon, title, sub)
        row.addView(switchFactory())
        return row
    }

    private fun plainRow(icon: Int, title: String, sub: String): View =
        rowShell(icon, title, sub).apply { isClickable = false }

    private fun languageRow(): View {
        val ctx = requireContext()
        val row = rowShell(
            R.drawable.ic_sparkle, getString(R.string.set_language),
            getString(R.string.set_language_sub)
        )
        val current = when (prefs.language) {
            "bn" -> "বাংলা"
            "hi" -> "हिन्दी"
            else -> "English"
        }
        row.addView(TextView(ctx).apply {
            text = current
            typeface = Fonts.bn(ctx)
            textSize = 12.5f
            setTextColor(accent)
        })
        row.isClickable = true
        row.setOnClickListener {
            Fx.tap(ctx, 20)
            val labels = arrayOf("English", "हिन्दी", "বাংলা")
            val values = arrayOf("en", "hi", "bn")
            val checked = values.indexOf(prefs.language).coerceAtLeast(0)
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.set_language))
                .setSingleChoiceItems(labels, checked) { dialog, which ->
                    val tag = values[which]
                    prefs.language = tag
                    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show()
        }
        return row
    }

    private fun accentPicker(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(Ui.dp(ctx, 4f), 0, Ui.dp(ctx, 4f), Ui.dp(ctx, 16f))
        }
        Prefs.ACCENTS.forEach { hex ->
            val c = Palette.accent(hex)
            val dot = View(ctx).apply {
                background = Ui.circle(
                    c, Ui.dp(ctx, 2f),
                    if (prefs.accent == hex) android.graphics.Color.WHITE else c
                )
                isClickable = true
                setOnClickListener {
                    Fx.tap(ctx, 20)
                    prefs.accent = hex
                    (activity as? Host)?.showToast(getString(R.string.toast_accent))
                    activity?.recreate()
                }
            }
            row.addView(dot, LinearLayout.LayoutParams(Ui.dp(ctx, 32f), Ui.dp(ctx, 32f)).apply {
                marginEnd = Ui.dp(ctx, 12f)
            })
        }
        return row
    }

    private fun customizeRow(): View {
        val ctx = requireContext()
        val row = rowShell(
            R.drawable.ic_grid, getString(R.string.set_customize), getString(R.string.set_customize_sub)
        )
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_chevron)
            imageTintList = ColorStateList.valueOf(Palette.MUTED2)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 18f), Ui.dp(ctx, 18f)))
        row.isClickable = true
        row.setOnClickListener {
            Fx.tap(ctx, 20)
            (activity as? Host)?.showToast(getString(R.string.set_customize_sub))
            (activity as? Host)?.goHomeForEditing()
        }
        return row
    }

    private fun resetWeekRow(): View {
        val ctx = requireContext()
        val row = rowShell(
            R.drawable.ic_reset, getString(R.string.set_reset_week), getString(R.string.set_reset_week_sub)
        )
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_chevron)
            imageTintList = ColorStateList.valueOf(Palette.MUTED2)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 18f), Ui.dp(ctx, 18f)))
        row.isClickable = true
        row.setOnClickListener {
            Fx.tap(ctx, 40)
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.dialog_reset_title))
                .setMessage(getString(R.string.dialog_reset_msg))
                .setPositiveButton(getString(R.string.dialog_reset_ok)) { _, _ ->
                    prefs.resetWeek()
                    (activity as? Host)?.showToast(getString(R.string.toast_week_reset))
                    activity?.recreate()
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show()
        }
        return row
    }

    private fun resetProtocolRow(): View {
        val ctx = requireContext()
        val row = rowShell(
            R.drawable.ic_shield, getString(R.string.reset_protocol), getString(R.string.reset_protocol_sub)
        )
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_chevron)
            imageTintList = ColorStateList.valueOf(Palette.MUTED2)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 18f), Ui.dp(ctx, 18f)))
        row.isClickable = true
        row.setOnClickListener {
            Fx.tap(ctx, 40)
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.dialog_reset_protocol_title))
                .setMessage(getString(R.string.dialog_reset_protocol_msg))
                .setPositiveButton(getString(R.string.dialog_reset_ok)) { _, _ ->
                    ScheduleStore(ctx).reset()
                    prefs.resetWeek()
                    ReminderScheduler.sync(ctx)
                    (activity as? Host)?.showToast(getString(R.string.toast_protocol_reset))
                    activity?.recreate()
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show()
        }
        return row
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}
