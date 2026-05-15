package com.moneymanager.data.ai

import android.content.Context
import android.content.Context
import com.google.mlkit.genai.common.PromptClient
import com.moneymanager.domain.ai.AiUnavailableException
import com.moneymanager.domain.ai.GenAiClient

class NanoAiClient(private val context: Context) : GenAiClient {

    override suspend fun generateDraft(prompt: String): Result<String> {
        return try {
            val promptClient = PromptClient.create(context)
            val response = promptClient.runPrompt(prompt)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(AiUnavailableException(e.message ?: "AICore error"))
        }
    }
}
