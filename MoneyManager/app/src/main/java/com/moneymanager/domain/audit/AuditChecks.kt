package com.moneymanager.domain.audit

import com.moneymanager.domain.capture.CaptureDedup
import kotlin.math.abs
import kotlin.math.ceil

/**
 * Deterministic second look at recently saved transactions (F08 A1–A8). Pure functions over [AuditTxn] inputs:
 * the same inputs always give the same findings, and nothing here changes data. The model is not used.
 */
object AuditChecks {
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val MIN_CATEGORY_SAMPLE = 3
    private const val MIN_CATEGORY_SHARE = 0.8
    private const val MIN_MERCHANT_SAMPLE = 5
    private const val MIN_CATEGORY_P95_SAMPLE = 10
    private const val REPEAT_DAYS = 3
    private const val BALANCE_TOLERANCE = 1.0
    private const val LEFTOVER_AFTER_MS = 3 * DAY_MS
    private const val SMS_DATE_TOLERANCE_MS = 2 * DAY_MS
    private const val RECENT_MS = 30 * DAY_MS

    /** Category-bearing types. Transfers, lend and borrow carry no category. */
    private val CATEGORY_TYPES = setOf("income", "expense", "savings")

    /**
     * [recent] is what the worker looks at (rows created in the last 30 days); [history] is every saved row, used for
     * the merchant and category baselines. [recent] is expected to be a subset of [history].
     */
    fun run(
        recent: List<AuditTxn>,
        history: List<AuditTxn>,
        memory: Map<String, MemoryHint>,
        balances: List<BalanceObservation>,
        now: Long,
    ): List<AuditFinding> {
        val byMerchant = history.filter { it.merchantKey != null }.groupBy { it.merchantKey!! }
        val byCategory = history.filter { it.categoryId != null }.groupBy { it.categoryId!! }
        val findings = mutableListOf<AuditFinding>()

        recent.forEach { t ->
            val merchantRows = t.merchantKey?.let { byMerchant[it] }.orEmpty()
            val categoryRows = t.categoryId?.let { byCategory[it] }.orEmpty()
            checkCategoryConflict(t, merchantRows, memory)?.let(findings::add)
            checkAmountSlip(t, merchantRows, categoryRows)?.let(findings::add)
            checkTypeMismatch(t)?.let(findings::add)
            checkAccountMismatch(t, now)?.let(findings::add)
            checkDuplicate(t, merchantRows)?.let(findings::add)
            checkDate(t, now)?.let(findings::add)
            checkLeftover(t, now)?.let(findings::add)
        }
        balances.forEach { checkBalanceDrift(it, history)?.let(findings::add) }
        return findings
    }

    /** A1: the merchant is filed under one category most of the time, and this row is under another. */
    private fun checkCategoryConflict(
        t: AuditTxn,
        merchantRows: List<AuditTxn>,
        memory: Map<String, MemoryHint>,
    ): AuditFinding? {
        val key = t.merchantKey ?: return null
        val current = t.categoryId ?: return null
        val counts = HashMap<Long, Int>()
        merchantRows.filter { it.id != t.id && it.type == t.type && it.categoryId != null }
            .forEach { counts.merge(it.categoryId!!, 1, Int::plus) }
        memory[key]?.let { counts.merge(it.categoryId, it.hitCount, Int::plus) }
        val total = counts.values.sum()
        val (best, bestCount) = counts.maxByOrNull { it.value } ?: return null
        if (best == current || bestCount < MIN_CATEGORY_SAMPLE) return null
        if (bestCount.toDouble() / total < MIN_CATEGORY_SHARE) return null
        return AuditFinding(
            check = AuditCheck.A1_CATEGORY,
            fingerprint = "A1|$key|$current",
            score = bestCount.toDouble() / total,
            txnId = t.id,
            suggestedCategoryId = best,
        )
    }

    /**
     * A2: the amount is 10× or 1/10 of the merchant's median (5+ earlier rows), or at least 5× the 95th percentile of
     * its category (10+ rows). The category check is per transaction, not per month, which keeps it simple and
     * still catches a typed extra zero.
     */
    private fun checkAmountSlip(t: AuditTxn, merchantRows: List<AuditTxn>, categoryRows: List<AuditTxn>): AuditFinding? {
        val sameMerchant = merchantRows.filter { it.id != t.id && it.type == t.type }.map { it.amount }.sorted()
        if (sameMerchant.size >= MIN_MERCHANT_SAMPLE) {
            val median = median(sameMerchant)
            if (median > 0 && (t.amount >= 10 * median || t.amount <= median / 10)) {
                return amountFinding(t, median, 0.9)
            }
        }
        val categoryAmounts = categoryRows.filter { it.id != t.id }.map { it.amount }.sorted()
        if (categoryAmounts.size >= MIN_CATEGORY_P95_SAMPLE) {
            val p95 = categoryAmounts[ceil(0.95 * categoryAmounts.size).toInt() - 1]
            if (p95 > 0 && t.amount >= 5 * p95) return amountFinding(t, p95, 0.6)
        }
        return null
    }

