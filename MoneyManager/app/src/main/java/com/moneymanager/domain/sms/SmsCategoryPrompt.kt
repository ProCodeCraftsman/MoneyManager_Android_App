package com.moneymanager.domain.sms

import com.moneymanager.domain.ai.CategoryEntry

/**
 * The closed-set question put to the on-device model when memory and history could not name a category:
 * "which of the user's own categories fits?". The model only ever picks a number from a short list the app
 * built, so it cannot invent a category, and a small model handles a multiple-choice answer far better than
 * free-form JSON. Pure Kotlin so the prompt and the answer handling are unit-tested without a model.
 */
object SmsCategoryPrompt {

    const val MAX_CANDIDATES = 15

    /** The user's categories for this kind of transaction, most used first. Empty when none exist for the type. */
    fun candidates(
        typeId: String?,
        mostUsed: List<CategoryEntry>,
        all: List<CategoryEntry>,
        limit: Int = MAX_CANDIDATES,
    ): List<CategoryEntry> {
        if (typeId == null) return emptyList()
        val ofType = { c: CategoryEntry -> c.type.equals(typeId, ignoreCase = true) }
        return (mostUsed.filter(ofType) + all.filter(ofType)).distinctBy { it.id }.take(limit)
    }

    /** Carries only the merchant and amount, never the SMS text, so account numbers and balances stay out. */
    fun build(typeId: String?, merchant: String, amount: Double?, candidates: List<CategoryEntry>): String = buildString {
        appendLine("Pick the category that best fits this transaction from the numbered list.")
        appendLine("Reply with the number and the category name, for example: 3. Travel")
        appendLine("Reply 0 if none fits. Do not explain.")
        appendLine()
        append("Transaction: ").append(typeId ?: "expense")
        if (amount != null) append(" of ").append(amount.toBigDecimal().stripTrailingZeros().toPlainString())
        appendLine(" at \"$merchant\"")
        appendLine("Categories:")
        candidates.forEachIndexed { i, c -> appendLine("${i + 1}. ${c.name}") }
        append("Answer:")
    }

    private val leadingNumber = Regex("""^\W*(\d{1,2})\b(.*)$""", setOf(RegexOption.DOT_MATCHES_ALL))

    /**
     * The chosen category, or null when the reply is not a clean pick: it must start with a number in range,
     * and a category name that follows must be that number's category, so a slipped index is not trusted.
     */
    fun parse(reply: String, candidates: List<CategoryEntry>): CategoryEntry? {
        val m = leadingNumber.find(reply.trim()) ?: return null
        val index = m.groupValues[1].toInt()
        if (index < 1 || index > candidates.size) return null
        val chosen = candidates[index - 1]
        val named = m.groupValues[2].lowercase()
        val otherNamed = candidates.any { it.id != chosen.id && it.name.length >= 3 && named.contains(it.name.lowercase()) }
        val chosenNamed = named.contains(chosen.name.lowercase())
        return if (otherNamed && !chosenNamed) null else chosen
    }
}
