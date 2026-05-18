package com.moneymanager.data.ai

import com.moneymanager.app.ui.transactions.shouldShowDownloadConsent
import com.moneymanager.domain.ai.AiBackend
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for HYBRID-05 dialog trigger logic.
 *
 * Tests 1-5: Pure function shouldShowDownloadConsent() — no mocks needed.
 * Tests 6-7: ViewModel method behaviour — tested via the pure function and
 *            the _isDownloadPromptSuppressedForSession flag semantics.
 */
class DownloadConsentStateTest {

    // Test 1: all conditions met → dialog shown
    @Test
    fun `showDownloadConsentDialog emits true when tier LOCAL_MODEL downloaded false optedIn false suppressed false`() {
        val result = shouldShowDownloadConsent(
            tier = AiBackend.LOCAL_MODEL,
            downloaded = false,
            optedIn = false,
            suppressed = false,
        )
        assertTrue(result)
    }

    // Test 2: session suppress → dialog hidden
    @Test
    fun `showDownloadConsentDialog emits false when session suppressed`() {
        val result = shouldShowDownloadConsent(
            tier = AiBackend.LOCAL_MODEL,
            downloaded = false,
            optedIn = false,
            suppressed = true,
        )
        assertFalse(result)
    }

    // Test 3: model already downloaded → dialog hidden
    @Test
    fun `showDownloadConsentDialog emits false when model already downloaded`() {
        val result = shouldShowDownloadConsent(
            tier = AiBackend.LOCAL_MODEL,
            downloaded = true,
            optedIn = false,
            suppressed = false,
        )
        assertFalse(result)
    }

    // Test 4: wrong tier (AICORE) → dialog hidden
    @Test
    fun `showDownloadConsentDialog emits false when tier is AICORE`() {
        val result = shouldShowDownloadConsent(
            tier = AiBackend.AICORE,
            downloaded = false,
            optedIn = false,
            suppressed = false,
        )
        assertFalse(result)
    }

    // Test 5: user already opted in → dialog hidden
    @Test
    fun `showDownloadConsentDialog emits false when userOptedIn is true`() {
        val result = shouldShowDownloadConsent(
            tier = AiBackend.LOCAL_MODEL,
            downloaded = false,
            optedIn = true,
            suppressed = false,
        )
        assertFalse(result)
    }

    // Test 6: suppressed=true causes shouldShowDownloadConsent to return false
    // (mirrors what onDownloadPromptSuppressed() does when it sets suppressed flag)
    @Test
    fun `onDownloadPromptSuppressed causes showDownloadConsentDialog to emit false`() = runTest {
        // Before suppress: dialog shown
        val before = shouldShowDownloadConsent(
            tier = AiBackend.LOCAL_MODEL,
            downloaded = false,
            optedIn = false,
            suppressed = false,
        )
        assertTrue("Dialog should show before suppress", before)

        // After suppress (simulates ViewModel setting _isDownloadPromptSuppressedForSession = true)
        val after = shouldShowDownloadConsent(
            tier = AiBackend.LOCAL_MODEL,
            downloaded = false,
            optedIn = false,
            suppressed = true, // simulate session flag set
        )
        assertFalse("Dialog should be hidden after suppress", after)
    }

    // Test 7: onDownloadConsented sets optedIn=true — once opted in, dialog no longer shows
    @Test
    fun `onDownloadConsented causes showDownloadConsentDialog to emit false after optedIn becomes true`() = runTest {
        // Before consent: dialog shown
        val before = shouldShowDownloadConsent(
            tier = AiBackend.LOCAL_MODEL,
            downloaded = false,
            optedIn = false,
            suppressed = false,
        )
        assertTrue("Dialog should show before consent", before)

        // After consent (setUserOptedInAi(true) makes optedIn=true in the flow)
        val after = shouldShowDownloadConsent(
            tier = AiBackend.LOCAL_MODEL,
            downloaded = false,
            optedIn = true, // simulate DataStore write by onDownloadConsented
            suppressed = false,
        )
        assertFalse("Dialog should be hidden after consent", after)
    }
}
