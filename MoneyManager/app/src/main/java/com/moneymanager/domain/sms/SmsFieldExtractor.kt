package com.moneymanager.domain.sms

enum class TxnDirection { DEBIT, CREDIT }

enum class SmsCategory { TRANSACTION, OTP, BALANCE, REMINDER, FAILED, PROMO, INFO, OTHER }

/** How well the baseline regex extractor understands a message. */
enum class ParseStatus { FULL, PARTIAL, NOT_TRANSACTION }

data class ExtractedFields(
    val amount: String?,
    val direction: TxnDirection?,
    val account: String?,
    val balance: String?,
    val merchant: String?,
    val reference: String?,
    val category: SmsCategory,
) {
    val present: Set<String>
        get() = buildSet {
            if (amount != null) add("amount")
            if (direction != null) add("direction")
            if (account != null) add("account")
            if (balance != null) add("balance")
            if (merchant != null) add("merchant")
            if (reference != null) add("reference")
        }

    val status: ParseStatus
        get() = when {
            category != SmsCategory.TRANSACTION -> ParseStatus.NOT_TRANSACTION
            amount != null && direction != null && (account != null || merchant != null || reference != null) -> ParseStatus.FULL
            else -> ParseStatus.PARTIAL
        }
}

/**
 * Baseline regex extractor (amount / direction / account / balance / merchant / reference),
 * in the style of transaction-sms-parser. It is deliberately simple: what it fails on is exactly
 * what the analyser reports as a gap.
 */
object SmsFieldExtractor {

    private const val NUM = """([\d,]+(?:\.\d{1,2})?)"""
    private val currency = Regex("""(?i)(?:rs\.?|inr|₹)\s*$NUM""")
    private val balance = Regex(
        """(?i)(?:avl\.?\s*(?:bal(?:ance)?|lmt|limit)|avbl\.?\s*(?:bal(?:ance)?|credit\s*limit)|available\s*(?:bal(?:ance)?|(?:credit\s*)?limit)|limit\s*available|a/c\s*bal|ac\s*bal|updated\s*balance|total\s*balance|new\s*balance|bal(?:ance)?|clear\s*bal)\s*(?:is|:|-)?\s*(?:rs\.?|inr|₹)?\s*$NUM"""
    )
    private val account = Regex(
        """(?i)(?:a/c|acct?|account|card)\s*(?:no\.?|number)?\s*[:.\-]?\s*((?:[x*]+)?\d{3,6})|ending\s*(?:with|in)?\s*(\d{3,6})"""
    )
    private val merchant = Regex(
        """(?i)\b(?:at|to|towards|from|info:?|trf to)\s+(?!a/c|ac\b|acct|account|your|card|you\b)([A-Za-z0-9&@._'\-/ ]{2,40}?)(?=\s+(?:on|via|ref|upi|using|avl|bal|available|for|is|has|dated)\b|[.,;]|$)"""
    )
    private val reference = Regex("""(?i)(?:ref(?:erence)?(?:\s*(?:no|id))?|utr|rrn|txn\s*id|upi\s*ref)\s*[:.#\-]?\s*(\w{6,})""")
    private val debitWords = Regex("""(?i)\b(debited|spent|withdrawn|withdrawal|paid|purchase|purchased|charged|sent|deducted|txn of)\b""")
    private val creditWords = Regex("""(?i)\b(credited|received|deposited|refund(?:ed)?)\b""")
    private val otpWords = Regex("""(?i)\b(otp|one[- ]time password|verification code|passcode|cvv)\b""")
    private val promoWords = Regex(
        """(?i)\b(offers?|discount|sale|cashback|win|apply (?:now|for|today|online|on|at)|click|limited time|pre-?approved|eligible|congrat(?:s|ulations)|rewards?|loans?|vouchers?|upgrade|recharge|can be credited|complete your (?:application|\d*-?min kyc))\b"""
    )
    private val failedWords = Regex("""(?i)\b(unsuccessful|failed|declined|could not be processed|not successful)\b""")
    private val walletAccount = Regex("""(?i)\b[a-z]*\s?wallet\b""")
    private val reminderWords = Regex(
        """(?i)\b(scheduled|will be (?:debited|deducted)|due (?:on|date|by)|reminder|upcoming|auto-?pay|amount due|min(?:imum)? (?:amt |amount )?due|statement|ignore if paid|has requested money|amount payable)\b"""
    )
    private val looseAccount = Regex("""(?i)(?<![a-z])[x*]{1,}\s?(\d{2,6})\b|\bfolio\s*(?:no\.?)?\s*(\d{4,})""")

    /** Balance / available limit amount printed in [body], without the currency. */
    fun balanceOf(body: String): String? = balance.find(body)?.groupValues?.get(1)

    fun extract(body: String): ExtractedFields {
        val balanceMatch = balance.find(body)
        val amountMatch = currency.findAll(body).firstOrNull { m ->
            balanceMatch == null || m.range.first !in balanceMatch.range
        }

        val debitPos = debitWords.find(body)?.range?.first
        val creditPos = creditWords.find(body)?.range?.first
        val direction = when {
            debitPos != null && (creditPos == null || debitPos < creditPos) -> TxnDirection.DEBIT
            creditPos != null -> TxnDirection.CREDIT
            else -> null
        }

        val acct = account.find(body)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }?.ifEmpty { null }
            ?: looseAccount.find(body)?.let { it.groupValues[1].ifEmpty { it.groupValues[2] } }?.ifEmpty { null }
            ?: walletAccount.find(body)?.value?.trim()
        val merch = merchant.find(body)?.groupValues?.get(1)?.trim()?.ifEmpty { null }
        val ref = reference.find(body)?.groupValues?.get(1)

        val amountStr = amountMatch?.groupValues?.get(1)
        val balanceStr = balanceMatch?.groupValues?.get(1)

        val strongEvidence = acct != null || ref != null || balanceStr != null
        val category = when {
            otpWords.containsMatchIn(body) -> SmsCategory.OTP
            failedWords.containsMatchIn(body) -> SmsCategory.FAILED
            promoWords.containsMatchIn(body) && !strongEvidence -> SmsCategory.PROMO
            amountStr != null && reminderWords.containsMatchIn(body) -> SmsCategory.REMINDER
            amountStr != null && direction != null -> SmsCategory.TRANSACTION
            balanceStr != null -> SmsCategory.BALANCE
            promoWords.containsMatchIn(body) -> SmsCategory.PROMO
            else -> SmsCategory.OTHER
        }

        return ExtractedFields(amountStr, direction, acct, balanceStr, merch, ref, category)
    }
}
