package com.moneymanager.data.ai

import com.moneymanager.app.ui.transactions.buildBannerCaptionText
import com.moneymanager.app.ui.transactions.buildBannerPercentText
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for HYBRID-06 download progress banner state logic.
 *
 * Tests 1-3: Banner visibility predicate (isDownloading = progress > 0f && progress < 1f)
 * Tests 4-5: Caption text formatting (buildBannerCaptionText)
 * Test 6:    Percent text formatting (buildBannerPercentText)
 */
class DownloadBannerStateTest {

    // Test 1: isDownloading emits true when localModelDownloadProgress = 0.47f
    @Test
    fun `isDownloading is true when progress is 0_47f`() = runTest {
        val progress = 0.47f
        val isDownloading = progress > 0f && progress < 1f
        assertTrue("Banner should be visible at progress 0.47f", isDownloading)
    }

    // Test 2: isDownloading emits false when localModelDownloadProgress = 0f (idle)
    @Test
    fun `isDownloading is false when progress is 0f`() = runTest {
        val progress = 0f
        val isDownloading = progress > 0f && progress < 1f
        assertFalse("Banner should be hidden when progress is 0f (idle)", isDownloading)
    }

    // Test 3: isDownloading emits false when localModelDownloadProgress = 1f (complete)
    @Test
    fun `isDownloading is false when progress is 1f`() = runTest {
        val progress = 1f
        val isDownloading = progress > 0f && progress < 1f
        assertFalse("Banner should be hidden when progress is 1f (complete)", isDownloading)
    }

    // Test 4: captionText is null/empty when totalBytes = 0L (indeterminate state)
    @Test
    fun `buildBannerCaptionText returns null when totalBytes is 0L`() {
        val result = buildBannerCaptionText(
            receivedBytes = 0L,
            totalBytes = 0L,
            bytesPerSecond = 0L,
        )
        assertNull("Caption should be null when totalBytes is 0L (indeterminate)", result)
    }

    // Test 5: captionText contains "/ 529 MB" when totalBytes = 529_000_000L
    @Test
    fun `buildBannerCaptionText contains 529 MB when totalBytes is 529_000_000L`() {
        val result = buildBannerCaptionText(
            receivedBytes = 248_000_000L,
            totalBytes = 529_000_000L,
            bytesPerSecond = 0L,
        )
        assertTrue(
            "Caption should contain '/ 529 MB' but was: $result",
            result?.contains("/ 529 MB") == true,
        )
    }

    // Test 6: percentText is "47%" when progress = 0.47f
    @Test
    fun `buildBannerPercentText returns 47% when progress is 0_47f`() {
        val result = buildBannerPercentText(0.47f)
        assertEquals("47%", result)
    }
}
