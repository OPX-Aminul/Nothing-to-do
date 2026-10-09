package com.hairgrowth.os.data

import android.content.Context
import com.hairgrowth.os.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

enum class TaskTint { MINOX, SHAMPOO, DERMA }

/**
 * A single routine slot. [titleKey]/[descKey] point at localized default texts;
 * [titleCustom]/[descCustom] hold user edits (null = use the localized default).
 */
data class Task(
    val id: String,
    val titleKey: String,
    val descKey: String,
    val time: String,
    val icon: String,
    val tint: TaskTint,
    val titleCustom: String? = null,
    val descCustom: String? = null,
    val custom: Boolean = false
) {
    val hour: Int get() = time.substring(0, 2).toInt()
    val minute: Int get() = time.substring(3, 5).toInt()

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("titleKey", titleKey)
        put("descKey", descKey)
        put("time", time)
        put("icon", icon)
        put("tint", tint.name)
        put("custom", custom)
        put("titleCustom", titleCustom ?: JSONObject.NULL)
        put("descCustom", descCustom ?: JSONObject.NULL)
    }

    companion object {
        fun fromJson(o: JSONObject): Task {
            val tint = try {
                TaskTint.valueOf(o.optString("tint", "MINOX"))
            } catch (_: Throwable) {
                TaskTint.MINOX
            }
            return Task(
                id = o.optString("id", UUID.randomUUID().toString()),
                titleKey = o.optString("titleKey", "minoxidil"),
                descKey = o.optString("descKey", "minox_morning"),
                time = o.optString("time", "09:00"),
                icon = o.optString("icon", "drop"),
                tint = tint,
                titleCustom = if (o.isNull("titleCustom")) null else o.optString("titleCustom"),
                descCustom = if (o.isNull("descCustom")) null else o.optString("descCustom"),
                custom = o.optBoolean("custom", false)
            )
        }
    }
}

data class Day(
    val id: String,
    val nameKey: String,
    val special: Boolean,
    val tasks: List<Task>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("nameKey", nameKey)
        put("special", special)
        val arr = JSONArray()
        tasks.forEach { arr.put(it.toJson()) }
        put("tasks", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): Day {
            val arr = o.optJSONArray("tasks") ?: JSONArray()
            val tasks = (0 until arr.length()).map { Task.fromJson(arr.getJSONObject(it)) }
            return Day(
                id = o.optString("id", "sun"),
                nameKey = o.optString("nameKey", "sun"),
                special = o.optBoolean("special", false),
                tasks = tasks
            )
        }
    }
}

/** Calendar helpers + localized text resolution for schedule entities. */
object Schedule {

    val dayOrder = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat")

    /** JS getDay(): 0=Sunday..6=Saturday, matching [dayOrder]. */
    fun todayIndex(): Int =
        (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)

    fun todayId(): String = dayOrder[todayIndex()]

    fun dayIndex(id: String): Int = dayOrder.indexOf(id).coerceAtLeast(0)

    /** Days until the next Saturday (ডার্মা ডে). */
    fun daysToDerma(): Int {
        val d = (6 - todayIndex() + 7) % 7
        return if (d == 0) 7 else d
    }

    /** Distinct reminder times for a day, sorted. */
    fun reminderTimes(day: Day): List<String> =
        day.tasks.map { it.time }.distinct().sorted()

    private val TITLE_RES = mapOf(
        "minoxidil" to R.string.task_minoxidil,
        "shampoo" to R.string.task_shampoo,
        "derma" to R.string.task_derma
    )

    private val DESC_RES = mapOf(
        "minox_morning" to R.string.desc_minox_morning,
        "minox_night" to R.string.desc_minox_night,
        "shampoo_wash" to R.string.desc_shampoo_wash,
        "shampoo_cleanse" to R.string.desc_shampoo_cleanse,
        "derma_rolling" to R.string.desc_derma_rolling
    )

    private val DAY_RES = mapOf(
        "sun" to R.string.day_sun, "mon" to R.string.day_mon, "tue" to R.string.day_tue,
        "wed" to R.string.day_wed, "thu" to R.string.day_thu, "fri" to R.string.day_fri,
        "sat" to R.string.day_sat
    )

    private val DAY_SHORT_RES = mapOf(
        "sun" to R.string.day_sun_short, "mon" to R.string.day_mon_short,
        "tue" to R.string.day_tue_short, "wed" to R.string.day_wed_short,
        "thu" to R.string.day_thu_short, "fri" to R.string.day_fri_short,
        "sat" to R.string.day_sat_short
    )

    fun title(ctx: Context, task: Task): String =
        task.titleCustom ?: ctx.getString(TITLE_RES[task.titleKey] ?: R.string.task_minoxidil)

    fun desc(ctx: Context, task: Task): String =
        task.descCustom ?: ctx.getString(DESC_RES[task.descKey] ?: R.string.desc_minox_morning)

