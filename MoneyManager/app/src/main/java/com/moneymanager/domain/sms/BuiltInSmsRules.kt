package com.moneymanager.domain.sms

import com.moneymanager.domain.sms.Instrument.BANK_ACCOUNT
import com.moneymanager.domain.sms.Instrument.CREDIT_CARD
import com.moneymanager.domain.sms.Instrument.MUTUAL_FUND
import com.moneymanager.domain.sms.Instrument.OTHER
import com.moneymanager.domain.sms.Instrument.WALLET
import com.moneymanager.domain.sms.SmsCategory.FAILED
import com.moneymanager.domain.sms.SmsCategory.INFO
import com.moneymanager.domain.sms.SmsCategory.REMINDER
import com.moneymanager.domain.sms.SmsCategory.TRANSACTION
import com.moneymanager.domain.sms.TxnDirection.CREDIT
import com.moneymanager.domain.sms.TxnDirection.DEBIT
import com.moneymanager.domain.sms.TxnNature.CARD_REPAYMENT
import com.moneymanager.domain.sms.TxnNature.CASHBACK
import com.moneymanager.domain.sms.TxnNature.INVESTMENT
import com.moneymanager.domain.sms.TxnNature.REFUND

/**
 * Rules derived from the SMS Analyzer export of a real inbox (see docs/SMS-PATTERN-CATALOG.md).
 * Order matters only within a sender: more specific / failure rules come first.
 */
object BuiltInSmsRules {

    // Shared regex fragments. Each named group may appear once per rule.
    private const val AMT = """(?:(?<cur>Rs\.?|INR|USD|₹)\s?)(?<amount>[\d,]+(?:\.\d{1,2})?)"""
    private const val ANY_AMT = """(?:Rs\.?|INR|USD|₹)\s?[\d,]+(?:\.\d{1,2})?"""
    private const val ACC = """(?:[Xx*]+\s?)?(?<acc>\d{3,6})"""
    private const val NUMERIC_DATE = """(?<date>\d{1,2}[-/][A-Za-z0-9]{2,3}[-/]\d{2,4}|\d{4}-\d{2}-\d{2})"""
    /** `30Mar25`, `02-10-26`, `02/10/2026`: banks of the same group mix formats between templates. */
    private const val COMPACT_DATE = """(?<date>\d{1,2}[-/]?[A-Za-z0-9]{2,3}[-/]?\d{2,4})"""

    private fun txn(
        id: String,
        senders: String,
        regex: String,
        direction: TxnDirection,
        instrument: Instrument,
        institution: String?,
        nature: TxnNature = TxnNature.NORMAL,
        counterparty: String? = null,
    ) = SmsRule(
        id, senders.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(), regex,
        TRANSACTION, direction, instrument, institution, nature, counterparty,
    )

    private fun other(id: String, senders: String, regex: String, category: SmsCategory) =
        SmsRule(id, senders.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet(), regex, category)

    private fun mutualFund(id: String, senders: String, regex: String) =
        txn(id, senders, regex, DEBIT, MUTUAL_FUND, null, INVESTMENT)

