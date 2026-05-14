package com.moneymanager.data.repository

import com.moneymanager.data.dao.GoalDao
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.domain.repository.GoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepositoryImpl @Inject constructor(
    private val goalDao: GoalDao
) : GoalRepository {

    override fun getAllGoals(): Flow<List<GoalEntity>> =
        goalDao.getAllGoals()

    override suspend fun getGoalById(id: Long): GoalEntity? =
        goalDao.getGoalById(id)

    override suspend fun insertGoal(goal: GoalEntity): Long =
        goalDao.insertGoal(goal)

    override suspend fun updateGoal(goal: GoalEntity) =
        goalDao.updateGoal(goal)

    override suspend fun deleteGoal(goal: GoalEntity) =
        goalDao.deleteGoal(goal)
}
