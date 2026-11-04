package com.pokemango.restockalerts

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationManagerCompat
import org.json.JSONArray
import org.json.JSONObject

class Bridge(private val activity: MainActivity) {
    @JavascriptInterface
    fun token(): String = Prefs.token(activity)

    @JavascriptInterface
    fun notificationsEnabled(): Boolean = NotificationManagerCompat.from(activity).areNotificationsEnabled()

    @JavascriptInterface
    fun requestNotifications() {
        activity.runOnUiThread { activity.askForNotifications() }
    }

    @JavascriptInterface
    fun openSettings() {
        activity.runOnUiThread {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            try { activity.startActivity(intent) } catch (e: Exception) { /* settings unavailable */ }
        }
    }

    @JavascriptInterface
    fun getTone(): String = Prefs.tone(activity)

    @JavascriptInterface
    fun setTone(tone: String) {
        Prefs.setTone(activity, tone)
    }

    @JavascriptInterface
    fun customName(): String = if (CustomSound.exists(activity)) Prefs.customName(activity) else ""

    @JavascriptInterface
    fun pickFile() {
        activity.runOnUiThread { activity.launchFilePicker() }
    }

    @JavascriptInterface
    fun pickSystemSound() {
        activity.runOnUiThread { activity.launchSystemPicker() }
    }

    @JavascriptInterface
    fun preview(tone: String) {
        TonePlayer.play(activity, Prefs.normalizeTone(activity, tone)) { }
    }

    @JavascriptInterface
    fun openChannelSettings() {
        activity.runOnUiThread {
            val channel = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, Notifier.CH_ALERTS)
            try {
                activity.startActivity(channel)
            } catch (e: Exception) {
                openSettings()
            }
        }
    }

    @JavascriptInterface
    fun openBatterySettings() {
        activity.runOnUiThread {
            val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", activity.packageName, null))
            try { activity.startActivity(details) } catch (e: Exception) { /* settings unavailable */ }
        }
    }

    @JavascriptInterface
    fun openSoundSettings() {
        activity.runOnUiThread {
            try {
                activity.startActivity(Intent(Settings.ACTION_SOUND_SETTINGS))
            } catch (e: Exception) { /* settings unavailable */ }
        }
    }

    @JavascriptInterface
    fun diagnose(): String {
        val issues = JSONArray()
        fun add(id: String, title: String, fix: String, action: String, label: String) {
            issues.put(JSONObject().put("id", id).put("title", title).put("fix", fix)
                .put("action", action).put("label", label))
        }
        val manager = activity.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!NotificationManagerCompat.from(activity).areNotificationsEnabled()) {
            add("notif_off", "Notifications are off for this app",
                "Alerts can't reach you until notifications are turned on.", "app", "Open notification settings")
        } else {
            val channel = manager.getNotificationChannel(Notifier.CH_ALERTS)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
                add("channel_off", "The alert category is turned off",
                    "Turn the alerts category back on in the notification settings.", "channel", "Open alert settings")
            }
        }
        val audio = activity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.getStreamVolume(AudioManager.STREAM_ALARM) == 0) {
            add("alarm_volume", "Alarm volume is at zero",
                "The alert sound plays at alarm volume. Raise it in your sound settings.", "sound", "Open sound settings")
        }
        if (manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            manager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN) {
            add("dnd", "Do Not Disturb is on",
                "Alerts may be silenced. Allow this app's alert category to override Do Not Disturb.", "channel", "Open alert settings")
        }
        return issues.toString()
    }

    @JavascriptInterface
    fun stopSound() {
        AlertService.stop(activity)
    }

    @JavascriptInterface
    fun history(): String = Prefs.history(activity)

    @JavascriptInterface
    fun clearHistory() {
        Prefs.clearHistory(activity)
    }

    @JavascriptInterface
    fun dismissAll() {
        AlertService.stop(activity)
        NotificationManagerCompat.from(activity).cancelAll()
    }

    @JavascriptInterface
    fun retry() {
        activity.runOnUiThread { activity.load() }
    }
}
