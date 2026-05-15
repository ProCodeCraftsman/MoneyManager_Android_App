package com.moneymanager.domain.ai

interface GenAiClient {
    suspend fun generateDraft(prompt: String): Result<String>
}
