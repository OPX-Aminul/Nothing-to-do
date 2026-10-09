package com.hairgrowth.os.data

import android.content.Context

/**
 * Persistent state: completion flags, user preferences, reminder toggles and
 * the chosen app language.
 */
class Prefs(context: Context) {

    private val sp = context.applicationContext
        .getSharedPreferences("hgos", Context.MODE_PRIVATE)

    private val todayKey: String
        get() = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())

    // ---- preferences ----
    var haptic: Boolean
        get() = sp.getBoolean("haptic", true)
        set(v) = sp.edit().putBoolean("haptic", v).apply()

    var sound: Boolean
        get() = sp.getBoolean("sound", true)
        set(v) = sp.edit().putBoolean("sound", v).apply()

    var accent: String
        get() = sp.getString("accent", ACCENTS[0]) ?: ACCENTS[0]
        set(v) = sp.edit().putString("accent", v).apply()

    /** Language tag: "en" (default), "bn" or "hi". */
    var language: String
        get() = sp.getString("language", "en") ?: "en"
        set(v) = sp.edit().putString("language", v).apply()

    /** Theme mode: "system" (default), "light" or "dark". */
    var themeMode: String
        get() = sp.getString("themeMode", "system") ?: "system"
        set(v) = sp.edit().putString("themeMode", v).apply()

    /** Selected reminder sound key (see SoundEngine.ALARMS). */
    var alarmSound: String
        get() = sp.getString("alarmSound", "default") ?: "default"
        set(v) = sp.edit().putString("alarmSound", v).apply()

    // ---- task completion ----
    val done: Set<String>
        get() = sp.getStringSet("state", emptySet()) ?: emptySet()

    fun isDone(id: String): Boolean = done.contains(id)

    fun setDone(id: String, value: Boolean) {
        val s = HashSet(done)
        if (value) s.add(id) else s.remove(id)
        sp.edit().putStringSet("state", s).apply()
        if (value) touchStreak()
    }

    fun replaceDone(ids: Set<String>) {
        sp.edit().putStringSet("state", ids).apply()
    }

    fun doneCount(): Int = done.size

    fun resetWeek() {
        sp.edit().putStringSet("state", emptySet()).apply()
    }

    // ---- reminders ----
    fun remindersOn(): Set<String> =
        sp.getStringSet("reminders", emptySet()) ?: emptySet()

    fun isReminderOn(dayId: String): Boolean = remindersOn().contains(dayId)

    fun setReminder(dayId: String, on: Boolean) {
        val s = HashSet(remindersOn())
        if (on) s.add(dayId) else s.remove(dayId)
        sp.edit().putStringSet("reminders", s).apply()
    }

    fun setAllReminders(on: Boolean) {
        if (on) sp.edit().putStringSet("reminders", Schedule.dayOrder.toSet()).apply()
        else sp.edit().putStringSet("reminders", emptySet()).apply()
    }

    // ---- streak (date based) ----
    var streak: Int
        get() = sp.getInt("streak", 0)
        private set(v) = sp.edit().putInt("streak", v).apply()

    private var lastStreakDate: String
        get() = sp.getString("streakDate", "") ?: ""
        set(v) = sp.edit().putString("streakDate", v).apply()

    private fun touchStreak() {
        val t = todayKey
        if (lastStreakDate == t) return
        val y = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date(System.currentTimeMillis() - 86_400_000L))
        streak = if (lastStreakDate == y) streak + 1 else 1
        lastStreakDate = t
    }

    /** Accent as a vivid ARGB int, for surfaces outside the activity (notifications). */
    fun accentColorInt(): Int = when (accent.lowercase()) {
        "#7ee0c3" -> 0xFF7EE0C3.toInt()
        "#e8a0b4" -> 0xFFE8A0B4.toInt()
        "#9ec5ff" -> 0xFF9EC5FF.toInt()
        else -> 0xFFD4AF7A.toInt()
    }

    companion object {
        val ACCENTS = listOf("#d4af7a", "#7ee0c3", "#e8a0b4", "#9ec5ff")
        val LANGUAGES = listOf("en", "hi", "bn")
        val THEMES = listOf("system", "light", "dark")
    }
}
