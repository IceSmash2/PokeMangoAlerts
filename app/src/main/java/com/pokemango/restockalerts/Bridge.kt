package com.pokemango.restockalerts

import android.content.Intent
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
    fun preview(tone: String) {
        TonePlayer.play(activity, if (tone == Prefs.TONE_SHORT) Prefs.TONE_SHORT else Prefs.TONE_FULL) { }
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
