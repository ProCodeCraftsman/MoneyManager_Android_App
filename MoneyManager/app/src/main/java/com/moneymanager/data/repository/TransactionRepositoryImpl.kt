package com.moneymanager.data.repository

import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override fun getTransactionsByType(type: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByType(type)

    override fun searchTransactions(query: String): Flow<List<TransactionEntity>> =
        transactionDao.searchTransactions(query)

    override fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByDateRange(startDate, endDate)

    override fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    override fun getTotalByType(type: String): Flow<Double> =
        transactionDao.getTransactionsByType(type).map { transactions ->
            transactions.sumOf { it.amount }
        }

    override suspend fun getTransactionById(id: Long): TransactionEntity? =
        transactionDao.getTransactionById(id)

    override suspend fun insertTransaction(transaction: TransactionEntity): Long =
        transactionDao.insertTransaction(transaction)

    override suspend fun updateTransaction(transaction: TransactionEntity) =
        transactionDao.updateTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        transactionDao.deleteTransaction(transaction)

    override suspend fun deleteAllTransactions() =
        transactionDao.deleteAllTransactions()

    // Filter methods
    override fun getTransactionsByTag(tagId: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByTag(tagId)

    override fun getTransactionsWithFilters(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsWithFilters(accountId, type, categoryId, tagId, startDate, endDate)
}
