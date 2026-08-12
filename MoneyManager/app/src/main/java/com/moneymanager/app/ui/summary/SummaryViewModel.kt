package com.moneymanager.app.ui.summary

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.app.ui.constants.TimeFilter
import com.moneymanager.app.ui.util.parseColor
import com.moneymanager.app.ui.util.generateDistinctColor
import com.moneymanager.data.entity.AccountEntity
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
import kotlinx.coroutines.Dispatchers
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
    private val selectedTrendType = MutableStateFlow(TrendType.OVERALL)
    private val trendTimeFilter = MutableStateFlow(TrendTimeFilter.YEAR_1)

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
            TimeFilter.MONTH -> SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
            TimeFilter.YEAR -> SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.time)
            TimeFilter.ALL -> "All Time"
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
                SummaryAggregator.deduplicateTransfers(SummaryAggregator.excludeSplitChildren(list))
            }
    }

    // --- Previous period transactions for trend calculation ---
    @OptIn(ExperimentalCoroutinesApi::class)
    private val prevPeriodTransactions: Flow<List<TransactionEntity>> = filterState.flatMapLatest { p ->
        val prevBaseDate = p.baseDate.clone() as Calendar
        when (p.filter) {
            TimeFilter.DAY -> prevBaseDate.add(Calendar.DAY_OF_YEAR, -1)
            TimeFilter.MONTH -> prevBaseDate.add(Calendar.MONTH, -1)
            TimeFilter.YEAR -> prevBaseDate.add(Calendar.YEAR, -1)
            else -> prevBaseDate.add(Calendar.MONTH, -1)
        }
        val (start, end) = getDateRangeForFilter(p.filter, prevBaseDate, null, null)
        transactionRepository.getTransactionsByDateRange(start, end)
            .map { list ->
                SummaryAggregator.deduplicateTransfers(SummaryAggregator.excludeSplitChildren(list))
            }
    }

    private val allTransactions: Flow<List<TransactionEntity>> = transactionRepository.getAllTransactions()
        .map { SummaryAggregator.deduplicateTransfers(SummaryAggregator.excludeSplitChildren(it)) }

    private val allPeers: Flow<List<PeerContact>> = peerContactRepository.getAllPeers()

    // --- Header & Common State ---
    private val headerState = combine(
        filteredTransactions,
        prevPeriodTransactions,
        preferencesManager.currency,
        filterState
    ) { txs, prevTxs, currency, params ->
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

        SummaryUiState(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            netBalance = netBalance,
            prevNetBalance = prevNetBalance,
            netBalanceTrendPercent = netBalanceTrendPercent,
            prevTotalIncome = prevTotalIncome,
            incomeTrendPercent = incomeTrendPercent,
            currency = currency,
            selectedFilter = params.filter,
            filterDisplayDate = getFilterDisplayDate(params.filter, params.baseDate),
            customStartDate = params.customStart,
            customEndDate = params.customEnd,
            isLoading = false,
            isEmpty = txs.isEmpty()
        )
    }.flowOn(Dispatchers.Default).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // --- Tab-specific Parallel States ---
    
    private val expenseState = combine(
        filteredTransactions,
        budgetRepository.getAllBudgets(),
        categoryRepository.getAllCategories(),
        accountRepository.getAllAccounts(),
        filterState
    ) { txs, allBudgets, categories, accounts, params ->
        val budgetMonthStr = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(params.baseDate.time)
        val activeBudgets = allBudgets.filter { it.month == budgetMonthStr }
        val totalExpense = SummaryAggregator.sumByType(txs, "expense")
        
        val expenseByCategory = SummaryAggregator.expenseByCategory(txs, categories) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#90a4ae") parseColor(hex) else generateDistinctColor(id.toInt())
        }
        val expenseByAccount = SummaryAggregator.expenseByAccount(txs, accounts) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#2a6049") parseColor(hex) else generateDistinctColor(id.toInt())
        }
        val topBudgetUtilization = SummaryAggregator.topBudgetUtilization(txs, activeBudgets, categories, 100) { hex ->
            parseColor(hex ?: "#FF5252")
        }
        val totalBudget = activeBudgets.sumOf { it.amount }
        val budgetRemaining = (totalBudget - totalExpense).coerceAtLeast(0.0)
        val budgetUtilizationPercent = if (totalBudget > 0) (totalExpense / totalBudget * 100.0).toFloat() else 0f

        SummaryUiState(
            expenseByCategory = expenseByCategory,
            expenseByAccount = expenseByAccount,
            topBudgetUtilization = topBudgetUtilization,
            totalBudget = totalBudget,
            budgetRemaining = budgetRemaining,
            budgetUtilizationPercent = budgetUtilizationPercent
        )
    }.flowOn(Dispatchers.Default).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val incomeState = combine(
        filteredTransactions,
        categoryRepository.getAllCategories(),
        accountRepository.getAllAccounts()
    ) { txs, categories, accounts ->
        val incomeByCategory = SummaryAggregator.incomeByCategory(txs, categories) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#90a4ae") parseColor(hex) else generateDistinctColor(id.toInt())
        }
        val incomeByCategoryPie = SummaryAggregator.incomeByCategoryPie(txs, categories) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#90a4ae") parseColor(hex) else generateDistinctColor(id.toInt())
        }
        val incomeByAccount = SummaryAggregator.incomeByAccount(txs, accounts) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#2a6049") parseColor(hex) else generateDistinctColor(id.toInt())
        }

        SummaryUiState(
            incomeByCategory = incomeByCategory,
            incomeByCategoryPie = incomeByCategoryPie,
            incomeByAccount = incomeByAccount
        )
    }.flowOn(Dispatchers.Default).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val lendingState = combine(
        allTransactions,
        allPeers
    ) { txs, peers ->
        val lendingPeople = SummaryAggregator.lendingPeople(txs, peers)
        val totalLent = lendingPeople.filter { it.isOwed }.sumOf { it.amount }
        val totalBorrowed = lendingPeople.filter { !it.isOwed }.sumOf { it.amount }
        val lentPeopleCount = lendingPeople.count { it.isOwed }
        val borrowedPeopleCount = lendingPeople.count { !it.isOwed }

        SummaryUiState(
            lendingPeople = lendingPeople,
            totalLent = totalLent,
            totalBorrowed = totalBorrowed,
            lentPeopleCount = lentPeopleCount,
            borrowedPeopleCount = borrowedPeopleCount,
            lendingNetBalance = totalLent - totalBorrowed
        )
    }.flowOn(Dispatchers.Default).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val transferState = combine(
        filteredTransactions,
        accountRepository.getAllAccounts()
    ) { txs, accounts ->
        val totalTransfersCount = txs.count { it.type == "transfer" }
        val totalTransferAmount = txs.filter { it.type == "transfer" }.sumOf { it.amount }
        val accountTransfers = SummaryAggregator.accountTransferSummary(txs, accounts)

        SummaryUiState(
            totalTransfersCount = totalTransfersCount,
            totalTransferAmount = totalTransferAmount,
            accountTransfers = accountTransfers
        )
    }.flowOn(Dispatchers.Default).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val savingsState = combine(
        filteredTransactions,
        prevPeriodTransactions,
        accountRepository.getAllAccounts(),
        goalRepository.getAllGoals(),
        allTransactions,
        categoryRepository.getAllCategories(),
        filterState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val txs = values[0] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val prevTxs = values[1] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = values[2] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val allGoals = values[3] as List<GoalEntity>
        @Suppress("UNCHECKED_CAST")
        val allTxs = values[4] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = values[5] as List<CategoryEntity>
        val params = values[6] as FilterParams

        val savingsAccounts = SummaryAggregator.savingsSummary(accounts)
        val savingsAccountIds = savingsAccounts.map { it.id }.toSet()
        val totalSavingsPeriod = SummaryAggregator.sumByType(txs, "savings")

        val allTimeGoalLinked = mutableMapOf<Long, Double>()
        allTxs.forEach { tx -> tx.goalId?.let { gid -> allTimeGoalLinked[gid] = (allTimeGoalLinked[gid] ?: 0.0) + tx.amount } }
        val totalSavings = savingsAccounts.sumOf { it.balance } + allGoals.sumOf { goal -> goal.currentAmount + (allTimeGoalLinked[goal.id] ?: 0.0) }

        val goalLinkedAmounts = mutableMapOf<Long, Double>()
        txs.forEach { tx -> tx.goalId?.let { gid -> goalLinkedAmounts[gid] = (goalLinkedAmounts[gid] ?: 0.0) + tx.amount } }
        val savingsGoals = SummaryAggregator.savingsGoals(allGoals, goalLinkedAmounts) { _, id ->
            val goalPalette = listOf("#673AB7", "#5E35B1", "#512DA8", "#4527A0", "#311B92")
            parseColor(goalPalette[(id % goalPalette.size).toInt()])
        }
        
        val currentSavingsInflow = SummaryAggregator.savingsInflow(txs, savingsAccountIds)
        val prevSavingsInflow = SummaryAggregator.savingsInflow(prevTxs, savingsAccountIds)
        val savingsGrowthPercent = if (prevSavingsInflow != 0.0) ((currentSavingsInflow - prevSavingsInflow) / kotlin.math.abs(prevSavingsInflow)) * 100.0 else if (currentSavingsInflow != 0.0) 100.0 else 0.0

        val savingsByCategory = SummaryAggregator.savingsByCategory(txs, categories) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#90a4ae") parseColor(hex) else generateDistinctColor(id.toInt())
        }
        val savingsByAccount = SummaryAggregator.savingsByAccount(txs, accounts) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#2a6049") parseColor(hex) else generateDistinctColor(id.toInt())
        }
        val savingsByCategorySpend = SummaryAggregator.savingsByCategorySpend(txs, categories) { hex, id ->
            if (hex != null && hex.isNotBlank() && hex.lowercase() != "#90a4ae") parseColor(hex) else generateDistinctColor(id.toInt())
        }

        SummaryUiState(
            totalSavings = totalSavings,
            totalSavingsPeriod = totalSavingsPeriod,
            savingsGrowthPercent = savingsGrowthPercent,
            savingsGrowthPeriod = "vs previous ${params.filter.name.lowercase()}",
            savingsGoals = savingsGoals,
            savingsAccounts = savingsAccounts,
            savingsByCategory = savingsByCategory,
            savingsByAccount = savingsByAccount,
            savingsByCategorySpend = savingsByCategorySpend
        )
    }.flowOn(Dispatchers.Default).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    private val trendsState = combine(
        allTransactions,
        selectedTrendType,
        trendTimeFilter
    ) { allTxs, trendType, trendFilter ->
        val trendResult = SummaryAggregator.calculateTrend(allTxs, trendType, trendFilter)
        
        val allTrendDataPoints = if (trendType == TrendType.OVERALL) {
            TrendType.entries.filter { it != TrendType.OVERALL }.associateWith { type ->
                SummaryAggregator.calculateTrend(allTxs, type, trendFilter).first
            }
        } else emptyMap()

        SummaryUiState(
            selectedTrendType = trendType,
            trendTimeFilter = trendFilter,
            trendDataPoints = trendResult.first,
            trendStats = trendResult.second,
            allTrendDataPoints = allTrendDataPoints
        )
    }.flowOn(Dispatchers.Default).shareIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // --- Expose combined UI state ---
    val uiState: StateFlow<SummaryUiState> = combine(
        headerState,
        activeTab,
        expenseState,
        incomeState,
        lendingState,
        transferState,
        savingsState,
        trendsState
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val header = values[0] as SummaryUiState
        val tab = values[1] as SummaryTab
        @Suppress("UNCHECKED_CAST")
        val expense = values[2] as SummaryUiState
        @Suppress("UNCHECKED_CAST")
        val income = values[3] as SummaryUiState
        @Suppress("UNCHECKED_CAST")
        val lending = values[4] as SummaryUiState
        @Suppress("UNCHECKED_CAST")
        val transfer = values[5] as SummaryUiState
        @Suppress("UNCHECKED_CAST")
        val savings = values[6] as SummaryUiState
        @Suppress("UNCHECKED_CAST")
        val trends = values[7] as SummaryUiState

        header.copy(
            activeTab = tab,
            // Expense
            expenseByCategory = expense.expenseByCategory,
            expenseByAccount = expense.expenseByAccount,
            topBudgetUtilization = expense.topBudgetUtilization,
            totalBudget = expense.totalBudget,
            budgetRemaining = expense.budgetRemaining,
            budgetUtilizationPercent = expense.budgetUtilizationPercent,
            // Income
            incomeByCategory = income.incomeByCategory,
            incomeByCategoryPie = income.incomeByCategoryPie,
            incomeByAccount = income.incomeByAccount,
            // Lending
            lendingPeople = lending.lendingPeople,
            totalLent = lending.totalLent,
            totalBorrowed = lending.totalBorrowed,
            lentPeopleCount = lending.lentPeopleCount,
            borrowedPeopleCount = lending.borrowedPeopleCount,
            lendingNetBalance = lending.lendingNetBalance,
            // Transfer
            totalTransfersCount = transfer.totalTransfersCount,
            totalTransferAmount = transfer.totalTransferAmount,
            accountTransfers = transfer.accountTransfers,
            // Savings
            totalSavings = savings.totalSavings,
            totalSavingsPeriod = savings.totalSavingsPeriod,
            savingsGrowthPercent = savings.savingsGrowthPercent,
            savingsGrowthPeriod = savings.savingsGrowthPeriod,
            savingsGoals = savings.savingsGoals,
            savingsAccounts = savings.savingsAccounts,
            savingsByCategory = savings.savingsByCategory,
            savingsByAccount = savings.savingsByAccount,
            savingsByCategorySpend = savings.savingsByCategorySpend,
            // Trends
            selectedTrendType = trends.selectedTrendType,
            trendTimeFilter = trends.trendTimeFilter,
            trendDataPoints = trends.trendDataPoints,
            trendStats = trends.trendStats,
            allTrendDataPoints = trends.allTrendDataPoints
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
            TimeFilter.MONTH -> newDate.add(Calendar.MONTH, offset)
            TimeFilter.YEAR -> newDate.add(Calendar.YEAR, offset)
            else -> {}
        }
        currentPeriodDate.value = newDate
    }

    fun setTrendType(type: TrendType) {
        selectedTrendType.value = type
    }

    fun setTrendTimeFilter(filter: TrendTimeFilter) {
        trendTimeFilter.value = filter
    }
}
