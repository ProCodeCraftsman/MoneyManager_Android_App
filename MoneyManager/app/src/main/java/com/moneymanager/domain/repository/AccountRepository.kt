package com.moneymanager.domain.repository

import com.moneymanager.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    fun getActiveAccounts(): Flow<List<AccountEntity>>
    fun getArchivedAccounts(): Flow<List<AccountEntity>>
    fun getTotalAssets(): Flow<Double>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
    suspend fun updateAccountBalance(accountId: Long, delta: Double)
    suspend fun archiveAccount(id: Long)
    suspend fun reactivateAccount(id: Long)
    suspend fun deleteAccount(account: AccountEntity)
}
