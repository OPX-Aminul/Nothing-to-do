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
import com.hairgrowth.os.reminder.NotificationHelper
import com.hairgrowth.os.reminder.ReminderScheduler
import com.hairgrowth.os.ui.Fonts
import com.hairgrowth.os.ui.Palette
import com.hairgrowth.os.ui.Ui
import com.hairgrowth.os.ui.views.SwitchView
import com.hairgrowth.os.util.Fx
import com.hairgrowth.os.util.SoundEngine

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var sounds: SoundEngine
    private var accent = Palette.GOLD

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        prefs = Prefs(ctx)
        sounds = SoundEngine(ctx)
        accent = Palette.accent(prefs.accent)

        val sv = ScrollView(ctx).apply {
            setPadding(0, 0, 0, Ui.dp(ctx, 130f))
        }
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 18f), 0, Ui.dp(ctx, 18f), Ui.dp(ctx, 24f))
        }
        sv.addView(root)

        root.addView(buildHead())
        root.addView(appearanceRow())
        root.addView(languageRow())
        root.addView(plainRow(R.drawable.ic_palette, getString(R.string.set_accent), getString(R.string.set_accent_sub)))
        root.addView(accentPicker())
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
        root.addView(setRow(R.drawable.ic_sparkle, getString(R.string.set_sound), getString(R.string.set_sound_sub)) {
            SwitchView(ctx).apply {
                setAccent(prefs.accent)
                setChecked(prefs.sound)
                setOnChange { v ->
                    prefs.sound = v
                    if (v) sounds.complete()
                }
            }
        })
        root.addView(alarmSoundRow())
        root.addView(customizeRow())
        root.addView(resetWeekRow())
        root.addView(resetProtocolRow())
        root.addView(plainRow(R.drawable.ic_shield, getString(R.string.set_privacy), getString(R.string.set_privacy_sub)))
        Ui.animIn(root, dy = 10f)
        return sv
    }

    private fun buildHead(): View {
        val ctx = requireContext()
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Ui.dp(ctx, 6f)
                bottomMargin = Ui.dp(ctx, 16f)
            }
        }
        col.addView(TextView(ctx).apply {
            text = getString(R.string.set_title)
            typeface = Fonts.bnSemi(ctx)
            textSize = 22f
            setTextColor(Palette.TEXT)
        })
        col.addView(Ui.underline(ctx, accent), LinearLayout.LayoutParams(
            Ui.dp(ctx, 34f), Ui.dp(ctx, 2.5f)
        ).apply { topMargin = Ui.dp(ctx, 10f) })
        return col
    }

    private fun rowShell(icon: Int, title: String, sub: String): LinearLayout {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 15f), Ui.dp(ctx, 15f), Ui.dp(ctx, 15f), Ui.dp(ctx, 15f))
            background = Ui.card(ctx, 18f)
            elevation = Ui.fdp(ctx, 4f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = Ui.dp(ctx, 10f) }
            foreground = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(withAlpha(Palette.TEXT, 0x1E)), null,
                Ui.round(android.graphics.Color.WHITE, Ui.fdp(ctx, 18f))
            )
        }
        row.addView(AppCompatImageView(ctx).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(Palette.GOLD_SOFT)
            background = Ui.round(withAlpha(accent, 0x1A), Ui.fdp(ctx, 12f), 1, withAlpha(accent, 0x38))
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

    /** Value label used on the right of a tappable row. */
    private fun valueText(text: String): TextView {
        val ctx = requireContext()
        return TextView(ctx).apply {
            this.text = text
            typeface = Fonts.bnSemi(ctx)
            textSize = 12.5f
            setTextColor(accent)
        }
    }

    // ------------------------------------------------------------ appearance

    private fun appearanceRow(): View {
        val ctx = requireContext()
        val row = rowShell(
            R.drawable.ic_moon, getString(R.string.set_appearance),
            getString(R.string.set_appearance_sub)
        )
        val value = valueText(appearanceLabel(prefs.themeMode))
        row.addView(value)
        row.isClickable = true
        row.setOnClickListener {
            Fx.tap(ctx, 20)
            sounds.tap()
            val labels = arrayOf(
                getString(R.string.appear_system),
                getString(R.string.appear_light),
                getString(R.string.appear_dark)
            )
            val checked = Prefs.THEMES.indexOf(prefs.themeMode).coerceAtLeast(0)
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.set_appearance))
                .setSingleChoiceItems(labels, checked) { dialog, which ->
                    val mode = Prefs.THEMES[which]
                    prefs.themeMode = mode
                    AppCompatDelegate.setDefaultNightMode(nightMode(mode))
                    dialog.dismiss()
                    (activity as? Host)?.showToast(getString(R.string.toast_appearance))
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show()
        }
        return row
    }

    private fun appearanceLabel(mode: String): String = getString(
        when (mode) {
            "light" -> R.string.appear_light
            "dark" -> R.string.appear_dark
            else -> R.string.appear_system
        }
    )

    private fun nightMode(mode: String): Int = when (mode) {
        "light" -> AppCompatDelegate.MODE_NIGHT_NO
        "dark" -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    // --------------------------------------------------------- reminder sound

    private fun alarmSoundRow(): View {
        val ctx = requireContext()
        val row = rowShell(
            R.drawable.ic_bell, getString(R.string.set_alarm_sound),
            getString(R.string.set_alarm_sound_sub)
        )
        val value = valueText(getString(SoundEngine.labelRes(prefs.alarmSound)))
        row.addView(value)
        row.isClickable = true
        row.setOnClickListener {
            Fx.tap(ctx, 20)
            val labels = SoundEngine.ALARMS
                .map { getString(SoundEngine.labelRes(it)) }
                .toTypedArray()
            val checked = SoundEngine.ALARMS.indexOf(prefs.alarmSound).coerceAtLeast(0)
            AlertDialog.Builder(ctx)
                .setTitle(getString(R.string.set_alarm_sound))
                .setMessage(getString(R.string.rem_test_hint))
                .setSingleChoiceItems(labels, checked) { dialog, which ->
                    val key = SoundEngine.ALARMS[which]
                    prefs.alarmSound = key
                    sounds.preview(key)
                    NotificationHelper.ensureChannels(ctx)
                    value.text = getString(SoundEngine.labelRes(key))
                    (activity as? Host)?.showToast(getString(R.string.toast_alarm_sound))
                    dialog.dismiss()
                }
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show()
        }
        return row
    }

    // --------------------------------------------------------------- language

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
        row.addView(valueText(current))
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
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 16f), Ui.dp(ctx, 14f), Ui.dp(ctx, 16f), Ui.dp(ctx, 14f))
            background = Ui.card(ctx, 18f)
            elevation = Ui.fdp(ctx, 4f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = Ui.dp(ctx, 10f) }
        }
        Prefs.ACCENTS.forEach { hex ->
            val c = Palette.accent(hex)
            val dot = View(ctx).apply {
                background = Ui.circle(
                    withAlpha(c, if (prefs.accent == hex) 0xFF else 0x55),
                    Ui.dp(ctx, 2f),
                    if (prefs.accent == hex) android.graphics.Color.WHITE else withAlpha(c, 0x99)
                )
                isClickable = true
                elevation = Ui.fdp(ctx, if (prefs.accent == hex) 6f else 2f)
                setOnClickListener {
                    if (prefs.accent == hex) return@setOnClickListener
                    Fx.tap(ctx, 20)
                    sounds.tap()
                    prefs.accent = hex
                    (activity as? Host)?.showToast(getString(R.string.toast_accent))
                    activity?.recreate()
                }
            }
            card.addView(dot, LinearLayout.LayoutParams(Ui.dp(ctx, 30f), Ui.dp(ctx, 30f)).apply {
                marginEnd = Ui.dp(ctx, 14f)
            })
        }
        card.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        card.addView(TextView(ctx).apply {
            text = getString(R.string.set_accent_sub)
            typeface = Fonts.bn(ctx)
            textSize = 11f
            setTextColor(Palette.MUTED)
        })
        return card
    }

    // ------------------------------------------------------------------ rows

    private fun chevronRow(
        icon: Int, title: String, sub: String, confirmTitle: String?, confirmMsg: String?,
        action: () -> Unit
    ): View {
        val ctx = requireContext()
        val row = rowShell(icon, title, sub)
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_chevron)
            imageTintList = ColorStateList.valueOf(Palette.MUTED2)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 18f), Ui.dp(ctx, 18f)))
        row.isClickable = true
        row.setOnClickListener {
            Fx.tap(ctx, 30)
            sounds.tap()
            if (confirmTitle == null) {
                action()
            } else {
                AlertDialog.Builder(ctx)
                    .setTitle(confirmTitle)
                    .setMessage(confirmMsg)
                    .setPositiveButton(getString(R.string.dialog_reset_ok)) { _, _ -> action() }
                    .setNegativeButton(getString(R.string.dialog_cancel), null)
                    .show()
            }
        }
        return row
    }

    private fun customizeRow(): View = chevronRow(
        R.drawable.ic_grid, getString(R.string.set_customize), getString(R.string.set_customize_sub),
        null, null
    ) {
        (activity as? Host)?.showToast(getString(R.string.set_customize_sub))
        (activity as? Host)?.goHomeForEditing()
    }

    private fun resetWeekRow(): View {
        val ctx = requireContext()
        return chevronRow(
            R.drawable.ic_reset, getString(R.string.set_reset_week), getString(R.string.set_reset_week_sub),
            getString(R.string.dialog_reset_title), getString(R.string.dialog_reset_msg)
        ) {
            prefs.resetWeek()
            (activity as? Host)?.showToast(getString(R.string.toast_week_reset))
            activity?.recreate()
        }
    }

    private fun resetProtocolRow(): View {
        val ctx = requireContext()
        return chevronRow(
            R.drawable.ic_shield, getString(R.string.reset_protocol), getString(R.string.reset_protocol_sub),
            getString(R.string.dialog_reset_protocol_title), getString(R.string.dialog_reset_protocol_msg)
        ) {
            ScheduleStore(ctx).reset()
            prefs.resetWeek()
            ReminderScheduler.sync(ctx)
            (activity as? Host)?.showToast(getString(R.string.toast_protocol_reset))
            activity?.recreate()
        }
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)

    override fun onDestroyView() {
        super.onDestroyView()
        sounds.release()
    }
}
