package com.pokemango.restockalerts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Prefs {
    const val TONE_FULL = "full"
    const val TONE_SHORT = "short"
    const val TONE_CUSTOM = "custom"
    private const val HISTORY_LIMIT = 100
    private val lock = Any()

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("ra", Context.MODE_PRIVATE)

    fun normalizeTone(ctx: Context, tone: String?): String = when (tone) {
        TONE_SHORT -> TONE_SHORT
        TONE_CUSTOM -> if (CustomSound.exists(ctx)) TONE_CUSTOM else TONE_FULL
        else -> TONE_FULL
    }

    fun tone(ctx: Context): String = normalizeTone(ctx, sp(ctx).getString("tone", TONE_FULL))

    fun setTone(ctx: Context, tone: String) {
        sp(ctx).edit().putString("tone", normalizeTone(ctx, tone)).apply()
    }

    fun customName(ctx: Context): String = sp(ctx).getString("custom_name", "") ?: ""

    fun setCustomName(ctx: Context, name: String) {
        sp(ctx).edit().putString("custom_name", name).apply()
    }

    fun token(ctx: Context): String = sp(ctx).getString("token", "") ?: ""

    fun setToken(ctx: Context, token: String) {
        sp(ctx).edit().putString("token", token).apply()
    }

    fun history(ctx: Context): String = synchronized(lock) { sp(ctx).getString("history", "[]") ?: "[]" }

    fun clearHistory(ctx: Context) {
        synchronized(lock) { sp(ctx).edit().putString("history", "[]").apply() }
    }

    /** Returns false when this alert is already in the list. */
    fun addHistory(ctx: Context, alert: Alert): Boolean = synchronized(lock) {
        val prefs = sp(ctx)
        val rows = try { JSONArray(prefs.getString("history", "[]")) } catch (e: Exception) { JSONArray() }
        for (i in 0 until rows.length()) {
            val row = rows.optJSONObject(i) ?: continue
            if (row.optString("id") == alert.id && row.optString("type") == alert.type) return false
        }
        val key = prefs.getLong("history_key", 0L) + 1
        val kept = mutableListOf<JSONObject>()
        for (i in 0 until rows.length()) rows.optJSONObject(i)?.let { kept.add(it) }
        kept.add(alert.toJson(key))
        val trimmed = if (kept.size > HISTORY_LIMIT) kept.subList(kept.size - HISTORY_LIMIT, kept.size) else kept
        val out = JSONArray()
        trimmed.forEach { out.put(it) }
        prefs.edit().putString("history", out.toString()).putLong("history_key", key).apply()
        true
    }
}
