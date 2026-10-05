package com.moneymanager.app.smsanalyzer

import com.moneymanager.domain.sms.ParseStatus
import com.moneymanager.domain.sms.SmsCategory
import com.moneymanager.domain.sms.SmsFieldExtractor
import com.moneymanager.domain.sms.TxnDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsAnalyzerTest {

    @Test
    fun `same template with different values masks identically`() {
        val a = SmsMasker.mask("Rs.1,250.00 debited from A/c XX3423 on 02-10-26. Avl Bal Rs.5,000.50. Ref 123456789012")
        val b = SmsMasker.mask("Rs.99.00 debited from A/c XX1111 on 15-03-25. Avl Bal Rs.12.00. Ref 998877665544")
        assertEquals(a, b)
        assertFalse(a.any { it.isDigit() })
    }

    @Test
    fun `masked output hides account url and vpa`() {
        val m = SmsMasker.mask("Sent Rs.10 from a/c XX9876 to rahul@okhdfc. Details https://bit.ly/abc123")
        assertFalse(m.contains("9876"))
        assertFalse(m.contains("rahul"))
        assertFalse(m.contains("bit.ly"))
    }

    @Test
    fun `normalizes operator prefixed sender`() {
        assertEquals("HDFCBK", SmsMasker.normalizeSender("AD-HDFCBK-S"))
        assertEquals("SBIINB", SmsMasker.normalizeSender("VM-SBIINB"))
        assertEquals("HDFCBK", SmsMasker.normalizeSender("HDFCBK"))
    }

    @Test
    fun `extracts debit fields`() {
        val f = SmsFieldExtractor.extract(
            "Rs.1,250.00 debited from A/c XX3423 on 02-10-26 at SWIGGY. Avl Bal Rs.5,000.50. Ref 123456789012"
        )
        assertEquals("1,250.00", f.amount)
        assertEquals("5,000.50", f.balance)
        assertEquals(TxnDirection.DEBIT, f.direction)
        assertEquals("XX3423", f.account)
        assertEquals(ParseStatus.FULL, f.status)
    }

    @Test
    fun `balance is not mistaken for amount`() {
        val f = SmsFieldExtractor.extract("Your A/c XX1234 credited with Rs.500. Avl Bal Rs.9,999")
        assertEquals("500", f.amount)
        assertEquals(TxnDirection.CREDIT, f.direction)
    }

    @Test
    fun `otp and promo are not transactions`() {
        assertEquals(SmsCategory.OTP, SmsFieldExtractor.extract("123456 is your OTP for txn of Rs.500. Do not share").category)
        assertEquals(SmsCategory.PROMO, SmsFieldExtractor.extract("Exclusive offer! Get 20% discount on loans").category)
    }

    @Test
    fun `analyzer groups by template and reports gaps`() {
        val an = SmsAnalyzer()
        an.add("AD-HDFCBK-S", "Sent Rs.100.00 From HDFC Bank A/C *1111 To SWIGGY On 01/01/26 Ref 123456789012 Not You?", 1000)
        an.add("AD-HDFCBK-S", "Sent Rs.250.00 From HDFC Bank A/C *2222 To SWIGGY On 05/02/26 Ref 998877665544 Not You?", 2000)
        an.add("VM-NEWBNK", "Rs.75 paid via card, thanks", 3000) // no rule, generic parse only -> reported as a gap
        an.add("+919876543210", "hello debited Rs.5", 4000) // personal, skipped
        val r = an.report()

        assertEquals(4, r.scanned)
        assertEquals(1, r.skippedPersonal)
        assertEquals(2, r.templates.size)
        assertEquals(2, r.templates.first().count)
        assertEquals(ParseStatus.FULL, r.templates.first().status)
        assertEquals(1, r.gapTemplates.size)
        assertEquals("NEWBNK", r.gapTemplates.first().sender)
        assertTrue(r.toJson().contains("\"templates\""))
    }

    @Test
    fun `credit card wording does not make a loan promo a transaction`() {
        val f = SmsFieldExtractor.extract(
            "Get a Personal Loan up to Rs.5 lakh credited to your Savings Account through your ICICI Bank Credit Card. Give a missed call."
        )
        assertEquals(SmsCategory.PROMO, f.category)
    }

    @Test
    fun `scapia card spend is a debit and complete without account number`() {
        val f = SmsFieldExtractor.extract(
            "Hi! Your txn of Rs.450.00 at ZOMATO on your Scapia Federal RuPay credit card was successful. Not you? Go to Settings on the app."
        )
        assertEquals(TxnDirection.DEBIT, f.direction)
        assertEquals(SmsCategory.TRANSACTION, f.category)
        assertEquals(ParseStatus.FULL, f.status)
    }

    @Test
    fun `scheduled autopay is a reminder not a transaction`() {
        val f = SmsFieldExtractor.extract(
            "Dear UPI User, UPI AutoPay for SAFE GOLD debit of Rs.500.00 is scheduled on 05-10-26, abc@sbi. Please ensure sufficient balance."
        )
        assertEquals(SmsCategory.REMINDER, f.category)
    }

    @Test
    fun `user name is masked in greetings and by supplied names`() {
        assertEquals("Dear <NAME>, hello", SmsMasker.mask("Dear Swaraj, hello"))
        val m = SmsMasker.mask("Info: UPI/ZOMATO:SWARAJ K S. done", listOf("Swaraj", "K"))
        assertFalse(m.uppercase().contains("SWARAJ"))
    }

    @Test
    fun `promo prose is not merchant masked and bank account phrase is kept`() {
        assertFalse(SmsMasker.mask("Get 75GB data at Airtel Postpaid for Rs.399. Upgrade now").contains("<MERCHANT>"))
        assertTrue(SmsMasker.mask("Rs.100 debited from HDFC Bank A/c XX1234 on 02-10-26. Avl bal Rs.5").contains("<ACC>"))
    }

    @Test
    fun `name variants with initials are all masked`() {
        val names = listOf("Swaraj", "K", "S")
        assertEquals("To <NAME> On", SmsMasker.mask("To SWARAJ K S On", names))
        assertEquals("Info <NAME>. Avl", SmsMasker.mask("Info SWARAJKS. Avl", names))
        assertEquals("Hello <NAME> now", SmsMasker.mask("Hello KS now", names))
    }

    @Test
    fun `failed payment and wallet and premium receipt`() {
        assertEquals(
            SmsCategory.FAILED,
            SmsFieldExtractor.extract("Your last payment attempt of Rs.399 was unsuccessful. Transaction ID : ABC12345. amount deducted refund").category
        )
        val w = SmsFieldExtractor.extract("Rs.50 debited from your RWallet Account on 01-01-26. Available Balance Rs.10.")
        assertEquals(ParseStatus.FULL, w.status)
        assertEquals(
            SmsCategory.TRANSACTION,
            SmsFieldExtractor.extract("We have received your premium payment of Rs.5000 for policy no. ABC1234567. T&C apply.").category
        )
    }
}
