package com.moneymanager.domain.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface GenAiClient {
    suspend fun generateDraft(prompt: String): Result<String>

    /**
     * Agentic variant: separate system instruction from user content.
     * Backends that support system instructions (EdgeAiClient with litertlm) override this
     * to wire [systemInstruction] into ConversationConfig and attach the TransactionToolSet.
     * All other backends fall back to a single combined prompt via [generateDraft].
     */
    suspend fun generateDraft(
        systemInstruction: String,
        userMessage: String,
    ): Result<String> = generateDraft("$systemInstruction\n\n$userMessage")

    /**
     * Streaming variant — tokens are delivered via [onToken] as they arrive.
     * Backends that support streaming (EdgeAiClient with litertlm) override this
     * to deliver tokens incrementally. Default falls back to full response.
     */
    suspend fun generateDraftStreaming(
        systemInstruction: String,
        userMessage: String,
        onToken: (String) -> Unit,
    ): Result<String> {
        val result = generateDraft(systemInstruction, userMessage)
        result.getOrNull()?.let { onToken(it) }
        return result
    }

    fun generateDraftWithProgress(prompt: String): Flow<AiResult<String>> = flow {
        emit(AiResult.Loading)
        emit(
            generateDraft(prompt).fold(
                onSuccess = { AiResult.Success(it) },
                onFailure = { AiResult.Error(it) }
            )
        )
    }
}
