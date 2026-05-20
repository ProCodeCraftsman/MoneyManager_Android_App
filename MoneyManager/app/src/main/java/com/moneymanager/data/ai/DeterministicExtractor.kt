package com.moneymanager.data.ai

import java.util.Calendar

/**
 * Fast, zero-AI extraction of the fields that don't require language understanding:
 * amount, transaction type, date, and merchant hint.
 *
 * Priority for amount (bills have many numbers):
 *   1. Labeled totals  ("NET PAYABLE", "GRAND TOTAL" …)   ← highest
 *   2. SMS debit/credit keyword pattern
 *   3. Last monetary value in bottom third of text (receipts)
 *   4. Largest monetary value found (fallback)
 *
 * Ignores numbers that appear near: Bill No, UPI Ref, GST No, phone, balance,
 * unit rate (@₹X/kg), minimum due — all common false-positive sources.
 */
object DeterministicExtractor {

    // ── Amount patterns ────────────────────────────────────────────────────────

    // SMS: "debited INR 185.30" / "paid Rs 450" / "transferred Rs.1500 to"
    private val smsDebitAmountPattern = Regex(
        """(?:debited?|deducted?|charged?|withdrawn?|paid|spent|transferred?|purchase[sd]?)\s+(?:with\s+)?(?:inr|rs\.?|₹|usd|\$)?\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    // SMS: "Rs.450.00 debited" (amount comes before keyword)
    private val smsReverseDebitPattern = Regex(
        """(?:inr|rs\.?|₹)\s*([\d,]+\.?\d*)\s+(?:debited?|deducted?|charged?)""",
        RegexOption.IGNORE_CASE
    )

    // SMS: "credited INR 5000" / "received Rs 2000"
    private val smsCreditAmountPattern = Regex(
        """(?:credited?|received?|deposited?|refunded?|cashback)\s+(?:with\s+)?(?:inr|rs\.?|₹|usd|\$)?\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    // SMS: "INR 5000 credited"
    private val smsReverseCreditPattern = Regex(
        """(?:inr|rs\.?|₹)\s*([\d,]+\.?\d*)\s+(?:credited?)""",
        RegexOption.IGNORE_CASE
    )

    // Receipts / any: labeled totals (highest confidence, wins over line items)
    private val labeledTotalPattern = Regex(
        """(?:net\s*payable|grand\s*total|total\s*amount|amount\s*due|net\s*amount|total\s*payable|net\s*total|payable\s*amount|bill\s*amount|invoice\s*total|amount\s*paid|total\s*bill|total\s*due|to\s*pay|total\s*payable)\s*[:\s=]*(?:inr|rs\.?|₹|usd|\$)?\s*([\d,]+\.?\d*)""",
        RegexOption.IGNORE_CASE
    )

    // Generic monetary value (currency symbol prefix or suffix)
    private val monetaryPattern = Regex(
        """(?:(?:inr|rs\.?|₹|usd|\$)\s*([\d,]+\.?\d*))|([\d,]+\.?\d*)\s*(?:rs\.?|inr|rupees?)""",
        RegexOption.IGNORE_CASE
    )

    // Lines containing these labels should NOT contribute an amount
    private val ignoreLinePattern = Regex(
        """(?:avail|avl\s*bal|avbl|closing\s*bal|min(?:imum)?\s*due|credit\s*limit|utr|ref\s*(?:no|:|\s*#)|txn\s*(?:id|ref|no)|bill\s*(?:no|num|#)|phone|mob|gst\s*no|invoice\s*no|order\s*(?:no|id|#)|@\s*(?:rs|inr|₹)|\bper\b|\bqty\b|\bquantity\b|\beach\b|unit\s*(?:price|rate|cost)|balance)""",
        RegexOption.IGNORE_CASE
    )

    // Numbers that look like phone/reference numbers (7+ consecutive digits)
    private val refNumberPattern = Regex("""\b\d{7,}\b""")

    // ── Type keywords ──────────────────────────────────────────────────────────

    private val debitKeywords = Regex(
        """(?:\bdebited?\b|\bdeducted?\b|\bcharged?\b|\bwithdrawn?\b|\bpurchase[sd]?\b|\bpaid\b|\bspent\b|\bexpense\b|\btransferred?\b)""",
        RegexOption.IGNORE_CASE
    )

    private val creditKeywords = Regex(
        """(?:\bcredited?\b|\breceived?\b|\bdeposited?\b|\brefunded?\b|\bcashback\b|\breversal\b|\bincome\b|\bsalary\b|\bpayment\s+received\b)""",
        RegexOption.IGNORE_CASE
    )

    // ── Date patterns ──────────────────────────────────────────────────────────

    private val ddMmYyyyPattern = Regex("""(\d{1,2})[/-](\d{1,2})[/-](\d{2,4})""")

    private val monthNamePattern = Regex(
        """(\d{1,2})\s*(?:st|nd|rd|th)?\s+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)\w*\s*,?\s*(\d{4})?""",
        RegexOption.IGNORE_CASE
    )

    private val monthNames = mapOf(
        "jan" to 0, "feb" to 1, "mar" to 2, "apr" to 3,
        "may" to 4, "jun" to 5, "jul" to 6, "aug" to 7,
        "sep" to 8, "oct" to 9, "nov" to 10, "dec" to 11
    )

    // ── Merchant patterns ──────────────────────────────────────────────────────

    private val smsMerchantPattern = Regex(
        """(?:towards\s+(?:payment\s+)?at|payment\s+to|paid\s+to|purchase\s+at|at\s+merchant|at|to)\s+([A-Z][A-Za-z0-9\s&._\-]{2,35})(?=\s*(?:\.|,|on\b|for\b|via\b|using\b|ref\b|utr\b|$))""",
        RegexOption.IGNORE_CASE
    )

    // ── Public API ─────────────────────────────────────────────────────────────

    fun extract(rawText: String, sourceType: String): PreExtraction {
        val lower = rawText.lowercase()
        return PreExtraction(
            amount = extractAmount(rawText, lower, sourceType),
            typeId = extractType(lower),
            epochMs = extractDate(rawText, lower),
            merchantHint = extractMerchant(rawText, sourceType),
        )
    }

    // ── Amount extraction ──────────────────────────────────────────────────────

    private fun extractAmount(raw: String, lower: String, sourceType: String): Double? {
        // 1. Labeled total — most reliable for any source type
        labeledTotalPattern.find(lower)?.let { m ->
            parseAmount(m.groupValues[1])?.let { return it }
        }

        // 2. SMS: keyword-based patterns (forward and reverse)
        if (sourceType == "SMS" || sourceType == "VOICE") {
            smsDebitAmountPattern.find(lower)?.let { m ->
                parseAmount(m.groupValues[1])?.let { return it }
            }
            smsReverseDebitPattern.find(lower)?.let { m ->
                parseAmount(m.groupValues[1])?.let { return it }
            }
            smsCreditAmountPattern.find(lower)?.let { m ->
                parseAmount(m.groupValues[1])?.let { return it }
            }
            smsReverseCreditPattern.find(lower)?.let { m ->
                parseAmount(m.groupValues[1])?.let { return it }
            }
        }

        // 3. Fallback: collect monetary amounts from non-ignored lines
        val lines = raw.split("\n")
        val candidates = mutableListOf<Pair<Double, Int>>() // (amount, lineIndex)

        for ((index, line) in lines.withIndex()) {
            if (ignoreLinePattern.containsMatchIn(line)) continue
            if (refNumberPattern.containsMatchIn(line.replace(Regex("""[₹$,.]"""), ""))) {
                // line has a long reference number — skip monetary values on this line
                // unless they match a currency symbol directly
                val hasExplicitCurrency = line.contains(Regex("""[₹$]|(?:inr|rs\.?)\s*\d""", RegexOption.IGNORE_CASE))
                if (!hasExplicitCurrency) continue
            }
            monetaryPattern.findAll(line).forEach { m ->
                val raw = m.groupValues[1].ifEmpty { m.groupValues[2] }
                parseAmount(raw)?.let { candidates.add(it to index) }
            }
        }

        if (candidates.isEmpty()) return null

        // For receipts: prefer the last prominent amount (totals appear near bottom)
        if (sourceType == "RECEIPT" && candidates.size > 1) {
            val cutoff = (lines.size * 2) / 3
            val bottomCandidates = candidates.filter { it.second >= cutoff }
            if (bottomCandidates.isNotEmpty()) {
                return bottomCandidates.maxByOrNull { it.first }?.first
            }
        }

        // Final fallback: largest amount (totals are usually bigger than line items)
        return candidates.maxByOrNull { it.first }?.first
    }

    // ── Type extraction ────────────────────────────────────────────────────────

    private fun extractType(lower: String): String? {
        val hasDebit = debitKeywords.containsMatchIn(lower)
        val hasCredit = creditKeywords.containsMatchIn(lower)
        return when {
            hasDebit && !hasCredit -> "expense"
            hasCredit && !hasDebit -> "income"
            // Both present (e.g. "debited … refund credited") → let AI decide
            else -> null
        }
    }

    // ── Date extraction ────────────────────────────────────────────────────────

    private fun extractDate(raw: String, lower: String): Long? {
        val cal = Calendar.getInstance()

        if (lower.contains("yesterday")) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }

        // DD/MM/YY or DD-MM-YYYY
        ddMmYyyyPattern.find(raw)?.let { m ->
            runCatching {
                val day = m.groupValues[1].toInt()
                val month = m.groupValues[2].toInt()
                val yearRaw = m.groupValues[3]
                val year = if (yearRaw.length == 2) 2000 + yearRaw.toInt() else yearRaw.toInt()
                if (day in 1..31 && month in 1..12 && year in 2000..2100) {
                    cal.set(year, month - 1, day, 0, 0, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    return cal.timeInMillis
                }
            }
        }

        // "15 May 2025" / "15th May"
        monthNamePattern.find(raw)?.let { m ->
            runCatching {
                val day = m.groupValues[1].toInt()
                val month = monthNames[m.groupValues[2].lowercase().take(3)] ?: return@runCatching
                val yearStr = m.groupValues[3]
                val year = if (yearStr.isNotBlank()) yearStr.toInt() else cal.get(Calendar.YEAR)
                cal.set(year, month, day, 0, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        }

        return null
    }

    // ── Merchant extraction ────────────────────────────────────────────────────

    private fun extractMerchant(raw: String, sourceType: String): String? {
        return when (sourceType) {
            "SMS" -> {
                smsMerchantPattern.find(raw)?.groupValues?.getOrNull(1)
                    ?.trim()
                    ?.filterMerchantName()
            }
            "RECEIPT" -> {
                raw.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { line ->
                        line.length in 3..50
                            && line.any { it.isLetter() }
                            && !refNumberPattern.containsMatchIn(line)
                            && !line.contains(Regex("""[:/\\]"""))
                            && !ignoreLinePattern.containsMatchIn(line)
                    }
                    ?.filterMerchantName()
            }
            "VOICE" -> {
                Regex("""(?:to|at|for|from)\s+([A-Za-z][A-Za-z\s]{2,25})""", RegexOption.IGNORE_CASE)
                    .find(raw)?.groupValues?.getOrNull(1)?.trim()
                    ?.filterMerchantName()
            }
            else -> null
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun parseAmount(raw: String): Double? =
        raw.replace(",", "").toDoubleOrNull()
            ?.takeIf { it > 0.0 && it < 10_000_000.0 }

    private fun String.filterMerchantName(): String? =
        trim().takeIf { it.length >= 3 && it.any { c -> c.isLetter() } }
}
