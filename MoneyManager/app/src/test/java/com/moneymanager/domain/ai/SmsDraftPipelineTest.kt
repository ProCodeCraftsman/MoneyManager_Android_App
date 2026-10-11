package com.moneymanager.domain.ai

import com.moneymanager.data.entity.MerchantCategoryMemoryEntity
import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.data.repository.SmsAccountLinkRepository
import com.moneymanager.domain.sms.NotATransactionException
import com.moneymanager.domain.sms.SmsAccountLinks
import com.moneymanager.domain.sms.SmsCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class SmsDraftPipelineTest {

    private lateinit var memory: MerchantCategoryMemoryRepository
    private lateinit var links: SmsAccountLinkRepository
    private lateinit var pipeline: SmsDraftPipeline

    private val accounts = listOf(AccountEntry(1, "HDFC Bank", "bank"), AccountEntry(3, "Scapia Card", "credit"))

    @Before
    fun setUp() = runTest {
        memory = mock()
        links = mock()
        whenever(links.load()).thenReturn(SmsAccountLinks.EMPTY)
        pipeline = SmsDraftPipeline(memory, links)
    }

    private val swiggyBody =
        "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808"

    @Test
    fun `otp is reported as not a transaction`() = runTest {
        val outcome = pipeline.fromRules(
            "123456 is your OTP for txn of Rs.500.00 at AMAZON. Do not share it.", "AD-HDFCBK-S", null, accounts,
        )
        val exception = (outcome as SmsRuleOutcome.NotATransaction).exception
        assertTrue(exception is NotATransactionException)
        assertEquals(SmsCategory.OTP, exception.category)
    }

    @Test
    fun `known template gives a draft with the memory category applied`() = runTest {
        whenever(memory.lookupWithHistory(eq("SWIGGY"), anyOrNull()))
            .thenReturn(MerchantCategoryMemoryEntity("swiggy", 7L, "Food", "expense"))

        val draft = (pipeline.fromRules(swiggyBody, "AD-HDFCBK-S", "/receipts/a.jpg", accounts) as SmsRuleOutcome.Draft).draft

        assertEquals("expense", draft.typeId)
        assertEquals(250.0, draft.amount!!, 0.001)
        assertEquals(1L, draft.accountId)
        assertEquals(7L, draft.categoryId)
        assertEquals("/receipts/a.jpg", draft.receiptPath)
        assertFalse(draft.needsReview)
    }

    @Test
    fun `known template without memory is a draft that needs review`() = runTest {
        val draft = (pipeline.fromRules(swiggyBody, "AD-HDFCBK-S", null, accounts) as SmsRuleOutcome.Draft).draft

        assertEquals(null, draft.categoryId)
        assertTrue(draft.needsReview)
    }

    @Test
    fun `unknown text stays unmatched with the generic parse for hints`() = runTest {
        val outcome = pipeline.fromRules("Lunch with Priya was great, see you Sunday", null, null, accounts)

        assertTrue(outcome is SmsRuleOutcome.Unmatched)
        assertEquals(SmsAccountLinks.EMPTY, (outcome as SmsRuleOutcome.Unmatched).links)
    }
}
