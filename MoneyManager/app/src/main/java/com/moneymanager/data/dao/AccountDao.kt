package com.moneymanager.data.dao

import androidx.room.*
import com.moneymanager.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY isArchived ASC, name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 0 ORDER BY name ASC")
    fun getActiveAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isArchived = 1 ORDER BY name ASC")
    fun getArchivedAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT SUM(balance) FROM accounts WHERE balance > 0 AND isArchived = 0")
    fun getTotalAssets(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET isArchived = :isArchived, archivedAt = :archivedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun setAccountArchived(id: Long, isArchived: Boolean, archivedAt: Long?, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("SELECT MAX(MAX(IFNULL(createdAt, 0)), MAX(IFNULL(updatedAt, 0))) FROM accounts")
    suspend fun getLatestTimestamp(): Long?
}