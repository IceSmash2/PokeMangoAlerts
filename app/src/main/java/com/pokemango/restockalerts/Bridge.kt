package com.pokemango.restockalerts

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationManagerCompat

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
