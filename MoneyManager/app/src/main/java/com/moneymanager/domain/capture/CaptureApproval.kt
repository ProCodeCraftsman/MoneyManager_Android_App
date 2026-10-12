package com.moneymanager.domain.capture

import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.TransactionRepository
import javax.inject.Inject

/** A saved draft and the transaction it created, kept so an undo can take both back. */
data class ApprovedCapture(val capture: PendingCapture, val transaction: TransactionEntity)

/**
 * Saves a pending draft as a transaction: inserts it, moves the account balance, and closes the ledger row. Undo
 * reverses all three. Nothing touches a balance before the user approves (F07).
 */
class CaptureApproval @Inject constructor(
    private val transactions: TransactionRepository,
    private val accounts: AccountRepository,
    private val review: CaptureReview,
) {

    suspend fun approve(capture: PendingCapture, now: Long = System.currentTimeMillis()): ApprovedCapture {
        val entity = capture.draft.toTransactionEntity(now)
        val id = transactions.insertTransaction(entity)
        val saved = entity.copy(id = id)
        moveBalance(saved, sign = 1.0)
        review.markSaved(capture.ledgerId, id)
        return ApprovedCapture(capture, saved)
    }

    suspend fun undo(approved: ApprovedCapture) {
        moveBalance(approved.transaction, sign = -1.0)
        transactions.deleteTransaction(approved.transaction)
        review.restorePending(approved.capture.ledgerId, approved.capture.draft)
    }

    /** Only expense, income and transfer reach here: [InboxRules] keeps other types out of bulk approve. */
    private suspend fun moveBalance(tx: TransactionEntity, sign: Double) {
        when (tx.type) {
            "income" -> accounts.updateAccountBalance(tx.accountId, sign * tx.amount)
            "expense" -> accounts.updateAccountBalance(tx.accountId, -sign * tx.amount)
            "transfer" -> {
                accounts.updateAccountBalance(tx.accountId, -sign * tx.amount)
                tx.toAccountId?.let { accounts.updateAccountBalance(it, sign * tx.amount) }
            }
        }
    }
}
