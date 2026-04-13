package com.moneymanager.data.repository

import com.moneymanager.data.dao.BudgetDao
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getAllBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getAllBudgets()

    override fun getActiveBudgets(): Flow<List<BudgetEntity>> =
        budgetDao.getActiveBudgets()

    override fun getBudgetsByPeriod(period: String): Flow<List<BudgetEntity>> =
        budgetDao.getBudgetsByPeriod(period)

    override suspend fun getBudgetById(id: Long): BudgetEntity? =
        budgetDao.getBudgetById(id)

    override suspend fun insertBudget(budget: BudgetEntity): Long =
        budgetDao.insertBudget(budget)

    override suspend fun updateBudget(budget: BudgetEntity) =
        budgetDao.updateBudget(budget)

    override suspend fun deleteBudget(budget: BudgetEntity) =
        budgetDao.deleteBudget(budget)

    override suspend fun deleteAllBudgets() =
        budgetDao.deleteAllBudgets()
}
