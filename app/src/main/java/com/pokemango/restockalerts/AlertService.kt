package com.pokemango.restockalerts

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

class AlertService : Service() {
    private var foreground = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            endPlayback()
            return START_NOT_STICKY
        }
        val alert = Alert.fromBundle(intent?.extras)
        if (alert == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        Notifier.ensureChannels(this)
        val notification = Notifier.build(this, alert, Notifier.CH_ALERTS, true)
        if (!foreground) {
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(alert.notifId(), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(alert.notifId(), notification)
            }
            foreground = true
        } else {
            Notifier.notifyQuietly(this, alert.notifId(), notification)
        }
        TonePlayer.play(this, Prefs.tone(this)) { endPlayback() }
        return START_NOT_STICKY
    }

    private fun endPlayback() {
        TonePlayer.stop()
        if (foreground) {
            stopForeground(STOP_FOREGROUND_DETACH)
            foreground = false
        }
        stopSelf()
    }

    override fun onDestroy() {
        TonePlayer.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_PLAY = "com.pokemango.restockalerts.PLAY"
        const val ACTION_STOP = "com.pokemango.restockalerts.STOP"

        fun play(ctx: Context, alert: Alert) {
            val intent = Intent(ctx, AlertService::class.java).setAction(ACTION_PLAY).putExtras(alert.toBundle())
            androidx.core.content.ContextCompat.startForegroundService(ctx, intent)
        }

        fun stop(ctx: Context) {
            TonePlayer.stop()
            ctx.stopService(Intent(ctx, AlertService::class.java))
        }
    }
}
