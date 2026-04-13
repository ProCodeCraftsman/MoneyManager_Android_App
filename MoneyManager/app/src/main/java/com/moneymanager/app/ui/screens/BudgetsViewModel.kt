package com.moneymanager.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.domain.repository.BudgetRepository
import com.moneymanager.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BudgetsUiState(
    val budgets: List<BudgetEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val currentMonth: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
)

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    val uiState: StateFlow<BudgetsUiState> = combine(
        budgetRepository.getAllBudgets(),
        categoryRepository.getAllCategories(),
    ) { budgets, categories ->
        BudgetsUiState(
            budgets = budgets,
            categories = categories.filter { it.type == "expense" },
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState()
    )
}