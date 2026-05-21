package com.moneymanager.domain.ai

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
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
) : Parcelable {
    fun isHighConfidence(): Boolean {
        if (needsReview) return false
        if (amount == null || amount <= 0) return false
        if (typeId.isNullOrBlank()) return false
        if (categoryId == null) return false
        if (accountId == null) return false
        
        // If confidence metadata exists, check critical fields
        if (confidence.isNotEmpty()) {
            if (confidence["amount"] == "low") return false
            if (confidence["typeId"] == "low") return false
            if (confidence["categoryId"] == "low") return false
        }
        
        return true
    }
}
