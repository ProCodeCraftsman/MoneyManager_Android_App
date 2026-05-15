package com.moneymanager.app.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.constants.TimeFilter
import com.moneymanager.app.ui.util.parseColor
import com.moneymanager.app.ui.util.generateDistinctColor
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.GoalEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.BudgetRepository
import com.moneymanager.domain.repository.CategoryRepository
import com.moneymanager.domain.repository.GoalRepository
import com.moneymanager.domain.repository.PeerContactRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    application: Application,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val goalRepository: GoalRepository,
    private val peerContactRepository: PeerContactRepository,
    private val preferencesManager: PreferencesManager
) : AndroidViewModel(application) {

    // --- Filter state flows ---
    private val selectedFilter = MutableStateFlow(TimeFilter.MONTH)
    private val currentPeriodDate = MutableStateFlow(Calendar.getInstance())
    private val customStartDate = MutableStateFlow<Long?>(null)
    private val customEndDate = MutableStateFlow<Long?>(null)
    private val activeTab = MutableStateFlow(SummaryTab.EXPENSE)

    // --- Filter params bundle ---
    data class FilterParams(
        val filter: TimeFilter,
        val baseDate: Calendar,
        val customStart: Long?,
        val customEnd: Long?
    )

    private val filterState: StateFlow<FilterParams> = combine(
        selectedFilter,
        currentPeriodDate,
        customStartDate,
        customEndDate
    ) { filter, baseDate, customStart, customEnd ->
        FilterParams(
            filter = filter,
            baseDate = baseDate,
            customStart = customStart,
            customEnd = customEnd
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilterParams(
            filter = TimeFilter.MONTH,
            baseDate = Calendar.getInstance(),
            customStart = null,
            customEnd = null
        )
    )

    // --- Date range helper ---
    private fun getDateRangeForFilter(
        filter: TimeFilter,
        baseDate: Calendar,
        customStart: Long?,
        customEnd: Long?
    ): Pair<Long, Long> {
        val startCal = baseDate.clone() as Calendar
        val endCal = baseDate.clone() as Calendar

        when (filter) {
            TimeFilter.DAY -> {
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.WEEK -> {
                startCal[Calendar.DAY_OF_WEEK] = startCal.firstDayOfWeek
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal.timeInMillis = startCal.timeInMillis
                endCal.add(Calendar.DAY_OF_MONTH, 6)
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
                endCal[Calendar.MILLISECOND] = 999
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
                endCal[Calendar.MILLISECOND] = 999
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
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.ALL -> {
                startCal.set(2000, Calendar.JANUARY, 1, 0, 0, 0)
                startCal[Calendar.MILLISECOND] = 0
                endCal.set(2100, Calendar.DECEMBER, 31, 23, 59, 59)
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.LAST_MONTH -> {
                startCal.add(Calendar.MONTH, -1)
                startCal[Calendar.DAY_OF_MONTH] = 1
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal.timeInMillis = startCal.timeInMillis
                endCal[Calendar.DAY_OF_MONTH] = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.THIS_QUARTER -> {
                val currentMonth = startCal.get(Calendar.MONTH)
                val quarterStartMonth = (currentMonth / 3) * 3
                startCal.set(Calendar.MONTH, quarterStartMonth)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal.set(Calendar.MONTH, quarterStartMonth + 2)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.LAST_QUARTER -> {
                startCal.add(Calendar.MONTH, -3)
                val quarterStart = (startCal.get(Calendar.MONTH) / 3) * 3
                startCal.set(Calendar.MONTH, quarterStart)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal.timeInMillis = startCal.timeInMillis
                endCal.set(Calendar.MONTH, quarterStart + 2)
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.TODAY -> {
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.LAST_7_DAYS -> {
                startCal.add(Calendar.DAY_OF_YEAR, -6)
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.LAST_30_DAYS -> {
                startCal.add(Calendar.DAY_OF_YEAR, -29)
                startCal[Calendar.HOUR_OF_DAY] = 0
                startCal[Calendar.MINUTE] = 0
                startCal[Calendar.SECOND] = 0
                startCal[Calendar.MILLISECOND] = 0
                endCal[Calendar.HOUR_OF_DAY] = 23
                endCal[Calendar.MINUTE] = 59
                endCal[Calendar.SECOND] = 59
                endCal[Calendar.MILLISECOND] = 999
            }
            TimeFilter.CUSTOM -> {
                if (customStart != null && customEnd != null) {
                    return Pair(customStart, customEnd)
                }
                // Fallback to month
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

    private fun getFilterDisplayDate(filter: TimeFilter, calendar: Calendar): String {
        return when (filter) {
            TimeFilter.DAY -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
            TimeFilter.WEEK -> {
                val start = calendar.clone() as Calendar
                start[Calendar.DAY_OF_WEEK] = start.firstDayOfWeek
                val end = start.clone() as Calendar
                end.add(Calendar.DAY_OF_MONTH, 6)
                val df = SimpleDateFormat("MMM dd", Locale.getDefault())
                "${df.format(start.time)} - ${df.format(end.time)}"
            }
            TimeFilter.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
            TimeFilter.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.time)
            TimeFilter.ALL -> "All Time"
            TimeFilter.LAST_MONTH -> "Last Month"
            TimeFilter.THIS_QUARTER -> "This Quarter"
            TimeFilter.LAST_QUARTER -> "Last Quarter"
            TimeFilter.TODAY -> "Today"
            TimeFilter.LAST_7_DAYS -> "Last 7 Days"
            TimeFilter.LAST_30_DAYS -> "Last 30 Days"
            TimeFilter.CUSTOM -> "Custom Range"
        }
    }

    fun getCurrentDateRange(): Pair<Long, Long> {
        val params = filterState.value
        return getDateRangeForFilter(params.filter, params.baseDate, params.customStart, params.customEnd)
    }

    // --- Filtered transactions pipeline (uses getTransactionsByDateRange ONLY) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    private val filteredTransactions: Flow<List<TransactionEntity>> = filterState.flatMapLatest { p ->
        val (start, end) = getDateRangeForFilter(p.filter, p.baseDate, p.customStart, p.customEnd)
        transactionRepository.getTransactionsByDateRange(start, end)
            .map { list ->
                SummaryAggregator.excludeSplitChildren(list)
            }
    }

    // --- Previous period transactions for trend calculation ---
    @OptIn(ExperimentalCoroutinesApi::class)
    private val prevPeriodTransactions: Flow<List<TransactionEntity>> = filterState.flatMapLatest { p ->
        val prevBaseDate = p.baseDate.clone() as Calendar
        when (p.filter) {
            TimeFilter.DAY -> prevBaseDate.add(Calendar.DAY_OF_YEAR, -1)
            TimeFilter.WEEK -> prevBaseDate.add(Calendar.WEEK_OF_YEAR, -1)
            TimeFilter.MONTH -> prevBaseDate.add(Calendar.MONTH, -1)
            TimeFilter.YEAR -> prevBaseDate.add(Calendar.YEAR, -1)
            else -> prevBaseDate.add(Calendar.MONTH, -1)
        }
        val (start, end) = getDateRangeForFilter(p.filter, prevBaseDate, null, null)
        transactionRepository.getTransactionsByDateRange(start, end)
            .map { list ->
                SummaryAggregator.excludeSplitChildren(list)
            }
    }

    private val allTransactions: Flow<List<TransactionEntity>> = transactionRepository.getAllTransactions()
        .map { SummaryAggregator.excludeSplitChildren(it) }

    private val allPeers: Flow<List<PeerContact>> = peerContactRepository.getAllPeers()

    // --- Expose combined UI state ---
    val uiState: StateFlow<SummaryUiState> = combine(
        filteredTransactions,
        prevPeriodTransactions,
        budgetRepository.getAllBudgets(),
        categoryRepository.getAllCategories(),
        accountRepository.getAllAccounts(),
        goalRepository.getAllGoals(),
        allTransactions,
        allPeers,
        preferencesManager.currency,
        activeTab,
        filterState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txs = values[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val prevTxs = values[1] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val allBudgets = values[2] as List<BudgetEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = values[3] as List<CategoryEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = values[4] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val allGoals = values[5] as List<GoalEntity>
        @Suppress("UNCHECKED_CAST")
        val allTxs = values[6] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val allPeers = values[7] as List<PeerContact>
        val currency = values[8] as String
        val tab = values[9] as SummaryTab
        val params = values[10] as FilterParams

        // Determine active period budget month string (for MONTH filter)
        val budgetMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(params.baseDate.time)
        // PRD: budget rows shown for current MONTH bucket only when filter != MONTH
        val activeBudgets = if (params.filter == TimeFilter.MONTH) {
            allBudgets.filter { it.month == budgetMonthStr }
        } else {
            allBudgets.filter { it.month == budgetMonthStr }
        }

        val totalIncome = SummaryAggregator.sumByType(txs, "income")
        val totalExpense = SummaryAggregator.sumByType(txs, "expense")
        val netBalance = totalIncome - totalExpense

        val prevTotalIncome = SummaryAggregator.sumByType(prevTxs, "income")
        val incomeTrendPercent = if (prevTotalIncome != 0.0) {
            ((totalIncome - prevTotalIncome) / kotlin.math.abs(prevTotalIncome)) * 100.0
        } else if (totalIncome != 0.0) 100.0 else 0.0

        val prevNetBalance = SummaryAggregator.netBalance(prevTxs)
        val netBalanceTrendPercent = if (prevNetBalance != 0.0) {
            ((netBalance - prevNetBalance) / kotlin.math.abs(prevNetBalance)) * 100.0
        } else if (netBalance != 0.0) 100.0 else 0.0

        val expenseByCategory = SummaryAggregator.expenseByCategory(txs, categories) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#90a4ae") {
                parseColor(hex)
            } else {
                generateDistinctColor(id.toInt())
            }
        }

        val expenseByAccount = SummaryAggregator.expenseByAccount(txs, accounts) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#2a6049") {
                parseColor(hex)
            } else {
                generateDistinctColor(id.toInt())
            }
        }

        val topBudgetUtilization = SummaryAggregator.topBudgetUtilization(txs, activeBudgets, categories, 100) { hex ->
            parseColor(hex ?: "#FF5252")
        }
        
        val incomeByCategory = SummaryAggregator.incomeByCategory(txs, categories) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#90a4ae") {
                parseColor(hex)
            } else {
                generateDistinctColor(id.toInt())
            }
        }

        val incomeByCategoryPie = SummaryAggregator.incomeByCategoryPie(txs, categories) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#90a4ae") {
                parseColor(hex)
            } else {
                generateDistinctColor(id.toInt())
            }
        }

        val incomeByAccount = SummaryAggregator.incomeByAccount(txs, accounts) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#2a6049") {
                parseColor(hex)
            } else {
                generateDistinctColor(id.toInt())
            }
        }

        val totalBudget = activeBudgets.sumOf { it.amount }
        val budgetRemaining = (totalBudget - totalExpense).coerceAtLeast(0.0)
        val budgetUtilizationPercent = if (totalBudget > 0) (totalExpense / totalBudget * 100.0).toFloat() else 0f

        // Lending calculations
        val lendingPeople = SummaryAggregator.lendingPeople(allTxs, allPeers)
        val totalLent = lendingPeople.filter { it.isOwed }.sumOf { it.amount }
        val totalBorrowed = lendingPeople.filter { !it.isOwed }.sumOf { it.amount }
        val lentPeopleCount = lendingPeople.count { it.isOwed }
        val borrowedPeopleCount = lendingPeople.count { !it.isOwed }
        val lendingNetBalance = totalLent - totalBorrowed
        
        val settledAmount = SummaryAggregator.sumByType(txs, "receive") + SummaryAggregator.sumByType(txs, "repay")
        val settledCount = txs.count { it.type == "receive" || it.type == "repay" }

        // Transfer calculations
        val totalTransfersCount = txs.count { it.type == "transfer" }
        val totalTransferAmount = txs.filter { it.type == "transfer" }.sumOf { it.amount }
        val accountTransfers = SummaryAggregator.accountTransferSummary(txs, accounts)

        // Savings calculations
        val savingsAccounts = SummaryAggregator.savingsSummary(accounts)
        val savingsAccountIds = savingsAccounts.map { it.id }.toSet()

        // All-time linked amounts for totalSavings (cumulative snapshot)
        val allTimeGoalLinked = mutableMapOf<Long, Double>()
        allTxs.forEach { tx ->
            tx.goalId?.let { gid ->
                allTimeGoalLinked[gid] = (allTimeGoalLinked[gid] ?: 0.0) + tx.amount
            }
        }
        val totalSavings = savingsAccounts.sumOf { it.balance } +
            allGoals.sumOf { goal -> goal.currentAmount + (allTimeGoalLinked[goal.id] ?: 0.0) }

        // Period-scoped linked amounts for savings goals list (time-filter-aware)
        val goalLinkedAmounts = mutableMapOf<Long, Double>()
        txs.forEach { tx ->
            tx.goalId?.let { gid ->
                goalLinkedAmounts[gid] = (goalLinkedAmounts[gid] ?: 0.0) + tx.amount
            }
        }
        val savingsGoals = SummaryAggregator.savingsGoals(allGoals, goalLinkedAmounts) { _, id ->
            val goalPalette = listOf("#673AB7", "#5E35B1", "#512DA8", "#4527A0", "#311B92")
            parseColor(goalPalette[(id % goalPalette.size).toInt()])
        }
        
        val currentSavingsInflow = SummaryAggregator.savingsInflow(txs, savingsAccountIds)
        val prevSavingsInflow = SummaryAggregator.savingsInflow(prevTxs, savingsAccountIds)
        val savingsGrowthPercent = if (prevSavingsInflow != 0.0) {
            ((currentSavingsInflow - prevSavingsInflow) / kotlin.math.abs(prevSavingsInflow)) * 100.0
        } else if (currentSavingsInflow != 0.0) 100.0 else 0.0
        val savingsGrowthPeriod = "vs previous ${params.filter.name.lowercase()}"

        val isEmpty = txs.isEmpty() && allGoals.isEmpty() && savingsAccounts.isEmpty()

        SummaryUiState(
            isLoading = false,
            isEmpty = isEmpty,
            activeTab = tab,
            selectedFilter = params.filter,
            filterDisplayDate = getFilterDisplayDate(params.filter, params.baseDate),
            customStartDate = params.customStart,
            customEndDate = params.customEnd,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            prevNetBalance = prevNetBalance,
            netBalanceTrendPercent = netBalanceTrendPercent,
            prevTotalIncome = prevTotalIncome,
            incomeTrendPercent = incomeTrendPercent,
            totalBudget = totalBudget,
            budgetRemaining = budgetRemaining,
            budgetUtilizationPercent = budgetUtilizationPercent,
            expenseByCategory = expenseByCategory,
            expenseByAccount = expenseByAccount,
            topBudgetUtilization = topBudgetUtilization,
            incomeByCategory = incomeByCategory,
            incomeByAccount = incomeByAccount,
            incomeByCategoryPie = incomeByCategoryPie,
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            lentPeopleCount = lentPeopleCount,
            borrowedPeopleCount = borrowedPeopleCount,
            lendingNetBalance = lendingNetBalance,
            settledAmount = settledAmount,
            settledCount = settledCount,
            lendingPeople = lendingPeople,
            totalTransfersCount = totalTransfersCount,
            totalTransferAmount = totalTransferAmount,
            accountTransfers = accountTransfers,
            totalSavings = totalSavings,
            savingsGrowthPercent = savingsGrowthPercent,
            savingsGrowthPeriod = savingsGrowthPeriod,
            savingsGoals = savingsGoals,
            savingsAccounts = savingsAccounts,
            currency = currency
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SummaryUiState()
    )

    // --- Public setters ---

    fun setTimeFilter(filter: TimeFilter, baseTimeMillis: Long? = null) {
        selectedFilter.value = filter
        baseTimeMillis?.let {
            val cal = Calendar.getInstance()
            cal.timeInMillis = it
            currentPeriodDate.value = cal
        }
        // Reset custom dates when switching away from CUSTOM
        if (filter != TimeFilter.CUSTOM) {
            customStartDate.value = null
            customEndDate.value = null
        }
    }

    fun setActiveTab(tab: SummaryTab) {
        activeTab.value = tab
    }

    fun setCustomDateRange(start: Long, end: Long) {
        customStartDate.value = start
        customEndDate.value = end
        selectedFilter.value = TimeFilter.CUSTOM
    }

    fun navigatePeriod(offset: Int) {
        val newDate = currentPeriodDate.value.clone() as Calendar
        when (selectedFilter.value) {
            TimeFilter.DAY -> newDate.add(Calendar.DAY_OF_YEAR, offset)
            TimeFilter.WEEK -> newDate.add(Calendar.WEEK_OF_YEAR, offset)
            TimeFilter.MONTH -> newDate.add(Calendar.MONTH, offset)
            TimeFilter.YEAR -> newDate.add(Calendar.YEAR, offset)
            else -> {}
        }
        currentPeriodDate.value = newDate
    }
}
