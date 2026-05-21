package com.moneymanager.domain.ai

import com.moneymanager.app.ui.aidraft.AgentStatus
import com.moneymanager.app.ui.aidraft.AgentStep
import com.moneymanager.data.ai.DeterministicExtractor
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.data.repository.MerchantCategoryMemoryRepository
import com.moneymanager.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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
        
        // Phase 1: Deterministic Extraction
        emit(AgentStatus.Progress(AgentStep.EXTRACTING))
        val pre = DeterministicExtractor.extract(rawText, sourceType)
        
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
            onFailure = { emit(AgentStatus.Failure(it)) }
        )
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
