package com.moneymanager.domain.ai

data class AiConversationEntry(
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
