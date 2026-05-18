package com.moneymanager.ui.dialog

import com.moneymanager.domain.ai.TransactionDraft
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the pure predicate helper functions in AddEditTransactionDialog:
 *   - showReviewBanner(draft: TransactionDraft?): Boolean
 *   - fieldIsLowConfidence(fieldName: String, draft: TransactionDraft?): Boolean
 *
 * These are the RED-phase tests; they will fail until the functions are added in Task 2.
 */
class ConfidenceBannerStateTest {

    // ── showReviewBanner tests ──

    @Test
    fun `showReviewBanner returns true when needsReview is true`() {
        val draft = TransactionDraft(needsReview = true)
        assertTrue(showReviewBanner(draft))
    }

    @Test
    fun `showReviewBanner returns false when needsReview is false`() {
        val draft = TransactionDraft(needsReview = false)
        assertFalse(showReviewBanner(draft))
    }

    @Test
    fun `showReviewBanner returns false when draft is null`() {
        assertFalse(showReviewBanner(null))
    }

    // ── fieldIsLowConfidence tests ──

    @Test
    fun `fieldIsLowConfidence returns true when confidence for field is low`() {
        val draft = TransactionDraft(confidence = mapOf("amount" to "low"))
        assertTrue(fieldIsLowConfidence("amount", draft))
    }

    @Test
    fun `fieldIsLowConfidence returns false when confidence for field is high`() {
        val draft = TransactionDraft(confidence = mapOf("amount" to "high"))
        assertFalse(fieldIsLowConfidence("amount", draft))
    }

    @Test
    fun `fieldIsLowConfidence returns false when draft is null`() {
        assertFalse(fieldIsLowConfidence("amount", null))
    }
}
