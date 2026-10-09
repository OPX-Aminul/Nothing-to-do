package com.hairgrowth.os.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hairgrowth.os.data.Prefs

/** Haptic feedback, honouring the user's haptic preference. */
object Fx {

    fun vibrator(ctx: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val m = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            m?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun tap(ctx: Context, ms: Long = 35) {
        if (!Prefs(ctx).haptic) return
        val v = vibrator(ctx) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") v.vibrate(ms)
            }
        } catch (_: Throwable) {
        }
    }

    /** Reminder vibration pattern: [140,70,140,70,240]. */
    fun reminderPattern(ctx: Context) {
        val v = vibrator(ctx) ?: return
        val pattern = longArrayOf(0, 140, 70, 140, 70, 240)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION") v.vibrate(pattern, -1)
            }
        } catch (_: Throwable) {
        }
    }

    fun celebratePattern(ctx: Context) {
        if (!Prefs(ctx).haptic) return
        val v = vibrator(ctx) ?: return
        val pattern = longArrayOf(0, 90, 50, 90, 50, 140)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION") v.vibrate(pattern, -1)
            }
        } catch (_: Throwable) {
        }
    }
}
