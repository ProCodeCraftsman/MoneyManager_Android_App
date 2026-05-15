package com.moneymanager.data.ai

import com.google.mlkit.genai.prompt.Generation
import com.moneymanager.domain.ai.AiUnavailableException
import com.moneymanager.domain.ai.GenAiClient

class NanoAiClient : GenAiClient {

    override suspend fun generateDraft(prompt: String): Result<String> {
        return try {
            val generativeModel = Generation.getClient()
            val response = generativeModel.generateContent(prompt)
            val text = response.candidates.firstOrNull()?.text ?: ""
            Result.success(text)
        } catch (e: Exception) {
            Result.failure(AiUnavailableException(e.message ?: "AICore error"))
        }
    }
}
