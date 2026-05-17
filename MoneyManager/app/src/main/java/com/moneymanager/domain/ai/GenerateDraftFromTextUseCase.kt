package com.moneymanager.domain.ai

import android.util.Log
import com.moneymanager.data.ai.CategoryPreFilter
import com.moneymanager.data.ai.DraftParser
import com.moneymanager.data.ai.PreExtraction
import com.moneymanager.data.ai.PromptBuilder
import com.moneymanager.data.ai.agent.DraftValidator
import com.moneymanager.domain.repository.AiConversationRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json

class GenerateDraftFromTextUseCase(
    private val client: GenAiClient?,
    private val conversationRepository: AiConversationRepository,
) {
    companion object {
        private const val INFERENCE_TIMEOUT_MS = 60_000L
        private const val TAG = "AiDraftDebug"
    }

    /**
     * Generates a transaction draft via the agentic two-step pipeline:
     *
     * Step 1 — Primary inference:
     *   Sends a compact system instruction + user message to [client].
     *   EdgeAiClient routes systemInstruction into ConversationConfig and attaches
     *   TransactionToolSet so the LLM resolves categories/accounts via tool calls.
     *   NanoAiClient falls back to a combined flat prompt (default GenAiClient impl).
     *
     * Step 2 — Validation + conditional retry:
     *   Runs DraftValidator on the parsed draft. If structural issues are found
     *   (invalid typeId, etc.) a single correction turn is sent with the issue list.
     *   DraftParser handles JSON repair (unclosed braces from token cutoff) before
     *   validation, so the retry only fires on semantic failures.
     *
     * DeterministicExtractor values (amount, typeId, date) override the AI output at
     * the end — they are more reliable than any model output on noisy text.
     */
    suspend operator fun invoke(
        rawText: String,
        context: PromptContext,
        sourceType: String = "",
        sourceSender: String? = null,
        preExtraction: PreExtraction? = null,
    ): Result<TransactionDraft> {
        if (client == null) {
            return Result.failure(AiUnavailableException("No AI backend available"))
        }

        val narrowed = CategoryPreFilter.narrow(
            merchantHint = preExtraction?.merchantHint,
            rawText = rawText,
            typeId = preExtraction?.typeId,
            allCategories = context.allCategories.ifEmpty { context.top20Categories },
        )

        val systemInstruction = PromptBuilder.buildSystemInstruction(context, narrowed)
        val userMessage = PromptBuilder.buildUserMessage(rawText, preExtraction)

        Log.d(TAG, "SYSTEM:\n$systemInstruction")
        Log.d(TAG, "USER:\n$userMessage")

        var rawResponse = ""

        return try {
            val draft: TransactionDraft = withTimeout(INFERENCE_TIMEOUT_MS) {

                // ── Primary call ──────────────────────────────────────────────
                rawResponse = client.generateDraft(systemInstruction, userMessage).getOrThrow()
                Log.d(TAG, "RAW_RESPONSE (primary):\n$rawResponse")

                val primaryDraft = DraftParser.parse(rawResponse, context).getOrNull()
                val primaryValidation = primaryDraft?.let { DraftValidator.validate(it) }

                val finalDraft = if (primaryDraft != null && primaryValidation?.valid != false) {
                    // Happy path — valid draft on first attempt
                    primaryDraft
                } else {
                    // ── Correction retry ──────────────────────────────────────
                    val issues = primaryValidation?.issues
                        ?: listOf("could not parse a valid JSON object from the response")
                    Log.d(TAG, "Validation issues: $issues — retrying")

                    val correctionMessage = buildString {
                        appendLine(userMessage)
                        appendLine()
                        appendLine("Your previous response had the following issues:")
                        issues.forEach { appendLine("  - $it") }
                        append("Output ONLY corrected JSON. No other text.")
                    }

                    rawResponse = client.generateDraft(systemInstruction, correctionMessage).getOrThrow()
                    Log.d(TAG, "RAW_RESPONSE (retry):\n$rawResponse")
                    DraftParser.parse(rawResponse, context).getOrThrow()
                }

                // DeterministicExtractor values win over AI output
                applyPreExtraction(finalDraft, preExtraction)
            }

            logConversation(
                rawText, sourceType, sourceSender,
                "$systemInstruction\n\n$userMessage", rawResponse,
                draft, success = true
            )
            Result.success(draft)

        } catch (e: TimeoutCancellationException) {
            logConversation(
                rawText, sourceType, sourceSender,
                "$systemInstruction\n\n$userMessage", rawResponse,
                null, success = false, error = "AI inference timed out"
            )
            Result.failure(AiUnavailableException("AI inference timed out"))

        } catch (e: CancellationException) {
            throw e  // never swallow coroutine cancellation

        } catch (e: Exception) {
            logConversation(
                rawText, sourceType, sourceSender,
                "$systemInstruction\n\n$userMessage", rawResponse,
                null, success = false, error = e.message ?: "Unknown error"
            )
            Result.failure(e)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun applyPreExtraction(draft: TransactionDraft, pre: PreExtraction?): TransactionDraft {
        if (pre == null) return draft
        return draft.copy(
            amount = pre.amount ?: draft.amount,
            typeId  = pre.typeId  ?: draft.typeId,
            date    = pre.epochMs  ?: draft.date,
        )
    }

    private suspend fun logConversation(
        rawText: String,
        sourceType: String,
        sourceSender: String?,
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
                rawText        = rawText,
                sourceType     = sourceType,
                sourceSender   = sourceSender,
                prompt         = prompt,
                response       = response,
                parsedDraftJson = draftJson,
                success        = success,
                errorMessage   = error,
            )
        )
    }
}