    private fun amountFinding(t: AuditTxn, expected: Double, score: Double) = AuditFinding(
        check = AuditCheck.A2_AMOUNT,
        fingerprint = "A2|${t.id}",
        score = score,
        txnId = t.id,
        expected = expected,
    )

    /** A3: an income category on an expense, a transfer to its own account, or a savings row with nowhere to go. */
    private fun checkTypeMismatch(t: AuditTxn): AuditFinding? {
        val categoryMismatch = t.type in CATEGORY_TYPES && t.categoryType != null && t.categoryType != t.type
        val selfTransfer = t.type == "transfer" && t.toAccountId == t.accountId
        val orphanSavings = t.type == "savings" && t.toAccountId == null && t.goalId == null
        if (!categoryMismatch && !selfTransfer && !orphanSavings) return null
        return AuditFinding(AuditCheck.A3_TYPE, "A3|${t.id}", 1.0, txnId = t.id)
    }

    /**
     * A4: the SMS it came from is linked to another account, or a recent row sits on an archived account.
     * A linked account is only a suggestion; the user decides.
     */
    private fun checkAccountMismatch(t: AuditTxn, now: Long): AuditFinding? {
        val linked = t.sourceAccountId
        if (linked != null && linked != t.accountId) {
            return AuditFinding(
                AuditCheck.A4_ACCOUNT, "A4|${t.id}", 0.9, txnId = t.id, suggestedAccountId = linked,
            )
        }
        if (t.accountArchived && now - t.createdAt <= RECENT_MS) {
            return AuditFinding(AuditCheck.A4_ACCOUNT, "A4|${t.id}", 0.7, txnId = t.id)
        }
        return null
    }

    /**
     * A5: the same merchant, amount and type within the duplicate window (F06). Reported once per pair, on the row
     * saved later. A merchant that repeats the same amount on 3+ separate days is a habit, not a duplicate.
     */
    private fun checkDuplicate(t: AuditTxn, merchantRows: List<AuditTxn>): AuditFinding? {
        if (t.merchantKey == null) return null
        val pairs = merchantRows.filter {
            it.id != t.id && it.type == t.type && abs(it.amount - t.amount) < 0.005 &&
                abs(it.date - t.date) <= CaptureDedup.WINDOW_MS
        }
        val earlier = pairs.firstOrNull { it.createdAt < t.createdAt || (it.createdAt == t.createdAt && it.id < t.id) }
            ?: return null
        val repeatDays = merchantRows
            .filter { it.type == t.type && abs(it.amount - t.amount) < 0.005 }
            .map { it.date / DAY_MS }
            .distinct()
            .size
        if (repeatDays >= REPEAT_DAYS) return null
        return AuditFinding(
            check = AuditCheck.A5_DUPLICATE,
            fingerprint = "A5|${t.id}",
            score = 0.8,
            txnId = t.id,
            otherTxnId = earlier.id,
        )
    }

    /** A7: dated in the future, or more than 2 days away from the SMS it came from. */
    private fun checkDate(t: AuditTxn, now: Long): AuditFinding? {
        val future = t.date > now + DAY_MS
        val farFromSms = t.smsDate != null && abs(t.date - t.smsDate) > SMS_DATE_TOLERANCE_MS
        if (!future && !farFromSms) return null
        return AuditFinding(AuditCheck.A7_DATE, "A7|${t.id}", 0.8, txnId = t.id)
    }

    /** A8: a category-bearing row still has no category after 3 days. */
    private fun checkLeftover(t: AuditTxn, now: Long): AuditFinding? {
        if (t.type !in CATEGORY_TYPES || t.categoryId != null) return null
        if (now - t.createdAt < LEFTOVER_AFTER_MS) return null
        return AuditFinding(AuditCheck.A8_LEFTOVER, "A8|${t.id}", 0.4, txnId = t.id)
    }

    /**
     * A6: the latest SMS balance differs from the app balance. Only compared when the SMS is not older than the
     * account's newest row, because an older SMS is expected to differ by whatever was spent since.
     */
    private fun checkBalanceDrift(obs: BalanceObservation, history: List<AuditTxn>): AuditFinding? {
        val lastRowDate = history.filter { it.accountId == obs.accountId }.maxOfOrNull { it.date } ?: 0L
        if (obs.observedAt < lastRowDate) return null
        if (abs(obs.smsBalance - obs.appBalance) < BALANCE_TOLERANCE) return null
        return AuditFinding(
            check = AuditCheck.A6_BALANCE,
            fingerprint = "A6|${obs.accountId}|${obs.smsBalance}",
            score = 0.7,
            accountId = obs.accountId,
            expected = obs.smsBalance,
        )
    }

    private fun median(sorted: List<Double>): Double {
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}

/** Picks what the inbox shows: highest score first, one finding per transaction, dismissed fingerprints removed. */
object AuditRanker {
    const val DAILY_CAP = 3

    fun top(findings: List<AuditFinding>, dismissed: Set<String>, limit: Int = DAILY_CAP): List<AuditFinding> =
        findings
            .filter { it.fingerprint !in dismissed }
            .sortedByDescending { it.score }
            .distinctBy { it.txnId?.let { id -> "t$id" } ?: "a${it.accountId}" }
            .take(limit)
}
