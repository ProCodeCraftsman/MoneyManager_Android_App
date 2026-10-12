package com.moneymanager.domain.capture

import com.moneymanager.domain.ai.TransactionDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InboxRulesTest {

    private val ready = TransactionDraft(
        typeId = "expense", amount = 250.0, categoryId = 3, accountId = 7, sourceType = "SMS",
    )

    @Test
    fun `a fully deterministic expense is ready to bulk approve`() {
        assertTrue(InboxRules.isReadyToApprove(ready))
    }

    @Test
    fun `a missing category, account or amount is never pre-approved`() {
        assertFalse(InboxRules.isReadyToApprove(ready.copy(categoryId = null)))
        assertFalse(InboxRules.isReadyToApprove(ready.copy(accountId = null)))
        assertFalse(InboxRules.isReadyToApprove(ready.copy(amount = null)))
        assertFalse(InboxRules.isReadyToApprove(ready.copy(amount = 0.0)))
    }

    @Test
    fun `review flags and open duplicate questions block bulk approve`() {
        assertFalse(InboxRules.isReadyToApprove(ready.copy(needsReview = true)))
        assertFalse(InboxRules.isReadyToApprove(ready.copy(flags = listOf("verify:amount"))))
        assertFalse(InboxRules.isReadyToApprove(ready.copy(flags = listOf("${CaptureFlags.MAYBE_DUPLICATE}12"))))
    }

    @Test
    fun `transfers and other types always need a look`() {
        assertFalse(InboxRules.isReadyToApprove(ready.copy(typeId = "transfer")))
        assertFalse(InboxRules.isReadyToApprove(ready.copy(typeId = "lend")))
    }

    @Test
    fun `maybe-duplicate flag parses the transaction id and is stripped on keep both`() {
        val flags = listOf("verify:x", "${CaptureFlags.MAYBE_DUPLICATE}42")
        assertEquals(42L, CaptureFlags.maybeDuplicateOf(flags))
        assertEquals(listOf("verify:x"), CaptureFlags.withoutMaybeDuplicate(flags))
        assertNull(CaptureFlags.maybeDuplicateOf(listOf("maybe_dup:notanumber")))
    }
}