    fun dayName(ctx: Context, day: Day): String =
        ctx.getString(DAY_RES[day.nameKey] ?: R.string.day_sun)

    fun dayShort(ctx: Context, day: Day): String =
        ctx.getString(DAY_SHORT_RES[day.nameKey] ?: R.string.day_sun_short)
}

/** The built-in default daily growth protocol (localized texts). */
object ScheduleTemplate {

    fun clone(): List<Day> = listOf(
        Day("sun", "sun", false, listOf(
            Task("sun_m", "minoxidil", "minox_morning", "09:00", "drop", TaskTint.MINOX),
            Task("sun_s", "shampoo", "shampoo_wash", "21:30", "shower", TaskTint.SHAMPOO),
            Task("sun_n", "minoxidil", "minox_night", "21:00", "moon", TaskTint.MINOX)
        )),
        Day("mon", "mon", false, listOf(
            Task("mon_m", "minoxidil", "minox_morning", "09:00", "drop", TaskTint.MINOX),
            Task("mon_n", "minoxidil", "minox_night", "21:00", "moon", TaskTint.MINOX)
        )),
        Day("tue", "tue", false, listOf(
            Task("tue_m", "minoxidil", "minox_morning", "09:00", "drop", TaskTint.MINOX),
            Task("tue_s", "shampoo", "shampoo_wash", "21:30", "shower", TaskTint.SHAMPOO),
            Task("tue_n", "minoxidil", "minox_night", "21:00", "moon", TaskTint.MINOX)
        )),
        Day("wed", "wed", false, listOf(
            Task("wed_m", "minoxidil", "minox_morning", "09:00", "drop", TaskTint.MINOX),
            Task("wed_n", "minoxidil", "minox_night", "21:00", "moon", TaskTint.MINOX)
        )),
        Day("thu", "thu", false, listOf(
            Task("thu_m", "minoxidil", "minox_morning", "09:00", "drop", TaskTint.MINOX),
            Task("thu_n", "minoxidil", "minox_night", "21:00", "moon", TaskTint.MINOX)
        )),
        Day("fri", "fri", false, listOf(
            Task("fri_m", "minoxidil", "minox_morning", "09:00", "drop", TaskTint.MINOX),
            Task("fri_n", "minoxidil", "minox_night", "21:00", "moon", TaskTint.MINOX)
        )),
        Day("sat", "sat", true, listOf(
            Task("sat_m", "minoxidil", "minox_morning", "09:00", "drop", TaskTint.MINOX),
            Task("sat_s", "shampoo", "shampoo_cleanse", "21:30", "shower", TaskTint.SHAMPOO),
            Task("sat_d", "derma", "derma_rolling", "22:00", "derma", TaskTint.DERMA)
        ))
    )
}

/** Persists the (possibly user-edited) schedule; falls back to the default template. */
class ScheduleStore(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("hgos", Context.MODE_PRIVATE)

    fun days(): List<Day> {
        val raw = sp.getString("schedule", null) ?: return ScheduleTemplate.clone()
        return try {
            val arr = JSONArray(raw)
            if (arr.length() == 0) ScheduleTemplate.clone()
            else (0 until arr.length()).map { Day.fromJson(arr.getJSONObject(it)) }
        } catch (_: Throwable) {
            ScheduleTemplate.clone()
        }
    }

    fun save(days: List<Day>) {
        val arr = JSONArray()
        days.forEach { arr.put(it.toJson()) }
        sp.edit().putString("schedule", arr.toString()).apply()
    }

    fun reset() {
        sp.edit().remove("schedule").apply()
    }

    fun updateTask(dayId: String, taskId: String, title: String, desc: String, time: String) {
        save(days().map { d ->
            if (d.id != dayId) d
            else d.copy(tasks = d.tasks.map { t ->
                if (t.id != taskId) t
                else t.copy(
                    titleCustom = title, descCustom = desc, time = time,
                    custom = true
                )
            })
        })
    }

    fun addTask(dayId: String, title: String, desc: String, time: String) {
        save(days().map { d ->
            if (d.id != dayId) d
            else d.copy(tasks = d.tasks + Task(
                id = "custom_" + UUID.randomUUID().toString().take(8),
                titleKey = "minoxidil", descKey = "minox_morning",
                time = time, icon = "drop", tint = TaskTint.MINOX,
                titleCustom = title, descCustom = desc, custom = true
            ))
        })
    }

    fun deleteTask(dayId: String, taskId: String) {
        save(days().map { d ->
            if (d.id != dayId) d else d.copy(tasks = d.tasks.filter { it.id != taskId })
        })
    }

    /** Removes user-completion flags for tasks that no longer exist. */
    fun pruneDone(prefs: Prefs) {
        val valid = days().flatMap { it.tasks }.map { it.id }.toSet()
        val kept = prefs.done.filter { valid.contains(it) }.toSet()
        if (kept.size != prefs.done.size) prefs.replaceDone(kept)
    }
}
