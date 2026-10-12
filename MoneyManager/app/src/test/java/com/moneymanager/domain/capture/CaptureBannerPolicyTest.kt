package com.moneymanager.domain.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureBannerPolicyTest {

    private val today = 20_000L

    @Test
    fun `shows when something is pending and not dismissed`() {
        assertTrue(CaptureBannerPolicy.isVisible(pending = 3, dismissedEpochDay = null, hidden = false, todayEpochDay = today))
    }

    @Test
    fun `hidden when nothing is pending`() {
        assertFalse(CaptureBannerPolicy.isVisible(pending = 0, dismissedEpochDay = null, hidden = false, todayEpochDay = today))
    }

    @Test
    fun `dismissed today stays hidden, but comes back tomorrow`() {
        assertFalse(CaptureBannerPolicy.isVisible(pending = 2, dismissedEpochDay = today, hidden = false, todayEpochDay = today))
        assertTrue(CaptureBannerPolicy.isVisible(pending = 2, dismissedEpochDay = today, hidden = false, todayEpochDay = today + 1))
    }

    @Test
    fun `don't show this wins over everything`() {
        assertFalse(CaptureBannerPolicy.isVisible(pending = 9, dismissedEpochDay = null, hidden = true, todayEpochDay = today))
    }
}
