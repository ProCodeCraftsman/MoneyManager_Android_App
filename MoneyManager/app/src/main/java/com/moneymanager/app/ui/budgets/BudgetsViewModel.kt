package com.moneymanager.app.ui.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.dao.TransactionDao
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.BudgetRepository
import com.moneymanager.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class BudgetsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionDao: TransactionDao,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val monthDateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance())

    private fun getMonthDateRange(calendar: Calendar): Pair<Long, Long> {
        val cal = calendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        
        cal.add(Calendar.MONTH, 1)
        val end = cal.timeInMillis - 1
        return Pair(start, end)
    }

    val uiState: StateFlow<BudgetsUiState> = combine(
        _selectedMonth,
        budgetRepository.getAllBudgets(),
        categoryRepository.getAllCategories(),
        transactionDao.getAllTransactions(), // We filter manually for better reactivity to month changes
        preferencesManager.currency,
    ) { calendar, allBudgets, categories, allTransactions, currencyCode ->
        val monthStr = monthDateFormat.format(calendar.time)
        val (startDate, endDate) = getMonthDateRange(calendar)
        
        val monthBudgets = allBudgets.filter { it.month == monthStr }
        val monthTransactions = allTransactions.filter { it.date in startDate..endDate }

        val budgetableTypes = setOf("expense") // Only expense categories for budgets by default

        val budgetsWithSpending = monthBudgets.map { budget ->
            val category = categories.find { it.id == budget.categoryId }
            val spent = monthTransactions.sumOf { tx ->
                if (tx.isSplitParent) 0.0
                else if (tx.categoryId == budget.categoryId) tx.amount
                else 0.0
            }
            BudgetWithSpending(budget, category, spent)
        }

        BudgetsUiState(
            budgetsWithSpending = budgetsWithSpending,
            categories = categories.filter { it.type in budgetableTypes && it.parentId == null },
            currencyCode = currencyCode,
            isLoading = false,
            currentMonth = monthStr,
            selectedMonth = calendar
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BudgetsUiState()
    )

    fun changeMonth(delta: Int) {
        val newCal = _selectedMonth.value.clone() as Calendar
        newCal.add(Calendar.MONTH, delta)
        _selectedMonth.value = newCal
    }

    fun addBudget(categoryId: Long, amount: Double, autoCreateNextMonth: Boolean) {
        viewModelScope.launch {
            val monthStr = monthDateFormat.format(_selectedMonth.value.time)
            val budget = BudgetEntity(
                categoryId = categoryId,
                amount = amount,
                month = monthStr
            )
            budgetRepository.insertBudget(budget)

            if (autoCreateNextMonth) {
                val nextCal = _selectedMonth.value.clone() as Calendar
                nextCal.add(Calendar.MONTH, 1)
                val nextMonthStr = monthDateFormat.format(nextCal.time)
                
                // Check if budget already exists for next month
                val existing = budgetRepository.getBudgetsByPeriod(nextMonthStr).first().find { it.categoryId == categoryId }
                if (existing == null) {
                    budgetRepository.insertBudget(
                        BudgetEntity(
                            categoryId = categoryId,
                            amount = amount,
                            month = nextMonthStr
                        )
                    )
                }
            }
        }
    }

    fun updateBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            budgetRepository.updateBudget(budget)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            budgetRepository.deleteBudget(budget)
        }
    }
}
