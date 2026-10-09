package com.hairgrowth.os

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.reminder.NotificationHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        applySavedLanguage()
        createChannel()
    }

    /**
     * The app's main language is English; the user may switch to Hindi or
     * Bangla from Settings. A language chosen at system level takes precedence.
     */
    private fun applySavedLanguage() {
        try {
            if (AppCompatDelegate.getApplicationLocales().isEmpty) {
                val tag = Prefs(this).language
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
            }
        } catch (_: Throwable) {
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(NotificationHelper.CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            NotificationHelper.CHANNEL_ID,
            getString(R.string.channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(140, 70, 140, 70, 240)
            enableLights(true)
            lightColor = 0xFFD4AF7A.toInt()
            setShowBadge(true)
            setSound(
                android.provider.Settings.System.DEFAULT_NOTIFICATION_URI,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
        nm.createNotificationChannel(channel)
    }
}
