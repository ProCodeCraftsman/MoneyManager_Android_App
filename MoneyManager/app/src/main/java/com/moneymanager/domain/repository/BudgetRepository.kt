package com.moneymanager.domain.repository

import com.moneymanager.data.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getAllBudgets(): Flow<List<BudgetEntity>>
    fun getActiveBudgets(): Flow<List<BudgetEntity>>
    fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>>
    suspend fun getBudgetById(id: Long): BudgetEntity?
    suspend fun insertBudget(budget: BudgetEntity): Long
    suspend fun updateBudget(budget: BudgetEntity)
    suspend fun deleteBudget(budget: BudgetEntity)
    suspend fun deleteAllBudgets()
}
