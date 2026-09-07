package com.moneymanager.data.dao

import androidx.room.*
import com.moneymanager.data.entity.EmiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmiDao {
    @Query("SELECT * FROM emis ORDER BY startDate DESC")
    fun getAllEmis(): Flow<List<EmiEntity>>

    @Query("SELECT * FROM emis WHERE status = 'ACTIVE' ORDER BY startDate DESC")
    fun getActiveEmis(): Flow<List<EmiEntity>>

    @Query("SELECT * FROM emis WHERE id = :id")
    suspend fun getEmiById(id: Long): EmiEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmi(emi: EmiEntity): Long

    @Update
    suspend fun updateEmi(emi: EmiEntity)

    @Delete
    suspend fun deleteEmi(emi: EmiEntity)
}
