package com.pokemango.restockalerts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Prefs {
    const val TONE_FULL = "full"
    const val TONE_SHORT = "short"
    private const val HISTORY_LIMIT = 100
    private val lock = Any()

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences("ra", Context.MODE_PRIVATE)

    fun tone(ctx: Context): String =
        if (sp(ctx).getString("tone", TONE_FULL) == TONE_SHORT) TONE_SHORT else TONE_FULL

    fun setTone(ctx: Context, tone: String) {
        sp(ctx).edit().putString("tone", if (tone == TONE_SHORT) TONE_SHORT else TONE_FULL).apply()
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
