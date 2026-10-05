package com.moneymanager.app.smsanalyzer

import com.moneymanager.domain.sms.ParseStatus
import com.moneymanager.domain.sms.SmsCategory
import com.moneymanager.domain.sms.SmsMessage
import com.moneymanager.domain.sms.SmsRuleEngine
import com.moneymanager.domain.sms.SmsSender
import com.moneymanager.domain.sms.SmsText
import com.moneymanager.domain.sms.SmsVerifier
import org.json.JSONArray
import org.json.JSONObject

data class TemplateInsight(
    val sender: String,
    val template: String,
    val count: Int,
    val category: SmsCategory,
    val status: ParseStatus,
    /** field name -> number of messages in this template where the parser found it. */
    val fieldHits: Map<String, Int>,
    /** Rule that recognised this template, or null when only the generic fallback did. */
    val ruleId: String?,
    /** Messages in this template recognised by a built-in rule. */
    val ruleMatched: Int,
    val firstSeen: Long,
    val lastSeen: Long,
    /** Distinct value-free token shapes (dates, times, amounts, accounts) seen; only kept for gaps. */
    val shapes: Set<String> = emptySet(),
    /** Draft outcome counters for rule-matched transactions; null when no accounts were supplied. */
    val quality: DraftQuality? = null,
    /** [SmsIssue] name -> messages whose parse the text did not back up; counts only. */
    val verifyIssues: Map<String, Int> = emptyMap(),
)

data class AnalysisReport(
    val scanned: Int,
    val skippedPersonal: Int,
    val templates: List<TemplateInsight>,
) {
    private val txn get() = templates.filter { it.category == SmsCategory.TRANSACTION }
    val transactionMessages: Int get() = txn.sumOf { it.count }
    val fullyParsedMessages: Int get() = txn.filter { it.status == ParseStatus.FULL }.sumOf { it.count }
    val ruleMatchedTransactionMessages: Int get() = txn.sumOf { it.ruleMatched }
    val draftQuality: DraftQuality? get() = txn.mapNotNull { it.quality }.takeIf { it.isNotEmpty() }?.reduce(DraftQuality::plus)
    val gapTemplates: List<TemplateInsight> get() = txn.filter { it.status != ParseStatus.FULL || it.ruleId == null }
    val verifyIssues: Map<String, Int>
        get() = txn.flatMap { it.verifyIssues.entries }.groupBy({ it.key }, { it.value }).mapValues { it.value.sum() }

    fun toJson(generatedAt: Long = System.currentTimeMillis()): String {
        val root = JSONObject()
        root.put("version", 4)
        root.put("generatedAt", generatedAt)
        root.put("scanned", scanned)
        root.put("skippedPersonal", skippedPersonal)
        root.put("transactionMessages", transactionMessages)
        root.put("fullyParsedMessages", fullyParsedMessages)
        root.put("ruleMatchedTransactionMessages", ruleMatchedTransactionMessages)
        draftQuality?.let { root.put("draftSummary", it.toJson()) }
        root.put("verifyIssues", JSONObject(verifyIssues as Map<*, *>))
        val arr = JSONArray()
        templates.forEach { t ->
            arr.put(
                JSONObject()
                    .put("sender", t.sender)
                    .put("template", t.template)
                    .put("count", t.count)
                    .put("category", t.category.name)
                    .put("status", t.status.name)
                    .put("fieldHits", JSONObject(t.fieldHits as Map<*, *>))
                    .put("ruleId", t.ruleId ?: JSONObject.NULL)
                    .put("ruleMatched", t.ruleMatched)
                    .put("firstSeen", t.firstSeen)
                    .put("lastSeen", t.lastSeen)
                    .put("shapes", JSONArray(t.shapes.toList()))
                    .put("verifyIssues", JSONObject(t.verifyIssues as Map<*, *>))
                    .also { o -> t.quality?.let { o.put("draftQuality", it.toJson()) } }
            )
        }
        root.put("templates", arr)
        return root.toString(2)
    }
}

/**
 * Feeds SMS one at a time through the real [SmsRuleEngine] and groups them by (sender, masked template).
 * Raw bodies are never retained: only the masked template and counters.
 */