    val all: List<SmsRule> = listOf(
        // ---- Non-transactions that must never become drafts ------------------------------------
        other("info.refund-initiated", "SWIGGY,MMTRIP",
            """Refund of $ANY_AMT has been initiated|initiated a refund of $ANY_AMT|refund reference number for $ANY_AMT""", INFO),
        other("info.settlement", "RZRPAY", """Settlement worth $ANY_AMT .*has been processed""", INFO),
        other("info.epf-contribution", "EPFOHO", """Contribution of $ANY_AMT/?-? ?for due month""", INFO),
        other("info.upi-mandate-created", "SBIUPI", """UPI-Mandate for $ANY_AMT is successfully created""", INFO),
        other("info.card-credit-balance", "AXISBK", """You have a credit balance of""", INFO),
        other("info.card-cashback-earned", "AXISBK", """earned a total cash back of""", INFO),
        other("info.lpg-subsidy-notice", "INDANE", """As per available Income-tax records""", INFO),
        other("promo.zepto-free-cash", "ZEPTON", """FREE CASH deposited in your""", SmsCategory.PROMO),
        other("promo.could-be-credited", "", """could be credited to your""", SmsCategory.PROMO),
        other("reminder.money-request", "PHONPE,SUPRMY,SLCBNK", """has requested money""", REMINDER),

        // ---- Scapia (Federal) credit card ------------------------------------------------------
        other("fedscp.declined", "FEDSCP", """txn of $AMT at (?<merchant>.+?) is declined""", FAILED),
        txn("fedscp.fuel-waiver", "FEDSCP",
            """fuel surcharge waiver of $AMT for your (?:fuel )?transaction at (?<merchant>.+?) on (?<date>\S+)""",
            CREDIT, CREDIT_CARD, "Scapia", CASHBACK),
        txn("fedscp.card-payment", "FEDSCP", """received your payment of $AMT towards your Scapia""",
            CREDIT, CREDIT_CARD, "Scapia", CARD_REPAYMENT),
        txn("fedscp.refund", "FEDSCP", """received a refund of $AMT from (?<merchant>.+?) on your Scapia""",
            CREDIT, CREDIT_CARD, "Scapia", REFUND),
        txn("fedscp.refund-alt", "FEDSCP", """Refund received of $AMT on your Scapia .*?from (?<merchant>[^.]+)""",
            CREDIT, CREDIT_CARD, "Scapia", REFUND),
        txn("fedscp.card-spend", "FEDSCP", """txn of $AMT at (?<merchant>.+?)\s+on your Scapia""",
            DEBIT, CREDIT_CARD, "Scapia"),

        // ---- HDFC Bank -------------------------------------------------------------------------
        txn("hdfc.upi-sent", "HDFCBK",
            """Sent\s+$AMT\s+From\s+HDFC Bank A/C\s*$ACC\s+To\s+(?<merchant>.+?)\s+On\s+(?<date>\S+)\s+Ref\s+(?<ref>\d+)""",
            DEBIT, BANK_ACCOUNT, "HDFC Bank"),
        txn("hdfc.card-spent", "HDFCBK",
            """Spent\s+$AMT\s+On\s+HDFC Bank Card\s*$ACC\s+At\s+(?<merchant>.+?)\s+On\s+$NUMERIC_DATE""",
            DEBIT, CREDIT_CARD, "HDFC Bank"),
        txn("hdfc.credit-alert", "HDFCBK",
            """Credit Alert!?\s*$AMT\s+credited to HDFC Bank A/c\s*$ACC\s+on\s+(?<date>\S+)\s+from\s+(?<merchant>.+?)\s*\(UPI\s*(?<ref>\d+)\)""",
            CREDIT, BANK_ACCOUNT, "HDFC Bank"),
        txn("hdfc.debited", "HDFCBK",
            """$AMT\s+debited from HDFC Bank\s+(?:A/c\s*)?$ACC\s+on\s+$NUMERIC_DATE(?:\.?\s*(?:Info|For):\s*(?<merchant>[^:.]+))?""",
            DEBIT, BANK_ACCOUNT, "HDFC Bank"),
        txn("hdfc.deposited", "HDFCBK",
            """$AMT\s+deposited in HDFC Bank A/c\s*$ACC\s+on\s+$NUMERIC_DATE\s+for\s+(?<merchant>.+?)\s*\.\s*Avl""",
            CREDIT, BANK_ACCOUNT, "HDFC Bank"),
        txn("hdfc.card-payment", "HDFCBK",
            """PAYMENT OF $AMT RECEIVED TOWARDS YOUR CREDIT CARD ENDING WITH\s*$ACC""",
            CREDIT, CREDIT_CARD, "HDFC Bank", CARD_REPAYMENT),
        txn("hdfc.card-refund", "HDFCBK",
            """Alert!?\s*$AMT\s+refunded by\s+(?<merchant>.+?)\s+on\s+$NUMERIC_DATE\s*&\s*adjusted against HDFC Bank Credit Card\s*$ACC""",
            CREDIT, CREDIT_CARD, "HDFC Bank", REFUND),
        txn("hdfc.card-payment-online", "HDFCBK",
            """Online Payment of $AMT vide Ref#\s*(?<ref>\w+) was credited to your card ending\s*$ACC""",
            CREDIT, CREDIT_CARD, "HDFC Bank", CARD_REPAYMENT),

        // ---- Utkarsh SuperCard -----------------------------------------------------------------
        txn("utkarsh.card-debit", "UTKSPR",
            // Real messages print "05 Oct 03:22 PM" with no year, so the date group is optional.
            """SuperCard\s*$ACC\s+debited for $AMT on\s+(?:$NUMERIC_DATE|\d{1,2}\s+[A-Za-z]{3}\b).*?for UPI\s*-\s*(?<ref>\d+)""",
            DEBIT, CREDIT_CARD, "Utkarsh"),
        txn("utkarsh.late-fee", "UTKSPR", """Late fee of (?:INR\s*)?$AMT has been charged to your SuperCard""",
            DEBIT, CREDIT_CARD, "Utkarsh", counterparty = "Late fee"),
        txn("utkarsh.card-payment", "UTKSPR", """received payment of $AMT for your SuperCard ending\s*$ACC""",
            CREDIT, CREDIT_CARD, "Utkarsh", CARD_REPAYMENT),

        // ---- ICICI Bank cards ------------------------------------------------------------------
        txn("icici.card-spent-using", "ICICIT",
            """$AMT\s+spent using ICICI Bank Card\s*$ACC\s+on\s+(?<date>\S+)\s+on\s+(?<merchant>.+?)\.\s*Avl Limit""",
            DEBIT, CREDIT_CARD, "ICICI Bank"),
        txn("icici.card-spent-at", "ICICIT",
            """$AMT\s+spent on ICICI Bank Card\s*$ACC\s+on\s+(?<date>\S+)\s+at\s+(?<merchant>.+?)\.\s*Avl Lmt""",
            DEBIT, CREDIT_CARD, "ICICI Bank"),
        txn("icici.card-upi", "ICICIT",
            """ICICI Bank Credit Card\s*$ACC\s+debited for $AMT on\s+(?<date>\S+)\s+for\s+(?<merchant>UPI-[^.]+?)\.""",
            DEBIT, CREDIT_CARD, "ICICI Bank"),
        txn("icici.card-payment", "ICICIT", """Payment of $AMT has been received on your ICICI Bank Credit Card\s*$ACC""",
            CREDIT, CREDIT_CARD, "ICICI Bank", CARD_REPAYMENT),

        // ---- Axis Bank cards -------------------------------------------------------------------
        txn("axis.card-spent-a", "AXISBK",
            """Spent Card no\.?\s*$ACC\s+$AMT\s+(?<date>\S+)\s+\d{1,2}:\d{2}(?::\d{2})?\s*(?:IST\s+)?(?<merchant>.+?)\s+Avl Lmt""",
            DEBIT, CREDIT_CARD, "Axis Bank"),
        txn("axis.card-spent-b", "AXISBK",
            """Spent\s+$AMT\s+Axis Bank Card no\.?\s*$ACC\s+(?<date>\S+)\s+\d{1,2}:\d{2}(?::\d{2})?\s*(?:IST\s+)?(?<merchant>.+?)\s+Avl Limit""",
            DEBIT, CREDIT_CARD, "Axis Bank"),
        txn("axis.card-payment", "AXISBK", """Payment of $AMT has been received towards your Axis Bank Credit Card\s*$ACC""",
            CREDIT, CREDIT_CARD, "Axis Bank", CARD_REPAYMENT),
        txn("axis.cashback-credited", "AXISBK", """Cashback of $AMT has been credited to your Axis Bank .*?Credit Card\s*\??$ACC""",
            CREDIT, CREDIT_CARD, "Axis Bank", CASHBACK),

        // ---- State Bank of India ---------------------------------------------------------------
        txn("sbi.cbs-debit", "CBSSBI",
            """A/C\s*$ACC\s+Debited\s+$AMT\s+on\s+(?<date>\S+)\s*-\s*Transferred to\s+(?<merchant>.+?)\.\s""",
            DEBIT, BANK_ACCOUNT, "SBI"),
        txn("sbi.cbs-credit", "CBSSBI",
            """A/C\s*$ACC\s+Credited\s+$AMT\s+on\s+(?<date>\S+)\s*-\s*Deposit by\s+(?<merchant>.+?)\.\s""",
            CREDIT, BANK_ACCOUNT, "SBI"),
        txn("sbi.cbs-td-closure", "CBSSBI",
            """A/C\s*$ACC\s+Credited\.\s*$AMT\s+on\s+(?<date>\S+)\s+on account of Closure of TD""",
            CREDIT, BANK_ACCOUNT, "SBI", counterparty = "TD closure"),
        txn("sbi.upi-credit", "SBIUPI",
            """A/c\s*$ACC\s*-\s*credited by $AMT on\s+$COMPACT_DATE\s+transfer from\s+(?<merchant>.+?)\s+Ref No\s+(?<ref>\d+)""",
            CREDIT, BANK_ACCOUNT, "SBI"),
        txn("sbi.neft-credit", "SBIPSG",
            """$AMT\s+credited to your A/c No\s*$ACC\s+on\s+(?<date>\S+)\s+through NEFT with UTR\s+(?<ref>\w+)\s+by\s+(?<merchant>.+?),\s*INFO:\s*(?<info>.*)""",
            CREDIT, BANK_ACCOUNT, "SBI"),
        txn("sbi.imps-credit", "SBIPSG",
            """a/c no\.?\s*$ACC\s+is credited by $AMT on\s+(?<date>\S+)\s+by\s+(?<merchant>.+?)\s*\(IMPS Ref#\s*(?<ref>\d+)\)""",
            CREDIT, BANK_ACCOUNT, "SBI"),
        txn("sbi.it-refund", "SBIBNK",
            """IT Refund amount of $AMT for AY.*?has been credited to your account\s*$ACC\s+on\s+(?<date>\S+)""",
            CREDIT, BANK_ACCOUNT, "SBI", counterparty = "Income Tax refund"),
        txn("sbi.yono-transfer", "SBYONO",
            """Ac\s*$ACC\s+debited\s+$AMT\s+for transfer to\s+(?<merchant>.+?)\s+Ac\s.*?dt\s+(?<date>\S+)\s+Ref\s+(?<ref>\d+)""",
            DEBIT, BANK_ACCOUNT, "SBI"),

        // ---- slice credit card -----------------------------------------------------------------
        txn("slice.card-spent", "SLCBNK",
            """$AMT\s+spent on your credit card\s*$ACC\s+at\s+(?<merchant>.+?)\s+on\s+(?<date>\S+)\s+\(UPI Ref:\s*(?<ref>\d+)\)""",
            DEBIT, CREDIT_CARD, "slice"),
        txn("slice.repayment", "SLCBNK", """Repayment of $AMT received for the slice credit card""",
            CREDIT, CREDIT_CARD, "slice", CARD_REPAYMENT),
        txn("slice.refund", "SLCBNK",
            """Refund of $AMT from (?<merchant>.+?) has been successfully processed to your credit card\s*$ACC""",
            CREDIT, CREDIT_CARD, "slice", REFUND),

        // ---- Federal Bank ----------------------------------------------------------------------
        txn("federal.upi-debit", "FEDBNK",
            """Debited $AMT from a/c\s*$ACC\s+on\s+$COMPACT_DATE.*?via UPI to\s+(?<merchant>.+?)\.\s*Ref\s+(?<ref>\d+)""",
            DEBIT, BANK_ACCOUNT, "Federal Bank"),
        txn("federal.fund-transfer", "FEDFIB",
            """$AMT\s+sent from your account\s*$ACC.*?on\s+(?<date>[A-Za-z]+\s+\d{1,2},\s*\d{1,4})""",
            DEBIT, BANK_ACCOUNT, "Federal Bank", counterparty = "Beneficiary"),

        // ---- Wallets ---------------------------------------------------------------------------
        txn("irctc.rwallet-debit", "IRSMSA", """$AMT\s+debited from your RWallet Account""",
            DEBIT, WALLET, "IRCTC RWallet", counterparty = "IRCTC"),
        txn("irctc.rwallet-credit", "IRSMSA", """$AMT\s+credited to your RWallet Account""",
            CREDIT, WALLET, "IRCTC RWallet", counterparty = "IRCTC"),

        // ---- Mutual funds, NPS, RD (savings) ---------------------------------------------------
        mutualFund("mf.absl-sip", "ABCAMC", """Purchase SIP in Folio\s+[\d/]+\s+in\s+(?<merchant>.+?)\s+for $AMT,?\s*NAV"""),
        mutualFund("mf.iprumf-sip", "IPRUMF", """SIP Purchase of $AMT in Folio\s+[\d/]+\s+in\s+(?<merchant>.+?)\s+for\s"""),
        mutualFund("mf.tata-sip", "TATAMF", """Purchase SIP in folio\s+[\d/]+\s+in scheme\s+(?<merchant>.+?)\s+for $AMT"""),
        mutualFund("mf.sbi-purchase", "SBIMFM,SBIMFD",
            """Purchase transaction in Folio No\.?\s*[\d/]+\s+in Scheme\s*:\s*(?<merchant>.+?)\s+for date\s.*?for amount of $AMT"""),
        mutualFund("mf.edelweiss-purchase", "EDLAMC",
            """purchase request dated\s+\S+\s+under\s+(?<merchant>.+?)\s+for $AMT has been processed"""),
        mutualFund("mf.hdfc-purchase", "HDFCMF",
            """Purchase in Folio\s+[\d/]+\s+under\s+(?<merchant>.+?)\s+for $AMT has been processed"""),
        mutualFund("mf.dsp-purchase", "DSPAMC", """Purchase request of $AMT in scheme\s+(?<merchant>.+?)\s+in your folio"""),
        mutualFund("mf.motilal-subscription", "MOAMCL",
            """received your subscription in\s+(?<merchant>.+?)\s+under folio\s+[\d/]+\s+for $AMT"""),
        mutualFund("mf.motilal-purchase", "MOAMCL",
            """purchase request dated\s+\S+\s+in scheme\s+(?<merchant>.+?)\s+under the Folio No\s+[\d/]+\s+for $AMT"""),
        txn("nps.contribution", "PTNNPS,NPSCRA", """PRAN\s*\S+:\s*Units for Voluntary contribution of $AMT credited""",
            DEBIT, OTHER, null, INVESTMENT, counterparty = "NPS"),
        txn("rd.cooperative-society", "ICCSLM", """You have (?:deposited|remitted) $AMT as (?:RD|your)""",
            DEBIT, OTHER, null, INVESTMENT, counterparty = "RD - Indian Cooperative Credit Society"),

        // ---- One-off merchant receipts ---------------------------------------------------------
        txn("snitch.purchase", "SNITCH", """Thanks for shopping at\s+(?<merchant>.+?),.*?$AMT\s+paid""",
            DEBIT, OTHER, null),
        txn("samsung.service", "SAMCCC", """amount paid for your Samsung Service Request No\.?\s*\d+ was $AMT""",
            DEBIT, OTHER, null, counterparty = "Samsung Service"),
        txn("policybazaar.payment", "POLBAZ", """Payment of $AMT is successful for the purchase of your .*? policy""",
            DEBIT, OTHER, null, counterparty = "PolicyBazaar"),
        txn("airtel.prepaid-bill", "650022", """Prepaid bill payment of $AMT was successful""",
            DEBIT, OTHER, null, counterparty = "Airtel"),
        txn("jiofiber.payment", "JIOFBR", """payment of $AMT against JioFiber""",
            DEBIT, OTHER, null, counterparty = "Jio"),
        txn("itd.challan", "ITDCPC", """Challan payment of $AMT against PAN/TAN .*has been successfully paid""",
            DEBIT, OTHER, null, counterparty = "Income Tax Department"),
        txn("vahan.puc", "VAAHAN", """payment received for certificate is $AMT""",
            DEBIT, OTHER, null, counterparty = "PUC certificate"),
    )
}
