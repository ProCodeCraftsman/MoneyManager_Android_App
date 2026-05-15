package com.moneymanager.data.ai

import com.moneymanager.domain.ai.TransactionDraft
import kotlinx.serialization.json.Json

object DraftParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String): Result<TransactionDraft> {
        return try {
            val stripped = raw
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .trim()
                .removeSuffix("```")
                .trim()

            val jsonStart = stripped.indexOf('{')
            val jsonEnd = stripped.lastIndexOf('}')
            if (jsonStart == -1 || jsonEnd == -1) {
                return Result.failure(IllegalArgumentException("No JSON object found in AI response"))
            }

            val extracted = stripped.substring(jsonStart, jsonEnd + 1)
            val draft = json.decodeFromString<TransactionDraft>(extracted)
            Result.success(draft)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
