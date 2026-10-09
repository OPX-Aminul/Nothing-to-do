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
import com.hairgrowth.os.util.SoundEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen.
 *
 * The **day selector is pinned to the top** and never scrolls away: the user
 * always picks the day from the same fixed strip, and only the routines of the
 * selected day live in the scrollable region underneath. Opening the app
 * selects **today** by default — no auto-scrolling, no hunting for the day.
 */
class HomeFragment : Fragment() {

    private lateinit var prefs: Prefs
    private lateinit var store: ScheduleStore
    private lateinit var sounds: SoundEngine
    private var accent = Palette.GOLD
    private var accentSoft = Palette.GOLD_SOFT
    private lateinit var days: List<Day>

    private var selectedId: String = Schedule.todayId()

    private val chipWraps = HashMap<String, LinearLayout>()
    private val chipCounts = HashMap<String, TextView>()
    private val chipLabels = HashMap<String, TextView>()
    private var titleView: TextView? = null
    private var metaView: TextView? = null
    private var pctChip: TextView? = null
    private var contentHost: FrameLayout? = null
    private var scroll: ScrollView? = null

    private val selected: Day
        get() = days.firstOrNull { it.id == selectedId } ?: days.first()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val ctx = requireContext()
        prefs = Prefs(ctx)
        store = ScheduleStore(ctx)
        sounds = SoundEngine(ctx)
        accent = Palette.accent(prefs.accent)
        accentSoft = Palette.accentSoft(prefs.accent)
        days = store.days()
        if (days.none { it.id == selectedId }) selectedId = days.first().id

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(buildTop())
        root.addView(buildPinnedStrip())
        root.addView(View(ctx).apply {
            background = Ui.linear(
                intArrayOf(Palette.LINE, 0x00000000), Ui.fdp(ctx, 1f)
            )
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 1f)))

        val host = FrameLayout(ctx)
        contentHost = host
        val sv = ScrollView(ctx).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, Ui.dp(ctx, 130f))
        }
        scroll = sv
        host.addView(sv, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        root.addView(host, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        renderDay(animate = false)
        return root
    }

    // ---------------------------------------------------------------- header

    private fun buildTop(): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 18f), Ui.dp(ctx, 4f), Ui.dp(ctx, 18f), Ui.dp(ctx, 12f))
        }
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        titleView = TextView(ctx).apply {
            typeface = Fonts.bnSemi(ctx)
            textSize = 22f
            setTextColor(Palette.TEXT)
            letterSpacing = -0.01f
        }
        metaView = TextView(ctx).apply {
            typeface = Fonts.bn(ctx)
            textSize = 11.5f
            setTextColor(Palette.MUTED)
            setPadding(0, Ui.dp(ctx, 5f), 0, 0)
        }
        col.addView(titleView)
        col.addView(metaView)
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        pctChip = TextView(ctx).apply {
            typeface = Fonts.sansX(ctx)
            textSize = 13f
            gravity = Gravity.CENTER
            setPadding(Ui.dp(ctx, 13f), Ui.dp(ctx, 9f), Ui.dp(ctx, 13f), Ui.dp(ctx, 9f))
            background = Ui.linear(
                intArrayOf(withAlpha(accent, 0x30), withAlpha(accent, 0x0C)),
                Ui.fdp(ctx, 22f)
            ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(accent, 0x70)) }
            elevation = Ui.fdp(ctx, 3f)
        }
        row.addView(pctChip)
        return row
    }

    /** The pinned selector: always visible at the very top, never scrolled away. */
    private fun buildPinnedStrip(): View {
        val ctx = requireContext()
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 14f), 0, Ui.dp(ctx, 14f), Ui.dp(ctx, 12f))
            elevation = Ui.fdp(ctx, 6f)
        }
        val strip = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        days.forEach { day ->
            val chip = buildDayChip(day)
            val lp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            lp.marginStart = Ui.dp(ctx, 3f)
            lp.marginEnd = Ui.dp(ctx, 3f)
            strip.addView(chip, lp)
        }
        wrap.addView(strip)
        return wrap
    }

    private fun buildDayChip(day: Day): View {
        val ctx = requireContext()
        val chip = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(ctx, 10f), 0, Ui.dp(ctx, 9f))
            isClickable = true
        }
        chip.addView(TextView(ctx).apply {
            text = Schedule.dayShort(ctx, day)
            typeface = Fonts.bn(ctx)
            textSize = 10.5f
            maxLines = 1
            gravity = Gravity.CENTER
            tag = "label"
        })
        val count = TextView(ctx).apply {
            typeface = Fonts.sansX(ctx)
            textSize = 13.5f
            gravity = Gravity.CENTER
            tag = "count"
        }
        chip.addView(count, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = Ui.dp(ctx, 4f) })
        chip.addView(View(ctx).apply { tag = "dot" }, LinearLayout.LayoutParams(
            Ui.dp(ctx, 4f), Ui.dp(ctx, 4f)
        ).apply {
            topMargin = Ui.dp(ctx, 5f)
            gravity = Gravity.CENTER_HORIZONTAL
        })

        chipLabels[day.id] = chip.findViewWithTag("label")
        chipCounts[day.id] = count
        chipWraps[day.id] = chip

        chip.setOnClickListener {
            if (selectedId == day.id) {
                scroll?.smoothScrollTo(0, 0)
                return@setOnClickListener
            }
            selectedId = day.id
            sounds.tap()
            Fx.tap(ctx, 18)
            paintStrip(animateSelection = true)
            renderDay(animate = true)
        }
        return chip
    }

    /** Repaints the pinned strip: selected, today and done/total states. */
    private fun paintStrip(animateSelection: Boolean = false) {
        val ctx = requireContext()
        days.forEach { day ->
            val chip = chipWraps[day.id] ?: return@forEach
            val label = chipLabels[day.id]
            val count = chipCounts[day.id]
            val dot = chip.findViewWithTag<View>("dot")
            val isSel = day.id == selectedId
            val isToday = day.id == Schedule.todayId()

            chip.background = if (isSel) {
                Ui.linear(
                    intArrayOf(withAlpha(accent, 0x38), withAlpha(accent, 0x0A)),
                    Ui.fdp(ctx, 15f)
                ).apply { setStroke(Ui.dp(ctx, 1f), accent) }
            } else if (isToday) {
                Ui.linear(
                    intArrayOf(withAlpha(accent, 0x16), withAlpha(accent, 0x04)),
                    Ui.fdp(ctx, 15f)
                ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(accent, 0x55)) }
            } else {
                Ui.round(Palette.PANEL, Ui.fdp(ctx, 15f), 1, Palette.LINE)
            }
            label?.setTextColor(if (isSel) accentSoft else Palette.MUTED)
            count?.setTextColor(if (isSel) accentSoft else Palette.TEXT)
            count?.text = "${doneCount(day)}/${day.tasks.size}"
            dot?.visibility = if (isToday || day.special) View.VISIBLE else View.INVISIBLE
            dot?.background = Ui.circle(if (day.special) Palette.ROSE else accent)
            chip.elevation = Ui.fdp(ctx, if (isSel) 6f else 0f)
            if (isSel && animateSelection && chip.layoutParams != null) {
                chip.scaleX = 0.94f
                chip.scaleY = 0.94f
                chip.animate().scaleX(1f).scaleY(1f).setDuration(260)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.4f)).start()
            }
        }
    }

    // --------------------------------------------------------------- content

    /** Rebuilds the scrollable region for [selectedId] (today by default). */
    private fun renderDay(animate: Boolean) {
        val ctx = requireContext()
        val host = contentHost ?: return
        host.removeAllViews()
        val day = selected
        val isToday = day.id == Schedule.todayId()

        titleView?.text = Schedule.dayName(ctx, day)
        metaView?.text = buildString {
            append(dateLabel(day))
            append(" · ")
            append(getString(R.string.home_done_meta, doneCount(day), day.tasks.size))
        }
        pctChip?.text = "${pct(day)}%"

        paintStrip(animateSelection = false)

        val sv = ScrollView(ctx).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, Ui.dp(ctx, 130f))
        }
        scroll = sv
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 18f), Ui.dp(ctx, 16f), Ui.dp(ctx, 18f), Ui.dp(ctx, 20f))
        }
        sv.addView(col)

        if (isToday) col.addView(todayBanner())
        col.addView(buildDayLabel(day))

        val section = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        section.addView(buildAddRow(day))
        if (day.tasks.isEmpty()) {
            section.addView(emptyNote())
        } else {
            day.tasks.forEach { section.addView(buildTaskRow(day, it)) }
        }
        col.addView(section)
        col.addView(footerNote())

        host.addView(sv, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ))
        if (animate) Ui.animIn(sv)
    }

    /** Small hero card shown only on the current day. */
    private fun todayBanner(): View {
        val ctx = requireContext()
        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 15f), Ui.dp(ctx, 14f), Ui.dp(ctx, 15f), Ui.dp(ctx, 14f))
            background = Ui.linear(
                intArrayOf(withAlpha(accent, 0x26), withAlpha(accent, 0x06)),
                Ui.fdp(ctx, 18f)
            ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(accent, 0x55)) }
            elevation = Ui.fdp(ctx, 6f)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = Ui.dp(ctx, 16f) }
            isClickable = true
            foreground = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(withAlpha(accent, 0x30)), null,
                Ui.round(Color.WHITE, Ui.fdp(ctx, 18f))
            )
        }
        val bell = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_dna)
            imageTintList = ColorStateList.valueOf(accent)
        }
        card.addView(bell, LinearLayout.LayoutParams(Ui.dp(ctx, 26f), Ui.dp(ctx, 26f)))
        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 13f), 0, Ui.dp(ctx, 8f), 0)
        }
        col.addView(TextView(ctx).apply {
            text = getString(R.string.home_today_card_title)
            typeface = Fonts.bnSemi(ctx)
            textSize = 14f
            setTextColor(Palette.TEXT)
        })
        col.addView(TextView(ctx).apply {
            text = getString(R.string.home_today_card_sub, doneCount(selected), selected.tasks.size)
            typeface = Fonts.bn(ctx)
            textSize = 11.5f
            setTextColor(Palette.MUTED)
            setPadding(0, Ui.dp(ctx, 3f), 0, 0)
        })
        card.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        card.addView(TextView(ctx).apply {
            text = "${pct(selected)}%"
            typeface = Fonts.sansX(ctx)
            textSize = 15f
            setTextColor(accentSoft)
        })
        card.setOnClickListener {
            sounds.tap()
            Fx.tap(ctx, 15)
            scroll?.smoothScrollTo(0, 0)
        }
        return card
    }

    private fun buildDayLabel(day: Day): View {
        val ctx = requireContext()
        val isToday = day.id == Schedule.todayId()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        row.addView(TextView(ctx).apply {
            text = getString(if (isToday) R.string.home_section_today else R.string.home_section_day)
            typeface = Fonts.bnSemi(ctx)
            textSize = 16f
            setTextColor(Palette.TEXT)
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
        row.addView(Ui.underline(ctx, accent), LinearLayout.LayoutParams(
            Ui.dp(ctx, 34f), Ui.dp(ctx, 2.5f)
        ).apply { marginStart = Ui.dp(ctx, 10f) })
        row.layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = Ui.dp(ctx, 12f) }
        return row
    }

    private fun buildAddRow(day: Day): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(ctx, 11f), 0, Ui.dp(ctx, 11f))
            background = Ui.outline(ctx, 16f, withAlpha(accent, 0x66))
            isClickable = true
            foreground = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(withAlpha(accent, 0x22)), null,
                Ui.round(Color.WHITE, Ui.fdp(ctx, 16f))
            )
            setOnClickListener {
                sounds.tap()
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
        lp.bottomMargin = Ui.dp(ctx, 10f)
        row.layoutParams = lp
        return row
    }

    private fun emptyNote(): View {
        val ctx = requireContext()
        return TextView(ctx).apply {
            text = getString(R.string.home_empty_day)
            typeface = Fonts.bn(ctx)
            textSize = 12f
            setTextColor(Palette.MUTED)
            gravity = Gravity.CENTER
            setPadding(0, Ui.dp(ctx, 26f), 0, Ui.dp(ctx, 20f))
        }
    }

    private fun footerNote(): View {
        val ctx = requireContext()
        return TextView(ctx).apply {
            text = getString(R.string.home_footer_note)
            typeface = Fonts.bn(ctx)
            textSize = 11f
            setTextColor(Palette.MUTED2)
            gravity = Gravity.CENTER
            setPadding(Ui.dp(ctx, 10f), Ui.dp(ctx, 14f), Ui.dp(ctx, 10f), 0)
        }
    }

    private fun buildTaskRow(day: Day, task: Task): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f), Ui.dp(ctx, 14f))
            isClickable = true
            isLongClickable = true
            elevation = Ui.fdp(ctx, 2f)
            foreground = android.graphics.drawable.RippleDrawable(
                ColorStateList.valueOf(withAlpha(Palette.TEXT, 0x24)), null,
                Ui.round(Color.WHITE, Ui.fdp(ctx, 18f))
            )
        }
        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        lp.bottomMargin = Ui.dp(ctx, 10f)
        row.layoutParams = lp

        val iconBox = FrameLayout(ctx)
        iconBox.addView(ImageView(ctx).apply {
            setImageResource(iconRes(task.icon))
            imageTintList = ColorStateList.valueOf(tintColor(task.tint))
        }, FrameLayout.LayoutParams(Ui.dp(ctx, 20f), Ui.dp(ctx, 20f)).apply { gravity = Gravity.CENTER })
        iconBox.background = Ui.linear(
            intArrayOf(withAlpha(tintColor(task.tint), 0x26), withAlpha(tintColor(task.tint), 0x08)),
            Ui.fdp(ctx, 13f)
        ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(tintColor(task.tint), 0x45)) }
        row.addView(iconBox, LinearLayout.LayoutParams(Ui.dp(ctx, 42f), Ui.dp(ctx, 42f)))

        val col = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(Ui.dp(ctx, 13f), 0, Ui.dp(ctx, 8f), 0)
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
            background = Ui.round(Palette.CHIP_BG, Ui.fdp(ctx, 9f), 1, Palette.LINE)
        })

        val tick = FrameLayout(ctx)
        val tickIcon = ImageView(ctx).apply {
            setImageResource(R.drawable.ic_check)
            imageTintList = ColorStateList.valueOf(if (Palette.dark) 0xFF06231B.toInt() else 0xFFFFFFFF.toInt())
        }
        tick.addView(tickIcon, FrameLayout.LayoutParams(Ui.dp(ctx, 14f), Ui.dp(ctx, 14f)).apply { gravity = Gravity.CENTER })
        row.addView(tick, LinearLayout.LayoutParams(Ui.dp(ctx, 27f), Ui.dp(ctx, 27f)).apply {
            marginStart = Ui.dp(ctx, 11f)
        })

        fun paint(doneState: Boolean) {
            if (doneState) {
                row.background = Ui.linear(
                    intArrayOf(withAlpha(Palette.MINT, 0x20), withAlpha(Palette.MINT, 0x06)),
                    Ui.fdp(ctx, 18f)
                ).apply { setStroke(Ui.dp(ctx, 1f), withAlpha(Palette.MINT, 0x44)) }
                tick.background = Ui.circle(Palette.MINT)
                title.setTextColor(Palette.MINT)
                title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                title.alpha = 0.9f
                tick.scaleX = 1.06f
                tick.scaleY = 1.06f
            } else {
                row.background = Ui.card(ctx, 18f)
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
            sounds.tap()
            Fx.tap(ctx, 30)
            val now = !prefs.isDone(task.id)
            prefs.setDone(task.id, now)
            paint(now)
            if (now) {
                tick.scaleX = 0.4f
                tick.scaleY = 0.4f
                tick.animate().scaleX(1.06f).scaleY(1.06f).setDuration(320)
                    .setInterpolator(android.view.animation.OvershootInterpolator(2.4f)).start()
                sounds.complete()
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
            sounds.tap()
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

    // ---------------------------------------------------------------- helpers

    private fun afterScheduleChange() {
        val ctx = requireContext()
        store.pruneDone(prefs)
        ReminderScheduler.sync(ctx)
        activity?.recreate()
    }

    private fun refreshCounters() {
        days = store.days()
        paintStrip()
        val day = selected
        pctChip?.text = "${pct(day)}%"
        metaView?.text = buildString {
            append(dateLabel(day))
            append(" · ")
            append(getString(R.string.home_done_meta, doneCount(day), day.tasks.size))
        }
    }

    private fun dateLabel(day: Day): String {
        val index = Schedule.dayIndex(day.id)
        val delta = (index - Schedule.todayIndex() + 7) % 7
        val date = Date(System.currentTimeMillis() + delta * 86_400_000L)
        val fmt = SimpleDateFormat("d MMM", if (prefs.language == "en") Locale.US else Locale.getDefault())
        return fmt.format(date)
    }

    private fun pct(day: Day): Int =
        Math.round(doneCount(day) * 100f / day.tasks.size.coerceAtLeast(1))

    private fun total(): Int = days.sumOf { it.tasks.size }

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

    override fun onDestroyView() {
        super.onDestroyView()
        sounds.release()
    }
}
