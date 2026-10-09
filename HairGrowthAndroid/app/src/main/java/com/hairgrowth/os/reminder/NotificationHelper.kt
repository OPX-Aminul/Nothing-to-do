package com.hairgrowth.os.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.hairgrowth.os.MainActivity
import com.hairgrowth.os.R

object NotificationHelper {

    const val CHANNEL_ID = "hgos_reminders"

    private fun openIntent(ctx: Context): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        return PendingIntent.getActivity(
            ctx, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun post(ctx: Context, title: String, body: String, tag: String) {
        if (!canPost(ctx)) return
        val notif = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_dna)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setColor(0xFFD4AF7A.toInt())
            .setAutoCancel(true)
            .setContentIntent(openIntent(ctx))
            .setVibrate(longArrayOf(140, 70, 140, 70, 240))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        val nm = ContextCompat.getSystemService(ctx, NotificationManager::class.java) ?: return
        try {
            nm.notify(tag, 0, notif)
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
