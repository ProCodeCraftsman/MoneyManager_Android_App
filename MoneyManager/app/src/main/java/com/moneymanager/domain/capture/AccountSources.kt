package com.moneymanager.domain.capture

import com.moneymanager.domain.sms.Instrument

/** One SMS account source (institution + instrument + last4) that has drafts waiting, and what it is filed under. */
data class AccountSourceSummary(
    val key: String,
    val label: String,
    val suggestedAccountType: String,
    val draftCount: Int,
    val mappedAccountId: Long?,
)

/**
 * Groups pending drafts by the SMS source they came from (AI-055). Keys are the ones [com.moneymanager.domain.sms.SmsAccountLinks.keyFor]
 * writes: "institution|INSTRUMENT|last4". Drafts with no source key cannot be mapped and are left out.
 */
object AccountSources {

    fun summarise(pending: List<PendingCapture>, links: Map<String, Long>): List<AccountSourceSummary> =
        pending
            .mapNotNull { it.draft.accountLink }
            .groupingBy { it }
            .eachCount()
            .map { (key, count) ->
                AccountSourceSummary(
                    key = key,
                    label = label(key),
                    suggestedAccountType = suggestedAccountType(key),
                    draftCount = count,
                    mappedAccountId = links[key],
                )
            }
            .sortedByDescending { it.draftCount }

    /** "Hdfc · credit card ••1234". The institution is the lowercase SMS name, so it is only capitalised for display. */
    fun label(key: String): String {
        val (institution, instrument, last4) = split(key)
        val name = institution.replaceFirstChar { it.uppercase() }.ifEmpty { "Unknown bank" }
        val kind = instrumentWords(instrument)
        val tail = if (last4.isNotEmpty()) " ••$last4" else ""
        return "$name · $kind$tail"
    }

    /** A card source suggests a credit account; everything else suggests a bank account. */
    fun suggestedAccountType(key: String): String =
        if (split(key).second == Instrument.CREDIT_CARD.name) "credit" else "bank"

    private fun split(key: String): Triple<String, String, String> {
        val parts = key.split("|")
        return Triple(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "" }, parts.getOrElse(2) { "" })
    }

    private fun instrumentWords(instrument: String): String = when (instrument) {
        Instrument.CREDIT_CARD.name -> "credit card"
        Instrument.BANK_ACCOUNT.name -> "bank account"
        Instrument.WALLET.name -> "wallet"
        Instrument.MUTUAL_FUND.name -> "mutual fund"
        else -> "account"
    }
}
