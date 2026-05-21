package com.moneymanager.app.ui.transactions

import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TagEntity

data class TransactionsUiState(
    val searchQuery: String = "",
    val filterType: String = "",
    val filterAccountId: Long? = null,
    val filterCategoryId: Long? = null,
    val filterTagId: Long? = null,
    val filterGoalId: Long? = null,
    val filterPeerId: Long? = null,
    val filterStartDate: Long? = null,
    val filterEndDate: Long? = null,
    val isAllExpanded: Boolean = true,
    val sortBy: TransactionSort = TransactionSort.NEWEST,
    val showSummary: Boolean = true,
    val showCategories: Boolean = true,
    val allTags: List<TagEntity> = emptyList(),
    val allCategories: List<CategoryEntity> = emptyList(),
    val allAccounts: List<AccountEntity> = emptyList(),
    val allGoals: List<GoalEntity> = emptyList(),
    val allPeers: List<PeerContact> = emptyList(),
    val currency: String = "INR",
    val categoryUsageCounts: Map<Long, Int> = emptyMap(),
    val imageAttachmentsEnabled: Boolean = true,
)
