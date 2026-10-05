package com.moneymanager.domain.sms

import com.moneymanager.domain.ai.AccountEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsDraftMapperTest {

    private val accounts = listOf(
        AccountEntry(1, "HDFC Bank", "bank"),
        AccountEntry(2, "SBI Savings", "savings"),
        AccountEntry(3, "Scapia Card", "credit"),
        AccountEntry(4, "ICICI Amazon Pay", "credit"),
        AccountEntry(5, "Ravi", "peer"),
        AccountEntry(6, "Cash", "cash"),
    )

    private fun parse(sender: String, body: String) = SmsRuleEngine.parse(SmsMessage(sender, body))

    @Test
    fun `upi expense matches the bank account by institution`() {
        val p = parse("AD-HDFCBK-S", "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You?")
        val d = SmsDraftMapper.toDraft(p, accounts, "AD-HDFCBK-S")!!
        assertEquals("expense", d.typeId)
        assertEquals(250.0, d.amount!!, 0.001)
        assertEquals(1L, d.accountId)
        assertEquals("SWIGGY", d.merchantHint)
        assertEquals("SMS", d.sourceType)
        assertTrue(d.flags.contains("sms_rule:hdfc.upi-sent"))
        assertFalse(d.needsReview)
    }

    @Test
    fun `credit card spend matches the credit account not the bank`() {
        val p = parse("AD-FEDSCP-S", "Hi! Your txn of Rs.450.00 at ZOMATO on your Scapia Federal RuPay credit card was successful. Not you?")
        assertEquals(3L, SmsDraftMapper.toDraft(p, accounts, "AD-FEDSCP-S")!!.accountId)
    }

    @Test
    fun `ambiguous or unknown institution leaves the account for the user`() {
        // Two credit accounts, neither named like the issuer.
        val p = parse("AX-AXISBK-S", "Spent Card no. XX1234 INR 450.00 02-10-26 14:05:33 KERALA VISI Avl Lmt INR 50,000.00 SMS BLOCK 1234 to 919951860002")
        val d = SmsDraftMapper.toDraft(p, accounts, "AX-AXISBK-S")!!
        assertNull(d.accountId)
        assertTrue(d.needsReview)
    }

    @Test
    fun `last digits in the account name disambiguate`() {
        val twoCards = listOf(AccountEntry(10, "Axis Card 1234", "credit"), AccountEntry(11, "Axis Card 9999", "credit"))
        val p = parse("AX-AXISBK-S", "Spent Card no. XX1234 INR 450.00 02-10-26 14:05:33 KERALA VISI Avl Lmt INR 50,000.00 SMS BLOCK 1234 to 919951860002")
        assertEquals(10L, SmsDraftMapper.toDraft(p, twoCards, null)!!.accountId)
    }

    @Test
    fun `card repayment becomes a transfer into the card`() {
        val p = parse("AD-FEDSCP-S", "Yay! We've received your payment of Rs.15,000.00 towards your Scapia Federal credit card. -Federal Bank")
        val d = SmsDraftMapper.toDraft(p, accounts, null)!!
        assertEquals("transfer", d.typeId)
        assertEquals(3L, d.toAccountId)
        assertNull(d.accountId)
        assertTrue(d.flags.contains("card_repayment"))
        assertTrue(d.needsReview)
    }

    @Test
    fun `refund and cashback are income`() {
        val refund = parse("AD-SLCBNK-S", "Refund of Rs.99.00 from AMAZON has been successfully processed to your credit card XX1234 - slice")
        val d = SmsDraftMapper.toDraft(refund, accounts, null)!!
        assertEquals("income", d.typeId)
        assertTrue(d.flags.contains("refund"))
        assertEquals("AMAZON", d.merchantHint)
    }

    @Test
    fun `mutual fund purchase is savings with the scheme as platform`() {
        val p = parse("AD-TATAMF-S", "Your Purchase SIP in folio 1234567/89 in scheme Tata Small Cap Fund Dir Pl Gr for Rs.1,500.00 for 10.5 units at NAV Rs.20.0 dated 02-10-2026 is successful with applicable load")
        val d = SmsDraftMapper.toDraft(p, accounts, null)!!
        assertEquals("savings", d.typeId)
        assertEquals("Tata Small Cap Fund Dir Pl Gr", d.investmentPlatform)
        assertTrue(d.flags.contains("investment"))
    }

    @Test
    fun `foreign currency needs review and keeps the flag`() {
        val p = parse("VM-ICICIT-S", "USD 12.00 spent using ICICI Bank Card XX1234 on 02-Oct-26 on PADDLE.NET* VPS. Avl Limit: INR 98,750.00. If not you, call 1800 2662.")
        val d = SmsDraftMapper.toDraft(p, accounts, null)!!
        assertTrue(d.needsReview)
        assertTrue(d.flags.contains("foreign_currency:USD"))
    }

    @Test
    fun `generic parse produces a review draft`() {
        val p = parse("AD-NEWBNK-S", "Your A/c XX4321 is debited by Rs.640.00 on 02-10-26 at CAFE COFFEE DAY. Avl Bal Rs.2,000.00")
        val d = SmsDraftMapper.toDraft(p, accounts, "AD-NEWBNK-S")!!
        assertEquals("expense", d.typeId)
        assertTrue(d.needsReview)
        assertTrue(d.flags.contains("sms_generic"))
    }

    @Test
    fun `no draft without amount or direction`() {
        val p = parse("AD-X-S", "Hello there, your order has shipped")
        assertNull(SmsDraftMapper.toDraft(p, accounts, null))
    }

    @Test
    fun `peers are never matched as accounts`() {
        val peersOnly = listOf(AccountEntry(5, "HDFC Ravi", "peer"))
        val p = parse("AD-HDFCBK-S", "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You?")
        assertNull(SmsDraftMapper.toDraft(p, peersOnly, null)!!.accountId)
    }

    @Test
    fun `cleanCounterparty strips upi prefixes and vpas`() {
        assertEquals("Samudra", SmsRuleEngine.cleanCounterparty("UPI-123456789012-Samudra"))
        assertEquals("zomato", SmsRuleEngine.cleanCounterparty("zomato@hdfcbank"))
        assertEquals("RAHUL KUMAR", SmsRuleEngine.cleanCounterparty("RAHUL KUMAR abc@okhdfc"))
        assertNull(SmsRuleEngine.cleanCounterparty("..."))
    }

    @Test
    fun `date parser handles bank formats`() {
        assertTrue(SmsDateParser.parse("02-10-26") != null)
        assertTrue(SmsDateParser.parse("30Mar25") != null)
        assertTrue(SmsDateParser.parse("02-OCT-26") != null)
        assertTrue(SmsDateParser.parse("02/10/2026.") != null)
        assertTrue(SmsDateParser.parse("October 5, 2026") != null)
        assertNull(SmsDateParser.parse("nonsense"))
        assertEquals(SmsDateParser.parse("02-10-26"), SmsDateParser.parse("02/10/2026"))
    }
}
