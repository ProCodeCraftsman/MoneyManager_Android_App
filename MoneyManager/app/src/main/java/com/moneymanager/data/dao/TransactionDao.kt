package com.moneymanager.data.dao

import androidx.paging.PagingSource
import androidx.room.*
import com.moneymanager.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CategoryCount(val categoryId: Long, val count: Int)

data class MerchantCategoryCount(
    val description: String,
    val type: String,
    val categoryId: Long,
    val categoryName: String,
    val count: Int,
)

data class AccountUsage(val type: String, val accountId: Long, val toAccountId: Long?, val count: Int)

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
        AND (:categoryId IS NULL OR categoryId = :categoryId OR categoryId IN (SELECT id FROM categories WHERE parentId = :categoryId))
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
        AND (:categoryId IS NULL OR categoryId = :categoryId OR categoryId IN (SELECT id FROM categories WHERE parentId = :categoryId))
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
        AND (:categoryId IS NULL OR categoryId = :categoryId OR categoryId IN (SELECT id FROM categories WHERE parentId = :categoryId))
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
        AND (:categoryId IS NULL OR categoryId = :categoryId OR categoryId IN (SELECT id FROM categories WHERE parentId = :categoryId))
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
        AND (:categoryId IS NULL OR categoryId = :categoryId OR categoryId IN (SELECT id FROM categories WHERE parentId = :categoryId))
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

    /** Which category the user filed each description under, aggregated so no row-level data is loaded. */
    @Query("""
        SELECT t.description AS description, t.type AS type, t.categoryId AS categoryId, c.name AS categoryName, COUNT(*) AS count
        FROM transactions t JOIN categories c ON c.id = t.categoryId
        WHERE t.isSplitChild = 0 AND t.description != '' AND c.isArchived = 0
        GROUP BY t.description, t.type, t.categoryId
    """)
    suspend fun getMerchantCategoryCounts(): List<MerchantCategoryCount>

    @Query("SELECT type, accountId, toAccountId, COUNT(*) AS count FROM transactions WHERE isSplitChild = 0 GROUP BY type, accountId, toAccountId")
    suspend fun getAccountUsage(): List<AccountUsage>

    @Query("SELECT * FROM transactions WHERE receiptPath IS NOT NULL")
    suspend fun getTransactionsWithAttachments(): List<TransactionEntity>

    @Query("UPDATE transactions SET receiptPath = NULL")
    suspend fun clearAllReceiptPaths()

    @Query("SELECT MAX(createdAt) FROM transactions")
    suspend fun getLatestTimestamp(): Long?

    @Query("SELECT * FROM transactions WHERE emiId IS NOT NULL AND postedToBalance = 0 AND date <= :date")
    suspend fun getDueUnpostedEmiInstallments(date: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE emiId = :emiId ORDER BY emiInstallmentNumber ASC")
    fun getTransactionsByEmi(emiId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE emiId = :emiId AND postedToBalance = 0")
    suspend fun getUnpostedInstallmentCountByEmi(emiId: Long): Int

    @Query("DELETE FROM transactions WHERE emiId = :emiId AND postedToBalance = 0")
    suspend fun deleteUnpostedInstallmentsByEmi(emiId: Long)

    /** Saved transactions of this amount within a window; used to tell an SMS that is already entered by hand. */
    @Query("SELECT COUNT(*) FROM transactions WHERE ABS(amount - :amount) < 0.005 AND date BETWEEN :from AND :to AND isSplitParent = 0")
    suspend fun countNearAmount(amount: Double, from: Long, to: Long): Int

    /**
     * Candidates for SMS duplicate checks (AI-054): not split children, and not already claimed by a captured
     * message, so one saved spend can absorb at most one SMS.
     */
    @Query("""
        SELECT * FROM transactions
        WHERE ABS(amount - :amount) < 0.005 AND date BETWEEN :from AND :to
          AND isSplitChild = 0
          AND id NOT IN (SELECT transactionId FROM captured_messages WHERE transactionId IS NOT NULL)
        ORDER BY date
    """)
    suspend fun findUnclaimedNearAmount(amount: Double, from: Long, to: Long): List<TransactionEntity>

    /** Saved rows with their account type and category name, for matching against SMS (F08 L1). Split parents are skipped; their children carry the amounts. */
    @Query("""
        SELECT t.id AS id, t.type AS type, t.amount AS amount, t.date AS date,
               t.categoryId AS categoryId, c.name AS categoryName,
               t.accountId AS accountId, a.name AS accountName, a.type AS accountType
        FROM transactions t
        INNER JOIN accounts a ON a.id = t.accountId
        LEFT JOIN categories c ON c.id = t.categoryId
        WHERE t.date BETWEEN :from AND :to AND t.isSplitParent = 0
        ORDER BY t.date
    """)
    suspend fun getRowsForHistoryJoin(from: Long, to: Long): List<HistoryJoinRow>
}

data class HistoryJoinRow(
    val id: Long,
    val type: String,
    val amount: Double,
    val date: Long,
    val categoryId: Long?,
    val categoryName: String?,
    val accountId: Long,
    val accountName: String,
    val accountType: String,
)
