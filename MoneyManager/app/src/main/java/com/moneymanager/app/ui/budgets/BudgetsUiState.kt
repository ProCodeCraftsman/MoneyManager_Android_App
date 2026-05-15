package com.moneymanager.app.ui.budgets

import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
import java.text.SimpleDateFormat
import java.util.*

data class BudgetWithSpending(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val spent: Double
)

data class BudgetsUiState(
    val budgetsWithSpending: List<BudgetWithSpending> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
    val currentMonth: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
    val selectedMonth: Calendar = Calendar.getInstance()
)
