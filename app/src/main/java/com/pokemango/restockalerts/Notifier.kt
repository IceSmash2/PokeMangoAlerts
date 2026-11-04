package com.pokemango.restockalerts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    const val CH_ALERTS = "alerts"
    const val CH_BACKUP = "alerts_backup"
    private val VIBRATION = longArrayOf(0, 600, 200, 600, 200, 600, 200, 1200)

    fun ensureChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alerts = NotificationChannel(CH_ALERTS, "Restock alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Restock alerts. The app plays the sound itself, on the alarm volume."
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val backup = NotificationChannel(CH_BACKUP, "Restock alerts (backup sound)", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Used only if the app can't start its own sound."
            setSound(
                Uri.parse("android.resource://${ctx.packageName}/${R.raw.alert_short}"),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            enableVibration(true)
            vibrationPattern = VIBRATION
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        nm.createNotificationChannels(listOf(alerts, backup))
    }

    private fun openIntent(ctx: Context, code: Int, url: String): PendingIntent {
        val intent = if (url.isEmpty()) {
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        } else {
            Intent(ctx, OpenActivity::class.java).putExtra("url", url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getActivity(ctx, code, intent, flags)
    }

    private fun stopIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, AlertService::class.java).setAction(AlertService.ACTION_STOP)
        return PendingIntent.getService(ctx, 1, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun build(ctx: Context, a: Alert, channel: String, playing: Boolean): Notification {
        val primary = a.url.ifEmpty { a.discord }
        val code = (a.notifId() * 31 + primary.hashCode())
        val builder = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat)
            .setContentTitle(a.title)
            .setContentText(a.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(a.body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setWhen(a.ts)
            .setShowWhen(true)
            .setAutoCancel(true)
            .setContentIntent(openIntent(ctx, code, primary))

        if (a.discord.isNotEmpty() && a.discord != primary) {
            builder.addAction(0, "Open Discord", openIntent(ctx, code + 1, a.discord))
        }
        if (playing) {
            builder.addAction(0, "Stop sound", stopIntent(ctx))
            builder.setDeleteIntent(stopIntent(ctx))
        }
        return builder.build()
    }

    fun notifyQuietly(ctx: Context, id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(ctx).notify(id, notification)
        } catch (e: SecurityException) {
            // notifications are not allowed; nothing to show
        }
    }
}
