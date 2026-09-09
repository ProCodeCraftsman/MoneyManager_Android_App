package com.moneymanager.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneymanager.data.dao.AccountDao
import com.moneymanager.data.dao.GoalDao
import com.moneymanager.data.dao.RecurringDao
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.transaction.SplitTransactionFactory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class RecurringGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringDao: RecurringDao,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val goalDao: GoalDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val currentTime = System.currentTimeMillis()
            val dueRecurring = recurringDao.getDueRecurring(currentTime)

            for (recurring in dueRecurring) {
                var currentNextDate = recurring.nextDate
                var isActive = recurring.isActive

                // Catch-up loop to process all past-due occurrences up to currentTime
                while (currentNextDate <= currentTime && isActive) {
                    if (recurring.endDate != null && currentNextDate > recurring.endDate) {
                        isActive = false
                        break
                    }

                    val occurrence = recurring.copy(nextDate = currentNextDate)
                    createTransactionFromRecurring(occurrence)

                    currentNextDate = calculateNextDate(currentNextDate, recurring.frequency)

                    if (recurring.endDate != null && currentNextDate > recurring.endDate) {
                        isActive = false
                    }
                }

                val updatedRecurring = recurring.copy(
                    nextDate = currentNextDate,
                    isActive = isActive
                )
                recurringDao.updateRecurring(updatedRecurring)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun createTransactionFromRecurring(recurring: RecurringEntity) {
        val hasSplit = !recurring.splitData.isNullOrBlank()
        val transaction = TransactionEntity(
            accountId = recurring.accountId,
            type = recurring.type,
            amount = recurring.amount,
            categoryId = if (hasSplit) null else recurring.categoryId,
            subCategoryId = if (hasSplit) null else recurring.subCategoryId,
            goalId = recurring.goalId,
            peerContactId = recurring.peerContactId,
            tagIds = recurring.tagIds,
            date = recurring.nextDate,
            description = recurring.description,
            note = recurring.note,
            receiptPath = recurring.receiptPath,
            isRecurring = true,
            recurringId = recurring.id,
            isSplitParent = hasSplit,
            isTransfer = recurring.type == "transfer" || recurring.type == "savings",
            toAccountId = recurring.toAccountId,
            investmentPlatform = recurring.investmentPlatform,
            expectedReturnDate = recurring.expectedReturnDate,
            createdAt = System.currentTimeMillis()
        )
        val parentId = transactionDao.insertTransaction(transaction)

        if (hasSplit) {
            val children = SplitTransactionFactory.buildSplitChildrenFromTemplate(
                parentId = parentId,
                accountId = recurring.accountId,
                type = recurring.type,
                date = recurring.nextDate,
                splitData = recurring.splitData,
            )
            children.forEach { transactionDao.insertTransaction(it) }
        }

        // Update source account balance
        val account = accountDao.getAccountById(recurring.accountId)
        if (account != null) {
            val delta = when (recurring.type) {
                "income" -> recurring.amount
                "transfer", "expense", "savings", "lend" -> -recurring.amount
                "borrow" -> recurring.amount
                else -> -recurring.amount
            }
            val updatedAccount = account.copy(
                balance = account.balance + delta,
                updatedAt = System.currentTimeMillis()
            )
            accountDao.updateAccount(updatedAccount)

            // If it's a transfer or savings with a destination account, update destination account balance too
            if ((recurring.type == "transfer" || recurring.type == "savings") && recurring.toAccountId != null) {
                val toAccount = accountDao.getAccountById(recurring.toAccountId)
                if (toAccount != null) {
                    val updatedToAccount = toAccount.copy(
                        balance = toAccount.balance + recurring.amount,
                        updatedAt = System.currentTimeMillis()
                    )
                    accountDao.updateAccount(updatedToAccount)
                }
            }
        }

        // If linked to a goal, update goal progress
        if (recurring.goalId != null) {
            val goal = goalDao.getGoalById(recurring.goalId)
            if (goal != null) {
                val newCurrentAmount = goal.currentAmount + recurring.amount
                val isCompleted = newCurrentAmount >= goal.targetAmount
                goalDao.updateGoal(goal.copy(
                    currentAmount = newCurrentAmount,
                    isCompleted = isCompleted
                ))
            }
        }
    }

    private fun calculateNextDate(currentDate: Long, frequency: String): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = currentDate

        when (frequency) {
            "daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "biweekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
            "monthly" -> calendar.add(Calendar.MONTH, 1)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
        }

        return calendar.timeInMillis
    }
}
