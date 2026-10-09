package com.hairgrowth.os

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.reminder.NotificationHelper

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        applyNightMode()
        applySavedLanguage()
        NotificationHelper.ensureChannels(this)
    }

    /**
     * Day / night handling: the app follows the system by default (light when
     * the device is in day mode, dark at night) and the user may override it
     * from Settings → Appearance.
     */
    private fun applyNightMode() {
        val mode = when (Prefs(this).themeMode) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        if (AppCompatDelegate.getDefaultNightMode() != mode) {
            AppCompatDelegate.setDefaultNightMode(mode)
        }
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
}
