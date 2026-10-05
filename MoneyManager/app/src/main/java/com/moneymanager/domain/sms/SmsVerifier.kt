package com.moneymanager.domain.sms

/** Something about a parse that the message text does not back up. */
enum class SmsIssue(val flag: String) {
    /** The parsed amount does not appear in the text: the template or its capture group has drifted. */
    AMOUNT_NOT_IN_TEXT("verify:amount_not_in_text"),

    /** The text only uses wording of the opposite direction to the one parsed. */
    DIRECTION_CONFLICT("verify:direction_conflict"),

    /** The merchant is a stray word or number, usually a sign the capture group moved. */
    MERCHANT_JUNK("verify:merchant_junk"),

    /** The date is later than the message could have been sent. */
    DATE_IN_FUTURE("verify:date_in_future"),
}

/**
 * Cross-checks a parse against the message it came from. A rule can keep matching after a bank changes its
 * wording while quietly capturing the wrong thing; these invariants hold for any real transaction SMS, so they
 * catch that without knowing the template. Pure Kotlin, no I/O.
 */
object SmsVerifier {

    private val debitWords = Regex("""\b(debited|spent|paid|sent|withdrawn|purchased?)\b""", RegexOption.IGNORE_CASE)
    private val creditWords = Regex("""\b(credited|received|refunded|refund|deposited)\b""", RegexOption.IGNORE_CASE)
    private val junkMerchants = setOf(
        "on", "at", "to", "from", "ref", "upi", "avl", "bal", "balance", "info", "not", "you", "your", "the", "via",
        "using", "is", "for", "inr", "rs", "ac", "no", "txn", "dt", "date",
    )
    private const val FUTURE_SLACK_MS = 36L * 60 * 60 * 1000

    /** [body] should be the text the parser saw ([SmsText.normalize]); [reference] is when the SMS arrived. */
    fun verify(body: String, parsed: ParsedSms, reference: Long = System.currentTimeMillis()): Set<SmsIssue> {
        if (parsed.category != SmsCategory.TRANSACTION) return emptySet()
        return buildSet {
            parsed.amount?.let { if (!amountAppears(body, it)) add(SmsIssue.AMOUNT_NOT_IN_TEXT) }
            if (directionConflicts(body, parsed.direction)) add(SmsIssue.DIRECTION_CONFLICT)
            parsed.counterparty?.let { if (isJunk(it)) add(SmsIssue.MERCHANT_JUNK) }
            parsed.dateMillis?.let { if (it > reference + FUTURE_SLACK_MS) add(SmsIssue.DATE_IN_FUTURE) }
        }
    }

    /**
     * Applies the verdict: a wrong amount makes the parse untrustworthy, so it is withdrawn and the message
     * falls through to the model; a junk merchant or impossible date is dropped; a direction conflict is kept
     * but marked so the draft is reviewed. Every issue is recorded in the flags.
     */
    fun apply(parsed: ParsedSms, issues: Set<SmsIssue>): ParsedSms {
        if (issues.isEmpty()) return parsed
        var p = parsed.copy(flags = parsed.flags + issues.map { it.flag })
        if (SmsIssue.AMOUNT_NOT_IN_TEXT in issues) p = p.copy(amount = null, matched = false)
        if (SmsIssue.MERCHANT_JUNK in issues) p = p.copy(counterparty = null)
        if (SmsIssue.DATE_IN_FUTURE in issues) p = p.copy(dateMillis = null)
        return p
    }

    private fun amountAppears(body: String, amount: Double): Boolean {
        val plain = java.math.BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString()
        val tail = if ('.' in plain) "0*" else """(?:\.0+)?"""
        val pattern = Regex("""(?<!\d)(?<!\d\.)""" + Regex.escape(plain) + tail + """(?!\d|\.\d)""")
        return pattern.containsMatchIn(body.replace(",", ""))
    }

    private fun directionConflicts(body: String, direction: TxnDirection?): Boolean {
        val debit = debitWords.containsMatchIn(body)
        val credit = creditWords.containsMatchIn(body)
        return when (direction) {
            TxnDirection.DEBIT -> credit && !debit
            TxnDirection.CREDIT -> debit && !credit
            else -> false
        }
    }

    private fun isJunk(merchant: String): Boolean {
        val key = merchant.lowercase().filter { it.isLetterOrDigit() }
        return key.length < 2 || key.all { it.isDigit() } || key in junkMerchants
    }
}
