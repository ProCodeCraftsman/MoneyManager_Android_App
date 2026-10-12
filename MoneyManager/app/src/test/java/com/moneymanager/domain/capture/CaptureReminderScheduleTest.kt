package com.moneymanager.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class CaptureReminderScheduleTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    private fun at(text: String): Long = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `before noon the next trigger is today at noon`() {
        assertEquals(at("2026-10-11T12:00"), CaptureReminderSchedule.nextTriggerMillis(at("2026-10-11T08:30"), zone))
    }

    @Test
    fun `between noon and evening the next trigger is the evening slot`() {
        assertEquals(at("2026-10-11T21:00"), CaptureReminderSchedule.nextTriggerMillis(at("2026-10-11T12:00"), zone))
    }

    @Test
    fun `after the evening slot the next trigger is tomorrow noon`() {
        assertEquals(at("2026-10-12T12:00"), CaptureReminderSchedule.nextTriggerMillis(at("2026-10-11T21:00"), zone))
        assertEquals(at("2026-10-12T12:00"), CaptureReminderSchedule.nextTriggerMillis(at("2026-10-11T23:50"), zone))
    }

    @Test
    fun `slot order does not depend on the order given`() {
        val next = CaptureReminderSchedule.nextTriggerMillis(at("2026-10-11T10:00"), zone, listOf(21 * 60, 12 * 60))
        assertEquals(at("2026-10-11T12:00"), next)
    }
}
