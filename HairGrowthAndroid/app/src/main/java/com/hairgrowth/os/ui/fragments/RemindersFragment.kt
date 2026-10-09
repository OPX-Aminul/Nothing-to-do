package com.hairgrowth.os.ui.fragments

import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.hairgrowth.os.R
import com.hairgrowth.os.data.Day
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.data.Schedule
import com.hairgrowth.os.data.ScheduleStore
import com.hairgrowth.os.reminder.NotificationHelper
import com.hairgrowth.os.reminder.ReminderScheduler
import com.hairgrowth.os.ui.EditDialogs
import com.hairgrowth.os.ui.Fonts
import com.hairgrowth.os.ui.Palette
import com.hairgrowth.os.ui.Ui
import com.hairgrowth.os.ui.views.SwitchView
import com.hairgrowth.os.util.Fx
import com.hairgrowth.os.util.TonePlayer

class RemindersFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var store: ScheduleStore
    private lateinit var tones: TonePlayer
    private var accent = Palette.GOLD
    private lateinit var days: List<Day>
    private var countChip: TextView? = null
    private var banner: LinearLayout? = null
    private var bannerIcon: ImageView? = null
    private var bannerSub: TextView? = null
    private var bannerBtn: TextView? = null
    private var listWrap: LinearLayout? = null
    private var lastScrollY = 0

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        prefs = Prefs(ctx)
        store = ScheduleStore(ctx)
        tones = TonePlayer(ctx)
        accent = Palette.accent(prefs.accent)
        days = store.days()

        val sv = ScrollView(ctx).apply {
            isFillViewport = true
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
        root.addView(buildBanner())
        root.addView(buildActions())
        root.addView(buildList())
        root.addView(buildNote())
        updateBanner()
        updateCount()
        return sv
    }

    override fun onResume() {
        super.onResume()
        updateBanner()
    }

    private fun buildHead(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = Ui.dp(ctx, 14f) }
        }
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_bell)
            imageTintList = ColorStateList.valueOf(accent)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f)))
        row.addView(TextView(ctx).apply {
            text = "  " + getString(R.string.rem_title)
            typeface = Fonts.bnSemi(ctx)
            textSize = 18f
            setTextColor(Palette.TEXT)
        })
        row.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        countChip = TextView(ctx).apply {
            typeface = Fonts.bn(ctx)
            textSize = 10.5f
            setTextColor(Palette.MUTED)
            setPadding(Ui.dp(ctx, 10f), Ui.dp(ctx, 4f), Ui.dp(ctx, 10f), Ui.dp(ctx, 4f))
            background = Ui.round(Palette.PANEL, Ui.fdp(ctx, 20f), 1, Palette.LINE)
        }
        row.addView(countChip)
        return row
    }

    private fun buildBanner(): View {
        val ctx = requireContext()
        val wrap = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 13f), Ui.dp(ctx, 14f), Ui.dp(ctx, 13f))
        }
        bannerIcon = ImageView(ctx).apply { setImageResource(R.drawable.ic_bell) }
        card.addView(bannerIcon, LinearLayout.LayoutParams(Ui.dp(ctx, 18f), Ui.dp(ctx, 18f)))
        val tx = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 12f), 0, Ui.dp(ctx, 8f), 0)
        }
        tx.addView(TextView(ctx).apply {
            text = getString(R.string.notif_banner_title)
            typeface = Fonts.bnSemi(ctx)
            textSize = 13.5f
            setTextColor(Palette.TEXT)
        })
        bannerSub = TextView(ctx).apply {
            typeface = Fonts.bn(ctx)
            textSize = 11f
            setTextColor(Palette.MUTED)
            setPadding(0, Ui.dp(ctx, 2f), 0, 0)
        }
        tx.addView(bannerSub)
        card.addView(tx, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bannerBtn = TextView(ctx).apply {
            typeface = Fonts.bnSemi(ctx)
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(0xFF14161C.toInt())
            setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 9f), Ui.dp(ctx, 14f), Ui.dp(ctx, 9f))
            background = Ui.linear(intArrayOf(accent, accent), Ui.fdp(ctx, 11f))
            isClickable = true
            setOnClickListener { (activity as? Host)?.requestNotifPermission() }
        }
        card.addView(bannerBtn)
        banner = card
        wrap.addView(card)
        return wrap
    }

    private fun updateBanner() {
        val ctx = requireContext()
        val ok = NotificationHelper.canPost(ctx)
        if (ok) {
            banner?.background = Ui.linear(
                intArrayOf(withAlpha(Palette.MINT, 0x22), withAlpha(Palette.MINT, 0x06)),
                Ui.fdp(ctx, 16f)
            ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(Palette.MINT, 0x55)) }
            bannerIcon?.imageTintList = ColorStateList.valueOf(Palette.MINT)
            bannerSub?.text = getString(R.string.notif_banner_on)
            bannerBtn?.text = getString(R.string.btn_on)
            bannerBtn?.alpha = 0.5f
            bannerBtn?.isEnabled = false
        } else {
            banner?.background = Ui.linear(
                intArrayOf(withAlpha(accent, 0x22), withAlpha(accent, 0x06)),
                Ui.fdp(ctx, 16f)
            ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(accent, 0x55)) }
            bannerIcon?.imageTintList = ColorStateList.valueOf(Palette.GOLD_SOFT)
            bannerSub?.text = getString(R.string.notif_banner_off)
            bannerBtn?.text = getString(R.string.btn_enable)
            bannerBtn?.alpha = 1f
            bannerBtn?.isEnabled = true
        }

        val wrap = banner?.parent as? LinearLayout
        while (wrap != null && wrap.childCount > 1) wrap.removeViewAt(1)
        if (!ReminderScheduler.canExact(ctx)) {
            val hint = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
                background = Ui.round(
                    withAlpha(Palette.DANGER, 0x14), Ui.fdp(ctx, 14f), 1,
                    withAlpha(Palette.DANGER, 0x44)
                )
                isClickable = true
                setOnClickListener { (activity as? Host)?.openExactAlarmSettings() }
            }
            hint.addView(TextView(ctx).apply {
                text = "⏰ " + getString(R.string.exact_alarm_hint)
                typeface = Fonts.bn(ctx)
                textSize = 11.5f
                setTextColor(Palette.DANGER)
            })
            wrap?.addView(hint, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = Ui.dp(ctx, 10f) })
        }
    }

    private fun buildActions(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = Ui.dp(ctx, 12f)
                bottomMargin = Ui.dp(ctx, 14f)
            }
        }

        fun mini(icon: Int, label: String, accentText: Boolean, onClick: () -> Unit): View {
            val btn = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                setPadding(Ui.dp(ctx, 6f), Ui.dp(ctx, 11f), Ui.dp(ctx, 6f), Ui.dp(ctx, 11f))
                background = Ui.round(Palette.PANEL, Ui.fdp(ctx, 12f), 1, Palette.LINE)
                isClickable = true
                foreground = android.graphics.drawable.RippleDrawable(
                    ColorStateList.valueOf(0x22FFFFFF), null,
                    Ui.round(android.graphics.Color.WHITE, Ui.fdp(ctx, 12f))
                )
                setOnClickListener {
                    Fx.tap(ctx, 20)
                    onClick()
                }
            }
            btn.addView(ImageView(ctx).apply {
                setImageResource(icon)
                imageTintList = ColorStateList.valueOf(if (accentText) accent else Palette.TEXT)
            }, LinearLayout.LayoutParams(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f)))
            btn.addView(TextView(ctx).apply {
                text = "  $label"
                typeface = Fonts.bn(ctx)
                textSize = 12f
                setTextColor(if (accentText) accent else Palette.TEXT)
            })
            return btn
        }

        val allOn = mini(R.drawable.ic_check_all, getString(R.string.rem_all_on), false) {
            prefs.setAllReminders(true)
            ReminderScheduler.sync(ctx)
            rebuildList()
            updateCount()
            updateBanner()
            if (!NotificationHelper.canPost(ctx)) (activity as? Host)?.requestNotifPermission()
            (activity as? Host)?.showToast(getString(R.string.rem_all_on_toast))
        }
        val allOff = mini(R.drawable.ic_close, getString(R.string.rem_all_off), false) {
            prefs.setAllReminders(false)
            ReminderScheduler.sync(ctx)
            rebuildList()
            updateCount()
            (activity as? Host)?.showToast(getString(R.string.rem_all_off_toast))
        }
        val test = mini(R.drawable.ic_send, getString(R.string.rem_test), true) {
            val today = days.firstOrNull { it.id == Schedule.todayId() }
            if (today != null && today.tasks.isNotEmpty()) {
                val t = today.tasks.first()
                NotificationHelper.postNow(
                    ctx, Schedule.title(ctx, t), Schedule.desc(ctx, t),
                    Schedule.dayName(ctx, today), "hgos-test", false
                )
            } else {
                NotificationHelper.post(ctx, getString(R.string.app_name), getString(R.string.rem_test_sent), "hgos-test")
            }
            Fx.reminderPattern(ctx)
            tones.play("reminder")
            if (!NotificationHelper.canPost(ctx)) (activity as? Host)?.requestNotifPermission()
            (activity as? Host)?.showToast(getString(R.string.rem_test_sent))
        }
        val lp1 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp1.marginEnd = Ui.dp(ctx, 4f)
        val lp2 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp2.marginStart = Ui.dp(ctx, 4f)
        lp2.marginEnd = Ui.dp(ctx, 4f)
        val lp3 = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        lp3.marginStart = Ui.dp(ctx, 4f)
        row.addView(allOn, lp1)
        row.addView(allOff, lp2)
        row.addView(test, lp3)
        return row
    }

    private fun buildList(): View {
        val wrap = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL }
        listWrap = wrap
        rebuildList()
        return wrap
    }

    private fun rebuildList() {
        val ctx = requireContext()
        val wrap = listWrap ?: return
        wrap.removeAllViews()
        days.forEach { day ->
            val on = prefs.isReminderOn(day.id)
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(Ui.dp(ctx, 13f), Ui.dp(ctx, 12f), Ui.dp(ctx, 13f), Ui.dp(ctx, 12f))
                background = if (on) Ui.linear(
                    intArrayOf(withAlpha(accent, 0x18), withAlpha(accent, 0x05)),
                    Ui.fdp(ctx, 16f)
                ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(accent, 0x50)) }
                else Ui.round(Palette.PANEL, Ui.fdp(ctx, 16f), 1, Palette.LINE)
            }
            val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            col.addView(TextView(ctx).apply {
                text = Schedule.dayName(ctx, day) + if (day.special) getString(R.string.rem_derma_suffix) else ""
                typeface = Fonts.bnSemi(ctx)
                textSize = 14f
                setTextColor(Palette.TEXT)
            })
            val chipsRow = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, Ui.dp(ctx, 7f), 0, 0)
            }
            Schedule.reminderTimes(day).forEach { time ->
                val chip = TextView(ctx).apply {
                    text = time
                    typeface = Fonts.sansSemi(ctx)
                    textSize = 10.5f
                    setTextColor(if (on) Palette.TEXT else Palette.MUTED)
                    setPadding(Ui.dp(ctx, 9f), Ui.dp(ctx, 4f), Ui.dp(ctx, 9f), Ui.dp(ctx, 4f))
                    background = Ui.round(
                        0x0DFFFFFF, Ui.fdp(ctx, 20f), 1,
                        if (on) withAlpha(accent, 0x40) else Palette.LINE
                    )
                    isClickable = true
                    setOnClickListener {
                        Fx.tap(ctx, 15)
                        val task = day.tasks.firstOrNull { it.time == time } ?: return@setOnClickListener
                        EditDialogs.show(
                            ctx, R.string.edit_title,
                            Schedule.title(ctx, task), Schedule.desc(ctx, task), task.time,
                            onSave = { t, d, tm ->
                                store.updateTask(day.id, task.id, t, d, tm)
                                store.pruneDone(prefs)
                                ReminderScheduler.sync(ctx)
                                activity?.recreate()
                            }
                        )
                    }
                }
                val clp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                clp.marginEnd = Ui.dp(ctx, 6f)
                chipsRow.addView(chip, clp)
            }
            col.addView(chipsRow)
            row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            row.addView(SwitchView(ctx).apply {
                setAccent(prefs.accent)
                setChecked(on)
                setOnChange { value ->
                    prefs.setReminder(day.id, value)
                    ReminderScheduler.sync(ctx)
                    rebuildList()
                    updateCount()
                    if (value) {
                        if (!NotificationHelper.canPost(ctx)) (activity as? Host)?.requestNotifPermission()
                        (activity as? Host)?.showToast(
                            getString(R.string.rem_day_on_toast, Schedule.dayName(ctx, day), day.tasks.size)
                        )
                    } else {
                        (activity as? Host)?.showToast(
                            getString(R.string.rem_day_off_toast, Schedule.dayName(ctx, day))
                        )
                    }
                }
            })

            wrap.addView(row, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = Ui.dp(ctx, 9f) })
        }
    }

    private fun updateCount() {
        countChip?.text = "${prefs.remindersOn().size}/7"
    }

    private fun buildNote(): View {
        val ctx = requireContext()
        val note = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 12f), Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
            background = Ui.round(0x08FFFFFF, Ui.fdp(ctx, 14f), 1, Palette.LINE_STRONG)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = Ui.dp(ctx, 4f) }
        }
        note.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_sparkle)
            imageTintList = ColorStateList.valueOf(accent)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 15f), Ui.dp(ctx, 15f)))
        note.addView(TextView(ctx).apply {
            text = "  " + getString(R.string.rem_note) + "\n  " + getString(R.string.rem_edit_hint)
            typeface = Fonts.bn(ctx)
            textSize = 11.5f
            setTextColor(Palette.MUTED)
        })
        return note
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}
