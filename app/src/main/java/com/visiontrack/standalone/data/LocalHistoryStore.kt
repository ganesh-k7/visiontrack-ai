package com.visiontrack.standalone.data

import android.content.Context
import com.visiontrack.standalone.model.HistoryItem
import org.json.JSONArray
import org.json.JSONObject

class LocalHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("visiontrack_history", Context.MODE_PRIVATE)

    fun load(): List<HistoryItem> {
        val raw = prefs.getString("items", "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(
                    HistoryItem(
                        type = o.optString("type"),
                        title = o.optString("title"),
                        subtitle = o.optString("subtitle"),
                        timestamp = o.optLong("timestamp")
                    )
                )
            }
        }.sortedByDescending { it.timestamp }
    }

    fun add(item: HistoryItem) {
        val current = load().toMutableList()
        current.add(0, item)
        val array = JSONArray()
        current.take(100).forEach {
            array.put(
                JSONObject()
                    .put("type", it.type)
                    .put("title", it.title)
                    .put("subtitle", it.subtitle)
                    .put("timestamp", it.timestamp)
            )
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    fun clear() {
        prefs.edit().remove("items").apply()
    }
}
