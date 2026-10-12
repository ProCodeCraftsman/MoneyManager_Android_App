package com.moneymanager.domain.capture

import java.time.Instant
import java.time.ZoneId

/**
 * Reminder slots as minutes after local midnight (F07 E9). Alarms are inexact: the window lets the system batch
 * them, so no exact-alarm permission is needed.
 */
object CaptureReminderSchedule {
    const val NOON_MINUTE = 12 * 60
    const val EVENING_MINUTE = 21 * 60
    val SLOTS = listOf(NOON_MINUTE, EVENING_MINUTE)
    const val WINDOW_MS = 30 * 60 * 1000L

    /** The first slot strictly after [now] in [zone], looking at today and tomorrow. */
    fun nextTriggerMillis(now: Long, zone: ZoneId, slots: List<Int> = SLOTS): Long {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return (0L..1L)
            .flatMap { dayOffset ->
                val day = today.plusDays(dayOffset).atStartOfDay(zone)
                slots.sorted().map { day.plusMinutes(it.toLong()).toInstant().toEpochMilli() }
            }
            .first { it > now }
    }
}
