package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_conversations")
data class AiConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawText: String,
    val sourceType: String,
    val sourceSender: String? = null,
    val prompt: String,
    val response: String,
    val parsedDraftJson: String? = null,
    val success: Boolean,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