class SmsAnalyzer(
    private val names: List<String> = emptyList(),
    private val draftContext: DraftContext? = null,
) {

    private class Bucket(
        val sender: String,
        val template: String,
        var category: SmsCategory,
        var firstSeen: Long,
        var lastSeen: Long,
    ) {
        var count = 0
        var fullCount = 0
        var ruleMatched = 0
        val ruleIds = HashMap<String, Int>()
        val fieldHits = HashMap<String, Int>()
        val shapes = LinkedHashSet<String>()
        var quality = DraftQuality()
        val verifyIssues = HashMap<String, Int>()
    }

    private val buckets = HashMap<String, Bucket>()
    private var scanned = 0
    private var skipped = 0

    fun add(address: String, body: String, dateMillis: Long) {
        scanned++
        if (!SmsMasker.isAlphanumericSender(address) || body.isBlank()) {
            skipped++
            return
        }
        val sender = SmsSender.normalize(address)
        val template = SmsMasker.mask(body, names)
        val raw = SmsRuleEngine.parse(SmsMessage(address, body, dateMillis))
        // Same trust check the app applies before building a draft, so the scan shows what it would reject.
        val issues = SmsVerifier.verify(SmsText.normalize(body).trim(), raw, dateMillis)
        val parsed = SmsVerifier.apply(raw, issues)
        val bucket = buckets.getOrPut("$sender|$template") {
            Bucket(sender, template, parsed.category, dateMillis, dateMillis)
        }
        bucket.count++
        issues.forEach { bucket.verifyIssues.merge(it.name, 1, Int::plus) }
        if (parsed.status == ParseStatus.FULL) bucket.fullCount++
        if (parsed.matched) {
            bucket.ruleMatched++
            parsed.ruleId?.let { bucket.ruleIds.merge(it, 1, Int::plus) }
        }
        // A template is a transaction template if any message in it parsed as one.
        if (parsed.category == SmsCategory.TRANSACTION) bucket.category = SmsCategory.TRANSACTION
        if (draftContext != null && parsed.matched && parsed.isActionableTransaction) {
            DraftQuality.evaluate(parsed, address, draftContext)?.let { bucket.quality = bucket.quality.add(it) }
        }
        if (!parsed.matched && bucket.shapes.size < MAX_SHAPES) {
            SmsMasker.shapes(body).forEach { if (bucket.shapes.size < MAX_SHAPES) bucket.shapes.add(it) }
        }
        bucket.firstSeen = minOf(bucket.firstSeen, dateMillis)
        bucket.lastSeen = maxOf(bucket.lastSeen, dateMillis)
        buildList {
            if (parsed.amount != null) add("amount")
            if (parsed.direction != null) add("direction")
            if (parsed.accountHint?.last4 != null) add("account")
            if (parsed.balance != null) add("balance")
            if (parsed.counterparty != null) add("merchant")
            if (parsed.reference != null) add("reference")
        }.forEach { bucket.fieldHits.merge(it, 1, Int::plus) }
    }

    fun report(): AnalysisReport {
        val templates = buckets.values.map { b ->
            val status = when {
                b.category != SmsCategory.TRANSACTION -> ParseStatus.NOT_TRANSACTION
                b.fullCount == b.count -> ParseStatus.FULL
                else -> ParseStatus.PARTIAL
            }
            TemplateInsight(
                b.sender, b.template, b.count, b.category, status,
                b.fieldHits.toSortedMap(), b.ruleIds.maxByOrNull { it.value }?.key, b.ruleMatched,
                b.firstSeen, b.lastSeen, b.shapes,
                b.quality.takeIf { draftContext != null && it.drafts > 0 },
                b.verifyIssues.toSortedMap(),
            )
        }.sortedByDescending { it.count }
        return AnalysisReport(scanned, skipped, templates)
    }

    private companion object {
        const val MAX_SHAPES = 12
    }
}

private fun DraftQuality.toJson() = JSONObject()
    .put("drafts", drafts).put("ready", ready).put("accountHinted", accountHinted)
    .put("accountMatched", accountMatched).put("categoryNeeded", categoryNeeded)
    .put("categoryFilled", categoryFilled).put("transfers", transfers)
