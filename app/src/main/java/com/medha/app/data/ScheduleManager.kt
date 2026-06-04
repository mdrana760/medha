package com.medha.app.data

import java.util.Calendar

/**
 * Decides whether Medha should be active right now based on the user's
 * configured active hours (e.g. only 9 PM–9 AM). Supports windows that wrap
 * past midnight.
 */
class ScheduleManager(private val prefs: Prefs) {

    fun isActiveNow(now: Calendar = Calendar.getInstance()): Boolean {
        if (!prefs.scheduleEnabled) return true
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val start = prefs.scheduleStartHour
        val end = prefs.scheduleEndHour
        return if (start == end) {
            true // 24h window
        } else if (start < end) {
            hour in start until end
        } else {
            // Wraps midnight, e.g. 21 -> 9.
            hour >= start || hour < end
        }
    }
}
