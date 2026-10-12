package com.moneymanager.domain.capture

import com.moneymanager.domain.ai.TransactionDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AccountSourcesTest {

    private fun pending(id: Long, key: String?) = PendingCapture(
        ledgerId = id,
        draft = TransactionDraft(typeId = "expense", amount = 10.0, accountLink = key),
        messageDate = 0L,
    )

    @Test
    fun `labels read the institution, instrument and last four`() {
        assertEquals("Hdfc · credit card ••1234", AccountSources.label("hdfc|CREDIT_CARD|1234"))
        assertEquals("Icici · bank account", AccountSources.label("icici|BANK_ACCOUNT|"))
    }

    @Test
    fun `card sources suggest a credit account, others a bank account`() {
        assertEquals("credit", AccountSources.suggestedAccountType("hdfc|CREDIT_CARD|1234"))
        assertEquals("bank", AccountSources.suggestedAccountType("hdfc|BANK_ACCOUNT|9999"))
        assertEquals("bank", AccountSources.suggestedAccountType("upi|WALLET|"))
    }

    @Test
    fun `drafts group by source, most waiting first, with the mapped account when linked`() {
        val rows = listOf(
            pending(1, "hdfc|BANK_ACCOUNT|1111"),
            pending(2, "hdfc|CREDIT_CARD|2222"),
            pending(3, "hdfc|CREDIT_CARD|2222"),
            pending(4, null),
        )
        val summary = AccountSources.summarise(rows, links = mapOf("hdfc|CREDIT_CARD|2222" to 9L))

        assertEquals(listOf("hdfc|CREDIT_CARD|2222", "hdfc|BANK_ACCOUNT|1111"), summary.map { it.key })
        assertEquals(2, summary[0].draftCount)
        assertEquals(9L, summary[0].mappedAccountId)
        assertNull(summary[1].mappedAccountId)
    }
}
