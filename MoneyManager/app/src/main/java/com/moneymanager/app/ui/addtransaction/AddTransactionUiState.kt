package com.moneymanager.app.ui.addtransaction

import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TagEntity

data class AddTransactionUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val peers: List<PeerContact> = emptyList(),
    val currency: String = "INR",
    val categoryUsageCounts: Map<Long, Int> = emptyMap(),
    val isLoading: Boolean = true,
    val imageAttachmentsEnabled: Boolean = true,
)
