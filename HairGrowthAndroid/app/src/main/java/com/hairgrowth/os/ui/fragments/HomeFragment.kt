package com.hairgrowth.os.ui.fragments

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import com.hairgrowth.os.data.Task
import com.hairgrowth.os.data.TaskTint
import com.hairgrowth.os.reminder.ReminderScheduler
import com.hairgrowth.os.ui.EditDialogs
import com.hairgrowth.os.ui.Fonts
import com.hairgrowth.os.ui.Palette
import com.hairgrowth.os.ui.Ui
import com.hairgrowth.os.util.Fx
import com.hairgrowth.os.util.TonePlayer

class HomeFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var store: ScheduleStore
    private lateinit var tones: TonePlayer
    private var accent = Palette.GOLD
    private var accentSoft = Palette.GOLD_SOFT
    private lateinit var days: List<Day>

    private val dayViews = HashMap<String, View>()
    private val dayChipCounts = HashMap<String, TextView>()
    private var countChip: TextView? = null
    private var scroll: ScrollView? = null
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
        accentSoft = Palette.accentSoft(prefs.accent)
        days = store.days()

        val sv = ScrollView(ctx).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, Ui.dp(ctx, 120f))
            setOnScrollChangeListener { _, _, y, _, _ ->
                (activity as? Host)?.dockOnScroll(y - lastScrollY)
                lastScrollY = y
            }
        }
        scroll = sv
        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 18f), Ui.dp(ctx, 6f), Ui.dp(ctx, 18f), Ui.dp(ctx, 20f))
        }
        sv.addView(root)
        root.addView(buildTop())
        root.addView(buildSectionHead())
        root.addView(buildWeekStrip())
        root.addView(buildSchedule())

        sv.postDelayed({
            dayViews[Schedule.todayId()]?.let {
                sv.smoothScrollTo(0, (it.top - Ui.dp(ctx, 90f)).coerceAtLeast(0))
            }
        }, 900)
        return sv
    }

    private fun total(): Int = days.sumOf { it.tasks.size }

    private fun buildTop(): View {
        val ctx = requireContext()
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, Ui.dp(ctx, 4f), 0, Ui.dp(ctx, 16f))
        }
        col.addView(TextView(ctx).apply {
            text = getString(R.string.home_title)
            typeface = Fonts.bnBold(ctx)
            textSize = 25f
            setTextColor(Palette.TEXT)
        })
        col.addView(TextView(ctx).apply {
            text = getString(R.string.home_sub)
            typeface = Fonts.bn(ctx)
            textSize = 12.5f
            setTextColor(Palette.MUTED)
            setPadding(0, Ui.dp(ctx, 6f), 0, 0)
        })
        return col
    }

    private fun buildSectionHead(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, Ui.dp(ctx, 14f))
        }
        row.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_grid)
            imageTintList = ColorStateList.valueOf(accent)
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 16f), Ui.dp(ctx, 16f)))
        row.addView(TextView(ctx).apply {
            text = "  " + getString(R.string.home_week)
            typeface = Fonts.bnSemi(ctx)
            textSize = 18f
            setTextColor(Palette.TEXT)
        })
        row.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        countChip = TextView(ctx).apply {
            typeface = Fonts.sansSemi(ctx)
            textSize = 11f
            setTextColor(Palette.MUTED)
            setPadding(Ui.dp(ctx, 10f), Ui.dp(ctx, 5f), Ui.dp(ctx, 10f), Ui.dp(ctx, 5f))
            background = Ui.round(Palette.PANEL, Ui.fdp(ctx, 20f), 1, Palette.LINE)
            text = "${prefs.doneCount()}/${total()}"
        }
        row.addView(countChip)
        return row
    }

    private fun buildWeekStrip(): View {
        val ctx = requireContext()
        val strip = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        strip.setPadding(0, 0, 0, Ui.dp(ctx, 20f))
        days.forEach { day ->
            val chip = buildDayChip(day)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = Ui.dp(ctx, 2.5f)
            lp.marginEnd = Ui.dp(ctx, 2.5f)
            strip.addView(chip, lp)
        }
        return strip
    }

    private fun buildDayChip(day: Day): View {
        val ctx = requireContext()
        val isToday = day.id == Schedule.todayId()
        val chip = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(ctx, 9f), 0, Ui.dp(ctx, 8f))
            background = if (isToday) {
                Ui.linear(
                    intArrayOf(withAlpha(accent, 0x33), withAlpha(accent, 0x08)),
                    Ui.fdp(ctx, 14f)
                ).apply { setStroke(Ui.dp(ctx, 1f), accent) }
            } else {
                Ui.round(Palette.PANEL, Ui.fdp(ctx, 14f), 1, Palette.LINE)
            }
            isClickable = true
            foreground = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(0x22FFFFFF), null,
                Ui.round(Color.WHITE, Ui.fdp(ctx, 14f))
            )
            setOnClickListener {
                Fx.tap(ctx, 18)
                dayViews[day.id]?.let { target ->
                    scroll?.smoothScrollTo(0, (target.top - Ui.dp(ctx, 80f)).coerceAtLeast(0))
                }
            }
        }
        chip.addView(TextView(ctx).apply {
            text = Schedule.dayShort(ctx, day)
            typeface = Fonts.bn(ctx)
            textSize = 10.5f
            setTextColor(Palette.MUTED)
            maxLines = 1
            gravity = Gravity.CENTER
        })
        val count = TextView(ctx).apply {
            text = "${doneCount(day)}/${day.tasks.size}"
            typeface = Fonts.sansSemi(ctx)
            textSize = 13f
            setTextColor(if (isToday) accentSoft else Palette.TEXT)
        }
        chip.addView(count)
        dayChipCounts[day.id] = count
        if (day.special) {
            val dot = View(ctx).apply { background = Ui.circle(accent) }
            val lp = LinearLayout.LayoutParams(Ui.dp(ctx, 4f), Ui.dp(ctx, 4f))
            lp.topMargin = Ui.dp(ctx, 5f)
            chip.addView(dot, lp)
        }
        return chip
    }

    private fun buildSchedule(): View {
        val ctx = requireContext()
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        days.forEach { day ->
            val group = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
            group.addView(buildDayLabel(day))
            group.addView(buildAddRow(day))
            day.tasks.forEach { group.addView(buildTaskRow(day, it)) }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.bottomMargin = Ui.dp(ctx, 18f)
            col.addView(group, lp)
            dayViews[day.id] = group
        }
        return col
    }

    private fun buildDayLabel(day: Day): View {
        val ctx = requireContext()
        val isToday = day.id == Schedule.todayId()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, Ui.dp(ctx, 9f))
        }
        row.addView(View(ctx).apply {
            background = Ui.round(if (isToday) accent else Palette.LINE_STRONG, Ui.fdp(ctx, 2f))
        }, LinearLayout.LayoutParams(Ui.dp(ctx, 3f), Ui.dp(ctx, 16f)))
        row.addView(TextView(ctx).apply {
            text = "  " + Schedule.dayName(ctx, day)
            typeface = Fonts.bnSemi(ctx)
            textSize = 15f
            setTextColor(if (isToday) accentSoft else Palette.TEXT)
        })
        row.addView(View(ctx), LinearLayout.LayoutParams(0, 1, 1f))
        val tagText = when {
            day.special -> getString(R.string.tag_derma)
            isToday -> getString(R.string.tag_today)
            else -> null
        }
        if (tagText != null) {
            val tagColor = if (day.special) Palette.ROSE else accentSoft
            row.addView(TextView(ctx).apply {
                text = tagText
                typeface = Fonts.bn(ctx)
                textSize = 10.5f
                setTextColor(tagColor)
                setPadding(Ui.dp(ctx, 9f), Ui.dp(ctx, 3f), Ui.dp(ctx, 9f), Ui.dp(ctx, 3f))
                background = Ui.round(
                    withAlpha(tagColor, 0x22), Ui.fdp(ctx, 20f), 1, withAlpha(tagColor, 0x55)
                )
            })
        }
        return row
    }

    private fun buildAddRow(day: Day): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(ctx, 10f), 0, Ui.dp(ctx, 10f))
            background = Ui.round(Color.TRANSPARENT, Ui.fdp(ctx, 16f), 1, Palette.LINE_STRONG)
            isClickable = true
            foreground = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(0x22FFFFFF), null,
                Ui.round(Color.WHITE, Ui.fdp(ctx, 16f))
            )
            setOnClickListener {
                Fx.tap(ctx, 20)
                EditDialogs.show(
                    ctx, R.string.edit_add,
                    "", "", "09:00",
                    onSave = { title, desc, time ->
                        store.addTask(day.id, title, desc, time)
                        afterScheduleChange()
                        (activity as? Host)?.showToast(getString(R.string.toast_task_added))
                    }
                )
            }
        }
        row.addView(TextView(ctx).apply {
            text = getString(R.string.add_task)
            typeface = Fonts.bn(ctx)
            textSize = 12f
            setTextColor(accent)
        })
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = Ui.dp(ctx, 9f)
        row.layoutParams = lp
        return row
    }

    private fun buildTaskRow(day: Day, task: Task): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f))
            isClickable = true
            isLongClickable = true
            foreground = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(0x24FFFFFF), null,
                Ui.round(Color.WHITE, Ui.fdp(ctx, 18f))
            )
        }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = Ui.dp(ctx, 9f)
        row.layoutParams = lp

        val iconBox = FrameLayout(ctx)
        iconBox.addView(ImageView(ctx).apply {
            setImageResource(iconRes(task.icon))
            imageTintList = ColorStateList.valueOf(tintColor(task.tint))
        }, FrameLayout.LayoutParams(Ui.dp(ctx, 20f), Ui.dp(ctx, 20f)).apply { gravity = Gravity.CENTER })
        row.addView(iconBox, LinearLayout.LayoutParams(Ui.dp(ctx, 40f), Ui.dp(ctx, 40f)))

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 12f), 0, Ui.dp(ctx, 8f), 0)
        }
        val title = TextView(ctx).apply {
            text = Schedule.title(ctx, task)
            typeface = Fonts.bnSemi(ctx)
            textSize = 14.5f
            setTextColor(Palette.TEXT)
        }
        val desc = TextView(ctx).apply {
            text = Schedule.desc(ctx, task)
            typeface = Fonts.bn(ctx)
            textSize = 11.5f
            setTextColor(Palette.MUTED)
            setPadding(0, Ui.dp(ctx, 3f), 0, 0)
        }
        col.addView(title)
        col.addView(desc)
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        row.addView(TextView(ctx).apply {
            text = task.time
            typeface = Fonts.sansSemi(ctx)
            textSize = 11f
            setTextColor(Palette.MUTED)
            setPadding(Ui.dp(ctx, 8f), Ui.dp(ctx, 4f), Ui.dp(ctx, 8f), Ui.dp(ctx, 4f))
            background = Ui.round(0x0DFFFFFF, Ui.fdp(ctx, 8f), 1, Palette.LINE)
        })

        val tick = FrameLayout(ctx)
        tick.addView(ImageView(ctx).apply {
            setImageResource(R.drawable.ic_check)
            imageTintList = ColorStateList.valueOf(0xFF06231B.toInt())
        }, FrameLayout.LayoutParams(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f)).apply { gravity = Gravity.CENTER })
        row.addView(tick, LinearLayout.LayoutParams(Ui.dp(ctx, 26f), Ui.dp(ctx, 26f)).apply {
            marginStart = Ui.dp(ctx, 10f)
        })

        fun paint(doneState: Boolean) {
            if (doneState) {
                row.background = Ui.linear(
                    intArrayOf(withAlpha(Palette.MINT, 0x1E), withAlpha(Palette.MINT, 0x05)),
                    Ui.fdp(ctx, 18f)
                ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(Palette.MINT, 0x40)) }
                tick.background = Ui.circle(Palette.MINT)
                title.setTextColor(Palette.MINT)
                title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                title.alpha = 0.9f
                tick.scaleX = 1.06f
                tick.scaleY = 1.06f
            } else {
                row.background = Ui.round(Palette.PANEL, Ui.fdp(ctx, 18f), 1, Palette.LINE)
                tick.background = Ui.circle(Color.TRANSPARENT, Ui.dp(ctx, 2f), Palette.MUTED2)
                title.setTextColor(Palette.TEXT)
                title.paintFlags = title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                title.alpha = 1f
                tick.scaleX = 1f
                tick.scaleY = 1f
            }
        }
        paint(prefs.isDone(task.id))

        row.setOnClickListener {
            Fx.tap(ctx, 35)
            val now = !prefs.isDone(task.id)
            prefs.setDone(task.id, now)
            paint(now)
            if (now) {
                tick.scaleX = 0.4f
                tick.scaleY = 0.4f
                tick.animate().scaleX(1.06f).scaleY(1.06f).setDuration(320)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.4f)).start()
                tones.play("complete")
                if (prefs.doneCount() >= total()) {
                    Fx.celebratePattern(ctx)
                    (activity as? Host)?.celebrate()
                    (activity as? Host)?.playTone("celebrate")
                    (activity as? Host)?.showToast(getString(R.string.toast_all_done))
                }
            }
            refreshCounters()
        }

        row.setOnLongClickListener {
            Fx.tap(ctx, 25)
            EditDialogs.show(
                ctx, R.string.edit_title,
                Schedule.title(ctx, task), Schedule.desc(ctx, task), task.time,
                onSave = { t, d, tm ->
                    store.updateTask(day.id, task.id, t, d, tm)
                    afterScheduleChange()
                    (activity as? Host)?.showToast(getString(R.string.toast_task_saved))
                },
                onDelete = {
                    store.deleteTask(day.id, task.id)
                    afterScheduleChange()
                    (activity as? Host)?.showToast(getString(R.string.toast_task_deleted))
                }
            )
            true
        }
        return row
    }

    private fun afterScheduleChange() {
        val ctx = requireContext()
        store.pruneDone(prefs)
        ReminderScheduler.sync(ctx)
        activity?.recreate()
    }

    private fun refreshCounters() {
        countChip?.text = "${prefs.doneCount()}/${total()}"
        days.forEach { day ->
            dayChipCounts[day.id]?.text = "${doneCount(day)}/${day.tasks.size}"
        }
    }

    private fun doneCount(day: Day): Int = day.tasks.count { prefs.isDone(it.id) }

    private fun iconRes(key: String): Int = when (key) {
        "shower" -> R.drawable.ic_shower
        "moon" -> R.drawable.ic_moon
        "derma" -> R.drawable.ic_derma
        else -> R.drawable.ic_drop
    }

    private fun tintColor(t: TaskTint): Int = when (t) {
        TaskTint.SHAMPOO -> Palette.ICE
        TaskTint.DERMA -> Palette.ROSE
        else -> accent
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or ((alpha and 0xFF) shl 24)
}
