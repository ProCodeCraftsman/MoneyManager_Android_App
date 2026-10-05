package com.moneymanager.domain.sms

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

/** Parses the date formats Indian banks print in SMS. Returns the start of that day, or null. */
object SmsDateParser {

    private val candidate = Regex(
        """\b\d{1,2}[-/. ](?:\d{1,2}|[A-Za-z]{3})[-/. ,]*\d{2,4}\b|\b\d{1,2}[A-Za-z]{3}\d{2,4}\b|\b[A-Za-z]{3,9}\s\d{1,2},\s\d{4}\b|\b\d{4}-\d{2}-\d{2}\b"""
    )

    private val formatters: List<DateTimeFormatter> = listOf(
        "d-M-yy", "d-M-yyyy", "d/M/yy", "d/M/yyyy", "d.M.yy", "d.M.yyyy",
        "d-MMM-yy", "d-MMM-yyyy", "d/MMM/yy", "d/MMM/yyyy", "d MMM yyyy", "d MMM yy",
        "dMMMyy", "dMMMyyyy", "yyyy-MM-dd", "MMMM d, yyyy", "MMM d, yyyy",
    ).map {
        DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(it).toFormatter(Locale.ENGLISH)
    }

    /** Finds the first date-looking token in [text] and parses it. */
    fun parse(text: String?, zone: ZoneId = ZoneId.systemDefault()): Long? {
        if (text.isNullOrBlank()) return null
        for (m in candidate.findAll(text)) {
            val token = m.value.trim().trimEnd('.', ',', ';', ':')
            // "05 Oct 03:22 PM" has no year: "03" is the hour, not 2003.
            val yearIsHour = token.any { it.isLetter() } && token.contains(' ') &&
                Regex(""":\d\d""").matchesAt(text, m.range.last + 1)
            if (yearIsHour) continue
            for (f in formatters) {
                val d = runCatching { LocalDate.parse(token, f) }.getOrNull() ?: continue
                if (d.year in 2000..2100) return d.atStartOfDay(zone).toInstant().toEpochMilli()
            }
        }
        return null
    }
}
