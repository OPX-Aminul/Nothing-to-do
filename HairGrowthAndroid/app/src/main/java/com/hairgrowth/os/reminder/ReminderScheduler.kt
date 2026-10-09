package com.hairgrowth.os.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.hairgrowth.os.data.Day
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.data.Schedule
import com.hairgrowth.os.data.ScheduleStore
import java.util.Calendar

/**
 * Native reminder engine. Each routine gets three exact alarms:
 *  • 10 minutes before  • 5 minutes before  • on time
 * Alarms fire from a BroadcastReceiver, so they work even when the app
 * is closed or in the background.
 */
object ReminderScheduler {

    const val EXTRA_DAY = "dayId"
    const val EXTRA_TASK = "taskId"
    const val EXTRA_ADVANCE = "advance"

    /** slots per task: 3 (advance 10 / advance 5 / on time) */
    private const val SLOTS_PER_TASK = 3
    private const val SLOT_SWEEP = 60

    private fun alarmManager(ctx: Context): AlarmManager? =
        ContextCompat.getSystemService(ctx, AlarmManager::class.java)

    fun canExact(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = alarmManager(ctx) ?: return false
        return am.canScheduleExactAlarms()
    }

    private fun requestCode(dayIndex: Int, slot: Int) = dayIndex * 100 + slot

    private fun pendingIntent(
        ctx: Context,
        dayIndex: Int,
        slot: Int,
        dayId: String,
        taskId: String,
        advance: Int
    ): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_DAY, dayId)
            putExtra(EXTRA_TASK, taskId)
            putExtra(EXTRA_ADVANCE, advance)
        }
        return PendingIntent.getBroadcast(
            ctx, requestCode(dayIndex, slot), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** Next occurrence of weekday+time, optionally shifted back by [minusMinutes]. */
    private fun nextTrigger(dayIndex: Int, hour: Int, minute: Int, minusMinutes: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val currentIndex = now.get(Calendar.DAY_OF_WEEK) - 1
        var delta = (dayIndex - currentIndex + 7) % 7
        if (delta == 0 && !target.after(now)) delta = 7
        target.add(Calendar.DAY_OF_YEAR, delta)
        target.add(Calendar.MINUTE, -minusMinutes)
        return target.timeInMillis
    }

    private fun setAlarm(ctx: Context, trigger: Long, pi: PendingIntent) {
        val am = alarmManager(ctx) ?: return
        val exact = canExact(ctx)
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
        } catch (_: SecurityException) {
            try {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } catch (_: Throwable) {
            }
        }
    }

    fun scheduleDay(ctx: Context, day: Day) {
        val dayIndex = Schedule.dayIndex(day.id)
        day.tasks.forEachIndexed { taskIndex, task ->
            val base = taskIndex * SLOTS_PER_TASK
            // 10 minutes before
            setAlarm(
                ctx, nextTrigger(dayIndex, task.hour, task.minute, 10),
                pendingIntent(ctx, dayIndex, base, day.id, task.id, 10)
            )
            // 5 minutes before
            setAlarm(
                ctx, nextTrigger(dayIndex, task.hour, task.minute, 5),
                pendingIntent(ctx, dayIndex, base + 1, day.id, task.id, 5)
            )
            // on time
            setAlarm(
                ctx, nextTrigger(dayIndex, task.hour, task.minute, 0),
                pendingIntent(ctx, dayIndex, base + 2, day.id, task.id, 0)
            )
        }
    }

    /** Cancels every possible slot for a day (works after tasks were edited/removed). */
    fun cancelDay(ctx: Context, dayIndex: Int) {
        val am = alarmManager(ctx) ?: return
        for (slot in 0 until SLOT_SWEEP) {
            val pi = PendingIntent.getBroadcast(
                ctx, requestCode(dayIndex, slot),
                Intent(ctx, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
            pi.cancel()
        }
    }

    /** Cancels everything, then schedules only the enabled days. */
    fun sync(ctx: Context) {
        val prefs = Prefs(ctx)
        for (dayIndex in 0 until 7) cancelDay(ctx, dayIndex)
        ScheduleStore(ctx).days().forEach { day ->
            if (prefs.isReminderOn(day.id)) scheduleDay(ctx, day)
        }
    }

    /** Re-schedules a single day (used after a notification fires for weekly repeat). */
    fun resyncDay(ctx: Context, dayId: String) {
        val prefs = Prefs(ctx)
        val dayIndex = Schedule.dayIndex(dayId)
        cancelDay(ctx, dayIndex)
        if (!prefs.isReminderOn(dayId)) return
        ScheduleStore(ctx).days().firstOrNull { it.id == dayId }?.let { scheduleDay(ctx, it) }
    }
}
