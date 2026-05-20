package com.moneymanager.domain.ai

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDraft(
    val typeId: String? = null,
    val amount: Double? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val accountId: Long? = null,
    val accountName: String? = null,
    val peerContactId: Long? = null,
    val peerContactName: String? = null,
    val tagIds: List<Long> = emptyList(),
    val description: String? = null,
    val note: String? = null,
    val date: Long? = null,
    val sourceType: String? = null,
    val sourceSender: String? = null,
    val receiptPath: String? = null,
    val merchantHint: String? = null,
    // Vision / confidence metadata — populated by ask-image path, empty for text path
    val confidence: Map<String, String> = emptyMap(),
    val needsReview: Boolean = false,
    val flags: List<String> = emptyList(),
)
