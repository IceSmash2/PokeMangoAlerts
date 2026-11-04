package com.pokemango.restockalerts

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class PushService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        Prefs.setToken(this, token)
        Events.onToken?.invoke()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: "alert"
        if (type != "alert" && type != "test") return
        val alert = Alert.fromData(data) ?: return
        if (!Prefs.addHistory(this, alert)) return
        Events.onHistory?.invoke()
        Notifier.ensureChannels(this)
        try {
            AlertService.play(this, alert)
        } catch (e: Exception) {
            Notifier.notifyQuietly(this, alert.notifId(), Notifier.build(this, alert, Notifier.CH_BACKUP, false))
        }
    }
}
