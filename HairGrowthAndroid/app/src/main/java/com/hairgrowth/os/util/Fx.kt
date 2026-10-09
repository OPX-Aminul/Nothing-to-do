package com.hairgrowth.os.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hairgrowth.os.data.Prefs
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

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

    /** Reference vibration pattern: [140,70,140,70,240]. */
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

/**
 * Sine-tone player reproducing the reference WebAudio chimes natively
 * (complete / celebrate / reminder) via a short PCM buffer on AudioTrack.
 */
class TonePlayer(private val ctx: Context) {

    private val sampleRate = 44100

    fun play(kind: String) {
        if (!Prefs(ctx).sound) return
        val seq: List<Triple<Double, Double, Double>> = when (kind) {
            "complete" -> listOf(
                Triple(783.99, 0.0, 0.28),
                Triple(1046.50, 0.12, 0.34),
                Triple(1318.50, 0.26, 0.40)
            )
            "celebrate" -> listOf(
                Triple(659.25, 0.0, 0.30),
                Triple(880.00, 0.13, 0.30),
                Triple(987.77, 0.26, 0.32),
                Triple(1318.50, 0.40, 0.50)
            )
            "reminder" -> listOf(
                Triple(880.00, 0.0, 0.30),
                Triple(1174.66, 0.18, 0.32),
                Triple(880.00, 0.36, 0.40)
            )
            else -> listOf(Triple(880.00, 0.0, 0.30))
        }
        Thread { write(seq) }.start()
    }

    private fun write(seq: List<Triple<Double, Double, Double>>) {
        try {
            val end = seq.maxOf { it.second + it.third } + 0.06
            val n = (sampleRate * end).toInt()
            val buf = ShortArray(n)
            for ((freq, start, dur) in seq) {
                val s = (start * sampleRate).toInt().coerceIn(0, n)
                val e = ((start + dur) * sampleRate).toInt().coerceIn(0, n)
                for (i in s until e) {
                    val t = (i - s).toDouble() / sampleRate
                    val env = exp(-3.2 * t / dur)
                    val v = sin(2 * PI * freq * t) * env * 0.30
                    val mixed = buf[i] + (v * Short.MAX_VALUE).toInt()
                    buf[i] = mixed.coerceIn(-32767, 32767).toShort()
                }
            }
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buf, 0, buf.size)
            track.setVolume(0.9f)
            track.play()
            Thread.sleep((end * 1000).toLong() + 120)
            try {
                track.stop()
            } catch (_: Throwable) {
            }
            track.release()
        } catch (_: Throwable) {
        }
    }
}
