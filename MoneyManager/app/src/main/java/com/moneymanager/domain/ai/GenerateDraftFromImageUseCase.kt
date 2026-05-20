package com.moneymanager.domain.ai

import android.util.Log
import com.moneymanager.data.ai.CategoryPreFilter
import com.moneymanager.data.ai.DraftParser
import com.moneymanager.data.ai.PromptBuilder
import com.moneymanager.data.ai.agent.DraftValidator
import com.moneymanager.domain.repository.AiConversationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class      GenerateDraftFromImageUseCase(
    private val client: GenAiClient?,
    private val conversationRepository: AiConversationRepository,
) {
    companion object {
        private const val INFERENCE_TIMEOUT_MS = 90_000L
        private const val TAG = "AiImageDraftDebug"
    }

    /**
     * Generates a transaction draft by sending raw image bytes directly to the LLM.
     * No OCR step — the vision model reads the receipt image itself.
     *
     * Pipeline:
     *   1. Build image-specific system instruction with tool order + confidence rules.
     *   2. Send imageBytes + user message to client.generateDraftFromImage().
     *   3. Parse + validate the JSON response (same repair logic as text path).
     *   4. Force needsReview=true if amount or typeId confidence is low.
     */
    suspend operator fun invoke(
        imageBytes: ByteArray,
        context: PromptContext,
        sourceType: String = "RECEIPT",
    ): Result<TransactionDraft> {
        if (client == null) {
            return Result.failure(AiUnavailableException("No AI backend available"))
        }

        val narrowed = CategoryPreFilter.narrow(
            merchantHint = null,
            rawText = "",
            typeId = null,
            allCategories = context.allCategories.ifEmpty { context.top20Categories },
        )

        val systemInstruction = PromptBuilder.buildImageSystemInstruction(context, narrowed)
        val userMessage = PromptBuilder.buildImageUserMessage()

        Log.d(TAG, "IMAGE SYSTEM INSTRUCTION:\n$systemInstruction")

        var rawResponse = ""

        return try {
            val draft: TransactionDraft = withTimeout(INFERENCE_TIMEOUT_MS) {

                // ── Primary call ──────────────────────────────────────────────
                rawResponse = client.generateDraftFromImage(systemInstruction, userMessage, imageBytes).getOrThrow()
                Log.d(TAG, "RAW_RESPONSE (image primary):\n$rawResponse")

                val primaryDraft = DraftParser.parse(rawResponse, context).getOrNull()
                val primaryValidation = primaryDraft?.let { DraftValidator.validate(it) }

                val parsedDraft = if (primaryDraft != null && primaryValidation?.valid != false) {
                    primaryDraft
                } else {
                    // ── Correction retry ──────────────────────────────────────
                    val issues = primaryValidation?.issues
                        ?: listOf("could not parse a valid JSON object from the response")
                    Log.d(TAG, "Image validation issues: $issues — retrying")

                    val correctionMessage = buildString {
                        appendLine(userMessage)
                        appendLine()
                        appendLine("Your previous response had the following issues:")
                        issues.forEach { appendLine("  - $it") }
                        append("Output ONLY corrected JSON. No other text.")
                    }

                    rawResponse = client.generateDraftFromImage(systemInstruction, correctionMessage, imageBytes).getOrThrow()
                    Log.d(TAG, "RAW_RESPONSE (image retry):\n$rawResponse")
                    DraftParser.parse(rawResponse, context).getOrThrow()
                }

                // Force needs_review when critical fields are low confidence
                val criticalLow = listOf("amount", "typeId").any { field ->
                    parsedDraft.confidence[field] == "low"
                }
                if (criticalLow && !parsedDraft.needsReview) parsedDraft.copy(needsReview = true)
                else parsedDraft
            }

            logConversation(sourceType, "$systemInstruction\n\n$userMessage", rawResponse, draft, success = true)
            Result.success(draft)

        } catch (e: TimeoutCancellationException) {
            logConversation(sourceType, "$systemInstruction\n\n$userMessage", rawResponse, null,
                success = false, error = "Image AI inference timed out")
            Result.failure(AiUnavailableException("AI inference timed out"))

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            logConversation(sourceType, "$systemInstruction\n\n$userMessage", rawResponse, null,
                success = false, error = e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    private suspend fun logConversation(
        sourceType: String,
        prompt: String,
        response: String,
        draft: TransactionDraft?,
        success: Boolean,
        error: String? = null,
    ) {
        val draftJson = draft?.let {
            runCatching { Json.encodeToString(TransactionDraft.serializer(), it) }.getOrNull()
        }
        conversationRepository.log(
            AiConversationEntry(
                rawText = "[image]",
                sourceType = sourceType,
                sourceSender = null,
                prompt = prompt,
                response = response,
                parsedDraftJson = draftJson,
                success = success,
                errorMessage = error,
            )
        )
    }
}
