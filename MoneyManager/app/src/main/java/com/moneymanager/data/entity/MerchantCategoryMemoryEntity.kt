package com.moneymanager.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_category_memory")
data class MerchantCategoryMemoryEntity(
    @PrimaryKey
    val merchantKey: String,     // normalized: lowercase alphanumeric, max 40 chars
    val categoryId: Long,
    val categoryName: String,    // denormalized — avoids join at lookup time
    val typeId: String? = null,  // "expense" / "income" / "transfer" — stored for fast-path draft
    val hitCount: Int = 1,
    val lastUsedAt: Long = System.currentTimeMillis(),
)
