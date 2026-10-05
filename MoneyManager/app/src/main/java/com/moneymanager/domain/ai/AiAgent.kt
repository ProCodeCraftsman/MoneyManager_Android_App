package com.moneymanager.domain.ai

import com.moneymanager.app.ui.aidraft.AgentStatus
import com.moneymanager.app.ui.aidraft.AgentStep
import com.moneymanager.data.ai.DeterministicExtractor
import com.moneymanager.data.ai.PreExtraction
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.data.repository.SmsAccountLinkRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.domain.sms.Instrument
import com.moneymanager.domain.sms.NotATransactionException
import com.moneymanager.domain.sms.ParsedSms
import com.moneymanager.domain.sms.SmsAccountLinks
import com.moneymanager.domain.sms.SmsDraftMapper
import com.moneymanager.domain.sms.SmsMessage
import com.moneymanager.domain.sms.SmsRuleEngine
import com.moneymanager.domain.sms.SmsText
import com.moneymanager.domain.sms.SmsVerifier
import com.moneymanager.domain.sms.TxnDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAgent @Inject constructor(
    private val generateDraftFromText: GenerateDraftFromTextUseCase,
    private val generateDraftFromImage: GenerateDraftFromImageUseCase,
    private val merchantMemory: MerchantCategoryMemoryRepository,
    private val transactionRepository: TransactionRepository,
    private val accountLinks: SmsAccountLinkRepository,
    private val categoryChooser: SmsCategoryChooser,
) {
    fun processText(
        rawText: String,
        context: PromptContext,
        sourceType: String,
        sourceSender: String? = null,
        attachmentPath: String? = null,
        autoCommit: Boolean = false,
    ): Flow<AgentStatus> = flow {
        emit(AgentStatus.Progress(AgentStep.READING))

        // Phase 0 (SMS only): known bank/card/UPI templates are parsed by rules, no AI needed.
        var smsParsed: ParsedSms? = null
        var smsLinks = SmsAccountLinks.EMPTY
        if (sourceType == "SMS") {
            emit(AgentStatus.Progress(AgentStep.EXTRACTING, "Matching known SMS formats"))
            val raw = SmsRuleEngine.parse(SmsMessage(sourceSender, rawText))
            if (raw.isDefinitelyNotATransaction) {
                emit(AgentStatus.Failure(NotATransactionException(raw.category, raw.ruleId)))
                return@flow
            }
            // A rule that matched but captured something the text does not back up is not trusted as a draft.
            val parsed = SmsVerifier.apply(raw, SmsVerifier.verify(SmsText.normalize(rawText).trim(), raw))
            smsParsed = parsed
            smsLinks = withContext(Dispatchers.IO) { accountLinks.load() }
            if (parsed.matched && parsed.isActionableTransaction) {
                val ruleDraft = SmsDraftMapper.toDraft(parsed, context.accounts, sourceSender, attachmentPath, smsLinks)
                if (ruleDraft != null) {
                    emitOrCommit(withCategoryFromAi(withCategoryFromMemory(ruleDraft), context), autoCommit)
                    return@flow
                }
            }
        }

        // Phase 1: Deterministic Extraction
        emit(AgentStatus.Progress(AgentStep.EXTRACTING))
        val basePre = DeterministicExtractor.extract(rawText, sourceType)
        val pre = smsParsed?.let { mergeSmsHints(basePre, it) } ?: basePre

        // Phase 2: Merchant Cache
        pre.merchantHint?.let { hint ->
            emit(AgentStatus.Progress(AgentStep.SEARCHING_CACHE, hint))
            val cached = withContext(Dispatchers.IO) { merchantMemory.lookup(hint) }
            if (cached != null) {
                val fastDraft = TransactionDraft(
                    amount = pre.amount,
                    typeId = pre.typeId ?: cached.typeId,
                    date = pre.epochMs,
                    categoryId = cached.categoryId,
                    categoryName = cached.categoryName,
                    accountId = context.accounts.firstOrNull { it.type != "peer" }?.id, // Default account
                    merchantHint = hint,
                    sourceType = sourceType,
                    sourceSender = sourceSender,
                    receiptPath = attachmentPath,
                )
                
                if (autoCommit && fastDraft.isHighConfidence()) {
                    emit(AgentStatus.Progress(AgentStep.AUTO_COMMITTING))
                    val id = withContext(Dispatchers.IO) { 
                        transactionRepository.insertTransaction(fastDraft.toEntity()) 
                    }
                    delay(500) // Visibility
                    emit(AgentStatus.Success(fastDraft.copy(flags = listOf("auto_committed", id.toString()))))
                } else {
                    emit(AgentStatus.Success(fastDraft))
                }
                return@flow
            }
        }

        // Phase 3: AI Inference
        emit(AgentStatus.Progress(AgentStep.CLASSIFYING))
        val result = generateDraftFromText(rawText, context, sourceType, sourceSender, pre)
        
        result.fold(
            onSuccess = { draft ->
                val finalDraft = draft.copy(
                    receiptPath = attachmentPath ?: draft.receiptPath,
                    merchantHint = pre.merchantHint ?: draft.merchantHint,
                )
                
                // Learn merchant association
                if (pre.merchantHint != null && finalDraft.categoryId != null) {
                    withContext(Dispatchers.IO) {
                        merchantMemory.record(
                            merchantHint = pre.merchantHint,
                            categoryId = finalDraft.categoryId,
                            categoryName = finalDraft.categoryName ?: "",
                            typeId = finalDraft.typeId,
                        )
                    }
                }

                if (autoCommit && finalDraft.isHighConfidence()) {
                    emit(AgentStatus.Progress(AgentStep.AUTO_COMMITTING))
                    val id = withContext(Dispatchers.IO) { 
                        transactionRepository.insertTransaction(finalDraft.toEntity()) 
                    }
                    delay(500)
                    emit(AgentStatus.Success(finalDraft.copy(flags = listOf("auto_committed", id.toString()))))
                } else {
                    emit(AgentStatus.Success(finalDraft))
                }
            },
            onFailure = { error ->
                // AI unavailable or failed: a reviewable draft from the generic SMS parse beats nothing.
                val fallback = smsParsed
                    ?.takeIf { it.isActionableTransaction }
                    ?.let { SmsDraftMapper.toDraft(it, context.accounts, sourceSender, attachmentPath, smsLinks) }
                if (fallback != null) {
                    emit(AgentStatus.Success(withCategoryFromMemory(fallback.copy(flags = fallback.flags + "ai_fallback"))))
                } else {
                    emit(AgentStatus.Failure(error))
                }
            }
        )
    }

    // ── SMS helpers ───────────────────────────────────────────────────────────

    /** Rule-engine facts override the generic extractor; extra facts travel to the model as hints. */
    private fun mergeSmsHints(base: PreExtraction, parsed: ParsedSms): PreExtraction {
        val hints = buildList {
            parsed.accountHint?.let { hint ->
                hint.institution?.let { add("institution=$it") }
                if (hint.instrument != Instrument.OTHER) add("instrument=${hint.instrument.name.lowercase()}")
                hint.last4?.let { add("accountLast4=$it") }
            }
            parsed.reference?.let { add("reference=$it") }
            parsed.balance?.let { add("balanceAfter=$it") }
        }
        return base.copy(
            amount = parsed.amount ?: base.amount,
            typeId = parsed.direction?.let { if (it == TxnDirection.DEBIT) "expense" else "income" } ?: base.typeId,
            epochMs = parsed.dateMillis ?: base.epochMs,
            merchantHint = parsed.counterparty ?: base.merchantHint,
            hints = hints,
        )
    }

    /** Fills the category from the on-device merchant memory when the merchant has been seen before. */
    private suspend fun withCategoryFromMemory(draft: TransactionDraft): TransactionDraft {
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

    /**
     * When neither memory nor history named a category, lets the model choose among the user's own categories.
     * A model pick is a suggestion: the draft is flagged and kept for review, never auto-committed. Saving it
     * teaches the merchant memory, so the same merchant is deterministic next time.
     */
    private suspend fun withCategoryFromAi(draft: TransactionDraft, context: PromptContext): TransactionDraft {
        if (!SmsDraftMapper.needsCategory(draft.typeId) || draft.categoryId != null) return draft
        val picked = categoryChooser.choose(draft, context) ?: return draft
        return draft.copy(
            categoryId = picked.id,
            categoryName = picked.name,
            confidence = draft.confidence + ("categoryId" to "medium"),
            needsReview = true,
            flags = draft.flags + "ai_category",
        )
    }

    private suspend fun FlowCollector<AgentStatus>.emitOrCommit(draft: TransactionDraft, autoCommit: Boolean) {
        if (autoCommit && draft.isHighConfidence()) {
            emit(AgentStatus.Progress(AgentStep.AUTO_COMMITTING))
            val id = withContext(Dispatchers.IO) { transactionRepository.insertTransaction(draft.toEntity()) }
            emit(AgentStatus.Success(draft.copy(flags = listOf("auto_committed", id.toString()))))
        } else {
            emit(AgentStatus.Success(draft))
        }
    }

    private fun TransactionDraft.toEntity(): TransactionEntity {
        return TransactionEntity(
            accountId = accountId ?: 0,
            type = typeId ?: "expense",
            amount = amount ?: 0.0,
            categoryId = categoryId,
            peerContactId = peerContactId,
            tagIds = tagIds.joinToString(","),
            date = date ?: System.currentTimeMillis(),
            note = note ?: description ?: "",
            description = description ?: note ?: "",
            receiptPath = receiptPath,
            createdAt = System.currentTimeMillis()
        )
    }

    fun processImage(
        imageBytes: ByteArray,
        context: PromptContext,
        sourceType: String = "RECEIPT",
        attachmentPath: String? = null,
        autoCommit: Boolean = false,
    ): Flow<AgentStatus> = flow {
        emit(AgentStatus.Progress(AgentStep.READING, "Image data received"))
        
        emit(AgentStatus.Progress(AgentStep.CLASSIFYING, "AI vision reading receipt…"))
        val result = generateDraftFromImage(imageBytes, context, sourceType)
        
        result.fold(
            onSuccess = { draft ->
                val finalDraft = draft.copy(
                    receiptPath = attachmentPath ?: draft.receiptPath,
                    accountId = context.accounts.firstOrNull { it.type != "peer" }?.id // Default account
                )
                
                // Learn merchant association if confidence is high
                if (finalDraft.merchantHint != null && finalDraft.categoryId != null &&
                    finalDraft.confidence["merchant"] != "low") {
                    withContext(Dispatchers.IO) {
                        merchantMemory.record(
                            merchantHint = finalDraft.merchantHint,
                            categoryId = finalDraft.categoryId,
                            categoryName = finalDraft.categoryName ?: "",
                            typeId = finalDraft.typeId,
                        )
                    }
                }
                
                if (autoCommit && finalDraft.isHighConfidence()) {
                    emit(AgentStatus.Progress(AgentStep.AUTO_COMMITTING))
                    val id = withContext(Dispatchers.IO) { 
                        transactionRepository.insertTransaction(finalDraft.toEntity()) 
                    }
                    delay(500)
                    emit(AgentStatus.Success(finalDraft.copy(flags = listOf("auto_committed", id.toString()))))
                } else {
                    emit(AgentStatus.Success(finalDraft))
                }
            },
            onFailure = { emit(AgentStatus.Failure(it)) }
        )
    }
}
