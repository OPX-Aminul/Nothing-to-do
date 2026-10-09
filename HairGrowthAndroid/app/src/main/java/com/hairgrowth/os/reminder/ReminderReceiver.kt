package com.hairgrowth.os.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.data.Schedule
import com.hairgrowth.os.data.ScheduleStore
import com.hairgrowth.os.util.Fx

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(ctx: Context, intent: Intent) {
        val dayId = intent.getStringExtra(ReminderScheduler.EXTRA_DAY) ?: return
        val taskId = intent.getStringExtra(ReminderScheduler.EXTRA_TASK) ?: return
        val advance = intent.getIntExtra(ReminderScheduler.EXTRA_ADVANCE, 0)

        val prefs = Prefs(ctx)
        if (!prefs.isReminderOn(dayId)) return

        val day = ScheduleStore(ctx).days().firstOrNull { it.id == dayId } ?: return
        val task = day.tasks.firstOrNull { it.id == taskId } ?: return

        val title = Schedule.title(ctx, task)
        val desc = Schedule.desc(ctx, task)
        val dayLabel = Schedule.dayName(ctx, day)
        val tag = "hgos-$dayId-$taskId-$advance"

        if (!prefs.isDone(taskId)) {
            if (advance > 0) {
                NotificationHelper.postAdvance(ctx, title, task.time, advance, tag)
            } else {
                val sameTime = day.tasks.filter { it.time == task.time }
                NotificationHelper.postNow(
                    ctx, title, desc, dayLabel, tag, sameTime.size > 1
                )
            }
            Fx.reminderPattern(ctx)
        }

        // weekly recurrence
        ReminderScheduler.resyncDay(ctx, dayId)
    }
}
