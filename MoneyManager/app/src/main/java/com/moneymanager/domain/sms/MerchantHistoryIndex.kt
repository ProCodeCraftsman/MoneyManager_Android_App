package com.moneymanager.domain.sms

/** One (description, type, category) group from the user's transaction history. */
data class HistoryRow(
    val description: String,
    val typeId: String,
    val categoryId: Long,
    val categoryName: String,
    val count: Int,
)

data class CategorySuggestion(val categoryId: Long, val categoryName: String, val typeId: String, val count: Int)

/**
 * Suggests a category for a bank-printed merchant from what the user already filed under similar
 * descriptions. Banks truncate merchant names (`KERALA VISI`), so a prefix match counts when both sides
 * are long enough to be meaningful. A suggestion needs a clear majority; ambiguity returns null.
 */
class MerchantHistoryIndex(rows: List<HistoryRow>) {

    private val byKey: Map<String, List<HistoryRow>> = rows
        .filter { it.typeId != "transfer" }
        .groupBy { normalize(it.description) }
        .filterKeys { it.length >= MIN_KEY }

    fun suggest(merchantHint: String, typeId: String?): CategorySuggestion? {
        val hint = normalize(merchantHint)
        if (hint.length < MIN_KEY) return null
        val candidates = byKey[hint]
            ?: byKey.filterKeys { k -> minOf(k.length, hint.length) >= MIN_PREFIX && (k.startsWith(hint) || hint.startsWith(k)) }
                .values.flatten().takeIf { it.isNotEmpty() }
            ?: return null

        val usable = candidates.filter { typeId == null || it.typeId == typeId }
        val total = usable.sumOf { it.count }
        if (total == 0) return null

        val best = usable.groupBy { it.categoryId }.maxByOrNull { (_, v) -> v.sumOf { it.count } }!!.value
        val bestCount = best.sumOf { it.count }
        if (bestCount.toDouble() / total < MIN_SHARE) return null
        val row = best.maxByOrNull { it.count }!!
        return CategorySuggestion(row.categoryId, row.categoryName, row.typeId, bestCount)
    }

    companion object {
        private const val MIN_KEY = 3
        private const val MIN_PREFIX = 5
        private const val MIN_SHARE = 0.6

        /** Same key as the merchant-category memory and the SMS join, so all three agree on a merchant. */
        fun normalize(raw: String): String = MerchantKey.canonical(raw)
    }
}
