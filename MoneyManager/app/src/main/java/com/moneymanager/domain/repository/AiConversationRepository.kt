package com.moneymanager.domain.repository

import com.moneymanager.domain.ai.AiConversationEntry
import kotlinx.coroutines.flow.Flow

interface AiConversationRepository {
    suspend fun log(entry: AiConversationEntry)
    fun getAllConversations(): Flow<List<AiConversationEntry>>
    suspend fun deleteById(id: Long)
    suspend fun deleteAll()
}
