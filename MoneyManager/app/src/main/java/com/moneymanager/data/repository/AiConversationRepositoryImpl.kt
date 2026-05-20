package com.moneymanager.data.repository

import com.moneymanager.data.dao.AiConversationDao
import com.moneymanager.data.entity.AiConversationEntity
import com.moneymanager.domain.ai.AiConversationEntry
import com.moneymanager.domain.repository.AiConversationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiConversationRepositoryImpl @Inject constructor(
    private val dao: AiConversationDao
) : AiConversationRepository {

    override suspend fun log(entry: AiConversationEntry) {
        dao.insert(
            AiConversationEntity(
                rawText = entry.rawText,
                sourceType = entry.sourceType,
                sourceSender = entry.sourceSender,
                prompt = entry.prompt,
                response = entry.response,
                parsedDraftJson = entry.parsedDraftJson,
                success = entry.success,
                errorMessage = entry.errorMessage,
                createdAt = entry.createdAt
            )
        )
    }

    override fun getAllConversations(): Flow<List<AiConversationEntry>> {
        return dao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }

    private fun AiConversationEntity.toDomain() = AiConversationEntry(
        id = id,
        rawText = rawText,
        sourceType = sourceType,
        sourceSender = sourceSender,
        prompt = prompt,
        response = response,
        parsedDraftJson = parsedDraftJson,
        success = success,
        errorMessage = errorMessage,
        createdAt = createdAt
    )
}
