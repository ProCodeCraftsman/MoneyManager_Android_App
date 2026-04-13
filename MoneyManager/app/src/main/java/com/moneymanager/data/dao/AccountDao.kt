package com.moneymanager.data.dao

import androidx.room.*
import com.moneymanager.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE id = :id")
    fun getAccountByIdFlow(id: Long): Flow<AccountEntity?>

    @Query("SELECT SUM(balance) FROM accounts WHERE type IN ('bank', 'cash', 'savings', 'investment')")
    fun getTotalAssets(): Flow<Double?>

    @Query("SELECT ABS(SUM(balance)) FROM accounts WHERE type = 'credit'")
    fun getTotalDebt(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()
}