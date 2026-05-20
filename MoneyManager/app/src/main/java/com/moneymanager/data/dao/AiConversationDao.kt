package com.moneymanager.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneymanager.data.entity.AiConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiConversationDao {
    @Query("SELECT * FROM ai_conversations ORDER BY createdAt DESC")
    fun getAllConversations(): Flow<List<AiConversationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: AiConversationEntity): Long

    @Query("DELETE FROM ai_conversations WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ai_conversations")
    suspend fun deleteAll()
}
