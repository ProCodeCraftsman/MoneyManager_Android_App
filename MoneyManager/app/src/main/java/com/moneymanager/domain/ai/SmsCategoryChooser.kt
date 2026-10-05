package com.moneymanager.domain.ai

import com.moneymanager.domain.sms.SmsCategoryPrompt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Last resort for the category of an SMS draft, after the rules, merchant memory and the user's history have
 * had their say. Asks the on-device model a multiple-choice question over the user's own categories (see
 * [SmsCategoryPrompt]) and returns null on any doubt: no model, timeout, unclear reply, nothing to choose from.
 */
@Singleton
class SmsCategoryChooser @Inject constructor(private val client: GenAiClient) {

    suspend fun choose(draft: TransactionDraft, context: PromptContext): CategoryEntry? {
        val merchant = draft.merchantHint ?: return null
        val candidates = SmsCategoryPrompt.candidates(draft.typeId, context.top20Categories, context.allCategories)
        if (candidates.isEmpty()) return null
        val prompt = SmsCategoryPrompt.build(draft.typeId, merchant, draft.amount, candidates)
        return try {
            // The flat-prompt path attaches no tools: this is a plain question with a one-number answer.
            val reply = withTimeoutOrNull(TIMEOUT_MS) { client.generateDraft(prompt).getOrNull() } ?: return null
            SmsCategoryPrompt.parse(reply, candidates)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val TIMEOUT_MS = 20_000L
    }
}
