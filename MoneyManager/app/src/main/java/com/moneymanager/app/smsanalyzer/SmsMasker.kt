package com.moneymanager.app.smsanalyzer

import com.moneymanager.domain.sms.SmsCategory
import com.moneymanager.domain.sms.SmsFieldExtractor
import com.moneymanager.domain.sms.SmsSender
import com.moneymanager.domain.sms.SmsText

/**
 * Turns a raw SMS body into a structural template by replacing every variable part
 * (amounts, dates, account numbers, ids, merchants, names...) with a placeholder, so that all
 * messages sent from the same bank template collapse into one string.
 *
 * The masked output never contains amounts, account digits, ids, links or (best effort)
 * merchant/person names, which is what makes it safe to store and share.
 */
object SmsMasker {

    private val url = Regex("""(?i)\b(?:https?://|www\.)\S+|\b[a-z0-9-]+\.(?:com|in|co|me|ly|io|link)/\S*""")
    private val email = Regex("""[\w.+-]+@[\w-]+\.[\w.]+""")
    private val vpa = Regex("""(?i)\b[\w.\-]{2,}@[a-z]{2,}\b""")
    private val amount = Regex("""(?i)(?:rs\.?|inr|₹)\s*-?[\d,]+(?:\.\d+)?""")
    private val date = Regex(
        """(?i)\b\d{1,2}[-/. ](?:\d{1,2}|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[-/. ,]*\d{2,4}\b|\b\d{4}-\d{2}-\d{2}\b"""
    )
    private val time = Regex("""(?i)\b\d{1,2}:\d{2}(?::\d{2})?\s*(?:am|pm)?\b""")
    private val account = Regex("""(?i)(?:\b[x*]{1,}|\bxx)\s?\d{2,6}\b|\*{2,}\d{2,6}\b""")
    private val longNumber = Regex("""\b\d{6,}\b""")
    private val alnumId = Regex("""\b(?=[A-Za-z0-9]*\d)(?=[A-Za-z0-9]*[A-Za-z])[A-Za-z0-9]{8,}\b""")
    private val number = Regex("""\b\d+(?:[.,]\d+)*\b""")
    private val merchant = Regex(
        """(?i)\b(at|to|towards|from|by|info:?|trf to|vpa|merchant:?)\s+(?!<)([A-Za-z0-9&@._'\-/ ]{2,40}?)(?=\s+(?:on|via|ref|upi|using|avl|bal|available|for|is|has|dated|txn|if|call|not|\.|-)\b|[.,;]|\s*<|$)"""
    )
    private val greetingName = Regex(
        """(?i)\b(dear|hi|hello|hey|congrats)\s+(?!customer\b|user\b|sir\b|madam\b|valued\b|there\b)([A-Za-z][A-Za-z.]*(?:\s[A-Za-z.]+){0,3}?)(?=\s*[,!:]|\s+(?:your|you|we|this|welcome|here)\b)"""
    )
    private val repeatedName = Regex("""(?:<NAME>\s*){2,}""")
    private val whitespace = Regex("""\s+""")

    private val merchantStopWords = setOf("a/c", "ac", "acct", "account", "your", "card", "the", "you", "me", "us")

    /** [names]: words of the account holder's own name (case-insensitive) to mask wherever they appear. */
    fun mask(body: String, names: List<String> = emptyList()): String {
        var s = SmsText.normalize(body).trim()
        val words = names.flatMap { it.split(whitespace) }.filter { it.isNotBlank() }
        val longWords = words.filter { it.length >= 2 }.distinct()
        // Banks print the same name as "SWARAJ K S", "SWARAJKS" or "KS": mask the whole phrase with
        // optional gaps, then any run of 2+ trailing initials on its own. A title can be glued on ("MRSWARAJKS").
        val phrase = words.joinToString("""\s*""") { Regex.escape(it) }
        val initials = words.reversed().takeWhile { it.length == 1 }.reversed()
        val title = """(?:mrs?|ms|dr|shri|smt)?"""
        val patterns = buildList {
            if (words.size >= 2) add("""\b$title$phrase\b""")
            if (initials.size >= 2) add("""\b${initials.joinToString("""\s?""") { Regex.escape(it) }}\b""")
            if (longWords.isNotEmpty()) add("""\b$title(?:${longWords.joinToString("|") { Regex.escape(it) }})\b""")
        }
        for (p in patterns) s = Regex("(?i)$p").replace(s, "<NAME>")
        s = greetingName.replace(s) { "${it.groupValues[1]} <NAME>" }
        s = repeatedName.replace(s, "<NAME> ")
        // Merchant masking only makes sense for real transaction / reminder messages; in promos
        // "at <something>" is just prose and masking it fragments templates.
        val transactional = SmsFieldExtractor.extract(body).category.let {
            it == SmsCategory.TRANSACTION || it == SmsCategory.REMINDER
        }
        s = url.replace(s, "<URL>")
        s = email.replace(s, "<EMAIL>")
        s = vpa.replace(s, "<VPA>")
        s = amount.replace(s, "<AMT>")
        s = date.replace(s, "<DATE>")
        s = time.replace(s, "<TIME>")
        s = account.replace(s, "<ACC>")
        s = longNumber.replace(s, "<NUM>")
        s = alnumId.replace(s, "<ID>")
        s = merchant.replace(s) { m ->
            val name = m.groupValues[2].trim()
            val lower = name.lowercase()
            if (!transactional || name.isEmpty() || lower.split(' ').first() in merchantStopWords ||
                lower.contains("a/c") || lower.endsWith(" bank")
            ) {
                m.value
            } else {
                "${m.groupValues[1]} <MERCHANT>"
            }
        }
        s = number.replace(s, "<N>")
        return whitespace.replace(s, " ").trim()
    }

    /**
     * Value-free shapes of the variable tokens in [body] (digits become `9`, e.g. `9999-99-99`,
     * `Rs.99,999.99`, `XX9999`). Lets us see the real date/time/amount formats behind a template
     * without exposing any value.
     */
    fun shapes(body: String): List<String> =
        listOf(amount, date, time, account).flatMap { re -> re.findAll(body).map { shapeOf(it.value) }.toList() }

    private fun shapeOf(token: String): String = token.map { if (it.isDigit()) '9' else it }.joinToString("")

    /** `AD-HDFCBK-S`, `VM-SBIINB`, `JD-ICICIT-T` -> `HDFCBK`, `SBIINB`, `ICICIT`. */
    fun normalizeSender(address: String): String = SmsSender.normalize(address)

    /** Phone-number senders are personal chats and are skipped by the analyser. */
    fun isAlphanumericSender(address: String): Boolean = address.any { it.isLetter() }
}
