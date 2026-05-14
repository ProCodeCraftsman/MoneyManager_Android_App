package com.moneymanager.domain.repository

import com.moneymanager.data.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

interface RecurringRepository {
    fun getAllRecurring(): Flow<List<RecurringEntity>>
    suspend fun getRecurringById(id: Long): RecurringEntity?
    suspend fun insertRecurring(recurring: RecurringEntity): Long
    suspend fun updateRecurring(recurring: RecurringEntity)
    suspend fun deleteRecurring(recurring: RecurringEntity)
}
