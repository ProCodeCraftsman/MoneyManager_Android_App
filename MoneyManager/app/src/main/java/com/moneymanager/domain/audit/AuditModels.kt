package com.moneymanager.domain.audit

/**
 * One saved transaction as the audit sees it: ids, amounts, dates and names only. No notes, descriptions or SMS
 * text. AI-074 fills it from the database; the checks never read the database themselves.
 */
data class AuditTxn(
    val id: Long,
    val type: String,
    val amount: Double,
    val date: Long,
    val createdAt: Long,
    val categoryId: Long?,
    val categoryType: String?,
    val accountId: Long,
    val accountArchived: Boolean,
    val toAccountId: Long?,
    val goalId: Long?,
    /** MerchantKey of the description, or of the SMS merchant when the row came from SMS. Null when there is none. */
    val merchantKey: String?,
    /** Account the SMS source is linked to, when the row came from SMS. */
    val sourceAccountId: Long?,
    /** Date of the source SMS, when the row came from SMS. */
    val smsDate: Long?,
)

/** A learned merchant memory entry: the category the user confirmed and how often. */
data class MemoryHint(val categoryId: Long, val hitCount: Int)

/** Latest "available balance" SMS for an account, with the app's balance for that account now. */
data class BalanceObservation(
    val accountId: Long,
    val smsBalance: Double,
    val observedAt: Long,
    val appBalance: Double,
)

enum class AuditCheck { A1_CATEGORY, A2_AMOUNT, A3_TYPE, A4_ACCOUNT, A5_DUPLICATE, A6_BALANCE, A7_DATE, A8_LEFTOVER }

/**
 * A possible mistake the user is asked about. It names the rows and the suggested fix; it never changes data.
 * [fingerprint] identifies the finding for "It's correct", so a dismissed finding is not raised again.
 */
data class AuditFinding(
    val check: AuditCheck,
    val fingerprint: String,
    val score: Double,
    val txnId: Long? = null,
    val otherTxnId: Long? = null,
    val accountId: Long? = null,
    val suggestedCategoryId: Long? = null,
    val suggestedAccountId: Long? = null,
    /** A2: the usual amount for the merchant or category. A6: the SMS balance. */
    val expected: Double? = null,
)
