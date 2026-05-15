package com.moneymanager.domain.ai

import com.moneymanager.data.ai.DraftParser
import com.moneymanager.data.ai.PromptBuilder

class GenerateDraftFromTextUseCase(
    private val client: GenAiClient?
) {
    suspend operator fun invoke(rawText: String, context: PromptContext): Result<TransactionDraft> {
        val activeClient = client ?: return Result.failure(AiUnavailableException())
        val prompt = PromptBuilder.build(rawText, context)
        return activeClient.generateDraft(prompt)
            .mapCatching { rawResponse ->
                DraftParser.parse(rawResponse).getOrThrow()
            }
    }
}
