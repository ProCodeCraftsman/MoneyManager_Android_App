package com.moneymanager.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneymanager.data.dao.AccountDao
import com.moneymanager.data.dao.TransactionDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Applies the balance effect of EMI installments as their due dates arrive.
 *
 * `addEmiExpense` (in [com.moneymanager.app.ui.transactions.TransactionsViewModel] and
 * [com.moneymanager.app.ui.addtransaction.AddTransactionViewModel]) inserts every future
 * installment as a [com.moneymanager.data.entity.TransactionEntity] up front, but only the
 * first installment's amount is deducted from the account balance immediately. Without this
 * worker, later installments would sit in transaction history with a future date but never
 * actually affect the running balance when that date arrives.
 */
@HiltWorker
class EmiPostingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val due = transactionDao.getDueUnpostedEmiInstallments(System.currentTimeMillis())
            for (installment in due) {
                val account = accountDao.getAccountById(installment.accountId) ?: continue
                val delta = when (installment.type) {
                    "income" -> installment.amount
                    "borrow" -> installment.amount
                    else -> -installment.amount
                }
                accountDao.updateAccount(
                    account.copy(balance = account.balance + delta, updatedAt = System.currentTimeMillis())
                )
                transactionDao.updateTransaction(installment.copy(postedToBalance = true))
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "emi_periodic_posting"
    }
}
