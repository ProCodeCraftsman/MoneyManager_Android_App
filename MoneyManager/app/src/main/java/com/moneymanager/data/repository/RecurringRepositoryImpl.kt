package com.moneymanager.data.repository

import com.moneymanager.data.dao.RecurringDao
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.domain.repository.RecurringRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepositoryImpl @Inject constructor(
    private val recurringDao: RecurringDao
) : RecurringRepository {

    override fun getAllRecurring(): Flow<List<RecurringEntity>> =
        recurringDao.getAllRecurring()

    override suspend fun getRecurringById(id: Long): RecurringEntity? =
        recurringDao.getRecurringById(id)

    override suspend fun insertRecurring(recurring: RecurringEntity): Long =
        recurringDao.insertRecurring(recurring)

    override suspend fun updateRecurring(recurring: RecurringEntity) =
        recurringDao.updateRecurring(recurring)

    override suspend fun deleteRecurring(recurring: RecurringEntity) =
        recurringDao.deleteRecurring(recurring)
}
