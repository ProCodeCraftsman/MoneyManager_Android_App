package com.moneymanager.domain.repository

import androidx.paging.PagingData
import com.moneymanager.data.dao.CategoryCount
import com.moneymanager.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>
    fun getTransactionsByGoal(goalId: Long): Flow<List<TransactionEntity>>
    fun getTransactionsPaged(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        goalId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String? = null,
        sortDescending: Boolean = true,
        sortByAmount: Boolean = false
    ): Flow<PagingData<TransactionEntity>>
    fun getSplitChildren(parentId: Long): Flow<List<TransactionEntity>>
    suspend fun deleteSplitChildren(parentId: Long)
    suspend fun getTransactionById(id: Long): TransactionEntity?
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun getTransferSiblings(accountId: Long, toAccountId: Long, amount: Double, excludeId: Long): List<TransactionEntity>
    suspend fun getCategoryUsageCounts(): Map<Long, Int>

    suspend fun getTransactionsWithAttachments(): List<TransactionEntity>
    suspend fun clearAllReceiptPaths()
}
