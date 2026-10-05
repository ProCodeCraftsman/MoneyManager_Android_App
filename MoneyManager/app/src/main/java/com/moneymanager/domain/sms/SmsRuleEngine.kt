package com.moneymanager.domain.sms

/** Sender id helpers shared by the engine and the analyzer. */
object SmsSender {
    private val operatorPrefixed = Regex("""^[A-Z]{2}-([A-Z0-9]+)(?:-[A-Z0-9])?$""")

    /** `AD-HDFCBK-S`, `VM-SBIINB`, `JD-ICICIT-T` -> `HDFCBK`, `SBIINB`, `ICICIT`. */
    fun normalize(address: String): String {
        val a = address.trim().uppercase()
        return operatorPrefixed.matchEntire(a)?.groupValues?.get(1) ?: a
    }
}

/** Some senders print NBSP / thin / zero-width spaces, which `\s` does not match. */
object SmsText {
    private val exoticSpaces = Regex("[\u00A0\u1680\u2000-\u200B\u202F\u205F\u3000\uFEFF]")

    fun normalize(body: String): String = exoticSpaces.replace(body, " ")
}

/**
 * Deterministic SMS parser: sender-scoped rules first, then rules on wording alone, then the generic
 * extractor. Pure Kotlin, no Android dependencies, no I/O.
 */
object SmsRuleEngine {

    private val whitespace = Regex("""\s+""")
    private val vpaToken = Regex("""\S+@[A-Za-z0-9.]+""")
    private val upiPrefix = Regex("""^UPI[-/ ]\d+[-/ ]""", RegexOption.IGNORE_CASE)

    fun parse(message: SmsMessage, rules: List<SmsRule> = BuiltInSmsRules.all): ParsedSms {
        val body = SmsText.normalize(message.body).trim()
        val sender = message.sender?.takeIf { it.isNotBlank() }?.let(SmsSender::normalize)

        val ordered = rules.sortedByDescending { rule ->
            when {
                sender != null && sender in rule.senders -> 2
                rule.senders.isEmpty() -> 1
                else -> 0
            }
        }
        for (rule in ordered) {
            val match = rule.pattern.find(body) ?: continue
            return fromRule(rule, match, body, message.receivedAt)
        }
        return fromGeneric(body, message.receivedAt)
    }

    // ── Rule match ─────────────────────────────────────────────────────────────

    private fun fromRule(rule: SmsRule, m: MatchResult, body: String, receivedAt: Long?): ParsedSms {
        val currency = normalizeCurrency(m.named("cur"))
        val amount = m.named("amount")?.let(::parseAmount)
        val balance = (m.named("balance") ?: SmsFieldExtractor.balanceOf(body))?.let(::parseAmount)
        val acc = m.named("acc")
        val isTxn = rule.category == SmsCategory.TRANSACTION

        val flags = buildList {
            if (amount != null && currency != "INR") add("foreign_currency:$currency")
            if (m.named("info")?.contains("salary", ignoreCase = true) == true) add("salary")
        }

        return ParsedSms(
            category = rule.category,
            direction = rule.direction,
            amount = amount,
            currency = currency,
            balance = balance,
            accountHint = if (isTxn) AccountHint(rule.institution, rule.instrument, acc?.takeLast(4)) else null,
            counterparty = cleanCounterparty(m.named("merchant")) ?: rule.counterparty,
            reference = m.named("ref"),
            dateMillis = SmsDateParser.parse(m.named("date")) ?: receivedAt,
            nature = rule.nature,
            ruleId = rule.id,
            matched = true,
            flags = flags,
        )
    }

    // ── Generic fallback ───────────────────────────────────────────────────────

    private fun fromGeneric(body: String, receivedAt: Long?): ParsedSms {
        val g = SmsFieldExtractor.extract(body)
        val last4 = g.account?.filter { it.isDigit() }?.takeLast(4)?.ifEmpty { null }
        return ParsedSms(
            category = g.category,
            direction = g.direction,
            amount = g.amount?.let(::parseAmount),
            currency = "INR",
            balance = g.balance?.let(::parseAmount),
            accountHint = accountHintFor(body, last4),
            counterparty = cleanCounterparty(g.merchant) ?: cleanCounterparty(vpaInBody.find(body)?.value),
            reference = g.reference,
            dateMillis = SmsDateParser.parse(body) ?: receivedAt,
            nature = TxnNature.NORMAL,
            ruleId = null,
            matched = false,
        )
    }

    // ── Generic account hints ──────────────────────────────────────────────────

    /** Wallet / card brands that identify the account when a message has no account number. */
    private val brandInstruments: Map<String, Instrument> = mapOf(
        "paytm" to Instrument.WALLET, "simpl" to Instrument.WALLET, "lazypay" to Instrument.WALLET,
        "amazon pay" to Instrument.WALLET, "slice" to Instrument.CREDIT_CARD,
        "uni card" to Instrument.CREDIT_CARD, "one card" to Instrument.CREDIT_CARD, "niyo" to Instrument.BANK_ACCOUNT,
    )
    private val brandPattern = Regex(
        """(?i)\b(${brandInstruments.keys.joinToString("|") { Regex.escape(it) }})\b"""
    )
    private val vpaInBody = Regex("""[A-Za-z0-9._-]{3,}@[A-Za-z]{2,}""")

    private fun accountHintFor(body: String, last4: String?): AccountHint? {
        val brand = brandPattern.find(body)?.groupValues?.get(1)?.lowercase()
        return when {
            brand != null -> AccountHint(brand, brandInstruments.getValue(brand), last4)
            last4 != null -> AccountHint(null, Instrument.OTHER, last4)
            else -> null
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun MatchResult.named(name: String): String? =
        try {
            groups[name]?.value
        } catch (_: IllegalArgumentException) {
            null // the rule's regex does not define this group
        }

    private fun parseAmount(raw: String): Double? =
        raw.replace(",", "").toDoubleOrNull()?.takeIf { it > 0.0 && it < 100_000_000.0 }

    private fun normalizeCurrency(raw: String?): String = when (raw?.trim()?.uppercase()) {
        null, "", "RS", "RS.", "INR", "₹" -> "INR"
        else -> raw.trim().uppercase()
    }

    /** Strips UPI prefixes, VPAs, punctuation and trailing noise from a bank-printed merchant string. */
    internal fun cleanCounterparty(raw: String?): String? {
        if (raw == null) return null
        var s = whitespace.replace(raw, " ").trim().trim('.', ',', ';', ':', '-', ' ')
        s = upiPrefix.replace(s, "")
        if (vpaToken.matchEntire(s) != null) {
            s = s.substringBefore('@')
        } else {
            s = vpaToken.replace(s, "")
        }
        s = whitespace.replace(s, " ").trim().trim('.', ',', ';', ':', '-', ' ')
        return s.takeIf { it.length >= 2 && it.any(Char::isLetter) }?.take(60)
    }
}
