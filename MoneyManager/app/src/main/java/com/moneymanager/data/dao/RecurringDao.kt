package com.moneymanager.data.dao

import androidx.room.*
import com.moneymanager.data.entity.RecurringEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring ORDER BY nextDate ASC")
    fun getAllRecurring(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE isActive = 1 ORDER BY nextDate ASC")
    fun getActiveRecurring(): Flow<List<RecurringEntity>>

    @Query("SELECT * FROM recurring WHERE id = :id")
    suspend fun getRecurringById(id: Long): RecurringEntity?

    @Query("SELECT * FROM recurring WHERE isActive = 1 AND nextDate <= :date")
    suspend fun getDueRecurring(date: Long): List<RecurringEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurring(recurring: RecurringEntity): Long

    @Update
    suspend fun updateRecurring(recurring: RecurringEntity)

    @Delete
    suspend fun deleteRecurring(recurring: RecurringEntity)

    @Query("DELETE FROM recurring")
    suspend fun deleteAllRecurring()
}