package com.moneymanager.domain.sms

import com.moneymanager.domain.ai.AccountEntry
import com.moneymanager.domain.ai.TransactionDraft

/** Turns a [ParsedSms] into a reviewable [TransactionDraft]. Category is resolved later by the caller. */
object SmsDraftMapper {

    /** Types that carry a category; transfers do not. */
    fun needsCategory(typeId: String?): Boolean = typeId == "expense" || typeId == "income" || typeId == "savings"

    /** A remembered merchant category only applies when it was learned for the same kind of transaction. */
    fun memoryApplies(draftTypeId: String?, memoryTypeId: String?): Boolean =
        memoryTypeId == null || memoryTypeId == draftTypeId

    /**
     * Account for a draft that did not come from [toDraft] (merchant cache, model). A learned link or a clear name
     * match from the SMS beats whatever default or model guess is already there; no match leaves the draft alone.
     * The link key is always attached so saving the draft still teaches the app.
     */
    fun withAccountFromSms(
        draft: TransactionDraft,
        parsed: ParsedSms?,
        accounts: List<AccountEntry>,
        links: SmsAccountLinks,
    ): TransactionDraft {
        val hint = parsed?.accountHint ?: return draft
        val withKey = draft.copy(accountLink = SmsAccountLinks.keyFor(hint))
        if (draft.typeId == "transfer") return withKey
        val account = AccountMatcher.match(hint, accounts, links) ?: return withKey
        return withKey.copy(
            accountId = account.id,
            accountName = account.name,
            confidence = draft.confidence + ("accountId" to "high"),
        )
    }

    fun toDraft(
        parsed: ParsedSms,
        accounts: List<AccountEntry>,
        sourceSender: String?,
        receiptPath: String? = null,
        links: SmsAccountLinks = SmsAccountLinks.EMPTY,
    ): TransactionDraft? {
        val amount = parsed.amount ?: return null
        val direction = parsed.direction ?: return null

        val account = AccountMatcher.match(parsed.accountHint, accounts, links)
        val isTransfer = parsed.nature == TxnNature.CARD_REPAYMENT

        val typeId = when {
            isTransfer -> "transfer"
            parsed.nature == TxnNature.INVESTMENT -> "savings"
            direction == TxnDirection.DEBIT -> "expense"
            else -> "income"
        }

        val flags = buildList {
            addAll(parsed.flags)
            add(if (parsed.matched) "sms_rule:${parsed.ruleId}" else "sms_generic")
            when (parsed.nature) {
                TxnNature.REFUND -> add("refund")
                TxnNature.CASHBACK -> add("cashback")
                TxnNature.CARD_REPAYMENT -> add("card_repayment")
                TxnNature.INVESTMENT -> add("investment")
                TxnNature.NORMAL -> Unit
            }
        }

        val description = parsed.counterparty ?: when (parsed.nature) {
            TxnNature.CARD_REPAYMENT -> "Credit card payment"
            TxnNature.REFUND -> "Refund"
            TxnNature.CASHBACK -> "Cashback"
            TxnNature.INVESTMENT -> "Investment"
            TxnNature.NORMAL -> null
        }

        val foreign = parsed.flags.any { it.startsWith("foreign_currency") }
        val unverified = parsed.flags.any { it.startsWith("verify:") }
        val needsReview = !parsed.matched || foreign || account == null || unverified ||
            parsed.nature == TxnNature.CARD_REPAYMENT || parsed.nature == TxnNature.INVESTMENT

        return TransactionDraft(
            typeId = typeId,
            amount = amount,
            accountId = if (isTransfer) null else account?.id,
            accountName = if (isTransfer) null else account?.name,
            toAccountId = if (isTransfer) account?.id else null,
            toAccountName = if (isTransfer) account?.name else null,
            description = description,
            date = parsed.dateMillis,
            sourceType = "SMS",
            sourceSender = sourceSender,
            receiptPath = receiptPath,
            merchantHint = parsed.counterparty,
            investmentPlatform = if (parsed.nature == TxnNature.INVESTMENT) parsed.counterparty else null,
            confidence = mapOf(
                "amount" to "high",
                "typeId" to when {
                    SmsIssue.DIRECTION_CONFLICT.flag in parsed.flags -> "low"
                    parsed.matched -> "high"
                    else -> "medium"
                },
                "accountId" to if (account != null) "high" else "low",
            ),
            needsReview = needsReview,
            flags = flags,
            accountLink = SmsAccountLinks.keyFor(parsed.accountHint),
        )
    }
}
