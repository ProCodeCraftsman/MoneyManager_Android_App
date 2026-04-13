package com.moneymanager.domain.repository

import com.moneymanager.data.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<AccountEntity>>
    fun getTotalAssets(): Flow<Double>
    fun getTotalDebt(): Flow<Double>
    suspend fun getAccountById(id: Long): AccountEntity?
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
    suspend fun updateBalance(accountId: Long, newBalance: Double)
    suspend fun deleteAllAccounts()
}
