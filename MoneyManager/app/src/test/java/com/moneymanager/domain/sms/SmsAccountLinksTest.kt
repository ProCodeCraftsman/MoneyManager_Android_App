package com.moneymanager.domain.sms

import com.moneymanager.domain.ai.AccountEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsAccountLinksTest {

    private val salary = AccountEntry(5, "Salary", "bank")
    private val spare = AccountEntry(6, "Spare", "bank")
    private val accounts = listOf(salary, spare, AccountEntry(7, "Travel Card", "credit"))

    private fun hint(last4: String?, institution: String? = "HDFC Bank") =
        AccountHint(institution, Instrument.BANK_ACCOUNT, last4)

    @Test
    fun `a learned link resolves an account whose name shares nothing with the bank`() {
        val key = SmsAccountLinks.keyFor(hint("1234"))!!
        val links = SmsAccountLinks(mapOf(key to 5L))
        assertEquals(salary, links.resolve(hint("1234"), accounts))
        assertEquals(salary, AccountMatcher.match(hint("1234"), accounts, links))
    }

    @Test
    fun `without a link the name matcher still has to find the account`() {
        assertNull(AccountMatcher.match(hint("1234"), accounts))
    }

    @Test
    fun `a link to an account that is gone is ignored`() {
        val links = SmsAccountLinks(mapOf(SmsAccountLinks.keyFor(hint("1234"))!! to 99L))
        assertNull(links.resolve(hint("1234"), accounts))
    }

    @Test
    fun `digits never seen before do not borrow another card's link`() {
        val links = SmsAccountLinks(mapOf(SmsAccountLinks.keyFor(hint("1234"))!! to 5L))
        assertNull(links.resolve(hint("9999"), accounts))
    }

    @Test
    fun `a message with no digits borrows the link only when every link for that source agrees`() {
        val one = SmsAccountLinks(mapOf(SmsAccountLinks.keyFor(hint("1234"))!! to 5L))
        assertEquals(salary, one.resolve(hint(null), accounts))

        val sameAccountTwice = SmsAccountLinks(
            mapOf(SmsAccountLinks.keyFor(hint("1234"))!! to 5L, SmsAccountLinks.keyFor(hint("5678"))!! to 5L),
        )
        assertEquals(salary, sameAccountTwice.resolve(hint(null), accounts))

        val disagreeing = SmsAccountLinks(
            mapOf(SmsAccountLinks.keyFor(hint("1234"))!! to 5L, SmsAccountLinks.keyFor(hint("5678"))!! to 6L),
        )
        assertNull(disagreeing.resolve(hint(null), accounts))
    }

    @Test
    fun `sources are told apart by institution and instrument`() {
        val links = SmsAccountLinks(mapOf(SmsAccountLinks.keyFor(hint("1234"))!! to 5L))
        assertNull(links.resolve(hint("1234", institution = "ICICI Bank"), accounts))
        assertNull(links.resolve(AccountHint("HDFC Bank", Instrument.CREDIT_CARD, "1234"), accounts))
    }

    @Test
    fun `a hint that says nothing has no key`() {
        assertNull(SmsAccountLinks.keyFor(null))
        assertNull(SmsAccountLinks.keyFor(AccountHint(null, Instrument.OTHER, null)))
    }

    @Test
    fun `key is stable across spelling of the institution`() {
        assertEquals(SmsAccountLinks.keyFor(hint("1234", "HDFC Bank")), SmsAccountLinks.keyFor(hint("1234", "hdfc  bank")))
    }
}
