package com.amirswitch.data.models

import org.json.JSONArray
import org.json.JSONObject
import java.util.TimeZone
import java.util.UUID

/**
 * Schedule data model.
 *
 * days: 1=Monday, 2=Tuesday, ... 7=Sunday
 */
data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val onTime: String = "08:00",
    val offTime: String = "18:00",
    val days: List<Int> = listOf(1, 2, 3, 4, 5),
    val enabled: Boolean = true
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("onTime", onTime)
        put("offTime", offTime)
        put("days", JSONArray(days))
        put("enabled", enabled)
    }

    companion object {
        fun fromJson(json: JSONObject): Schedule = Schedule(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name", ""),
            onTime = json.optString("onTime", "08:00"),
            offTime = json.optString("offTime", "18:00"),
            days = json.optJSONArray("days")?.let { arr ->
                (0 until arr.length()).map { arr.getInt(it) }
            } ?: listOf(1, 2, 3, 4, 5),
            enabled = json.optBoolean("enabled", true)
        )

        fun toDocument(
            schedules: List<Schedule>,
            version: Int,
            timezone: String
        ): String {
            val tz = TimeZone.getTimeZone(timezone)
            val offsetSeconds = tz.rawOffset / 1000
            return JSONObject().apply {
                put("version", version)
                put("updatedAt", System.currentTimeMillis() / 1000)
                put("timezone", timezone)
                put("utcOffsetSeconds", offsetSeconds)
                put("schedules", JSONArray().apply {
                    schedules.forEach { put(it.toJson()) }
                })
            }.toString()
        }

        fun fromDocument(
            json: String
        ): Pair<Int, List<Schedule>> {
            val doc = JSONObject(json)
            val version = doc.optInt("version", 0)
            val arr = doc.optJSONArray("schedules") ?: JSONArray()
            val schedules = (0 until arr.length()).map {
                fromJson(arr.getJSONObject(it))
            }
            return version to schedules
        }

        val DAY_NAMES = listOf(
            "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
        )
    }
}
