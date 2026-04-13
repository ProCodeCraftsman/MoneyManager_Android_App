package com.moneymanager.app.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.BudgetRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.app.ui.components.PieChartEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class TimeFilter(val displayName: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All"),
    CUSTOM("Custom")
}

data class BudgetWithProgress(
    val budget: BudgetEntity,
    val categoryName: String,
    val spent: Double,
    val percentage: Float
)

data class DashboardUiState(
    val netWorth: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val expenseBreakdown: List<PieChartEntry> = emptyList(),
    val isLoading: Boolean = true,
    val selectedFilter: TimeFilter = TimeFilter.MONTH,
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedCategory: PieChartEntry? = null,
    val categoryTransactions: List<TransactionEntity> = emptyList(),
    val budgetsWithProgress: List<BudgetWithProgress> = emptyList(),
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(TimeFilter.MONTH)
    private val customStartDate = MutableStateFlow<Long?>(null)
    private val customEndDate = MutableStateFlow<Long?>(null)
    private val selectedCategory = MutableStateFlow<PieChartEntry?>(null)

    private val categoryColors = mapOf(
        "food" to Color(0xFFE57373),
        "transport" to Color(0xFF64B5F6),
        "shopping" to Color(0xFFBA68C8),
        "bills" to Color(0xFFFFD54F),
        "entertainment" to Color(0xFF4DB6AC),
        "health" to Color(0xFFFF8A65),
        "other" to Color(0xFF90A4AE)
    )

    private fun getDateRangeForFilter(filter: TimeFilter, customStart: Long?, customEnd: Long?): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()

        when (filter) {
            TimeFilter.DAY -> {
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
            }
            TimeFilter.WEEK -> {
                startCal[Calendar.DAY_OF_WEEK] = now.firstDayOfWeek
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal.add(Calendar.WEEK_OF_YEAR, 1)
                endCal.add(Calendar.MILLISECOND, -1)
            }
            TimeFilter.MONTH -> {
                startCal[Calendar.DAY_OF_MONTH] = 1
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal[Calendar.DAY_OF_MONTH] = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
            }
            TimeFilter.YEAR -> {
                startCal[Calendar.DAY_OF_YEAR] = 1
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal[Calendar.MONTH] = Calendar.DECEMBER
                endCal[Calendar.DAY_OF_MONTH] = 31
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
            }
            TimeFilter.ALL -> {
                startCal.set(2000, Calendar.JANUARY, 1, 0, 0, 0)
                startCal[Calendar.MILLISECOND] = 0
                endCal.set(2100, Calendar.DECEMBER, 31, 23, 59, 59)
            }
            TimeFilter.CUSTOM -> {
                if (customStart != null && customEnd != null) {
                    return Pair(customStart, customEnd)
                }
                // Fallback to month if custom dates not set
                startCal[Calendar.DAY_OF_MONTH] = 1
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal[Calendar.DAY_OF_MONTH] = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
            }
        }
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    // Flow that emits the current filter state
    private val filterState = combine(
        selectedFilter,
        customStartDate,
        customEndDate
    ) { filter, start, end ->
        Triple(filter, start, end)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Triple(TimeFilter.MONTH, null, null)
    )

    // Query transactions based on dynamic date range
    private val filteredTransactions: Flow<List<TransactionEntity>> = filterState.flatMapLatest { (filter, start, end) ->
        val (rangeStart, rangeEnd) = getDateRangeForFilter(filter, start, end)
        transactionRepository.getTransactionsByDateRange(rangeStart, rangeEnd)
    }

    // Query category transactions when a category is selected
    private val categoryTransactionsFlow: Flow<List<TransactionEntity>> = combine(
        filterState,
        selectedCategory
    ) { filterPair, category ->
        Pair(filterPair, category)
    }.flatMapLatest { (filterTriple, category) ->
        if (category == null) {
            flowOf(emptyList())
        } else {
            val (filter, start, end) = filterTriple
            val (rangeStart, rangeEnd) = getDateRangeForFilter(filter, start, end)
            transactionRepository.getTransactionsByDateRange(rangeStart, rangeEnd)
                .map { transactions ->
                    transactions.filter { tx ->
                        val categoryName = getCategoryNameFromId(tx.categoryId?.toString() ?: "other")
                        categoryName == category.label
                    }
                }
        }
    }

    // Get current month dates for budget calculation
    private fun getCurrentMonthDates(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val startCal = Calendar.getInstance()
        val endCal = Calendar.getInstance()
        
        startCal[Calendar.DAY_OF_MONTH] = 1
        startCal[Calendar.HOUR_OF_DAY] = 0
        startCal[Calendar.MINUTE] = 0
        startCal[Calendar.SECOND] = 0
        startCal[Calendar.MILLISECOND] = 0
        
        endCal[Calendar.DAY_OF_MONTH] = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        endCal[Calendar.HOUR_OF_DAY] = 23
        endCal[Calendar.MINUTE] = 59
        endCal[Calendar.SECOND] = 59
        
        return Pair(startCal.timeInMillis, endCal.timeInMillis)
    }

    // Query budgets with progress
    private val budgetsWithProgressFlow: Flow<List<BudgetWithProgress>> = combine(
        budgetRepository.getActiveBudgets(),
        filteredTransactions
    ) { budgets, transactions ->
        val (monthStart, monthEnd) = getCurrentMonthDates()
        val monthExpenses = transactions.filter { it.type == "expense" }
        
        budgets.map { budget ->
            val spent = monthExpenses
                .filter { it.categoryId == budget.categoryId }
                .sumOf { it.amount }
            val percentage = if (budget.amount > 0) (spent / budget.amount * 100).toFloat() else 0f
            
            BudgetWithProgress(
                budget = budget,
                categoryName = getCategoryNameFromId(budget.categoryId.toString()),
                spent = spent,
                percentage = percentage
            )
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getTotalAssets(),
        accountRepository.getTotalDebt(),
        filteredTransactions,
        transactionRepository.getRecentTransactions(10),
        accountRepository.getAllAccounts(),
        filterState,
        selectedCategory,
        categoryTransactionsFlow,
        budgetsWithProgressFlow
    ) { values ->
        val totalAssets = values[0] as Double
        val totalDebt = values[1] as Double
        val monthTransactions = values[2] as List<TransactionEntity>
        val recentTx = values[3] as List<TransactionEntity>
        val accounts = values[4] as List<AccountEntity>
        val filterTriple = values[5] as Triple<TimeFilter, Long?, Long?>
        val filter = filterTriple.first
        val customStart = filterTriple.second
        val customEnd = filterTriple.third
        val selectedCat = values[6] as PieChartEntry?
        val catTransactions = values[7] as List<TransactionEntity>
        val budgets = values[8] as List<BudgetWithProgress>

        val totalIncome = monthTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = monthTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        
        val expensesByCategory = monthTransactions
            .filter { it.type == "expense" }
            .groupBy { it.categoryId?.toString() ?: "other" }
            .map { (category, transactions) ->
                val categoryName = getCategoryNameFromId(category)
                val color = categoryColors[category.lowercase()] ?: categoryColors["other"]!!
                PieChartEntry(
                    label = categoryName,
                    value = transactions.sumOf { it.amount },
                    color = color
                )
            }
            .sortedByDescending { it.value }

        DashboardUiState(
            netWorth = totalAssets - totalDebt,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            recentTransactions = recentTx,
            accounts = accounts,
            expenseBreakdown = expensesByCategory,
            isLoading = false,
            selectedFilter = filter,
            customStartDate = customStart,
            customEndDate = customEnd,
            selectedCategory = selectedCat,
            categoryTransactions = catTransactions,
            budgetsWithProgress = budgets,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setTimeFilter(filter: TimeFilter) {
        selectedFilter.value = filter
    }

    fun setCustomDateRange(startDate: Long, endDate: Long) {
        customStartDate.value = startDate
        customEndDate.value = endDate
    }

    fun selectCategory(entry: PieChartEntry?) {
        selectedCategory.value = entry
    }

    private fun getCategoryNameFromId(categoryId: String): String {
        return when (categoryId.lowercase()) {
            "1" -> "Food"
            "2" -> "Transport"
            "3" -> "Shopping"
            "4" -> "Bills"
            "5" -> "Entertainment"
            "6" -> "Health"
            else -> "Other"
        }
    }

    fun transferMoney(fromAccountId: Long, toAccountId: Long, amount: Double, note: String) {
        viewModelScope.launch {
            val fromAccount = accountRepository.getAccountById(fromAccountId)
            val toAccount = accountRepository.getAccountById(toAccountId)
            
            if (fromAccount != null && toAccount != null) {
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = fromAccountId,
                        type = "transfer",
                        amount = -amount,
                        note = note.ifEmpty { "Transfer to ${toAccount.name}" },
                        categoryId = 4
                    )
                )
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = toAccountId,
                        type = "transfer",
                        amount = amount,
                        note = note.ifEmpty { "Transfer from ${fromAccount.name}" },
                        categoryId = 4
                    )
                )
                accountRepository.updateBalance(fromAccountId, fromAccount.balance - amount)
                accountRepository.updateBalance(toAccountId, toAccount.balance + amount)
            }
        }
    }
}
