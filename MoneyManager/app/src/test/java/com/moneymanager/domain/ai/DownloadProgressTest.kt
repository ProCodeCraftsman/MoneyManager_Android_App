package com.moneymanager.domain.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadProgressTest {

    @Test
    fun `fraction returns correct value for partial progress`() {
        val progress = DownloadProgress(50, 100, false)
        assertEquals(0.5f, progress.fraction)
    }

    @Test
    fun `fraction returns zero when total is zero`() {
        val progress = DownloadProgress(0, 0, false)
        assertEquals(0f, progress.fraction)
    }

    @Test
    fun `fraction returns one when complete`() {
        val progress = DownloadProgress(100, 100, true)
        assertEquals(1f, progress.fraction)
    }

    @Test
    fun `isComplete true when bytes match total`() {
        val progress = DownloadProgress(100, 100, true)
        assertTrue(progress.isComplete)
    }

    @Test
    fun `error is null by default`() {
        val progress = DownloadProgress(0, 100, false)
        assertEquals(null, progress.error)
    }

    @Test
    fun `error is propagated correctly`() {
        val progress = DownloadProgress(0, 100, false, "Network error")
        assertEquals("Network error", progress.error)
    }

    @Test
    fun `fraction works for complete download`() {
        val progress = DownloadProgress(100, 100, true)
        assertEquals(1f, progress.fraction)
    }
}
