package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: String, // bank, cash, credit, savings, investment, peer
    val initialBalance: Double = 0.0,
    val balance: Double = 0.0,
    val currency: String = "INR",
    val emoji: String = "🏦",
    val color: String = "#2a6049",
    val peerContactId: Long? = null, // Link to peer if type = "peer"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        val VALID_TYPES = listOf("bank", "cash", "credit", "savings", "investment", "peer")
    }
}