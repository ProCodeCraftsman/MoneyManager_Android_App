package com.moneymanager.app.ui.emi

import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.EmiEntity

data class EmiUiState(
    val emis: List<EmiEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
)
