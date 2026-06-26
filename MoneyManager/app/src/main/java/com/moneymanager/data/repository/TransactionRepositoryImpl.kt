package com.moneymanager.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.dao.TransactionSummary
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<TransactionEntity>> =
        transactionDao.getAllTransactions()

    override fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByAccount(accountId)

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    override fun getTransactionsByGoal(goalId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByGoal(goalId)

    override fun getSplitChildren(parentId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getSplitChildren(parentId)

    override suspend fun deleteSplitChildren(parentId: Long) =
        transactionDao.deleteSplitChildren(parentId)

    override suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    override suspend fun getTransferSiblings(accountId: Long, toAccountId: Long, amount: Double, excludeId: Long): List<TransactionEntity> =
        transactionDao.getTransferSiblings(accountId, toAccountId, amount, excludeId)

    override suspend fun getCategoryUsageCounts(): Map<Long, Int> =
        transactionDao.getCategoryUsageCounts().associate { it.categoryId to it.count }

    override suspend fun getTransactionsWithAttachments(): List<TransactionEntity> {
        return transactionDao.getTransactionsWithAttachments()
    }

    override suspend fun clearAllReceiptPaths() {
        transactionDao.clearAllReceiptPaths()
    }

    override fun getTransactionsPaged(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        goalId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?,
        sortDescending: Boolean,
        sortByAmount: Boolean
    ): Flow<PagingData<TransactionEntity>> {
        return Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                initialLoadSize = 50,
                prefetchDistance = 100
            )
        ) {
            when {
                sortByAmount && sortDescending -> transactionDao.getTransactionsWithFiltersHighest(accountId, type, categoryId, goalId, tagId, startDate, endDate, query)
                sortByAmount && !sortDescending -> transactionDao.getTransactionsWithFiltersLowest(accountId, type, categoryId, goalId, tagId, startDate, endDate, query)
                !sortByAmount && !sortDescending -> transactionDao.getTransactionsWithFiltersOldest(accountId, type, categoryId, goalId, tagId, startDate, endDate, query)
                else -> transactionDao.getTransactionsWithFilters(accountId, type, categoryId, goalId, tagId, startDate, endDate, query)
            }
        }.flow
    }

    override fun getTransactionSummary(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        goalId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): Flow<TransactionSummary> {
        return transactionDao.getTransactionSummaryWithFilters(
            accountId, type, categoryId, goalId, tagId, startDate, endDate, query
        )
    }
}
