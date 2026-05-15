package com.moneymanager.app.ui.recurring

import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.RecurringEntity
import java.text.SimpleDateFormat
import java.util.*

data class RecurringUiState(
    val recurringList: List<RecurringEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
    val currentDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
)
