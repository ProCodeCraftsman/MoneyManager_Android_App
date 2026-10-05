package com.moneymanager.domain.sms

/**
 * One rule per SMS template family.
 *
 * [regex] is matched with `find` (case-insensitive) against the whole message and may define the named
 * groups `cur`, `amount`, `acc`, `merchant`, `balance`, `ref`, `date`, `info`; any group may be absent.
 * [senders] are normalised sender ids (`HDFCBK`); the engine tries rules for the message's sender first,
 * then the rest, so an unknown or mistyped sender still matches on wording alone.
 *
 * A literal space in [regex] matches any run of whitespace and `.` also crosses line breaks, because the
 * analyzer's templates collapse whitespace but real messages break lines in the middle of phrases.
 */
class SmsRule(
    val id: String,
    val senders: Set<String>,
    regex: String,
    val category: SmsCategory,
    val direction: TxnDirection? = null,
    val instrument: Instrument = Instrument.OTHER,
    val institution: String? = null,
    val nature: TxnNature = TxnNature.NORMAL,
    /** Fixed counterparty for templates that do not print one (e.g. "Airtel"). */
    val counterparty: String? = null,
) {
    val pattern: Regex = Regex(flexibleSpaces(regex), setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    init {
        if (category == SmsCategory.TRANSACTION) {
            require(direction != null) { "Transaction rule $id needs a direction" }
        }
    }

    companion object {
        /** Outside character classes, a literal space becomes `\s+` (or `\s` when a quantifier follows it). */
        internal fun flexibleSpaces(regex: String): String {
            val out = StringBuilder()
            var inClass = false
            var i = 0
            while (i < regex.length) {
                val c = regex[i]
                when {
                    c == '\\' && i + 1 < regex.length -> {
                        out.append(c).append(regex[i + 1])
                        i++
                    }
                    c == '[' -> { inClass = true; out.append(c) }
                    c == ']' -> { inClass = false; out.append(c) }
                    c == ' ' && !inClass -> {
                        val next = regex.getOrNull(i + 1)
                        out.append(if (next == '?' || next == '*' || next == '+' || next == '{') """\s""" else """\s+""")
                    }
                    else -> out.append(c)
                }
                i++
            }
            return out.toString()
        }
    }
}
