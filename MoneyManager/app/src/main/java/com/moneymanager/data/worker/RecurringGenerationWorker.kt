package com.moneymanager.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneymanager.data.dao.RecurringDao
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.data.entity.TransactionEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar

@HiltWorker
class RecurringGenerationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringDao: RecurringDao,
    private val transactionDao: TransactionDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val currentTime = System.currentTimeMillis()
            val dueRecurring = recurringDao.getDueRecurring(currentTime)

            for (recurring in dueRecurring) {
                createTransactionFromRecurring(recurring)
                updateNextDate(recurring)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun createTransactionFromRecurring(recurring: RecurringEntity) {
        val transaction = TransactionEntity(
            accountId = recurring.accountId,
            type = recurring.type,
            amount = recurring.amount,
            categoryId = recurring.categoryId,
            date = recurring.nextDate,
            note = recurring.note,
            isRecurring = true,
            recurringId = recurring.id,
            createdAt = System.currentTimeMillis()
        )
        transactionDao.insertTransaction(transaction)
    }

    private suspend fun updateNextDate(recurring: RecurringEntity) {
        val newNextDate = calculateNextDate(recurring.nextDate, recurring.frequency)
        val updatedRecurring = recurring.copy(nextDate = newNextDate)
        recurringDao.updateRecurring(updatedRecurring)
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