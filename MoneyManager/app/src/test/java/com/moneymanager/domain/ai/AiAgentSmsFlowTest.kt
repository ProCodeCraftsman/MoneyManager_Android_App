package com.moneymanager.domain.ai

import com.moneymanager.app.ui.aidraft.AgentStatus
import com.moneymanager.data.ai.PreExtraction
import com.moneymanager.data.entity.MerchantCategoryMemoryEntity
import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.data.repository.SmsAccountLinkRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.domain.sms.NotATransactionException
import com.moneymanager.domain.sms.SmsAccountLinks
import com.moneymanager.domain.sms.SmsCategory
import com.moneymanager.domain.sms.SmsMessage
import com.moneymanager.domain.sms.SmsRuleEngine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AiAgentSmsFlowTest {

    private lateinit var textUseCase: GenerateDraftFromTextUseCase
    private lateinit var imageUseCase: GenerateDraftFromImageUseCase
    private lateinit var memory: MerchantCategoryMemoryRepository
    private lateinit var repository: TransactionRepository
    private lateinit var links: SmsAccountLinkRepository
    private lateinit var chooser: SmsCategoryChooser
    private lateinit var agent: AiAgent

    private val context = PromptContext(
        top20Categories = emptyList(),
        accounts = listOf(AccountEntry(1, "HDFC Bank", "bank"), AccountEntry(3, "Scapia Card", "credit")),
        peers = emptyList(),
        tags = emptyList(),
    )

    @Before
    fun setUp() {
        textUseCase = mock()
        imageUseCase = mock()
        memory = mock()
        repository = mock()
        links = mock()
        runBlocking { whenever(links.load()).thenReturn(SmsAccountLinks.EMPTY) }
        chooser = mock()
        agent = AiAgent(textUseCase, imageUseCase, memory, repository, links, chooser)
    }

    private suspend fun run(text: String, sender: String?) =
        agent.processText(text, context, "SMS", sender).toList()

    @Test
    fun `otp is rejected without calling the ai`() = runTest {
        val events = run("123456 is your OTP for txn of Rs.500.00 at AMAZON. Do not share it.", "AD-HDFCBK-S")
        val failure = events.last() as AgentStatus.Failure
        assertTrue(failure.throwable is NotATransactionException)
        assertEquals(SmsCategory.OTP, (failure.throwable as NotATransactionException).category)
        verify(textUseCase, never()).invoke(any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `reminder is rejected`() = runTest {
        val events = run(
            "Dear RAHUL, UPI AutoPay for INDmoney debit of Rs.500.00 is scheduled on .05-10-26, abc@sbi. Please ensure sufficient balance in your account. -SBI",
            "AD-SBIUPI-S",
        )
        assertEquals(SmsCategory.REMINDER, ((events.last() as AgentStatus.Failure).throwable as NotATransactionException).category)
    }

    @Test
    fun `known template produces a draft without the ai and fills the category from memory`() = runTest {
        whenever(memory.lookupWithHistory(eq("SWIGGY"), anyOrNull())).thenReturn(MerchantCategoryMemoryEntity("swiggy", 7L, "Food", "expense"))
        val events = run(
            "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808",
            "AD-HDFCBK-S",
        )
        val draft = (events.last() as AgentStatus.Success).draft
        assertEquals("expense", draft.typeId)
        assertEquals(250.0, draft.amount!!, 0.001)
        assertEquals(1L, draft.accountId)
        assertEquals(7L, draft.categoryId)
        assertEquals("Food", draft.categoryName)
        assertEquals(false, draft.needsReview)
        verify(textUseCase, never()).invoke(any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `a learned account link fills the account when no account name matches`() = runTest {
        val body = "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You?"
        val key = SmsAccountLinks.keyFor(SmsRuleEngine.parse(SmsMessage("AD-HDFCBK-S", body)).accountHint)!!
        whenever(links.load()).thenReturn(SmsAccountLinks(mapOf(key to 5L)))
        val salaryOnly = context.copy(accounts = listOf(AccountEntry(5, "Salary", "bank")))

        val draft = (agent.processText(body, salaryOnly, "SMS", "AD-HDFCBK-S").toList().last() as AgentStatus.Success).draft

        assertEquals(5L, draft.accountId)
        assertEquals(key, draft.accountLink)
    }

    @Test
    fun `the model picks a category only when memory and history have none, and the draft stays for review`() = runTest {
        whenever(chooser.choose(any(), any())).thenReturn(CategoryEntry(9L, "Travel", "expense"))
        val draft = (run(
            "Sent Rs.250.00 From HDFC Bank A/C *1234 To KERALA VISI On 02/10/26 Ref 123456789012 Not You?",
            "AD-HDFCBK-S",
        ).last() as AgentStatus.Success).draft

        assertEquals(9L, draft.categoryId)
        assertTrue(draft.flags.contains("ai_category"))
        assertTrue(draft.needsReview)
        assertEquals("medium", draft.confidence["categoryId"])
    }

    @Test
    fun `a memory hit skips the model`() = runTest {
        whenever(memory.lookupWithHistory(eq("SWIGGY"), anyOrNull())).thenReturn(MerchantCategoryMemoryEntity("swiggy", 7L, "Food", "expense"))
        run("Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You?", "AD-HDFCBK-S")
        verify(chooser, never()).choose(any(), any())
    }

    @Test
    fun `memory category of a different type is not applied`() = runTest {
        whenever(memory.lookupWithHistory(eq("SWIGGY"), anyOrNull())).thenReturn(MerchantCategoryMemoryEntity("swiggy", 7L, "Food", "income"))
        val events = run(
            "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You?",
            "AD-HDFCBK-S",
        )
        val draft = (events.last() as AgentStatus.Success).draft
        assertNull(draft.categoryId)
        assertTrue(draft.needsReview)
    }

    @Test
    fun `unknown template goes to the ai with rule hints`() = runTest {
        val aiDraft = TransactionDraft(typeId = "expense", amount = 640.0, categoryId = 9L, accountId = 1L)
        whenever(textUseCase.invoke(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Result.success(aiDraft))
        val events = run("Your A/c XX4321 is debited by Rs.640.00 on 02-10-26 at CAFE COFFEE DAY. Avl Bal Rs.2,000.00", "AD-NEWBNK-S")

        assertEquals(9L, (events.last() as AgentStatus.Success).draft.categoryId)
        val pre = argumentCaptor<PreExtraction>()
        verify(textUseCase).invoke(any(), any(), any(), anyOrNull(), pre.capture())
        assertEquals(640.0, pre.firstValue.amount!!, 0.001)
        assertEquals("expense", pre.firstValue.typeId)
        assertTrue(pre.firstValue.hints.contains("accountLast4=4321"))
        assertTrue(pre.firstValue.hints.contains("balanceAfter=2000.0"))
    }

    @Test
    fun `ai failure falls back to the generic sms draft`() = runTest {
        whenever(textUseCase.invoke(any(), any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(AiUnavailableException("no model")))
        val events = run("Your A/c XX4321 is debited by Rs.640.00 on 02-10-26 at CAFE COFFEE DAY. Avl Bal Rs.2,000.00", "AD-NEWBNK-S")
        val draft = (events.last() as AgentStatus.Success).draft
        assertEquals(640.0, draft.amount!!, 0.001)
        assertTrue(draft.flags.contains("ai_fallback"))
        assertTrue(draft.needsReview)
    }

    @Test
    fun `pasted free text without a bank format still reaches the ai`() = runTest {
        val aiDraft = TransactionDraft(typeId = "expense", amount = 450.0)
        whenever(textUseCase.invoke(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Result.success(aiDraft))
        val events = run("paid 450 to ravi for lunch", null)
        assertEquals(450.0, (events.last() as AgentStatus.Success).draft.amount!!, 0.001)
        verify(textUseCase).invoke(any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `a model-picked category from the ai draft is not written to merchant memory`() = runTest {
        val aiDraft = TransactionDraft(typeId = "expense", amount = 640.0, categoryId = 9L, categoryName = "Travel", accountId = 1L)
        whenever(textUseCase.invoke(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Result.success(aiDraft))
        val draft = (run("Your A/c XX4321 is debited by Rs.640.00 on 02-10-26 at CAFE COFFEE DAY. Avl Bal Rs.2,000.00", "AD-NEWBNK-S")
            .last() as AgentStatus.Success).draft

        assertEquals(9L, draft.categoryId)
        verify(memory, never()).record(any(), any(), any(), anyOrNull())
    }

    @Test
    fun `a model category pick on a rule draft is not written to merchant memory`() = runTest {
        whenever(chooser.choose(any(), any())).thenReturn(CategoryEntry(9L, "Travel", "expense"))
        run("Sent Rs.250.00 From HDFC Bank A/C *1234 To KERALA VISI On 02/10/26 Ref 123456789012 Not You?", "AD-HDFCBK-S")
        verify(memory, never()).record(any(), any(), any(), anyOrNull())
    }

    @Test
    fun `an image draft with a category is not written to merchant memory`() = runTest {
        val imageDraft = TransactionDraft(
            typeId = "expense", amount = 320.0, categoryId = 7L, categoryName = "Food",
            merchantHint = "Cafe Coffee Day", confidence = mapOf("merchant" to "high"),
        )
        whenever(imageUseCase.invoke(any(), any(), any())).thenReturn(Result.success(imageDraft))
        val draft = (agent.processImage(ByteArray(1), context).toList().last() as AgentStatus.Success).draft

        assertEquals(7L, draft.categoryId)
        verify(memory, never()).record(any(), any(), any(), anyOrNull())
    }

    @Test
    fun `ai failure on text with nothing parseable stays a failure`() = runTest {
        whenever(textUseCase.invoke(any(), any(), any(), anyOrNull(), anyOrNull()))
            .thenReturn(Result.failure(AiUnavailableException("no model")))
        val events = run("lunch with ravi", null)
        assertTrue(events.last() is AgentStatus.Failure)
    }
}
