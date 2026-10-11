package com.moneymanager.domain.sms

import com.moneymanager.domain.ai.AccountEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsVerifierTest {

    private val now = 1_800_000_000_000L

    private fun parsed(
        amount: Double? = 250.0,
        direction: TxnDirection? = TxnDirection.DEBIT,
        merchant: String? = "SWIGGY",
        date: Long? = now,
        category: SmsCategory = SmsCategory.TRANSACTION,
        matched: Boolean = true,
    ) = ParsedSms(
        category = category, direction = direction, amount = amount, currency = "INR", balance = null,
        accountHint = AccountHint("HDFC Bank", Instrument.BANK_ACCOUNT, "1234"), counterparty = merchant,
        reference = null, dateMillis = date, nature = TxnNature.NORMAL, ruleId = "r", matched = matched,
    )

    private fun verify(body: String, p: ParsedSms) = SmsVerifier.verify(body, p, now)

    @Test
    fun `a real rule match has no issues`() {
        val body = "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You?"
        val p = SmsRuleEngine.parse(SmsMessage("AD-HDFCBK-S", body, now))
        assertEquals(250.0, p.amount!!, 0.001)
        assertTrue(verify(SmsText.normalize(body), p).isEmpty())
    }

    @Test
    fun `amount is found through commas and trailing zeros`() {
        assertTrue(verify("Rs.1,234.50 debited from A/c XX1234", parsed(amount = 1234.5)).isEmpty())
        assertTrue(verify("INR 99 debited from A/c XX1234", parsed(amount = 99.0)).isEmpty())
        assertTrue(verify("Rs 99.00 debited", parsed(amount = 99.0)).isEmpty())
    }

    @Test
    fun `an amount that only matches part of a longer number is not backed`() {
        assertEquals(setOf(SmsIssue.AMOUNT_NOT_IN_TEXT), verify("Rs.1,234.50 debited", parsed(amount = 1234.0)))
        assertEquals(setOf(SmsIssue.AMOUNT_NOT_IN_TEXT), verify("Rs.12345 debited", parsed(amount = 1234.0)))
        assertEquals(setOf(SmsIssue.AMOUNT_NOT_IN_TEXT), verify("Rs.500 debited", parsed(amount = 999.0)))
    }

    @Test
    fun `a wrong amount withdraws the parse so the message falls through to the model`() {
        val checked = SmsVerifier.apply(parsed(amount = 999.0), setOf(SmsIssue.AMOUNT_NOT_IN_TEXT))
        assertNull(checked.amount)
        assertFalse(checked.matched)
        assertFalse(checked.isActionableTransaction)
        assertTrue("verify:amount_not_in_text" in checked.flags)
    }

    @Test
    fun `direction conflicts only when the text uses the opposite wording alone`() {
        assertEquals(setOf(SmsIssue.DIRECTION_CONFLICT), verify("Rs.250 credited to your A/c", parsed(matched = false)))
        assertEquals(
            setOf(SmsIssue.DIRECTION_CONFLICT),
            verify("Rs.250 debited from your A/c", parsed(direction = TxnDirection.CREDIT, matched = false)),
        )
        // A transfer message legitimately says both.
        assertTrue(verify("Rs.250 debited from A/c and credited to SWIGGY", parsed(matched = false)).isEmpty())
        // No direction wording at all (late fee style) is not a conflict.
        assertTrue(verify("Late fee of Rs.250 has been charged", parsed(merchant = null, matched = false)).isEmpty())
    }

    @Test
    fun `rules whose own wording uses the opposite verb are not direction conflicts`() {
        val bodies = listOf(
            "PTNNPS" to "PRAN 1100XXXX1234: Units for Voluntary contribution of Rs.1000.00 credited with NAV of 01/07/26 -Protean",
            "VAAHAN" to "Your PUC certificate validity is 01/01/27 and payment received for certificate is Rs.100 (excluding GST). (MoRTH)",
            "ICCSLM" to "Dear MEMBER You have deposited Rs.500 as RD with certificate No. 123 for 5 Years in Indian Cooperative Credit Society limited",
            "JIOFBR" to "Dear Customer, Thank you for your payment of Rs.1000 against JioFiber/JioAirFiber request No. AB12 Ref Transaction ID : CD34 A Voucher worth of Rs.100 will be credited to your MyJio account",
        )
        for ((sender, body) in bodies) {
            val p = SmsRuleEngine.parse(SmsMessage("AD-$sender-S", body, now))
            assertTrue("$sender should match a rule", p.matched)
            assertTrue("$sender: ${verify(SmsText.normalize(body), p)}", verify(SmsText.normalize(body), p).isEmpty())
        }
    }

    @Test
    fun `a direction conflict keeps the draft but marks it for review`() {
        val p = SmsVerifier.apply(parsed(), setOf(SmsIssue.DIRECTION_CONFLICT))
        val draft = SmsDraftMapper.toDraft(p, listOf(AccountEntry(1, "HDFC Bank", "bank")), "AD-HDFCBK-S")!!
        assertTrue(draft.needsReview)
        assertEquals("low", draft.confidence["typeId"])
    }

    @Test
    fun `junk merchants are flagged and dropped`() {
        assertEquals(setOf(SmsIssue.MERCHANT_JUNK), verify("Rs.250 debited On 02", parsed(merchant = "On")))
        assertEquals(setOf(SmsIssue.MERCHANT_JUNK), verify("Rs.250 debited 123456", parsed(merchant = "123456")))
        assertNull(SmsVerifier.apply(parsed(merchant = "On"), setOf(SmsIssue.MERCHANT_JUNK)).counterparty)
        assertTrue(verify("Rs.250 debited to SWIGGY", parsed(merchant = "SWIGGY")).isEmpty())
    }

    @Test
    fun `a date after the message could have been sent is flagged and dropped`() {
        val day = 24L * 60 * 60 * 1000
        assertEquals(setOf(SmsIssue.DATE_IN_FUTURE), verify("Rs.250 debited", parsed(date = now + 10 * day)))
        assertTrue(verify("Rs.250 debited", parsed(date = now + day / 2)).isEmpty())
        assertNull(SmsVerifier.apply(parsed(date = now + 10 * day), setOf(SmsIssue.DATE_IN_FUTURE)).dateMillis)
    }

    @Test
    fun `messages that are not transactions are left alone`() {
        assertTrue(verify("Your OTP is 123456", parsed(category = SmsCategory.OTP, amount = 5.0)).isEmpty())
    }
}
