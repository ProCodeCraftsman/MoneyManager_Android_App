package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.domain.repository.BudgetRepository
import com.moneymanager.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BudgetWithSpending(
    val budget: BudgetEntity,
    val category: CategoryEntity?,
    val spent: Double
)

data class BudgetsUiState(
    val budgetsWithSpending: List<BudgetWithSpending> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val currentMonth: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionDao: TransactionDao,
) : ViewModel() {

    private val monthDateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    private fun getMonthDateRange(): Pair<Long, Long> {
        val now = Date()
        val monthStart = monthDateFormat.parse(monthDateFormat.format(now))!!
        val calendar = Calendar.getInstance()
        calendar.time = monthStart
        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.time
        return Pair(monthStart.time, monthEnd.time - 1)
    }

    val uiState: StateFlow<BudgetsUiState> = run {
        val (startDate, endDate) = getMonthDateRange()
        combine(
            budgetRepository.getAllBudgets(),
            categoryRepository.getAllCategories(),
            transactionDao.getTransactionsByDateRange(startDate, endDate),
        ) { budgets, categories, transactions ->
            val expenseTransactions = transactions.filter { it.type == "expense" }

            val budgetsWithSpending = budgets.map { budget ->
                val category = categories.find { it.id == budget.categoryId }
                val spent = expenseTransactions
                    .filter { it.categoryId == budget.categoryId }
                    .sumOf { it.amount }
                BudgetWithSpending(budget, category, spent)
            }

            BudgetsUiState(
                budgetsWithSpending = budgetsWithSpending,
                categories = categories.filter { it.type == "expense" },
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BudgetsUiState()
        )
    }
}