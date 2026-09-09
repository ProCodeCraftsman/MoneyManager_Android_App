package com.moneymanager.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneymanager.data.MoneyManagerDatabase
import com.moneymanager.data.dao.AccountDao
import com.moneymanager.data.dao.EmiDao
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
    private val database: MoneyManagerDatabase,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val emiDao: EmiDao,
    private val locks: GenerationLocks,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // See GenerationLocks: this worker can be triggered both by its own periodic chain
        // and (once due) a one-time launch trigger; only one should process the due list.
        if (!locks.emiPosting.tryLock()) {
            return Result.success()
        }
        return try {
            val due = transactionDao.getDueUnpostedEmiInstallments(System.currentTimeMillis())
            for (installment in due) {
                val account = accountDao.getAccountById(installment.accountId) ?: continue
                val delta = when (installment.type) {
                    "income" -> installment.amount
                    "borrow" -> installment.amount
                    else -> -installment.amount
                }

                // Balance update + postedToBalance flag write happen together, so a crash
                // between them can't cause a retry to double-apply this installment's delta.
                database.withTransaction {
                    accountDao.updateAccount(
                        account.copy(balance = account.balance + delta, updatedAt = System.currentTimeMillis())
                    )
                    transactionDao.updateTransaction(installment.copy(postedToBalance = true))

                    val emiId = installment.emiId
                    if (emiId != null && transactionDao.getUnpostedInstallmentCountByEmi(emiId) == 0) {
                        emiDao.getEmiById(emiId)?.let { emi ->
                            if (emi.status == "ACTIVE") {
                                emiDao.updateEmi(emi.copy(status = "COMPLETED"))
                            }
                        }
                    }
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        } finally {
            locks.emiPosting.unlock()
        }
    }

    companion object {
        const val WORK_NAME = "emi_periodic_posting"
    }
}
