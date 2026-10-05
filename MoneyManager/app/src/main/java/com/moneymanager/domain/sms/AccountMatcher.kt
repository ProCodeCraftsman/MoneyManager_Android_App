package com.moneymanager.domain.sms

import com.moneymanager.domain.ai.AccountEntry

/**
 * Maps the institution / instrument / trailing digits printed in an SMS onto one of the user's accounts.
 * Returns null unless exactly one account is the clear best match; guessing the wrong account is worse
 * than asking the user.
 */
object AccountMatcher {

    private val noiseWords = setOf(
        "bank", "card", "credit", "debit", "account", "savings", "limited", "ltd", "pvt", "the", "a", "c", "ac",
    )
    private val splitter = Regex("""[^a-z0-9]+""")

    /** A link the user taught the app wins over guessing from names. */
    fun match(hint: AccountHint?, accounts: List<AccountEntry>, links: SmsAccountLinks = SmsAccountLinks.EMPTY): AccountEntry? {
        if (hint == null) return null
        links.resolve(hint, accounts)?.let { return it }
        val wanted = tokens(hint.institution)
        val scored = accounts
            .filter { it.type != "peer" && accepts(hint.instrument, it.type) }
            .map { it to score(it, wanted, hint.last4) }
            .filter { it.second > 0 }
        val best = scored.maxOfOrNull { it.second } ?: return null
        return scored.filter { it.second == best }.singleOrNull()?.first
    }

    private fun accepts(instrument: Instrument, accountType: String): Boolean = when (instrument) {
        Instrument.CREDIT_CARD -> accountType == "credit"
        Instrument.BANK_ACCOUNT -> accountType == "bank" || accountType == "savings"
        Instrument.WALLET -> accountType == "bank" || accountType == "cash" || accountType == "savings"
        Instrument.MUTUAL_FUND, Instrument.OTHER -> true
    }

    private fun score(account: AccountEntry, wanted: Set<String>, last4: String?): Int {
        val name = account.name.lowercase()
        val overlap = tokens(name).count { it in wanted }
        val digits = if (last4 != null && last4.length >= 3 && name.contains(last4)) 3 else 0
        return overlap * 2 + digits
    }

    private fun tokens(text: String?): Set<String> =
        text.orEmpty().lowercase().split(splitter).filter { it.isNotEmpty() && it !in noiseWords }.toSet()
}
