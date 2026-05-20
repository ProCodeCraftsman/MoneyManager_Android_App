package com.moneymanager.data.ai

import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.ai.CategoryEntry
import com.moneymanager.domain.ai.PeerEntry
import com.moneymanager.domain.ai.PromptContext
import com.moneymanager.domain.ai.TransactionDraft
import kotlinx.serialization.json.Json

object DraftParser {

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(raw: String, context: PromptContext? = null): Result<TransactionDraft> {
        return try {
            val stripped = raw
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .trim()
                .removeSuffix("```")
                .trim()

            val jsonStart = stripped.indexOf('{')
            if (jsonStart == -1) {
                return Result.failure(IllegalArgumentException("No JSON object found in AI response"))
            }

            // Use last '}' if present; otherwise attempt repair by appending missing closing braces
            val jsonEnd = stripped.lastIndexOf('}')
            val candidate = if (jsonEnd != -1) {
                stripped.substring(jsonStart, jsonEnd + 1)
            } else {
                repairJson(stripped.substring(jsonStart))
            }

            // First attempt: parse as-is
            val draft = runCatching { json.decodeFromString<TransactionDraft>(candidate) }
                .recoverCatching {
                    // Second attempt: close any unclosed brackets (model cut off mid-output)
                    json.decodeFromString<TransactionDraft>(repairJson(candidate))
                }
                .getOrThrow()

            val resolved = resolveNameToId(draft, context)
            Result.success(resolved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Closes unclosed '{' brackets by appending the right number of '}' characters.
     * Handles the common failure mode where the model is cut off at maxNumTokens mid-JSON.
     */
    private fun repairJson(broken: String): String {
        val opens = broken.count { it == '{' } - broken.count { it == '}' }
        return if (opens > 0) broken + "}".repeat(opens) else broken
    }

    private fun resolveNameToId(draft: TransactionDraft, context: PromptContext?): TransactionDraft {
        if (context == null) return draft

        val normalizedTypeId = draft.typeId?.lowercase()

        val candidateCategories = context.allCategories.ifEmpty { context.top20Categories }
        val categoryId = draft.categoryId ?: resolveCategoryName(draft.categoryName, candidateCategories)

        val accountId = draft.accountId ?: context.accounts
            .firstOrNull { it.name.equals(draft.accountName, ignoreCase = true) }?.id
        val peerContactId = draft.peerContactId ?: context.peers
            .firstOrNull { it.name.equals(draft.peerContactName, ignoreCase = true) }?.id

        return draft.copy(
            typeId = normalizedTypeId,
            categoryId = categoryId,
            accountId = accountId,
            peerContactId = peerContactId,
        )
    }

    private fun resolveCategoryName(name: String?, candidates: List<CategoryEntry>): Long? {
        if (name.isNullOrBlank()) return null
        val trimmed = name.trim()
        // 1. Exact match
        candidates.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }?.id?.let { return it }
        // 2. Model may have returned "Parent > Child" format — try the child part
        val childPart = trimmed.substringAfterLast(">").trim()
        if (childPart.isNotEmpty() && childPart != trimmed) {
            candidates.firstOrNull { it.name.equals(childPart, ignoreCase = true) }?.id?.let { return it }
        }
        // 3. Contains match — prefer subcategories (parentId != null) over parents
        val containsMatches = candidates.filter { it.name.contains(trimmed, ignoreCase = true) || trimmed.contains(it.name, ignoreCase = true) }
        return containsMatches.firstOrNull { it.parentId != null }?.id ?: containsMatches.firstOrNull()?.id
    }
}
