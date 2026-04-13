package com.moneymanager.app.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.BudgetRepository
import com.moneymanager.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

enum class TimeRange(val label: String, val days: Int) {
    WEEK("Week", 7),
    MONTH("Month", 30),
    QUARTER("Quarter", 90),
    YEAR("Year", 365)
}

data class TrendPoint(
    val label: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class BudgetProgress(
    val categoryName: String,
    val budgeted: Double,
    val actual: Double,
    val percentage: Float,
    val color: Color
)

data class ReportsUiState(
    val selectedTimeRange: TimeRange = TimeRange.MONTH,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val previousIncome: Double = 0.0,
    val previousExpense: Double = 0.0,
    val incomeChange: Float = 0f,
    val expenseChange: Float = 0f,
    val trendData: List<TrendPoint> = emptyList(),
    val categoryBreakdown: List<PieChartEntry> = emptyList(),
    val budgetProgress: List<BudgetProgress> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(TimeRange.MONTH)
    
    private val categoryColors = mapOf(
        "Food" to Color(0xFFE57373),
        "Transport" to Color(0xFF64B5F6),
        "Shopping" to Color(0xFFBA68C8),
        "Bills" to Color(0xFFFFD54F),
        "Entertainment" to Color(0xFF4DB6AC),
        "Health" to Color(0xFFFF8A65),
        "Other" to Color(0xFF90A4AE)
    )

    val uiState: StateFlow<ReportsUiState> = combine(
        _selectedTimeRange,
        transactionRepository.getAllTransactions(),
        budgetRepository.getAllBudgets()
    ) { timeRange, allTransactions, budgets ->
        val (startDate, endDate) = getDateRange(timeRange)
        val (prevStart, prevEnd) = getPreviousPeriod(timeRange)
        
        val currentPeriod = allTransactions.filter { it.date in startDate..endDate }
        val previousPeriod = allTransactions.filter { it.date in prevStart..prevEnd }
        
        val totalIncome = currentPeriod.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = currentPeriod.filter { it.type == "expense" }.sumOf { it.amount }
        val previousIncome = previousPeriod.filter { it.type == "income" }.sumOf { it.amount }
        val previousExpense = previousPeriod.filter { it.type == "expense" }.sumOf { it.amount }
        
        val incomeChange = if (previousIncome > 0) {
            ((totalIncome - previousIncome) / previousIncome * 100).toFloat()
        } else 0f
        
        val expenseChange = if (previousExpense > 0) {
            ((totalExpense - previousExpense) / previousExpense * 100).toFloat()
        } else 0f
        
        val trendData = generateTrendData(currentPeriod, timeRange)
        val categoryBreakdown = generateCategoryBreakdown(currentPeriod)
        val budgetProgress = generateBudgetProgress(currentPeriod, budgets)
        
        ReportsUiState(
            selectedTimeRange = timeRange,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netSavings = totalIncome - totalExpense,
            previousIncome = previousIncome,
            previousExpense = previousExpense,
            incomeChange = incomeChange,
            expenseChange = expenseChange,
            trendData = trendData,
            categoryBreakdown = categoryBreakdown,
            budgetProgress = budgetProgress,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportsUiState()
    )

    fun setTimeRange(range: TimeRange) {
        _selectedTimeRange.value = range
    }

    private fun getDateRange(timeRange: TimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, -timeRange.days)
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        val startDate = calendar.timeInMillis
        
        return Pair(startDate, endDate)
    }

    private fun getPreviousPeriod(timeRange: TimeRange): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, -timeRange.days)
        val midDate = calendar.timeInMillis
        
        calendar.add(Calendar.DAY_OF_YEAR, -timeRange.days)
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        val startDate = calendar.timeInMillis
        
        return Pair(startDate, midDate)
    }

    private fun generateTrendData(
        transactions: List<com.moneymanager.data.entity.TransactionEntity>,
        timeRange: TimeRange
    ): List<TrendPoint> {
        val points = mutableListOf<TrendPoint>()
        val calendar = Calendar.getInstance()
        val groupByDays = when (timeRange) {
            TimeRange.WEEK -> 1
            TimeRange.MONTH -> 7
            TimeRange.QUARTER -> 14
            TimeRange.YEAR -> 30
        }
        
        var currentDate = calendar.timeInMillis
        while (points.size < 8) {
            val dayStart = getDayStart(currentDate)
            val dayEnd = getDayEnd(currentDate)
            
            val dayTransactions = transactions.filter { it.date in dayStart..dayEnd }
            val income = dayTransactions.filter { it.type == "income" }.sumOf { it.amount }
            val expense = dayTransactions.filter { it.type == "expense" }.sumOf { it.amount }
            
            val label = when (timeRange) {
                TimeRange.WEEK -> java.text.SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dayStart))
                TimeRange.MONTH -> java.text.SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dayStart))
                TimeRange.QUARTER -> java.text.SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dayStart))
                TimeRange.YEAR -> java.text.SimpleDateFormat("MMM", Locale.getDefault()).format(Date(dayStart))
            }
            
            points.add(0, TrendPoint(label, income, expense, income - expense))
            
            calendar.timeInMillis = currentDate
            calendar.add(Calendar.DAY_OF_YEAR, -groupByDays)
            currentDate = calendar.timeInMillis
        }
        
        return points.reversed()
    }

    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar[Calendar.HOUR_OF_DAY] = 0
        calendar[Calendar.MINUTE] = 0
        calendar[Calendar.SECOND] = 0
        calendar[Calendar.MILLISECOND] = 0
        return calendar.timeInMillis
    }

    private fun getDayEnd(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar[Calendar.HOUR_OF_DAY] = 23
        calendar[Calendar.MINUTE] = 59
        calendar[Calendar.SECOND] = 59
        calendar[Calendar.MILLISECOND] = 999
        return calendar.timeInMillis
    }

    private fun generateCategoryBreakdown(
        transactions: List<com.moneymanager.data.entity.TransactionEntity>
    ): List<PieChartEntry> {
        return transactions
            .filter { it.type == "expense" }
            .groupBy { getCategoryName(it.categoryId) }
            .map { (category, txns) ->
                PieChartEntry(
                    label = category,
                    value = txns.sumOf { it.amount },
                    color = categoryColors[category] ?: categoryColors["Other"]!!
                )
            }
            .sortedByDescending { it.value }
    }

    private fun generateBudgetProgress(
        transactions: List<com.moneymanager.data.entity.TransactionEntity>,
        budgets: List<com.moneymanager.data.entity.BudgetEntity>
    ): List<BudgetProgress> {
        return budgets.map { budget ->
            val actual = transactions
                .filter { it.type == "expense" && it.categoryId == budget.categoryId }
                .sumOf { it.amount }
            val percentage = if (budget.amount > 0) (actual / budget.amount * 100).toFloat() else 0f
            
            BudgetProgress(
                categoryName = getCategoryName(budget.categoryId),
                budgeted = budget.amount,
                actual = actual,
                percentage = percentage.coerceIn(0f, 150f),
                color = when {
                    percentage > 100 -> Color(0xFFE57373)
                    percentage > 80 -> Color(0xFFFFD54F)
                    else -> Color(0xFF4DB6AC)
                }
            )
        }
    }

    private fun getCategoryName(categoryId: Long?): String {
        return when (categoryId) {
            1L -> "Food"
            2L -> "Transport"
            3L -> "Shopping"
            4L -> "Bills"
            5L -> "Entertainment"
            6L -> "Health"
            else -> "Other"
        }
    }
}
