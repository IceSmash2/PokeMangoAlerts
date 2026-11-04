package com.pokemango.restockalerts

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.core.content.ContextCompat

class AlertService : Service() {
    private var foreground = false
    private var volumeReceiver: BroadcastReceiver? = null
    private var volumeObserver: ContentObserver? = null
    private var watchStarted = 0L
    private var watchVolume = 0

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
        watchVolumeKeys()
        return START_NOT_STICKY
    }

    private fun watchVolumeKeys() {
        unwatchVolumeKeys()
        val audio = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        watchVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        watchStarted = SystemClock.elapsedRealtime()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val stream = intent?.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1)
                if (stream == AudioManager.STREAM_ALARM) silenced()
            }
        }
        ContextCompat.registerReceiver(
            this, receiver, IntentFilter("android.media.VOLUME_CHANGED_ACTION"), ContextCompat.RECEIVER_EXPORTED,
        )
        volumeReceiver = receiver

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                if (audio.getStreamVolume(AudioManager.STREAM_ALARM) != watchVolume) silenced()
            }
        }
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, observer)
        volumeObserver = observer
    }

    private fun unwatchVolumeKeys() {
        volumeReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { /* already removed */ }
        }
        volumeReceiver = null
        volumeObserver?.let { contentResolver.unregisterContentObserver(it) }
        volumeObserver = null
    }

    private fun silenced() {
        if (SystemClock.elapsedRealtime() - watchStarted < 800) return
        endPlayback()
    }

    private fun endPlayback() {
        unwatchVolumeKeys()
        TonePlayer.stop()
        if (foreground) {
            stopForeground(STOP_FOREGROUND_DETACH)
            foreground = false
        }
        stopSelf()
    }

    override fun onDestroy() {
        unwatchVolumeKeys()
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
