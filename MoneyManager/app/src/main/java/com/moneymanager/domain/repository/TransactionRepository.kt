package com.moneymanager.domain.repository

import com.moneymanager.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>
    fun getTotalByType(type: String): Flow<Double>
    // Filter methods
    fun getTransactionsByTag(tagId: Long): Flow<List<TransactionEntity>>
    fun getTransactionsWithFilters(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?
    ): Flow<List<TransactionEntity>>
    suspend fun getTransactionById(id: Long): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun deleteAllTransactions()
}
