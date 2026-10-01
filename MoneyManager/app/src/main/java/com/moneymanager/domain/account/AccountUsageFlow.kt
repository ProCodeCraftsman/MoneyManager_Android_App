package com.moneymanager.domain.account

import com.moneymanager.data.dao.AccountUsage
import com.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Account usage counts that refresh whenever any transaction changes. */
fun TransactionRepository.observeAccountUsage(): Flow<List<AccountUsage>> =
    getAllTransactions().map { getAccountUsage() }
