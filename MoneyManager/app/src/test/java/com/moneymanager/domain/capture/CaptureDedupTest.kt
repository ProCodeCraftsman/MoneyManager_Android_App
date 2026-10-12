package com.moneymanager.domain.capture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureDedupTest {

    private val now = 100 * DAY
    private val swiggy = DedupDraft(typeId = "expense", amount = 450.0, date = now, accountId = 7, merchantHint = "SWIGGY")

    private fun saved(
        id: Long = 1,
        amount: Double = 450.0,
        date: Long = now - HOUR,
        accountId: Long? = 7,
        text: String = "Swiggy order",
        type: String? = "expense",
    ) = CaptureCandidate(id, type, amount, date, accountId, text)

    private fun pending(id: Long = 50, merchant: String = "SWIGGY", accountId: Long? = 7) =
        CaptureCandidate(id, "expense", 450.0, now - HOUR, accountId, merchant)

    @Test
    fun `reference number match wins and points at the saved transaction`() {
        val verdict = CaptureDedup.classify(swiggy, LedgerMatch(ledgerId = 3, transactionId = 99), emptyList(), emptyList())
        assertEquals(DedupVerdict.SavedMatch(99), verdict)
    }

    @Test
    fun `reference number match on a pending row merges into that draft`() {
        val verdict = CaptureDedup.classify(swiggy, LedgerMatch(ledgerId = 3, transactionId = null), emptyList(), emptyList())
        assertEquals(DedupVerdict.PendingMatch(3), verdict)
    }

    @Test
    fun `same amount account and merchant inside the window is a saved match`() {
        val verdict = CaptureDedup.classify(swiggy, null, listOf(saved()), emptyList())
        assertEquals(DedupVerdict.SavedMatch(1), verdict)
    }

    @Test
    fun `same amount with no merchant match is uncertain, never silently dropped`() {
        val verdict = CaptureDedup.classify(swiggy, null, listOf(saved(text = "")), emptyList())
        assertEquals(DedupVerdict.Uncertain(1), verdict)
    }

    @Test
    fun `different account is uncertain even when the merchant matches`() {
        val verdict = CaptureDedup.classify(swiggy, null, listOf(saved(accountId = 8)), emptyList())
        assertEquals(DedupVerdict.Uncertain(1), verdict)
    }

    @Test
    fun `outside the 48 hour window is new`() {
        val old = saved(date = now - CaptureDedup.WINDOW_MS - HOUR)
        assertEquals(DedupVerdict.New, CaptureDedup.classify(swiggy, null, listOf(old), emptyList()))
    }

    @Test
    fun `different amount or type is new`() {
        assertEquals(DedupVerdict.New, CaptureDedup.classify(swiggy, null, listOf(saved(amount = 451.0)), emptyList()))
        assertEquals(DedupVerdict.New, CaptureDedup.classify(swiggy, null, listOf(saved(type = "income")), emptyList()))
    }

    @Test
    fun `pending draft with the same merchant merges into it`() {
        val verdict = CaptureDedup.classify(swiggy, null, emptyList(), listOf(pending()))
        assertEquals(DedupVerdict.PendingMatch(50), verdict)
    }

    @Test
    fun `a saved match is preferred over a pending match`() {
        val verdict = CaptureDedup.classify(swiggy, null, listOf(saved()), listOf(pending()))
        assertEquals(DedupVerdict.SavedMatch(1), verdict)
    }

    @Test
    fun `short merchant strings never match`() {
        assertFalse(CaptureDedup.merchantMatches("tea", "teaspoon shop"))
        assertFalse(CaptureDedup.merchantMatches(null, "Swiggy"))
        assertTrue(CaptureDedup.merchantMatches("SWIGGY", "swiggy order 12"))
    }

    @Test
    fun `unknown draft account matches any account`() {
        val noAccount = swiggy.copy(accountId = null)
        assertEquals(DedupVerdict.SavedMatch(1), CaptureDedup.classify(noAccount, null, listOf(saved(accountId = 9)), emptyList()))
    }

    private companion object {
        const val HOUR = 60 * 60 * 1000L
        const val DAY = 24 * HOUR
    }
}
