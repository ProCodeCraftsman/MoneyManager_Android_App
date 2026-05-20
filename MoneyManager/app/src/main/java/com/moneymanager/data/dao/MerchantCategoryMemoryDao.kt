package com.moneymanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneymanager.data.entity.MerchantCategoryMemoryEntity

@Dao
interface MerchantCategoryMemoryDao {

    @Query("SELECT * FROM merchant_category_memory WHERE merchantKey = :key LIMIT 1")
    suspend fun findByKey(key: String): MerchantCategoryMemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: MerchantCategoryMemoryEntity)

    @Query("""
        UPDATE merchant_category_memory
        SET hitCount = hitCount + 1, lastUsedAt = :now
        WHERE merchantKey = :key
    """)
    suspend fun incrementHit(key: String, now: Long = System.currentTimeMillis())
}
