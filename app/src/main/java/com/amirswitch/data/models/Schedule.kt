package com.amirswitch.data.models

/**
 * Schedule data model matching Firebase structure.
 *
 * days: 1=Monday, 2=Tuesday, ... 7=Sunday
 */
data class Schedule(
    val id: String = "",
    val name: String = "",
    val onTime: String = "08:00",   // HH:mm format
    val offTime: String = "18:00",  // HH:mm format
    val days: List<Int> = listOf(1, 2, 3, 4, 5), // Mon-Fri default
    val enabled: Boolean = true
) {
    /**
     * Convert to a Map for Firebase storage.
     */
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "onTime" to onTime,
        "offTime" to offTime,
        "days" to days,
        "enabled" to enabled
    )

    companion object {
        /**
         * Create a Schedule from a Firebase snapshot map.
         */
        fun fromMap(id: String, map: Map<String, Any?>): Schedule {
            return Schedule(
                id = id,
                name = map["name"] as? String ?: "",
                onTime = map["onTime"] as? String ?: "08:00",
                offTime = map["offTime"] as? String ?: "18:00",
                days = (map["days"] as? List<*>)?.mapNotNull {
                    (it as? Long)?.toInt() ?: (it as? Int)
                } ?: listOf(1, 2, 3, 4, 5),
                enabled = map["enabled"] as? Boolean ?: true
            )
        }

        val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    }
}
