package com.moneymanager.domain.sms

/** What kind of account the message is about; drives account matching and the draft type. */
enum class Instrument { BANK_ACCOUNT, CREDIT_CARD, WALLET, MUTUAL_FUND, OTHER }

/** How a transaction maps onto the app's transaction types. */
enum class TxnNature {
    NORMAL,

    /** Merchant refund credited back; recorded as income. */
    REFUND,

    /** Cashback / fuel-surcharge waiver; recorded as income. */
    CASHBACK,

    /** Payment received by a credit card; recorded as a transfer into the card. */
    CARD_REPAYMENT,

    /** SIP / NPS / RD contribution; recorded as savings. */
    INVESTMENT,
}

/** Who the message came from, as far as the SMS says. [last4] is the trailing digits printed (account, card or folio). */
data class AccountHint(
    val institution: String?,
    val instrument: Instrument,
    val last4: String?,
)

data class SmsMessage(
    val sender: String?,
    val body: String,
    val receivedAt: Long? = null,
)

data class ParsedSms(
    val category: SmsCategory,
    val direction: TxnDirection?,
    val amount: Double?,
    val currency: String,
    val balance: Double?,
    val accountHint: AccountHint?,
    val counterparty: String?,
    val reference: String?,
    val dateMillis: Long?,
    val nature: TxnNature,
    val ruleId: String?,
    /** True when a built-in rule recognised the template; false for the generic fallback. */
    val matched: Boolean,
    val flags: List<String> = emptyList(),
) {
    val isActionableTransaction: Boolean
        get() = category == SmsCategory.TRANSACTION && amount != null && direction != null

    /** Coverage status used by the analyzer: can this message become a draft without help? */
    val status: ParseStatus
        get() = when {
            category != SmsCategory.TRANSACTION -> ParseStatus.NOT_TRANSACTION
            isActionableTransaction && (matched || accountHint != null || counterparty != null || reference != null) ->
                ParseStatus.FULL
            else -> ParseStatus.PARTIAL
        }

    /** Categories for which the user is told "this is not a transaction" instead of getting a draft. */
    val isDefinitelyNotATransaction: Boolean
        get() = when (category) {
            SmsCategory.OTP, SmsCategory.PROMO, SmsCategory.REMINDER, SmsCategory.FAILED, SmsCategory.INFO -> true
            else -> false
        }
}

class NotATransactionException(val category: SmsCategory, val ruleId: String? = null) :
    Exception(messageFor(category)) {
    companion object {
        fun messageFor(category: SmsCategory): String = when (category) {
            SmsCategory.OTP -> "This message is an OTP, not a transaction."
            SmsCategory.PROMO -> "This message is a promotion, not a transaction."
            SmsCategory.REMINDER -> "This message is a payment reminder or statement notice, not a completed transaction."
            SmsCategory.FAILED -> "This payment did not go through, so there is nothing to record."
            SmsCategory.INFO -> "This message is informational; there is no new transaction to record."
            else -> "No transaction found in this message."
        }
    }
}
