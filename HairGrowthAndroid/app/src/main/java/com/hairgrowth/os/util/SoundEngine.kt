package com.hairgrowth.os.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.hairgrowth.os.R
import com.hairgrowth.os.data.Prefs

/**
 * Plays the app's built-in (procedurally generated, license-free) sounds.
 * Short UI cues go through SoundPool for low latency; reminder tones play
 * through MediaPlayer so they can be auditioned from Settings.
 */
class SoundEngine(private val ctx: Context) {

    private val prefs = Prefs(ctx)
    private var pool: SoundPool? = null
    private val loaded = HashMap<Int, Int>()
    private var alarms: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null

    private fun ensurePool(): SoundPool {
        pool?.let { return it }
        val p = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()
        pool = p
        return p
    }

    private fun cue(resId: Int, volume: Float) {
        if (!prefs.sound) return
        try {
            val p = ensurePool()
            var id = loaded[resId]
            if (id == null) {
                id = p.load(ctx, resId, 1)
                loaded[resId] = id
            }
            if (id != 0) p.play(id, volume, volume, 1, 0, 1f)
        } catch (_: Throwable) {
        }
    }

    fun tap() = cue(R.raw.ui_tap, 0.55f)

    fun complete() = cue(R.raw.ui_complete, 0.75f)

    fun celebrate() = cue(R.raw.ui_celebrate, 0.85f)

    /** Plays the chosen reminder tone (used by the Test button and previews). */
    fun playAlarm(key: String = prefs.alarmSound) {
        if (!prefs.sound) return
        try {
            alarms?.release()
            alarms = MediaPlayer.create(ctx, resFor(key))?.apply {
                setVolume(0.9f, 0.9f)
                start()
            }
        } catch (_: Throwable) {
        }
    }

    /** Audition a tone from the Settings picker regardless of the sound toggle. */
    fun preview(key: String) {
        try {
            previewPlayer?.release()
            previewPlayer = MediaPlayer.create(ctx, resFor(key))?.apply {
                setVolume(0.9f, 0.9f)
                start()
            }
        } catch (_: Throwable) {
        }
    }

    fun release() {
        try {
            pool?.release()
        } catch (_: Throwable) {
        }
        pool = null
        loaded.clear()
        alarms?.release(); alarms = null
        previewPlayer?.release(); previewPlayer = null
    }

    companion object {
        /** Keys offered in Settings. */
        val ALARMS = listOf("default", "chime", "bell", "marimba", "pulse", "digital")

        fun resFor(key: String): Int = when (key) {
            "default" -> R.raw.alarm_default
            "bell" -> R.raw.alarm_bell
            "marimba" -> R.raw.alarm_marimba
            "pulse" -> R.raw.alarm_pulse
            "digital" -> R.raw.alarm_digital
            else -> R.raw.alarm_chime
        }

        fun channelSuffix(key: String): String =
            if (ALARMS.contains(key)) key else "default"

        /** Localized human name of a tone (used in Settings and channel names). */
        fun labelRes(key: String): Int = when (key) {
            "chime" -> R.string.tone_chime
            "bell" -> R.string.tone_bell
            "marimba" -> R.string.tone_marimba
            "pulse" -> R.string.tone_pulse
            "digital" -> R.string.tone_digital
            else -> R.string.tone_default
        }
    }
}
