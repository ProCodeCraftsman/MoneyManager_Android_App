package com.moneymanager.data.repository

import com.moneymanager.data.dao.AccountDao
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<AccountEntity>> =
        accountDao.getAllAccounts()

    override fun getTotalAssets(): Flow<Double> =
        accountDao.getTotalAssets().map { it ?: 0.0 }

    override suspend fun getAccountById(id: Long): AccountEntity? =
        accountDao.getAccountById(id)

    override suspend fun insertAccount(account: AccountEntity): Long =
        accountDao.insertAccount(account)

    override suspend fun updateAccount(account: AccountEntity) =
        accountDao.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))

    override suspend fun deleteAccount(account: AccountEntity) =
        accountDao.deleteAccount(account)

    override suspend fun updateAccountBalance(accountId: Long, delta: Double) {
        accountDao.getAccountById(accountId)?.let { account ->
            accountDao.updateAccount(account.copy(
                balance = account.balance + delta,
                updatedAt = System.currentTimeMillis()
            ))
        }
    }
}
