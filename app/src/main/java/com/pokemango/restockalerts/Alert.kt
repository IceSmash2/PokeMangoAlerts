package com.pokemango.restockalerts

import android.net.Uri
import android.os.Bundle
import org.json.JSONObject

data class Alert(
    val id: String,
    val type: String,
    val group: String,
    val title: String,
    val body: String,
    val url: String,
    val discord: String,
    val ts: Long,
) {
    fun notifId(): Int = Math.abs((type + id).hashCode() % 1_000_000_000) + 2

    fun toBundle(): Bundle = Bundle().apply {
        putString("id", id)
        putString("type", type)
        putString("group", group)
        putString("title", title)
        putString("body", body)
        putString("url", url)
        putString("discord", discord)
        putLong("ts", ts)
    }

    fun toJson(key: Long): JSONObject = JSONObject().apply {
        put("key", key)
        put("type", type)
        put("id", id)
        put("group", group)
        put("title", title)
        put("body", body)
        put("url", url)
        put("discord", discord)
        put("ts", ts)
    }

    companion object {
        fun safeUrl(value: String?): String {
            if (value.isNullOrEmpty() || value.length > 2000) return ""
            return try {
                val uri = Uri.parse(value)
                val scheme = uri.scheme?.lowercase()
                if ((scheme == "https" || scheme == "http") && !uri.host.isNullOrEmpty()) value else ""
            } catch (e: Exception) {
                ""
            }
        }

        fun fromData(d: Map<String, String>): Alert? {
            val type = if (d["type"] == "test") "test" else "alert"
            val now = System.currentTimeMillis()
            val id = (d["id"] ?: "").take(120).ifEmpty { "t$now" }
            return Alert(
                id = id,
                type = type,
                group = (d["group"] ?: "").take(40),
                title = (d["title"] ?: "").take(120).ifEmpty { "Restock alert" },
                body = (d["body"] ?: "").take(1800),
                url = if (type == "alert") safeUrl(d["url"]) else "",
                discord = if (type == "alert") safeUrl(d["discord"]) else "",
                ts = d["ts"]?.toLongOrNull() ?: now,
            )
        }

        fun fromBundle(b: Bundle?): Alert? {
            if (b == null) return null
            return Alert(
                id = b.getString("id") ?: return null,
                type = b.getString("type") ?: "alert",
                group = b.getString("group") ?: "",
                title = b.getString("title") ?: "Restock alert",
                body = b.getString("body") ?: "",
                url = b.getString("url") ?: "",
                discord = b.getString("discord") ?: "",
                ts = b.getLong("ts", System.currentTimeMillis()),
            )
        }
    }
}
