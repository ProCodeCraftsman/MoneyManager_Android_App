package com.moneymanager.domain.ai

import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.data.repository.SmsAccountLinkRepository
import com.moneymanager.domain.sms.NotATransactionException
import com.moneymanager.domain.sms.ParsedSms
import com.moneymanager.domain.sms.SmsAccountLinks
import com.moneymanager.domain.sms.SmsDraftMapper
import com.moneymanager.domain.sms.SmsMessage
import com.moneymanager.domain.sms.SmsRuleEngine
import com.moneymanager.domain.sms.SmsText
import com.moneymanager.domain.sms.SmsVerifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** What the model-free SMS stages decided for one message. */
sealed interface SmsRuleOutcome {
    /** OTP, reminder, statement and similar: no transaction, no model call. */
    data class NotATransaction(val exception: NotATransactionException) : SmsRuleOutcome

    /** A verified, actionable rule match. Merchant memory has been applied; the category may still be missing. */
    data class Draft(val draft: TransactionDraft, val reference: String? = null) : SmsRuleOutcome

    /**
     * No rule matched, or the match was not trusted. [parsed] is the generic parse, kept as hints for the
     * extractor and model; [links] are the learned account links already loaded for this message.
     */
    data class Unmatched(val parsed: ParsedSms, val links: SmsAccountLinks) : SmsRuleOutcome
}

/**
 * The SMS stages that need neither a model nor the UI: rule match, verification, learned account links and
 * merchant memory. The draft screen flow (AiAgent) and the background inbox scan both call this, so the same
 * message gets the same verdict wherever it is read. Model-based steps (category pick, text fallback) stay in
 * AiAgent and are not part of the scan.
 */
@Singleton
class SmsDraftPipeline @Inject constructor(
    private val merchantMemory: MerchantCategoryMemoryRepository,
    private val accountLinks: SmsAccountLinkRepository,
) {

    /** Takes the account list, not a full [PromptContext]: building that context loads every transaction. */
    suspend fun fromRules(
        rawText: String,
        sourceSender: String?,
        attachmentPath: String?,
        accounts: List<AccountEntry>,
    ): SmsRuleOutcome {
        val raw = SmsRuleEngine.parse(SmsMessage(sourceSender, rawText))
        if (raw.isDefinitelyNotATransaction) {
            return SmsRuleOutcome.NotATransaction(NotATransactionException(raw.category, raw.ruleId))
        }
        // A rule that matched but captured something the text does not back up is not trusted as a draft.
        val parsed = SmsVerifier.apply(raw, SmsVerifier.verify(SmsText.normalize(rawText).trim(), raw))
        val links = withContext(Dispatchers.IO) { accountLinks.load() }
        if (parsed.matched && parsed.isActionableTransaction) {
            val ruleDraft = SmsDraftMapper.toDraft(parsed, accounts, sourceSender, attachmentPath, links)
            if (ruleDraft != null) return SmsRuleOutcome.Draft(withCategoryFromMemory(ruleDraft), parsed.reference)
        }
        return SmsRuleOutcome.Unmatched(parsed, links)
    }

    /** Fills the category from the on-device merchant memory when the merchant has been seen before. */
    suspend fun withCategoryFromMemory(draft: TransactionDraft): TransactionDraft {
        val needsCategory = SmsDraftMapper.needsCategory(draft.typeId)
        val hint = draft.merchantHint
        val cached = if (needsCategory && hint != null && draft.categoryId == null) {
            withContext(Dispatchers.IO) { merchantMemory.lookupWithHistory(hint, draft.typeId) }
                ?.takeIf { SmsDraftMapper.memoryApplies(draft.typeId, it.typeId) }
        } else null
        val withCategory = if (cached != null) {
            draft.copy(categoryId = cached.categoryId, categoryName = cached.categoryName)
        } else draft
        return if (needsCategory && withCategory.categoryId == null) withCategory.copy(needsReview = true) else withCategory
    }
}
