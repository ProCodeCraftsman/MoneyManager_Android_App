package com.moneymanager.data.ai

import com.moneymanager.domain.ai.CategoryEntry

/**
 * Narrows the full category list to a focused set of candidates before AI classification.
 *
 * Strategy (in order):
 * 1. Type filter — keep only expense/income/transfer categories that match the pre-extracted typeId.
 *    A correctly typed pool of 10 beats an untyped pool of 20.
 * 2. Word-overlap scoring — score each remaining category by how many of its name tokens appear
 *    in merchant + raw text. "Swiggy" → "food", "dining" → Food/Dining categories score higher.
 * 3. Fallback — if scoring yields < 3 matches, return the top-[MAX] from the typed pool
 *    (which is already sorted by usage frequency in PromptContextBuilder).
 */
object CategoryPreFilter {

    private const val MAX_CANDIDATES = 7

    fun narrow(
        merchantHint: String?,
        rawText: String,
        typeId: String?,
        allCategories: List<CategoryEntry>,
    ): List<CategoryEntry> {
        if (allCategories.size <= MAX_CANDIDATES) return allCategories

        // Step 1: type filter
        val pool = if (typeId != null) {
            allCategories.filter { it.type.equals(typeId, ignoreCase = true) }
                .takeIf { it.size >= 3 } ?: allCategories
        } else allCategories

        if (pool.size <= MAX_CANDIDATES) return pool

        // Step 2: word-overlap scoring against merchant + truncated raw text
        val searchText = "${merchantHint ?: ""} ${rawText.take(400)}".lowercase()

        data class Scored(val entry: CategoryEntry, val score: Int)

        val scored = pool.map { cat ->
            val nameTokens = cat.name.lowercase().split(Regex("\\W+")).filter { it.length > 2 }
            val score = nameTokens.count { token -> searchText.contains(token) }
            Scored(cat, score)
        }

        val topMatches = scored.filter { it.score > 0 }
            .sortedByDescending { it.score }
            .map { it.entry }

        return if (topMatches.size >= 3) topMatches.take(MAX_CANDIDATES)
        else pool.take(MAX_CANDIDATES)
    }
}
