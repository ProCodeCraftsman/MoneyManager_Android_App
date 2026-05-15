package com.moneymanager.app.ui.accounts

import com.moneymanager.app.ui.components.AccountBarData
import com.moneymanager.data.entity.AccountEntity

data class AccountsUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalAssets: Double = 0.0,
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
    val accountComparisonData: List<AccountBarData> = emptyList()
)
