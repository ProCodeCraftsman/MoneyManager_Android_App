package com.moneymanager.domain.sms

import com.moneymanager.domain.ai.AccountEntry

/**
 * What the user has taught the app about which of their accounts an SMS source belongs to, learned from the
 * account they chose when saving a draft. Keys come from [keyFor]; values are account ids.
 *
 * A link beats name matching because it is the user's own decision, and it works even when the account is
 * named something like "Salary" that shares no word with the bank's SMS.
 */
class SmsAccountLinks(private val links: Map<String, Long> = emptyMap()) {

    /** The linked account that is still active, or null when this source was never linked or is ambiguous. */
    fun resolve(hint: AccountHint?, accounts: List<AccountEntry>): AccountEntry? {
        val key = keyFor(hint) ?: return null
        val byId = accounts.associateBy { it.id }
        links[key]?.let { id -> byId[id]?.let { return it } }

        // A source whose digits were never seen can only borrow a link when the SMS prints no digits at all and
        // every link for that institution and instrument agrees; otherwise it may be a different card.
        if (hint?.last4 != null) return null
        val prefix = key.substringBeforeLast('|') + "|"
        val ids = links.filterKeys { it.startsWith(prefix) }.values.filter { it in byId }.toSet()
        return ids.singleOrNull()?.let { byId[it] }
    }

    companion object {
        val EMPTY = SmsAccountLinks()

        /** Stable identity of an SMS account source, or null when the SMS says too little to tell sources apart. */
        fun keyFor(hint: AccountHint?): String? {
            if (hint == null) return null
            val institution = hint.institution.orEmpty().lowercase().filter { it.isLetterOrDigit() }
            val last4 = hint.last4.orEmpty().filter { it.isLetterOrDigit() }
            if (institution.isEmpty() && last4.isEmpty()) return null
            return "$institution|${hint.instrument.name}|$last4"
        }
    }
}
