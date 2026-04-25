package com.moneymanager.app.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.app.ui.components.TrendPoint
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.BudgetRepository
import com.moneymanager.domain.repository.PeerContactRepository
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

data class BudgetProgress(
    val categoryName: String,
    val budgeted: Double,
    val actual: Double,
    val percentage: Float,
    val color: Color
)

data class LendingSummary(
    val peerId: Long,
    val name: String,
    val totalGiven: Double,
    val totalReceived: Double,
    val outstanding: Double
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
    val lendingSummary: List<LendingSummary> = emptyList(),
    val totalLent: Double = 0.0,
    val totalBorrowed: Double = 0.0,
    val totalOutstandingLending: Double = 0.0,
    val currencyCode: String = "INR",
    val isLoading: Boolean = true,
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val budgetRepository: BudgetRepository,
    private val peerContactRepository: PeerContactRepository,
    private val preferencesManager: PreferencesManager,
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
        budgetRepository.getAllBudgets(),
        peerContactRepository.getAllPeers(),
        preferencesManager.currency,
    ) { timeRange, allTransactions, budgets, peers, currencyCode ->
        val (startDate, endDate) = getDateRange(timeRange)
        val (prevStart, prevEnd) = getPreviousPeriod(timeRange)
        
        val currentPeriod = allTransactions.filter { it.date in startDate..endDate }
        val previousPeriod = allTransactions.filter { it.date in prevStart..prevEnd }
        
        // Simpler approach: filter out split parents and use all other transactions (including split children)
        val reportingTransactions = currentPeriod.filter { !it.isSplitParent }
        val prevReportingTransactions = previousPeriod.filter { !it.isSplitParent }

        val totalIncome = reportingTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = reportingTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        val previousIncome = prevReportingTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val previousExpense = prevReportingTransactions.filter { it.type == "expense" }.sumOf { it.amount }
        
        val incomeChange = if (previousIncome > 0) {
            ((totalIncome - previousIncome) / previousIncome * 100).toFloat()
        } else 0f
        
        val expenseChange = if (previousExpense > 0) {
            ((totalExpense - previousExpense) / previousExpense * 100).toFloat()
        } else 0f
        
        val trendData = generateTrendData(allTransactions.filter { !it.isSplitParent }, timeRange)
        val categoryBreakdown = generateCategoryBreakdown(reportingTransactions)
        val budgetProgress = generateBudgetProgress(reportingTransactions, budgets)

        val totalLent = peers.sumOf { it.totalGiven }
        val totalBorrowed = peers.sumOf { it.totalReceived }
        
        val lendingSummary = peers.filter { it.totalGiven > 0 || it.totalReceived > 0 }.map { peer ->
            LendingSummary(
                peerId = peer.id,
                name = peer.displayName,
                totalGiven = peer.totalGiven,
                totalReceived = peer.totalReceived,
                outstanding = peer.outstandingBalance
            )
        }.sortedByDescending { it.outstanding }
        
        val totalOutstandingLending = lendingSummary.sumOf { it.outstanding }
        
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
            lendingSummary = lendingSummary,
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            totalOutstandingLending = totalOutstandingLending,
            currencyCode = currencyCode,
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
        
        // Determine number of points and duration of each point based on time range
        val (numPoints, daysPerPoint) = when (timeRange) {
            TimeRange.WEEK -> 7 to 1      // 7 days, 1 point per day
            TimeRange.MONTH -> 8 to 7     // 8 weeks (~2 months) to see the trend
            TimeRange.QUARTER -> 6 to 15  // 3 months, point every 15 days
            TimeRange.YEAR -> 12 to 30    // 1 year, 1 point per month
        }
        
        var currentEndTime = calendar.timeInMillis
        
        repeat(numPoints) {
            val periodEnd = getDayEnd(currentEndTime)
            calendar.timeInMillis = currentEndTime
            calendar.add(Calendar.DAY_OF_YEAR, -daysPerPoint + 1)
            val periodStart = getDayStart(calendar.timeInMillis)
            
            val periodTransactions = transactions.filter { it.date in periodStart..periodEnd }
            val income = periodTransactions.filter { it.type == "income" }.sumOf { it.amount }
            val expense = periodTransactions.filter { it.type == "expense" }.sumOf { it.amount }
            
            val label = when (timeRange) {
                TimeRange.WEEK -> java.text.SimpleDateFormat("EEE", Locale.getDefault()).format(Date(periodStart))
                TimeRange.MONTH, TimeRange.QUARTER -> java.text.SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(periodStart))
                TimeRange.YEAR -> java.text.SimpleDateFormat("MMM", Locale.getDefault()).format(Date(periodStart))
            }
            
            points.add(0, TrendPoint(label, income, expense, income - expense))
            
            // Move to the day before this period started
            calendar.timeInMillis = periodStart
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            currentEndTime = calendar.timeInMillis
        }
        
        return points
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
