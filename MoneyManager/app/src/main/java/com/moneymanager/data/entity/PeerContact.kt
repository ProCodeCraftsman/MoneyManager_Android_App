package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peer_contacts")
data class PeerContact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val lookupKey: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val description: String = "",
    val photoUri: String? = null,
    val totalGiven: Double = 0.0,      // Cumulative amount lent to this peer
    val totalReceived: Double = 0.0,   // Cumulative amount received from this peer
    val contactDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val outstandingBalance: Double
        get() = totalGiven - totalReceived

    val effectiveDisplayName: String
        get() = if (contactDeleted) "Deleted Contact" else displayName
}