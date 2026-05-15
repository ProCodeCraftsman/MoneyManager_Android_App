package com.moneymanager.app.ui.dialogs

/**
 * Typed form state for each transaction type.
 * Replaces the flat bag-of-variables in AddEditTransactionDialog.
 * Currently used by TransactionFormConverter; future *Form.kt composables will consume it directly.
 */
sealed class TransactionFormState {

    data class ExpenseIncome(
        val type: String,                           // "expense" or "income"
        val amount: String = "",
        val accountId: Long? = null,
        val categoryId: Long? = null,               // leaf category id (sub or parent)
        val date: Long = System.currentTimeMillis(),
        val note: String = "",
        val peerId: Long? = null,
        val tagIds: Set<Long> = emptySet(),
        val receiptPath: String? = null,
        val isSplitParent: Boolean = false,
        val splitRows: List<SplitRowData> = listOf(SplitRowData(0), SplitRowData(1)),
        val goalId: Long? = null,
        val investmentPlatform: String? = null,
    ) : TransactionFormState()

    data class Transfer(
        val fromAccountId: Long? = null,            // outgoing / source account
        val toAccountId: Long? = null,              // incoming / destination account
        val amount: String = "",
        val date: Long = System.currentTimeMillis(),
        val note: String = "",
        val tagIds: Set<Long> = emptySet(),
        val receiptPath: String? = null,
    ) : TransactionFormState()

    data class LendBorrow(
        val type: String,                           // "lend" or "borrow"
        val peerId: Long? = null,
        val amount: String = "",
        val accountId: Long? = null,
        val dueDate: Long? = null,
        val date: Long = System.currentTimeMillis(),
        val note: String = "",
        val tagIds: Set<Long> = emptySet(),
        val receiptPath: String? = null,
    ) : TransactionFormState()

    data class Savings(
        val goalId: Long? = null,
        val investmentPlatform: String? = null,
        val amount: String = "",
        val accountId: Long? = null,
        val date: Long = System.currentTimeMillis(),
        val note: String = "",
        val tagIds: Set<Long> = emptySet(),
        val receiptPath: String? = null,
    ) : TransactionFormState()
}

fun String.parseTagIds(): Set<Long> =
    if (isEmpty()) emptySet()
    else split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
