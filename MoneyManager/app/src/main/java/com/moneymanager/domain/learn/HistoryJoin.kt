package com.moneymanager.domain.learn

import com.moneymanager.domain.sms.MerchantHistoryIndex

/** One verified SMS spend. Built from a rule match: merchant string, amount, type, date and source key only. */
data class SmsSpend(
    val date: Long,
    val amount: Double,
    val typeId: String,
    val merchantHint: String?,
    val sourceKey: String?,
)

/** The category the user picked for an SMS merchant, [count] of the [total] kept pairs for that merchant. */
data class MerchantLabel(
    val merchantKey: String,
    val merchantHint: String,
    val typeId: String,
    val categoryId: Long,
    val categoryName: String,
    val count: Int,
    val total: Int,
) {
    /** Every kept pair agrees; the only case the confirm screen pre-ticks. */
    val unanimous: Boolean get() = count == total
}

/** The account an SMS source is filed under, from kept pairs on non-cash accounts. */
data class SourceLabel(
    val sourceKey: String,
    val accountId: Long,
    val accountName: String,
    val count: Int,
    val total: Int,
) {
    /** Every kept pair agrees; the only case the confirm screen pre-ticks. */
    val unanimous: Boolean get() = count == total
}

/** Share of past non-cash transactions in the window that had exactly one matching SMS. */
data class JoinCoverage(val matched: Int, val total: Int)

data class HistoryLabels(
    val merchants: List<MerchantLabel>,
    val sources: List<SourceLabel>,
    val coverage: JoinCoverage,
)

/**
 * Joins SMS spends to saved transactions to learn merchant categories and source accounts without a model
 * (F08 L1). A pair is kept only when it is unambiguous on both sides: one saved candidate for the spend, and that
 * candidate matches no other spend. Repeat purchases (daily tea) therefore drop out instead of teaching a guess.
 */
object HistoryJoin {
    const val DAY_MS = 24 * 60 * 60 * 1000L
    const val MIN_COUNT = 2
    const val MIN_SHARE = 0.6

    /** Types that carry a category; a transfer's account is not a merchant's home account. */
    private val CATEGORY_TYPES = setOf("expense", "income", "savings")
    private val COVERAGE_TYPES = setOf("expense", "income", "savings", "transfer")

    /** Account type the app uses for cash wallets; a cash row says nothing about where an SMS came from. */
    private const val CASH = "cash"

    fun join(spends: List<SmsSpend>, saved: List<SavedRow>): HistoryLabels {
        val candidates = spends.map { spend -> spend to saved.filter { compatible(spend, it) } }

        val claims = HashMap<Long, Int>()
        candidates.forEach { (_, options) -> options.singleOrNull()?.let { claims.merge(it.id, 1, Int::plus) } }
        val pairs = candidates.mapNotNull { (spend, options) ->
            val row = options.singleOrNull() ?: return@mapNotNull null
            if (claims[row.id] == 1) spend to row else null
        }

        val merchants = pairs
            .filter { (spend, row) -> spend.typeId in CATEGORY_TYPES && row.categoryId != null && spend.merchantKey().isNotEmpty() }
            .groupBy { (spend, _) -> spend.merchantKey() }
            .mapNotNull { (key, group) ->
                val byCategory = group.groupBy { (_, row) -> row.categoryId!! }
                val winner = byCategory.values.maxBy { it.size }
                val (spend, row) = winner.first()
                val label = MerchantLabel(
                    merchantKey = key,
                    merchantHint = spend.merchantHint.orEmpty(),
                    typeId = spend.typeId,
                    categoryId = row.categoryId!!,
                    categoryName = row.categoryName.orEmpty(),
                    count = winner.size,
                    total = group.size,
                )
                label.takeIf { isConvincing(it.count, it.total) }
            }
            .sortedByDescending { it.count }

        val sources = pairs
            .filter { (spend, row) -> spend.typeId in CATEGORY_TYPES && row.accountType != CASH && spend.sourceKey != null }
            .groupBy { (spend, _) -> spend.sourceKey!! }
            .mapNotNull { (key, group) ->
                val byAccount = group.groupBy { (_, row) -> row.accountId }
                val winner = byAccount.values.maxBy { it.size }
                val row = winner.first().second
                SourceLabel(key, row.accountId, row.accountName, winner.size, group.size)
                    .takeIf { isConvincing(it.count, it.total) }
            }
            .sortedByDescending { it.count }

        val nonCash = saved.filter { it.accountType != CASH && it.typeId in COVERAGE_TYPES }
        val matchedIds = pairs.map { it.second.id }.toSet()
        val coverage = JoinCoverage(matched = nonCash.count { it.id in matchedIds }, total = nonCash.size)

        return HistoryLabels(merchants, sources, coverage)
    }

    /** Same kind of money, same amount to the paisa, within a day. The account is deliberately not compared. */
    private fun compatible(spend: SmsSpend, row: SavedRow): Boolean =
        row.typeId == spend.typeId &&
            kotlin.math.abs(row.amount - spend.amount) < 0.005 &&
            kotlin.math.abs(row.date - spend.date) <= DAY_MS

    private fun isConvincing(count: Int, total: Int): Boolean =
        count >= MIN_COUNT && total > 0 && count.toDouble() / total >= MIN_SHARE

    private fun SmsSpend.merchantKey(): String = MerchantHistoryIndex.normalize(merchantHint.orEmpty())
}

