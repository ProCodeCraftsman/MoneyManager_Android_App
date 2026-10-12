package com.moneymanager.domain.capture

import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.ai.TransactionDraft

/** One draft waiting in the inbox. [ledgerId] is the captured_messages row that holds it. */
data class PendingCapture(
    val ledgerId: Long,
    val draft: TransactionDraft,
    val messageDate: Long,
) {
    /** Saved transaction this draft may duplicate; set only when the dedup check was uncertain (AI-054). */
    val maybeDuplicateOf: Long? get() = CaptureFlags.maybeDuplicateOf(draft.flags)
}

object CaptureFlags {
    /** Flag prefix written by the dedup check: "maybe_dup:<transaction id>". */
    const val MAYBE_DUPLICATE = "maybe_dup:"

    fun maybeDuplicateOf(flags: List<String>): Long? =
        flags.firstNotNullOfOrNull { flag -> flag.takeIf { it.startsWith(MAYBE_DUPLICATE) }?.removePrefix(MAYBE_DUPLICATE)?.toLongOrNull() }

    fun withoutMaybeDuplicate(flags: List<String>): List<String> = flags.filterNot { it.startsWith(MAYBE_DUPLICATE) }
}

/**
 * Whether a draft may be saved in bulk without the user opening it (F07 "Bulk approve"). Only fully deterministic
 * expense or income rows qualify: every field filled, nothing flagged for review, and no duplicate question open.
 * A model guess or a missing field is never pre-approved.
 */
object InboxRules {
    private val BULK_TYPES = setOf("expense", "income")

    fun isReadyToApprove(draft: TransactionDraft): Boolean {
        val amount = draft.amount ?: return false
        return draft.typeId in BULK_TYPES &&
            amount > 0 &&
            draft.categoryId != null &&
            draft.accountId != null &&
            !draft.needsReview &&
            draft.flags.isEmpty()
    }
}

/** The transaction a pending draft becomes when the user saves it. Balance effects are applied by the caller. */
fun TransactionDraft.toTransactionEntity(now: Long = System.currentTimeMillis()): TransactionEntity = TransactionEntity(
    accountId = accountId ?: 0,
    type = typeId ?: "expense",
    amount = amount ?: 0.0,
    categoryId = categoryId,
    peerContactId = peerContactId,
    tagIds = tagIds.joinToString(","),
    date = date ?: now,
    note = note ?: description ?: "",
    description = description ?: note ?: "",
    receiptPath = receiptPath,
    toAccountId = toAccountId,
    isTransfer = typeId == "transfer",
    createdAt = now,
)
