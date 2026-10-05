package com.moneymanager.domain.sms

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsRuleEngineTest {

    private fun parse(sender: String?, body: String) = SmsRuleEngine.parse(SmsMessage(sender, body))

    private fun ParsedSms.assertTxn(
        rule: String,
        direction: TxnDirection,
        amount: Double,
        merchant: String? = null,
        instrument: Instrument? = null,
        last4: String? = null,
        nature: TxnNature = TxnNature.NORMAL,
    ) {
        assertEquals("rule", rule, ruleId)
        assertEquals(SmsCategory.TRANSACTION, category)
        assertEquals(direction, this.direction)
        assertEquals(amount, this.amount!!, 0.001)
        assertEquals(nature, this.nature)
        if (merchant != null) assertEquals(merchant, counterparty)
        if (instrument != null) assertEquals(instrument, accountHint?.instrument)
        if (last4 != null) assertEquals(last4, accountHint?.last4)
        assertTrue(matched)
    }

    // ── Credit cards ───────────────────────────────────────────────────────────

    @Test
    fun `scapia card spend`() {
        parse(
            "AD-FEDSCP-S",
            "Hi! Your txn of Rs.450.00 at ZOMATO on your Scapia Federal RuPay credit card was successful. Not you? Go to Settings on the app.- Federal Bank"
        ).assertTxn("fedscp.card-spend", TxnDirection.DEBIT, 450.0, "ZOMATO", Instrument.CREDIT_CARD)
    }

    @Test
    fun `scapia declined is failed`() {
        val p = parse(
            "AD-FEDSCP-S",
            "Oh no! Your txn of Rs.450.00 at ZOMATO is declined as you've disabled the Domestic ECOM facility on your Scapia Federal Visa credit card."
        )
        assertEquals(SmsCategory.FAILED, p.category)
        assertTrue(p.isDefinitelyNotATransaction)
    }

    @Test
    fun `scapia fuel waiver refund and card payment`() {
        parse(
            "AD-FEDSCP-S",
            "Hi! You have received a fuel surcharge waiver of Rs.12.00 for your fuel transaction at BHARAT PETROLEUM on 02-10-2026 14:05:33using your Scapia Federal RuPay credit card - Federal Bank"
        ).assertTxn("fedscp.fuel-waiver", TxnDirection.CREDIT, 12.0, "BHARAT PETROLEUM", nature = TxnNature.CASHBACK)
        parse(
            "AD-FEDSCP-S",
            "Hi! You've received a refund of Rs.999.00 from AMAZON on your Scapia Federal Visa credit card. If you have opted for an EMI, please cancel."
        ).assertTxn("fedscp.refund", TxnDirection.CREDIT, 999.0, "AMAZON", nature = TxnNature.REFUND)
        parse("AD-FEDSCP-S", "Yay! We've received your payment of Rs.15,000.00 towards your Scapia Federal credit card. -Federal Bank")
            .assertTxn("fedscp.card-payment", TxnDirection.CREDIT, 15000.0, nature = TxnNature.CARD_REPAYMENT)
    }

    @Test
    fun `icici card spend variants`() {
        parse(
            "VM-ICICIT-S",
            "INR 1,250.00 spent using ICICI Bank Card XX1234 on 02-Oct-26 on BOOKMYSHOW. Avl Limit: INR 98,750.00. If not you, call 1800 2662/SMS BLOCK 1234 to 9215676766."
        ).assertTxn("icici.card-spent-using", TxnDirection.DEBIT, 1250.0, "BOOKMYSHOW", Instrument.CREDIT_CARD, "1234")
        parse(
            "VM-ICICIT-S",
            "USD 12.00 spent using ICICI Bank Card XX1234 on 02-Oct-26 on PADDLE.NET* VPS. Avl Limit: INR 98,750.00. If not you, call 1800 2662/SMS BLOCK 1234 to 9215676766."
        ).also {
            it.assertTxn("icici.card-spent-using", TxnDirection.DEBIT, 12.0, "PADDLE.NET* VPS")
            assertTrue(it.flags.contains("foreign_currency:USD"))
        }
        parse(
            "VM-ICICIT-S",
            "INR 500.00 spent on ICICI Bank Card XX1234 on 02-Oct-26 at PAY*CLEARTRIP C. Avl Lmt: INR 90,000.00. To dispute, call 1800 2662."
        ).assertTxn("icici.card-spent-at", TxnDirection.DEBIT, 500.0, "PAY*CLEARTRIP C")
        parse(
            "VM-ICICIT-S",
            "ICICI Bank Credit Card XX1234 debited for INR 300.00 on 02-Oct-26 for UPI-123456789012-Samudra. To dispute call 1800/SMS BLOCK 1234 to 9215676766"
        ).assertTxn("icici.card-upi", TxnDirection.DEBIT, 300.0, "Samudra")
        parse("VM-ICICIT-S", "Payment of INR 5,000.00 has been received on your ICICI Bank Credit Card XX1234 through Bharat Bill Payment System on 02-Oct-26.")
            .assertTxn("icici.card-payment", TxnDirection.CREDIT, 5000.0, nature = TxnNature.CARD_REPAYMENT)
    }

    @Test
    fun `axis card spend variants`() {
        parse(
            "AX-AXISBK-S",
            "Spent Card no. XX1234 INR 450.00 02-10-26 14:05:33 KERALA VISI Avl Lmt INR 50,000.00 SMS BLOCK 1234 to 919951860002, if not you - Axis Bank"
        ).assertTxn("axis.card-spent-a", TxnDirection.DEBIT, 450.0, "KERALA VISI", Instrument.CREDIT_CARD, "1234")
        parse(
            "AX-AXISBK-S",
            "Spent INR 799.00 Axis Bank Card no. XX1234 02-10-26 14:05:33 IST Flipkart Avl Limit: INR 50,000.00 Not you? SMS BLOCK 1234 to 919951860002"
        ).assertTxn("axis.card-spent-b", TxnDirection.DEBIT, 799.0, "Flipkart")
        parse("AX-AXISBK-S", "Payment of INR 2,000.00 has been received towards your Axis Bank Credit Card XX1234 on 02-10-26 - Axis Bank")
            .assertTxn("axis.card-payment", TxnDirection.CREDIT, 2000.0, nature = TxnNature.CARD_REPAYMENT)
        parse(
            "AX-AXISBK-S",
            "Congratulations! Cashback of INR 80.00 has been credited to your Axis Bank Flipkart Visa Credit Card XX1234 towards your last month spends - Axis Bank"
        ).assertTxn("axis.cashback-credited", TxnDirection.CREDIT, 80.0, nature = TxnNature.CASHBACK)
    }

    @Test
    fun `hdfc card slice utkarsh`() {
        parse("AD-HDFCBK-S", "Spent Rs.700.00 On HDFC Bank Card 1234 At SWIGGY On 02-10-26:14:05:33.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 1234 to 7308080808")
            .assertTxn("hdfc.card-spent", TxnDirection.DEBIT, 700.0, "SWIGGY", Instrument.CREDIT_CARD, "1234")
        parse("AD-SLCBNK-S", "Rs.250.00 spent on your credit card XX1234 at BIG BAZAAR on 02-10-2026 (UPI Ref: 123456789012). Not you? Call 1-800-123-4567 - slice")
            .assertTxn("slice.card-spent", TxnDirection.DEBIT, 250.0, "BIG BAZAAR")
        parse("AD-SLCBNK-S", "Repayment of Rs.5,000.00 received for the slice credit card. The amount has been credited to your card account - slice")
            .assertTxn("slice.repayment", TxnDirection.CREDIT, 5000.0, nature = TxnNature.CARD_REPAYMENT)
        parse("AD-UTKSPR-S", "Dear RAHUL, your SuperCard 1234 debited for Rs.150.00 on 02-10-2026:02:05 PM for UPI - 123456789012. To dispute call 18001234 - Utkarsh SFBL")
            .assertTxn("utkarsh.card-debit", TxnDirection.DEBIT, 150.0, instrument = Instrument.CREDIT_CARD, last4 = "1234")
        parse("AD-UTKSPR-S", "We have received payment of Rs.1,000.00 for your SuperCard ending 1234. Your available limit is now Rs.9,000.00 -Utkarsh SFBL")
            .assertTxn("utkarsh.card-payment", TxnDirection.CREDIT, 1000.0, nature = TxnNature.CARD_REPAYMENT)
    }

    // ── Bank accounts ──────────────────────────────────────────────────────────

    @Test
    fun `hdfc bank messages`() {
        parse(
            "AD-HDFCBK-S",
            "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You? Call 18002586161/SMS BLOCK UPI to 7308080808"
        ).assertTxn("hdfc.upi-sent", TxnDirection.DEBIT, 250.0, "SWIGGY", Instrument.BANK_ACCOUNT, "1234")
        parse(
            "AD-HDFCBK-S",
            "Credit Alert! Rs.1,000.00 credited to HDFC Bank A/c XX1234 on 02-10-26 from VPA rahul@okhdfc RAHUL KUMAR (UPI 123456789012)"
        ).assertTxn("hdfc.credit-alert", TxnDirection.CREDIT, 1000.0, instrument = Instrument.BANK_ACCOUNT, last4 = "1234")
        parse(
            "AD-HDFCBK-S",
            "UPDATE: INR 1,200.00 debited from HDFC Bank XX1234 on 02-OCT-26. Info: UPI-123456789012-ZOMATO. Avl bal:INR 5,000.00"
        ).also {
            it.assertTxn("hdfc.debited", TxnDirection.DEBIT, 1200.0, "ZOMATO")
            assertEquals(5000.0, it.balance!!, 0.001)
        }
        parse(
            "AD-HDFCBK-S",
            "UPDATE: INR 590.00 debited from HDFC Bank A/c XX1234 on 02-MAY-26. For: DEBIT CARD ANNUAL FEE-May-26-ABC123"
        ).assertTxn("hdfc.debited", TxnDirection.DEBIT, 590.0)
        parse(
            "AD-HDFCBK-S",
            "Update! INR 4,000.00 deposited in HDFC Bank A/c XX1234 on 02-OCT-26 for HDFC ERGO GENE-12345.Avl bal INR 9,000.00. Cheque deposits in A/C are subject to realization"
        ).assertTxn("hdfc.deposited", TxnDirection.CREDIT, 4000.0, "HDFC ERGO GENE-12345")
        parse(
            "AD-HDFCBK-S",
            "DEAR RAHUL , PAYMENT OF Rs.5,000.00 RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH 1234 ON 02-10-2026.YOUR AVAILABLE LIMIT IS Rs.90,000.00"
        ).assertTxn("hdfc.card-payment", TxnDirection.CREDIT, 5000.0, nature = TxnNature.CARD_REPAYMENT)
    }

    @Test
    fun `sbi messages`() {
        parse("AD-CBSSBI-S", "Your A/C XXXXX123456 Debited Rs.1,000.00 on 02/10/26 -Transferred to RAHUL. SWARAJ. Avl Balance Rs.5,000.00-SBI")
            .assertTxn("sbi.cbs-debit", TxnDirection.DEBIT, 1000.0, "RAHUL", Instrument.BANK_ACCOUNT, "3456")
        parse("AD-CBSSBI-S", "Your A/C XXXXX123456 Credited Rs.2,500.00 on 02/10/26 -Deposit by TRANSFER FROM NAVI. SWARAJ. Avl Bal Rs.7,500.00-SBI")
            .assertTxn("sbi.cbs-credit", TxnDirection.CREDIT, 2500.0, "TRANSFER FROM NAVI")
        parse("AD-SBIUPI-S", "Dear SWARAJ, your A/c X1234-credited by Rs.500.00 on 30Mar25 transfer from RAHUL KUMAR Ref No 123456789012 -SBI")
            .assertTxn("sbi.upi-credit", TxnDirection.CREDIT, 500.0, "RAHUL KUMAR")
        parse(
            "AD-SBIPSG-S",
            "Dear Customer, Rs.55,000.00 credited to your A/c No XX1234 on 02/10/26 through NEFT with UTR ABCD1234567890 by GPS RENEWABLES PRIVATE LIMITED, INFO: BATCHID:123 SALARY OCT26-SBI"
        ).also {
            it.assertTxn("sbi.neft-credit", TxnDirection.CREDIT, 55000.0, "GPS RENEWABLES PRIVATE LIMITED")
            assertTrue(it.flags.contains("salary"))
        }
        parse(
            "AD-SBIPSG-S",
            "Dear Customer, Your a/c no. XX1234 is credited by Rs.700.00 on 02-10-26 by a/c linked to mobile 98XXXXX123-GROWW INVE (IMPS Ref# 123456789012)-SBI"
        ).assertTxn("sbi.imps-credit", TxnDirection.CREDIT, 700.0)
        parse("AD-SBYONO-S", "Your Ac X1234 debited Rs.800.00 for transfer to RAHUL Ac X5678 dt 02-10-26 Ref 123456789012. If not done by you, call 1800. YONO SBI")
            .assertTxn("sbi.yono-transfer", TxnDirection.DEBIT, 800.0, "RAHUL")
        parse("AD-SBIBNK-S", "Dear Customer, For PAN ABCDE1234F, An IT Refund amount of Rs.15,000.00 for AY-2025-26 has been credited to your account XX1234 on 02-10-26. -SBI")
            .assertTxn("sbi.it-refund", TxnDirection.CREDIT, 15000.0, "Income Tax refund")
    }

    @Test
    fun `federal messages`() {
        parse("AD-FEDBNK-S", "Debited Rs.120.00 from a/c XX1234 on 24Jul26 14:05:33via UPI to RAHUL. Ref 123456789012.Bal Rs.900.00. Not you?Call 18004201199 -Federal Bank")
            .assertTxn("federal.upi-debit", TxnDirection.DEBIT, 120.0, "RAHUL", Instrument.BANK_ACCOUNT, "1234")
        parse("AD-FEDFIB-S", "Rs.5,000.00 sent from your account XX1234 Sent to your beneficiary on October 5, 2026. If this transaction wasn't done by you, call 1800-Federal Bank")
            .assertTxn("federal.fund-transfer", TxnDirection.DEBIT, 5000.0, "Beneficiary")
    }

    @Test
    fun `wallet`() {
        parse("AD-IRSMSA-S", "Rs.40.00 debited from your RWallet Account on Jan 5 2026 14:05 . Available Balance Rs.60.00. Indian Railway")
            .assertTxn("irctc.rwallet-debit", TxnDirection.DEBIT, 40.0, "IRCTC", Instrument.WALLET)
        parse("AD-IRSMSA-S", "Rs.100.00 credited to your RWallet Account on Sep 5 2026 14:05 .Now the Available Balance is Rs.160.00. IR-CRIS")
            .assertTxn("irctc.rwallet-credit", TxnDirection.CREDIT, 100.0)
    }

    // ── Investments ────────────────────────────────────────────────────────────

    @Test
    fun `mutual fund purchases are savings`() {
        val n = TxnNature.INVESTMENT
        parse("AD-ABCAMC-S", "Purchase SIP in Folio 12345678 in ABSL PSU Equity Fund Direct-Growth for Rs.1,000.00, NAV 20.5/Trade date 02-10-2026 is processed. Balance Units 50.Statement will be sent in 5 Days. ABSLMF.")
            .assertTxn("mf.absl-sip", TxnDirection.DEBIT, 1000.0, "ABSL PSU Equity Fund Direct-Growth", Instrument.MUTUAL_FUND, nature = n)
        parse("AD-IPRUMF-S", "Dear RAHUL, Your SIP Purchase of Rs.2,000.00 in Folio 12345678 in Commodities Fund DP Growth for 10.5 units has been processed for NAV of Rs.20.0 on 02-10-2026 - IPRUMF")
            .assertTxn("mf.iprumf-sip", TxnDirection.DEBIT, 2000.0, "Commodities Fund DP Growth", nature = n)
        parse("AD-TATAMF-S", "Your Purchase SIP in folio 1234567/89 in scheme Tata Small Cap Fund Dir Pl Gr for Rs.1,500.00 for 10.5 units at NAV Rs.20.0 dated 02-10-2026 is successful with applicable load, Click http://x.in to view.")
            .assertTxn("mf.tata-sip", TxnDirection.DEBIT, 1500.0, "Tata Small Cap Fund Dir Pl Gr", nature = n)
        parse("AD-SBIMFM-S", "Dear RAHUL, Purchase transaction in Folio No. 12345678 in Scheme : SBI ELSS Tax Saver Fund Dir Gr for date 02-10-2026 for amount of Rs.3,000.00 at NAV Rs.20.5 is processed for number of units 100.0 - SBIMF")
            .assertTxn("mf.sbi-purchase", TxnDirection.DEBIT, 3000.0, "SBI ELSS Tax Saver Fund Dir Gr", nature = n)
        parse("AD-EDLAMC-S", "Hello, your purchase request dated 02-10-2026 under Edelweiss Balanced Advantage Fund - Direct Plan Growth for Rs.1,000.00 has been processed @ NAV of Rs.20.0 and 50.0 units are allotted in Folio: 12345678 -Edelweiss")
            .assertTxn("mf.edelweiss-purchase", TxnDirection.DEBIT, 1000.0, "Edelweiss Balanced Advantage Fund - Direct Plan Growth", nature = n)
        parse("AD-HDFCMF-S", "Your Purchase in Folio 12345678/12 under HDFC ELSS Tax saver - DP - Growth for Rs.500.00 has been processed at the NAV of 20.0 for 25.0 units and 02-10-2026. Your smart statement http://x.in")
            .assertTxn("mf.hdfc-purchase", TxnDirection.DEBIT, 500.0, "HDFC ELSS Tax saver - DP - Growth", nature = n)
        parse("AD-DSPAMC-S", "Transaction CONFIRMATION Alert Purchase request of Rs.1,000.00 in scheme ELSS Tax Saver Fund - Dir - G in your folio 123456/12 has been processed for 02-10-2026 with NAV of Rs.20.0. TnC Apply. - DSPMF")
            .assertTxn("mf.dsp-purchase", TxnDirection.DEBIT, 1000.0, "ELSS Tax Saver Fund - Dir - G", nature = n)
        parse("AD-MOAMCL-S", "Dear RAHUL, We have received your subscription in Motilal Oswal BSE Enhanced Value Index Fund under folio 12345678 for Rs.5,000.00. Units will be allotted after necessary validation.")
            .assertTxn("mf.motilal-subscription", TxnDirection.DEBIT, 5000.0, "Motilal Oswal BSE Enhanced Value Index Fund", nature = n)
        parse("AD-PTNNPS-S", "PRAN 123456789012: Units for Voluntary contribution of Rs.5,000.00 credited with NAV of 02-10-2026 -Protean")
            .assertTxn("nps.contribution", TxnDirection.DEBIT, 5000.0, "NPS", nature = n)
        parse("AD-ICCSLM-S", "Dear RAHUL You have deposited Rs.1,000.00 as RD with certificate No. 12345 for 5 Years in Indian Cooperative Credit Society limited")
            .assertTxn("rd.cooperative-society", TxnDirection.DEBIT, 1000.0, nature = n)
    }

    // ── One-off merchant receipts ──────────────────────────────────────────────

    @Test
    fun `merchant receipts`() {
        parse("AD-SNITCH-S", "Thanks for shopping at SNITCH, Trivandrum. Rs.1,799.00 paid. View your invoice & all past purchases in one place, download our app: http://x.in")
            .assertTxn("snitch.purchase", TxnDirection.DEBIT, 1799.0, "SNITCH")
        parse("AD-650022-S", "Important Update for 9876543210: Your Prepaid bill payment of Rs.299.00 was successful. You could've paid up to Rs.30.00 less with Airtel Axis Bank Credit Card. View Details http://x.in")
            .assertTxn("airtel.prepaid-bill", TxnDirection.DEBIT, 299.0, "Airtel")
        parse("AD-ITDCPC-S", "Dear User, Challan payment of Rs.5,000.00 against PAN/TAN ABCDE1234F for Assessment Year 2026-27 has been successfully paid. e-Filing, ITD.")
            .assertTxn("itd.challan", TxnDirection.DEBIT, 5000.0, "Income Tax Department")
        parse("AD-POLBAZ-S", "Hi RAHUL, Payment of Rs.2,000.00 is successful for the purchase of your Twowheeler policy. Please log in to PB for queries - http://x.in - PolicyBazaar")
            .assertTxn("policybazaar.payment", TxnDirection.DEBIT, 2000.0, "PolicyBazaar")
        parse("AD-VAAHAN-S", "Your PUC certificate validity is 02-10-2027 and payment received for certificate is Rs.100.00 (excluding GST). (MoRTH)")
            .assertTxn("vahan.puc", TxnDirection.DEBIT, 100.0, "PUC certificate")
    }

    // ── Non-transactions ───────────────────────────────────────────────────────

    @Test
    fun `reminders info failed otp and promos are not transactions`() {
        fun cat(sender: String?, body: String) = parse(sender, body).category

        assertEquals(SmsCategory.REMINDER, cat("AD-SBIUPI-S", "Dear RAHUL, UPI AutoPay for INDmoney debit of Rs.500.00 is scheduled on .05-10-26, abc@sbi. Please ensure sufficient balance in your account. -SBI"))
        assertEquals(SmsCategory.REMINDER, cat("VM-ICICIT-S", "ICICI Bank Credit Card XX1234 Statement is sent to sw**@gmail.com Total of Rs.5,000.00 or minimum of Rs.250.00 is due by 05-10-26."))
        assertEquals(SmsCategory.REMINDER, cat("VM-ICICIT-S", "Pay Total Amount Due of Rs.5,000.00 or Minimum Amount Due of Rs.250.00 by 05-10-26 towards ICICI Card XX1234. Ignore if paid."))
        assertEquals(SmsCategory.REMINDER, cat("AX-PHONPE-S", "Godaddy has requested money from you on PhonePe.Rs.500.00 will be debited from your account on approving the request - http://x.in"))
        assertEquals(SmsCategory.REMINDER, cat("AX-AXISBK-S", "Statement for your Axis Bank Credit Card no. XX1234 has been generated. Due on: 05-10-26 Total amt: Rs.5,000.00 Min amt due: Rs.250.00 To pay, visit http://x.in"))
        assertEquals(SmsCategory.FAILED, cat("AD-JIOPAY-S", "Your last payment attempt of Rs.299.00 on Jun 5, 2026 14:05 for Jio number 9876543210 was unsuccessful. Please try again. Transaction ID : ABC12345."))
        assertEquals(SmsCategory.FAILED, cat("AD-AIRINF-S", "Hi, Payment of Rs.299.00 has failed for your Airtel Mobile 9876543210. Any amount, if debited will be refunded to your source account within a day."))
        assertEquals(SmsCategory.INFO, cat("AD-SWIGGY-S", "Refund of Rs.250.00 has been initiated for Swiggy order 123456789. Updated balance should reflect in 3 hours. http://x.in"))
        assertEquals(SmsCategory.INFO, cat("AD-RZRPAY-S", "Settlement worth Rs.5,000.00 for Razorpay MID ABC123 has been processed. It will be credited in your bank account before 5 PM today!"))
        assertEquals(SmsCategory.INFO, cat("AD-EPFOHO-S", "Dear 1234, your passbook balance against PYBOM12345 is Rs.50,000/-. Contribution of Rs.3,000/- for due month Dec-25 has been received."))
        assertEquals(SmsCategory.INFO, cat("AX-AXISBK-S", "You have earned a total cash back of Rs.80.00 in May on your Flipkart Axis Bank Credit Card XX1234. It will be credited in the next statement. T&C."))
        assertEquals(SmsCategory.OTP, cat("AD-HDFCBK-S", "123456 is your OTP for txn of Rs.500.00 at AMAZON. Do not share it with anyone."))
        assertEquals(SmsCategory.PROMO, cat("AD-HDFCBN-S", "Dear Swaraj, Congrats! Claim your Lifetime Free HDFC Bank Credit Card+Upto Rs.500 Amazon Voucher. Hurry: http://x.in T&C"))
        assertEquals(SmsCategory.PROMO, cat("VM-ICICIS-S", "Get a Personal Loan up to Rs.5 lakh credited to your Savings Account through your ICICI Bank Credit Card. Give a missed call to 9999999999. T&Cs apply."))
        assertEquals(SmsCategory.PROMO, cat("AD-650025-S", "Never run out of data. Get 5 GB + 5 GB rollover on Postpaid at just Rs.399. Upgrade now http://x.in"))
    }

    // ── Engine behaviour ───────────────────────────────────────────────────────

    @Test
    fun `rules match on wording when sender is missing or wrong`() {
        val body = "Spent Rs.700.00 On HDFC Bank Card 1234 At SWIGGY On 02-10-26:14:05:33.Not You?"
        assertEquals("hdfc.card-spent", parse(null, body).ruleId)
        assertEquals("hdfc.card-spent", parse("AD-OTHERX-S", body).ruleId)
    }

    @Test
    fun `unknown template falls back to generic parse`() {
        val p = parse("AD-NEWBNK-S", "Your A/c XX4321 is debited by Rs.640.00 on 02-10-26 at CAFE COFFEE DAY. Avl Bal Rs.2,000.00")
        assertFalse(p.matched)
        assertNull(p.ruleId)
        assertEquals(SmsCategory.TRANSACTION, p.category)
        assertEquals(640.0, p.amount!!, 0.001)
        assertEquals(TxnDirection.DEBIT, p.direction)
        assertEquals("4321", p.accountHint?.last4)
    }

    @Test
    fun `received time is used when the sms has no date`() {
        val p = SmsRuleEngine.parse(
            SmsMessage("AD-IRSMSA-S", "Rs.40.00 debited from your RWallet Account. Available Balance Rs.60.00.", receivedAt = 1_700_000_000_000L)
        )
        assertEquals(1_700_000_000_000L, p.dateMillis)
    }

    @Test
    fun `parsed sms date is the sms date not received time`() {
        val p = SmsRuleEngine.parse(
            SmsMessage("AD-HDFCBK-S", "Sent Rs.250.00 From HDFC Bank A/C *1234 To SWIGGY On 02/10/26 Ref 123456789012 Not You?", receivedAt = 1L)
        )
        assertNotNull(p.dateMillis)
        assertTrue(p.dateMillis != 1L)
    }

    @Test
    fun `all built-in rule ids are unique`() {
        val ids = BuiltInSmsRules.all.map { it.id }
        // Several rules intentionally share a family prefix, but the exact id must be unique except for
        // deliberate variants listed here.
        val dupes = ids.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("duplicate rule ids: $dupes", dupes.isEmpty())
    }

    // ── Ideas adopted from transaction-sms-parser (priority 2) ─────────────────

    @Test
    fun `reference sample from transaction-sms-parser`() {
        val p = parse("AD-NEWBNK-S", "INR 2000 debited from A/c no. XX3423 on date IST at SMAPLE Avl Bal- INR 2343.23.")
        assertEquals(TxnDirection.DEBIT, p.direction)
        assertEquals(2000.0, p.amount!!, 0.001)
        assertEquals("3423", p.accountHint?.last4)
        assertEquals(2343.23, p.balance!!, 0.001)
    }

    @Test
    fun `extended balance keywords`() {
        fun bal(text: String) = SmsRuleEngine.parse(SmsMessage("AD-NEWBNK-S", text)).balance
        assertEquals(100.5, bal("Rs.50 debited from A/c XX1234. Updated balance Rs 100.50")!!, 0.001)
        assertEquals(5000.0, bal("Rs.50 spent on card XX1234. Avbl. credit limit INR 5,000")!!, 0.001)
        assertEquals(750.0, bal("Rs.50 spent on card XX1234. Limit available Rs.750")!!, 0.001)
        assertEquals(900.0, bal("Rs.50 debited from A/c XX1234. New balance Rs.900")!!, 0.001)
        assertEquals(1200.0, bal("Rs.50 spent on card XX1234. Avl Lmt Rs.1200")!!, 0.001)
    }

    @Test
    fun `wallet and card brands identify the account without a number`() {
        val paytm = parse("AD-NEWBNK-S", "Rs.120.00 paid from Paytm wallet to ZOMATO Ref 123456789")
        assertEquals("paytm", paytm.accountHint?.institution)
        assertEquals(Instrument.WALLET, paytm.accountHint?.instrument)

        val slice = parse("AD-NEWBNK-S", "Rs.80.00 spent using your slice credit card at CAFE")
        assertEquals("slice", slice.accountHint?.institution)
        assertEquals(Instrument.CREDIT_CARD, slice.accountHint?.instrument)
    }

    @Test
    fun `vpa is used as the merchant when the text names none`() {
        val p = parse("AD-NEWBNK-S", "Rs.60.00 debited from A/c XX1234 on 02-10-26 via UPI zomato@hdfcbank Ref 123456789012")
        assertEquals("zomato", p.counterparty)
    }

    @Test
    fun `charged and purchased count as debits but received payment stays a credit`() {
        assertEquals(TxnDirection.DEBIT, parse("AD-NEWBNK-S", "Rs.99.00 charged to your card XX1234 at NETFLIX").direction)
        assertEquals(TxnDirection.DEBIT, parse("AD-NEWBNK-S", "Rs.99.00 purchased on card XX1234 at STEAM").direction)
        // The reference library classifies "payment" as a debit; we must not.
        assertEquals(TxnDirection.CREDIT, parse("AD-NEWBNK-S", "Payment of Rs.500.00 received in A/c XX1234 on 02-10-26").direction)
    }

    // ── Formats seen in a real inbox export (sms_insights 3) ────────────────────

    @Test
    fun `non breaking spaces do not defeat rules`() {
        parse("AX-AXISBK-S", "Spent Card no. XX1234 INR 450 02-10-26 14:05:33 KERALA VISI Avl Lmt INR 50000.00 SMS BLOCK 1234 to 919951860002, if not you - Axis Bank")
            .assertTxn("axis.card-spent-a", TxnDirection.DEBIT, 450.0, "KERALA VISI", Instrument.CREDIT_CARD, "1234")
        parse("AD-VAAHAN-S", "Your PUC certificate validity is 2027-10-02 and payment received for certificate is Rs. 100 (excluding GST). (MoRTH)")
            .assertTxn("vahan.puc", TxnDirection.DEBIT, 100.0, "PUC certificate")
    }

    @Test
    fun `iso dates and yearless utkarsh dates`() {
        val hdfc = parse("AD-HDFCBK-S", "Spent Rs.700.00 On HDFC Bank Card 1234 At SWIGGY On 2026-10-02:14:05:33.Not You? To Block+Reissue Call 18002586161/SMS BLOCK CC 1234 to 7308080808")
        hdfc.assertTxn("hdfc.card-spent", TxnDirection.DEBIT, 700.0, "SWIGGY", Instrument.CREDIT_CARD, "1234")
        assertNotNull(hdfc.dateMillis)
        val utk = parse("AD-UTKSPR-S", "Dear RAHUL, your SuperCard 1234 debited for INR 150.50 on 05 Oct 03:22 PM for UPI - 123456789012. To dispute call 18001234 - Utkarsh SFBL", )
        utk.assertTxn("utkarsh.card-debit", TxnDirection.DEBIT, 150.5, instrument = Instrument.CREDIT_CARD, last4 = "1234")
        assertNull("yearless date must fall back to the received time", SmsDateParser.parse("on 05 Oct 03:22 PM"))
    }

    @Test
    fun `hdfc card refund late fee and cash promos`() {
        parse("AD-HDFCBK-S", "Alert! Rs. 250.00 refunded by AMAZON on 02/OCT/2026 & adjusted against HDFC Bank Credit Card 1234 View updated balance here: http://x.in")
            .assertTxn("hdfc.card-refund", TxnDirection.CREDIT, 250.0, "AMAZON", Instrument.CREDIT_CARD, "1234", TxnNature.REFUND)
        parse("AD-UTKSPR-S", "Late fee of INR 100.00 has been charged to your SuperCard as dues are still unpaid. Pay now to avoid more charges: http://x.in - Utkarsh SFBL")
            .assertTxn("utkarsh.late-fee", TxnDirection.DEBIT, 100.0, "Late fee", Instrument.CREDIT_CARD)
        assertEquals(SmsCategory.PROMO, parse("AD-ZEPTON-S", "Dear Customer, Rs. 50 FREE CASH deposited in your Zepto wallet. Plus, FREE delivery! Avail the offers now - http://x.in").category)
        assertEquals(SmsCategory.PROMO, parse("AD-FIGRUY-S", "Dear Customer, Rs.50,000 could be credited to your wallet A/c on 05-10-2026 Click to apply http://x.in Finance Guru").category)
    }

    @Test
    fun `line breaks inside phrases and doubled currency`() {
        parse("AX-AXISBK-S", "Spent\nCard no. XX1234\nINR 450\n02-10-26 14:05:33\nKERALA VISI\nAvl\nLmt INR 50000.00\nSMS BLOCK 1234 to 919951860002, if not you - Axis Bank")
            .assertTxn("axis.card-spent-a", TxnDirection.DEBIT, 450.0, "KERALA VISI", Instrument.CREDIT_CARD, "1234")
        parse("AD-VAAHAN-S", "Your PUC certificate validity is 2027-10-02 and payment\nreceived for certificate is Rs. 100 (excluding GST). (MoRTH)")
            .assertTxn("vahan.puc", TxnDirection.DEBIT, 100.0, "PUC certificate")
        parse("AD-UTKSPR-S", "Late fee of INR INR 99.99 has been charged to your SuperCard as dues are still unpaid. Pay now: http://x.in - Utkarsh SFBL")
            .assertTxn("utkarsh.late-fee", TxnDirection.DEBIT, 99.99, "Late fee")
    }

    @Test
    fun `flexible spaces leave classes and quantified spaces alone`() {
        assertEquals("""a\s+b[x ]c""", SmsRule.flexibleSpaces("a b[x ]c"))
        assertEquals("""a\s?b""", SmsRule.flexibleSpaces("a ?b"))
        assertEquals("""\ b""", SmsRule.flexibleSpaces("""\ b"""))
    }
}
