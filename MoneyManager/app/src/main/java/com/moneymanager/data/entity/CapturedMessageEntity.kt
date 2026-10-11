package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per captured source message (SMS today). It is the idempotency ledger for automatic capture and also
 * holds the pending draft until the user decides. The message text itself is never stored, only hashes of it,
 * so a rescan cannot recreate a draft and a rejected message stays rejected.
 */
@Entity(
    tableName = "captured_messages",
    indices = [
        Index(value = ["messageHash"], unique = true),
        Index(value = ["refHash"]),
        Index(value = ["status"]),
    ],
)
data class CapturedMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageHash: String,            // hash of sender + body + received time; the idempotency key
    val refHash: String? = null,        // hash of the bank/UPI reference number, the strongest duplicate key
    val amount: Double? = null,
    val messageDate: Long,              // when the message was received
    val sourceKey: String? = null,      // SmsAccountLinks.keyFor
    val status: String = STATUS_PENDING,
    val transactionId: Long? = null,    // set when status == saved
    val mergedIntoId: Long? = null,     // set when status == merged: the ledger row that kept the draft
    val draftJson: String? = null,      // serialized TransactionDraft while pending, cleared once decided
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val STATUS_PENDING = "pending"
        const val STATUS_SAVED = "saved"
        const val STATUS_REJECTED = "rejected"
        const val STATUS_MERGED = "merged"
        val VALID_STATUSES = listOf(STATUS_PENDING, STATUS_SAVED, STATUS_REJECTED, STATUS_MERGED)
    }
}
