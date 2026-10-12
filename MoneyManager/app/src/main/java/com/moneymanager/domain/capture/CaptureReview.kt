package com.moneymanager.domain.capture

import com.moneymanager.domain.ai.TransactionDraft
import kotlinx.coroutines.flow.Flow

/** Read and decide the drafts held in the capture ledger. Implemented over Room in the data layer. */
interface CaptureReview {
    fun observePending(): Flow<List<PendingCapture>>

    suspend fun pendingDrafts(): List<PendingCapture>

    /** Closes a draft as saved as [transactionId]: used on save, and on "Merge" with a suspected duplicate. */
    suspend fun markSaved(ledgerId: Long, transactionId: Long)

    suspend fun markRejected(ledgerId: Long)

    /** Puts a draft back in the inbox, used by undo. */
    suspend fun restorePending(ledgerId: Long, draft: TransactionDraft)

    /** Replaces the pending draft's content, for inline fixes and account mapping. */
    suspend fun updateDraft(ledgerId: Long, draft: TransactionDraft)
}
