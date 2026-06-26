package com.moneymanager.data.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.moneymanager.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryCount(val categoryId: Long, val count: Int)

data class TransactionSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val totalCount: Int
)

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE (accountId = :accountId OR toAccountId = :accountId) ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE goalId = :goalId ORDER BY date DESC")
    fun getTransactionsByGoal(goalId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE note LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchTransactions(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE tagIds LIKE '%' || :tagId || '%' ORDER BY date DESC")
    fun getTransactionsByTag(tagId: Long): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        WHERE (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:goalId IS NULL OR goalId = :goalId)
        AND (:tagId IS NULL OR tagIds LIKE '%' || :tagId || '%')
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:query IS NULL OR :query = '' OR
            note LIKE '%' || :query || '%' OR
            description LIKE '%' || :query || '%' OR
            categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%') OR
            accountId IN (SELECT id FROM accounts WHERE name LIKE '%' || :query || '%') OR
            CAST(amount AS TEXT) LIKE '%' || :query || '%'
        )
        ORDER BY date DESC
    """)
    fun getTransactionsWithFilters(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        goalId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): PagingSource<Int, TransactionEntity>

    @Query("""
        SELECT * FROM transactions
        WHERE (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:goalId IS NULL OR goalId = :goalId)
        AND (:tagId IS NULL OR tagIds LIKE '%' || :tagId || '%')
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:query IS NULL OR :query = '' OR
            note LIKE '%' || :query || '%' OR
            description LIKE '%' || :query || '%' OR
            categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%') OR
            accountId IN (SELECT id FROM accounts WHERE name LIKE '%' || :query || '%') OR
            CAST(amount AS TEXT) LIKE '%' || :query || '%'
        )
        ORDER BY date ASC
    """)
    fun getTransactionsWithFiltersOldest(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        goalId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): PagingSource<Int, TransactionEntity>

    @Query("""
        SELECT * FROM transactions
        WHERE (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:goalId IS NULL OR goalId = :goalId)
        AND (:tagId IS NULL OR tagIds LIKE '%' || :tagId || '%')
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:query IS NULL OR :query = '' OR
            note LIKE '%' || :query || '%' OR
            description LIKE '%' || :query || '%' OR
            categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%') OR
            accountId IN (SELECT id FROM accounts WHERE name LIKE '%' || :query || '%') OR
            CAST(amount AS TEXT) LIKE '%' || :query || '%'
        )
        ORDER BY amount DESC
    """)
    fun getTransactionsWithFiltersHighest(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        goalId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): PagingSource<Int, TransactionEntity>

    @Query("""
        SELECT * FROM transactions
        WHERE (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:goalId IS NULL OR goalId = :goalId)
        AND (:tagId IS NULL OR tagIds LIKE '%' || :tagId || '%')
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:query IS NULL OR :query = '' OR
            note LIKE '%' || :query || '%' OR
            description LIKE '%' || :query || '%' OR
            categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%') OR
            accountId IN (SELECT id FROM accounts WHERE name LIKE '%' || :query || '%') OR
            CAST(amount AS TEXT) LIKE '%' || :query || '%'
        )
        ORDER BY amount ASC
    """)
    fun getTransactionsWithFiltersLowest(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        goalId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): PagingSource<Int, TransactionEntity>

    @Query("""
        SELECT 
            TOTAL(CASE WHEN type = 'income' THEN amount ELSE 0 END) as totalIncome,
            TOTAL(CASE WHEN type = 'expense' THEN amount ELSE 0 END) as totalExpense,
            COUNT(*) as totalCount
        FROM transactions
        WHERE isSplitChild = 0
        AND (:accountId IS NULL OR accountId = :accountId OR toAccountId = :accountId)
        AND (:type IS NULL OR type = :type)
        AND (:categoryId IS NULL OR categoryId = :categoryId)
        AND (:goalId IS NULL OR goalId = :goalId)
        AND (:tagId IS NULL OR tagIds LIKE '%' || :tagId || '%')
        AND (:startDate IS NULL OR date >= :startDate)
        AND (:endDate IS NULL OR date <= :endDate)
        AND (:query IS NULL OR :query = '' OR
            note LIKE '%' || :query || '%' OR
            description LIKE '%' || :query || '%' OR
            categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%') OR
            accountId IN (SELECT id FROM accounts WHERE name LIKE '%' || :query || '%') OR
            CAST(amount AS TEXT) LIKE '%' || :query || '%'
        )
    """)
    fun getTransactionSummaryWithFilters(
        accountId: Long?,
        type: String?,
        categoryId: Long?,
        goalId: Long?,
        tagId: Long?,
        startDate: Long?,
        endDate: Long?,
        query: String?
    ): Flow<TransactionSummary>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE parentTransactionId = :parentId")
    fun getSplitChildren(parentId: Long): Flow<List<TransactionEntity>>

    @Query("DELETE FROM transactions WHERE parentTransactionId = :parentId")
    suspend fun deleteSplitChildren(parentId: Long)

    @Query("SELECT * FROM transactions WHERE accountId = :toAccountId AND toAccountId = :accountId AND type = 'transfer' AND amount = :amount AND id != :excludeId")
    suspend fun getTransferSiblings(accountId: Long, toAccountId: Long, amount: Double, excludeId: Long): List<TransactionEntity>

    @Query("SELECT categoryId, COUNT(*) AS count FROM transactions WHERE categoryId IS NOT NULL AND isSplitChild = 0 GROUP BY categoryId ORDER BY count DESC")
    suspend fun getCategoryUsageCounts(): List<CategoryCount>

    @Query("SELECT * FROM transactions WHERE receiptPath IS NOT NULL")
    suspend fun getTransactionsWithAttachments(): List<TransactionEntity>

    @Query("UPDATE transactions SET receiptPath = NULL")
    suspend fun clearAllReceiptPaths()
}