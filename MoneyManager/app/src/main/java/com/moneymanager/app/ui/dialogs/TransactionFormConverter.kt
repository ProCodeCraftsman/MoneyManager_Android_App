package com.moneymanager.app.ui.dialogs

import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TransactionEntity

/**
 * Converts between TransactionEntity and TransactionFormState.
 *
 * Transfer normalisation:
 *   A transfer is stored as two legs. The incoming leg has note "Transfer from …" and has
 *   accountId=destination, toAccountId=source — the opposite of what the form should display.
 *   toFormState() always returns a Transfer state with fromAccountId=source, toAccountId=dest.
 */
fun TransactionEntity.toFormState(categories: List<CategoryEntity> = emptyList()): TransactionFormState {
    val parsedTags = tagIds.parseTagIds()

    return when (type) {
        "transfer" -> {
            val isIncomingLeg = note.contains("transfer from", ignoreCase = true)
            TransactionFormState.Transfer(
                fromAccountId = if (isIncomingLeg) toAccountId else accountId,
                toAccountId   = if (isIncomingLeg) accountId   else toAccountId,
                amount        = amount.toBigDecimal().stripTrailingZeros().toPlainString(),
                date          = date,
                note          = if (isIncomingLeg) "" else note
                    .replace("Transfer to Account", "", ignoreCase = true).trim(),
                tagIds        = parsedTags,
                receiptPath   = receiptPath,
            )
        }

        "lend", "borrow" -> TransactionFormState.LendBorrow(
            type        = type,
            peerId      = peerContactId,
            amount      = amount.toBigDecimal().stripTrailingZeros().toPlainString(),
            accountId   = accountId,
            dueDate     = expectedReturnDate,
            date        = date,
            note        = note,
            tagIds      = parsedTags,
            receiptPath = receiptPath,
        )

        "savings" -> TransactionFormState.Savings(
            goalId              = goalId,
            investmentPlatform  = investmentPlatform,
            amount              = amount.toBigDecimal().stripTrailingZeros().toPlainString(),
            accountId           = accountId,
            date                = date,
            note                = note,
            tagIds              = parsedTags,
            receiptPath         = receiptPath,
        )

        else -> {
            // expense, income — display under ExpenseIncome form
            val cat = categories.firstOrNull { it.id == categoryId }
            val isSub = cat?.parentId != null
            TransactionFormState.ExpenseIncome(
                type        = type,
                amount      = amount.toBigDecimal().stripTrailingZeros().toPlainString(),
                accountId   = accountId,
                categoryId  = categoryId,
                date        = date,
                note        = note,
                peerId      = peerContactId,
                tagIds      = parsedTags,
                receiptPath = receiptPath,
                isSplitParent = isSplitParent,
                goalId              = goalId,
                investmentPlatform  = investmentPlatform,
            )
        }
    }
}

/**
 * Converts a filled-in form state back to a TransactionEntity ready for insert or update.
 * Returns null if the form is incomplete (amount invalid, required account missing, etc.).
 */
fun TransactionFormState.toTransactionEntity(
    originalId: Long = 0,
    originalCreatedAt: Long = System.currentTimeMillis(),
    originalIsRecurring: Boolean = false,
    originalRecurringId: Long? = null,
): TransactionEntity? = when (this) {

    is TransactionFormState.ExpenseIncome -> {
        val amt = amount.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        if (accountId == null) return null
        TransactionEntity(
            id                  = originalId,
            accountId           = accountId,
            type                = type,
            amount              = amt,
            categoryId          = categoryId,
            goalId              = goalId,
            peerContactId       = peerId,
            investmentPlatform  = investmentPlatform,
            tagIds              = tagIds.joinToString(","),
            date                = date,
            note                = note,
            description         = note,
            receiptPath         = receiptPath,
            isRecurring         = originalIsRecurring,
            recurringId         = originalRecurringId,
            isSplitParent       = isSplitParent,
            createdAt           = originalCreatedAt,
        )
    }

    is TransactionFormState.Transfer -> {
        val amt = amount.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        if (fromAccountId == null || toAccountId == null) return null
        TransactionEntity(
            id          = originalId,
            accountId   = fromAccountId,
            toAccountId = toAccountId,
            type        = "transfer",
            isTransfer  = true,
            amount      = amt,
            tagIds      = tagIds.joinToString(","),
            date        = date,
            note        = note,
            description = note,
            receiptPath = receiptPath,
            createdAt   = originalCreatedAt,
        )
    }

    is TransactionFormState.LendBorrow -> {
        val amt = amount.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        if (accountId == null) return null
        TransactionEntity(
            id                 = originalId,
            accountId          = accountId,
            type               = type,
            amount             = amt,
            peerContactId      = peerId,
            expectedReturnDate = dueDate,
            tagIds             = tagIds.joinToString(","),
            date               = date,
            note               = note,
            description        = note,
            receiptPath        = receiptPath,
            isRecurring        = originalIsRecurring,
            recurringId        = originalRecurringId,
            createdAt          = originalCreatedAt,
        )
    }

    is TransactionFormState.Savings -> {
        val amt = amount.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        if (accountId == null) return null
        TransactionEntity(
            id                  = originalId,
            accountId           = accountId,
            type                = "savings",
            amount              = amt,
            goalId              = goalId,
            investmentPlatform  = investmentPlatform,
            tagIds              = tagIds.joinToString(","),
            date                = date,
            note                = note,
            description         = note,
            receiptPath         = receiptPath,
            isRecurring         = originalIsRecurring,
            recurringId         = originalRecurringId,
            createdAt           = originalCreatedAt,
        )
    }
}
