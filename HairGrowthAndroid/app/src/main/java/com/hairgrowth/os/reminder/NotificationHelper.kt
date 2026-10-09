package com.hairgrowth.os.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hairgrowth.os.MainActivity
import com.hairgrowth.os.R
import com.hairgrowth.os.data.Prefs
import com.hairgrowth.os.util.SoundEngine

/**
 * Notification plumbing. Reminder sounds are user-selectable, and Android ties
 * a sound to the *channel*, so one channel is prepared per selectable tone —
 * switching the tone in Settings switches the channel used for reminders.
 */
object NotificationHelper {

    /** Channel used for the default tone (kept stable for existing installs). */
    const val CHANNEL_BASE = "hgos_reminders"

    fun channelId(key: String): String =
        if (key == "default") CHANNEL_BASE else CHANNEL_BASE + "_" + SoundEngine.channelSuffix(key)

    private fun audioAttributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** `android.resource://<pkg>/<resId>` for the built-in reminder tone. */
    fun toneUri(ctx: Context, key: String): Uri =
        Uri.parse("android.resource://${ctx.packageName}/${SoundEngine.resFor(key)}")

    private fun manager(ctx: Context): NotificationManager? =
        ContextCompat.getSystemService(ctx, NotificationManager::class.java)

    /** Creates every tone channel (idempotent) — safe to call on each launch. */
    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = manager(ctx) ?: return
        SoundEngine.ALARMS.forEach { key ->
            val id = channelId(key)
            if (nm.getNotificationChannel(id) != null) return@forEach
            val label = ctx.getString(SoundEngine.labelRes(key))
            val name = if (key == "default") {
                ctx.getString(R.string.channel_name)
            } else {
                ctx.getString(R.string.channel_name_tone, label)
            }
            val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                description = ctx.getString(R.string.channel_desc)
                enableVibration(true)
                vibrationPattern = longArrayOf(140, 70, 140, 70, 240)
                enableLights(true)
                lightColor = 0xFFD4AF7A.toInt()
                setShowBadge(true)
                setSound(toneUri(ctx, key), audioAttributes())
            }
            nm.createNotificationChannel(channel)
        }
    }

    /** Renders the launcher icon into a bitmap (used as the notification large icon). */
    fun appIcon(ctx: Context): Bitmap? = try {
        val drawable = ContextCompat.getDrawable(ctx, R.mipmap.ic_launcher) ?: null
        val size = ctx.resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable?.setBounds(0, 0, size, size)
        drawable?.draw(canvas)
        bmp
    } catch (_: Throwable) {
        null
    }

    private fun openIntent(ctx: Context): android.app.PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return android.app.PendingIntent.getActivity(
            ctx, 0, i,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun post(ctx: Context, title: String, body: String, tag: String) {
        if (!canPost(ctx)) return
        val prefs = Prefs(ctx)
        val key = prefs.alarmSound
        val channel = channelId(key)

        val builder = NotificationCompat.Builder(ctx, channel)
            // Small icon = the app's own mark (status-bar silhouette).
            .setSmallIcon(R.drawable.ic_stat_hairgrowth)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(prefs.accentColorInt())
            .setAutoCancel(true)
            .setContentIntent(openIntent(ctx))
            .setVibrate(longArrayOf(140, 70, 140, 70, 240))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        // Pre-Android 8 has no channels, so the tone rides on the notification.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setSound(toneUri(ctx, key))
        }
        appIcon(ctx)?.let { builder.setLargeIcon(it) }

        val nm = manager(ctx) ?: return
        try {
            nm.notify(tag, 0, builder.build())
        } catch (_: SecurityException) {
        }
    }

    /** Advance notice: "starts in N minutes". */
    fun postAdvance(
        ctx: Context,
        title: String,
        startTime: String,
        minutes: Int,
        tag: String
    ) {
        val t = ctx.getString(R.string.notif_advance_title, title, minutes)
        val b = ctx.getString(R.string.notif_advance_body, title, startTime)
        post(ctx, t, b, tag)
    }

    /** On-time reminder. */
    fun postNow(
        ctx: Context,
        title: String,
        desc: String,
        dayLabel: String?,
        tag: String,
        multiple: Boolean
    ) {
        val t = if (multiple && dayLabel != null) {
            ctx.getString(R.string.notif_day_title, dayLabel)
        } else {
            ctx.getString(R.string.notif_now_title, title)
        }
        val b = ctx.getString(R.string.notif_now_body, title, desc)
        post(ctx, t, b, tag)
    }
}
