package com.moneymanager.app.ui.screens

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.BudgetEntity
import com.moneymanager.data.entity.RecurringEntity
import com.moneymanager.data.entity.TransactionEntity
import com.moneymanager.domain.repository.AccountRepository
import com.moneymanager.domain.repository.BudgetRepository
import com.moneymanager.domain.repository.RecurringRepository
import com.moneymanager.domain.repository.TransactionRepository
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.app.ui.components.PieChartEntry
import com.moneymanager.domain.repository.PeerContactRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
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

enum class DashboardType {
    OVERVIEW, EXPENSE, INCOME, ACCOUNTS, SAVINGS, BUDGET, LENDING
}

data class LendingDashboardSummary(
    val netPosition: Double,
    val totalOwedToMe: Double,
    val totalIOwe: Double,
    val partnersOwedToMe: List<PartnerSummary>,
    val partnersIOwe: List<PartnerSummary>
)

data class PartnerSummary(
    val peer: PeerContact,
    val principalAmount: Double,
    val repaidAmount: Double,
    val remainingBalance: Double,
    val percentage: Float,
    val isOwedToMe: Boolean,
    val nextDueDate: Long? = null
)

data class BudgetSummary(
    val totalBudget: Double,
    val totalSpent: Double,
    val utilization: Float,
    val balance: Double,
    val activeBudgets: Int,
    val underControl: Int,
    val overrun: Int
)

data class AccountPeriodSummary(
    val account: AccountEntity,
    val currentBalance: Double,
    val inflow: Double,
    val outflow: Double
)

data class BudgetWithProgress(
    val budget: BudgetEntity,
    val categoryName: String,
    val categoryEmoji: String,
    val categoryColor: Color,
    val spent: Double,
    val percentage: Float
)

data class SavingsDestination(
    val id: Long,
    val name: String,
    val emoji: String,
    val amountSaved: Double,
    val percentage: Float,
    val targetAmount: Double,
    val currentProgress: Double, // Total progress (all time)
    val periodContribution: Double,
    val isGoal: Boolean
)

data class PeriodSummary(
    val amount: Double,
    val prevAmount: Double,
    val percentChange: Double
)

data class DashboardUiState(
    val netWorth: Double = 0.0,
    val periodBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val totalSavings: Double = 0.0,
    val totalLending: Double = 0.0,
    val totalBorrowing: Double = 0.0,
    val netBorrowing: Double = 0.0,
    
    // Period summaries for tiles
    val incomeSummary: PeriodSummary = PeriodSummary(0.0, 0.0, 0.0),
    val expenseSummary: PeriodSummary = PeriodSummary(0.0, 0.0, 0.0),
    val savingsSummary: PeriodSummary = PeriodSummary(0.0, 0.0, 0.0),
    val lendingSummary: PeriodSummary = PeriodSummary(0.0, 0.0, 0.0),
    val borrowingSummary: PeriodSummary = PeriodSummary(0.0, 0.0, 0.0),
    val netBorrowingSummary: PeriodSummary = PeriodSummary(0.0, 0.0, 0.0),
    
    val savingsGrowth: Double = 0.0,
    val avgMonthlySavings: Double = 0.0,
    val savingsBreakdown: List<PieChartEntry> = emptyList(),
    val savingsDestinations: List<SavingsDestination> = emptyList(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val categories: List<com.moneymanager.data.entity.CategoryEntity> = emptyList(),
    val expenseBreakdown: List<PieChartEntry> = emptyList(),
    val incomeBreakdown: List<PieChartEntry> = emptyList(),
    val averageIncome: Double = 0.0,
    val dashboardType: DashboardType = DashboardType.EXPENSE,
    val accountSummaries: List<AccountPeriodSummary> = emptyList(),
    val totalInflow: Double = 0.0,
    val totalOutflow: Double = 0.0,
    val budgetSummary: BudgetSummary = BudgetSummary(0.0, 0.0, 0f, 0.0, 0, 0, 0),
    val lendingDashboardSummary: LendingDashboardSummary = LendingDashboardSummary(0.0, 0.0, 0.0, emptyList(), emptyList()),
    val isLoading: Boolean = true,
    val selectedFilter: TimeFilter = TimeFilter.MONTH,
    val filterDisplayDate: String = "",
    val customStartDate: Long? = null,
    val customEndDate: Long? = null,
    val selectedAccountIds: Set<Long> = emptySet(),
    val selectedCategoryIds: Set<Long> = emptySet(),
    val showPercentages: Boolean = true,
    val carryOver: Boolean = false,
    val selectedCategory: PieChartEntry? = null,
    val categoryTransactions: List<TransactionEntity> = emptyList(),
    val budgetsWithProgress: List<BudgetWithProgress> = emptyList(),
    val upcomingRecurring: List<RecurringEntity> = emptyList(),
    val currency: String = "INR",
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val recurringRepository: RecurringRepository,
    private val categoryRepository: com.moneymanager.domain.repository.CategoryRepository,
    private val goalRepository: com.moneymanager.domain.repository.GoalRepository,
    private val peerContactRepository: PeerContactRepository,
    private val preferencesManager: com.moneymanager.data.preferences.PreferencesManager
) : AndroidViewModel(application) {

    private val selectedFilter = MutableStateFlow(TimeFilter.MONTH)
    private val dashboardType = MutableStateFlow(DashboardType.OVERVIEW)
    private val currentPeriodDate = MutableStateFlow(Calendar.getInstance())
    private val customStartDate = MutableStateFlow<Long?>(null)
    private val customEndDate = MutableStateFlow<Long?>(null)
    private val selectedAccountIds = MutableStateFlow<Set<Long>>(emptySet())
    private val selectedCategoryIds = MutableStateFlow<Set<Long>>(emptySet())
    private val showPercentages = MutableStateFlow(true)
    private val carryOver = MutableStateFlow(false)
    private val selectedCategory = MutableStateFlow<PieChartEntry?>(null)

    fun getDateRangeForFilter(
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
            TimeFilter.CUSTOM -> "Custom Range"
        }
    }

    // Flow that emits the current filter state
    private val filterState = combine(
        selectedFilter,
        currentPeriodDate,
        customStartDate,
        customEndDate,
        selectedAccountIds,
        selectedCategoryIds
    ) { flows ->
        val filter = flows[0] as TimeFilter
        val baseDate = flows[1] as Calendar
        val start = flows[2] as Long?
        val end = flows[3] as Long?
        val accounts = flows[4] as Set<Long>
        val categories = flows[5] as Set<Long>
        FilterParams(filter, baseDate, start, end, accounts, categories)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilterParams(TimeFilter.MONTH, Calendar.getInstance(), null, null, emptySet(), emptySet())
    )

    data class FilterParams(
        val filter: TimeFilter,
        val baseDate: Calendar,
        val startDate: Long?,
        val endDate: Long?,
        val accountIds: Set<Long>,
        val categoryIds: Set<Long>
    )

    private val filteredTransactions: Flow<List<TransactionEntity>> = filterState.flatMapLatest { params ->
        val (rangeStart, rangeEnd) = getDateRangeForFilter(params.filter, params.baseDate, params.startDate, params.endDate)
        transactionRepository.getTransactionsByDateRange(rangeStart, rangeEnd)
            .map { transactions ->
                transactions.filter { tx ->
                    val accountMatch = params.accountIds.isEmpty() || tx.accountId in params.accountIds || (tx.toAccountId != null && tx.toAccountId in params.accountIds)
                    val categoryMatch = params.categoryIds.isEmpty() || tx.categoryId in params.categoryIds
                    accountMatch && categoryMatch
                }
            }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val categoryTransactionsFlow: Flow<List<TransactionEntity>> = combine(
        filterState,
        selectedCategory,
        categoryRepository.getAllCategories()
    ) { params, category, categories ->
        Triple(params, category, categories)
    }.flatMapLatest { (params, category, categories) ->
        if (category == null) {
            flowOf(emptyList())
        } else {
            val (rangeStart, rangeEnd) = getDateRangeForFilter(params.filter, params.baseDate, params.startDate, params.endDate)
            transactionRepository.getTransactionsByDateRange(rangeStart, rangeEnd)
                .map { transactions ->
                    transactions.filter { tx ->
                        val cat = categories.find { it.id == tx.categoryId }
                        val categoryName = cat?.name ?: "Other"
                        val accountMatch = params.accountIds.isEmpty() || tx.accountId in params.accountIds || (tx.toAccountId != null && tx.toAccountId in params.accountIds)
                        categoryName == category.label && accountMatch
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
        budgetRepository.getAllBudgets(),
        transactionRepository.getAllTransactions(),
        categoryRepository.getAllCategories(),
        filterState
    ) { allBudgets, allTransactions, categories, params ->
        val (monthStart, monthEnd) = getDateRangeForFilter(params.filter, params.baseDate, params.startDate, params.endDate)
        
        // Convert baseDate to "yyyy-MM" format for budget matching if filter is MONTH
        val budgetMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(params.baseDate.time)
        
        val periodTransactions = allTransactions.filter { it.date in monthStart..monthEnd && !it.isSplitChild }
        
        val filteredBudgets = if (params.filter == TimeFilter.MONTH) {
            allBudgets.filter { it.month == budgetMonth }
        } else {
            // If not month filter, we might want to aggregate or just show all for simplicity
            // But the requirement says "based on the period globally selected"
            // For now, let's stick to month matching if possible, or all if not.
            allBudgets
        }

        filteredBudgets.map { budget ->
            val category = categories.find { it.id == budget.categoryId }
            
            val spent = periodTransactions
                .filter { it.categoryId == budget.categoryId }
                .sumOf { it.amount }
                
            val percentage = if (budget.amount > 0) (spent / budget.amount * 100).toFloat() else 0f
            
            BudgetWithProgress(
                budget = budget,
                categoryName = category?.name ?: "Other",
                categoryEmoji = category?.emoji ?: "📁",
                categoryColor = com.moneymanager.app.ui.util.parseColor(category?.color ?: "#90A4AE"),
                spent = spent,
                percentage = percentage
            )
        }
    }

    private val budgetSummaryFlow: Flow<BudgetSummary> = budgetsWithProgressFlow.map { budgets ->
        val totalBudget = budgets.sumOf { it.budget.amount }
        val totalSpent = budgets.sumOf { it.spent }
        val utilization = if (totalBudget > 0) (totalSpent / totalBudget * 100).toFloat() else 0f
        val balance = (totalBudget - totalSpent).coerceAtLeast(0.0)
        val activeBudgets = budgets.size
        val underControl = budgets.count { it.spent <= it.budget.amount }
        val overrun = budgets.count { it.spent > it.budget.amount }
        
        BudgetSummary(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            utilization = utilization,
            balance = balance,
            activeBudgets = activeBudgets,
            underControl = underControl,
            overrun = overrun
        )
    }

    private val expensesByCategory: Flow<List<PieChartEntry>> = combine(
        filteredTransactions,
        categoryRepository.getAllCategories()
    ) { transactions, categories ->
        transactions
            .filter { it.type == "expense" && !it.isSplitChild }
            .groupBy { it.categoryId }
            .map { (categoryId, txs) ->
                val category = categories.find { it.id == categoryId }
                PieChartEntry(
                    label = category?.name ?: "Other",
                    value = txs.sumOf { it.amount },
                    color = com.moneymanager.app.ui.util.parseColor(category?.color ?: "#90A4AE")
                )
            }
            .sortedByDescending { it.value }
    }

    private val incomesByCategory: Flow<List<PieChartEntry>> = combine(
        filteredTransactions,
        categoryRepository.getAllCategories()
    ) { transactions, categories ->
        transactions
            .filter { it.type == "income" && !it.isSplitChild }
            .groupBy { it.categoryId }
            .map { (categoryId, txs) ->
                val category = categories.find { it.id == categoryId }
                PieChartEntry(
                    label = category?.name ?: "Other",
                    value = txs.sumOf { it.amount },
                    color = com.moneymanager.app.ui.util.parseColor(category?.color ?: "#00C853")
                )
            }
            .sortedByDescending { it.value }
    }

    private val averageIncomeFlow: Flow<Double> = currentPeriodDate.flatMapLatest { baseDate ->
        val endCal = baseDate.clone() as Calendar
        endCal[Calendar.DAY_OF_MONTH] = endCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        endCal[Calendar.HOUR_OF_DAY] = 23
        endCal[Calendar.MINUTE] = 59
        endCal[Calendar.SECOND] = 59
        
        val startCal = baseDate.clone() as Calendar
        startCal.add(Calendar.MONTH, -2) // Last 3 months including current
        startCal[Calendar.DAY_OF_MONTH] = 1
        startCal[Calendar.HOUR_OF_DAY] = 0
        startCal[Calendar.MINUTE] = 0
        startCal[Calendar.SECOND] = 0
        
        transactionRepository.getTransactionsByDateRange(startCal.timeInMillis, endCal.timeInMillis)
            .map { transactions ->
                val totalIncome = transactions
                    .filter { it.type == "income" && !it.isSplitChild }
                    .sumOf { it.amount }
                totalIncome / 3.0
            }
    }

    // Query upcoming recurring transactions respecting reminderDays
    private val upcomingRecurringFlow: Flow<List<RecurringEntity>> = recurringRepository.getActiveRecurring()
        .map { recurringList ->
            val now = System.currentTimeMillis()
            recurringList
                .filter { recurring ->
                    if (!recurring.reminderEnabled) return@filter false
                    val reminderMillis = recurring.reminderDays * 24 * 60 * 60 * 1000L
                    recurring.nextDate >= now && (recurring.nextDate - now) <= reminderMillis
                }
                .sortedBy { it.nextDate }
                .take(5)
        }

    private val cumulativeTransactions: Flow<List<TransactionEntity>> = filterState.flatMapLatest { params ->
        val (_, rangeEnd) = getDateRangeForFilter(params.filter, params.baseDate, params.startDate, params.endDate)
        transactionRepository.getTransactionsByDateRange(0, rangeEnd)
            .map { transactions ->
                transactions.filter { tx ->
                    val accountMatch = params.accountIds.isEmpty() || tx.accountId in params.accountIds || (tx.toAccountId != null && tx.toAccountId in params.accountIds)
                    val categoryMatch = params.categoryIds.isEmpty() || tx.categoryId in params.categoryIds
                    accountMatch && categoryMatch
                }
            }
    }

    // Query account summaries
    private val accountSummariesFlow: Flow<List<AccountPeriodSummary>> = combine(
        accountRepository.getAllAccounts(),
        filteredTransactions
    ) { accounts, transactions ->
        accounts.map { account ->
            val accountTxs = transactions.filter { it.accountId == account.id || it.toAccountId == account.id }
            val inflow = accountTxs.filter { 
                (it.type == "income" && it.accountId == account.id) || 
                (it.type == "transfer" && it.toAccountId == account.id) ||
                (it.type == "receive" && it.accountId == account.id)
            }.sumOf { it.amount }
            
            val outflow = accountTxs.filter { 
                (it.type == "expense" && it.accountId == account.id) || 
                (it.type == "transfer" && it.accountId == account.id) ||
                (it.type == "lend" && it.accountId == account.id)
            }.sumOf { it.amount }
            
            AccountPeriodSummary(
                account = account,
                currentBalance = account.balance,
                inflow = inflow,
                outflow = outflow
            )
        }
    }

    private val savingsDestinationsFlow: Flow<List<SavingsDestination>> = combine(
        goalRepository.getAllGoals(),
        accountRepository.getAllAccounts(),
        transactionRepository.getAllTransactions(),
        filterState
    ) { goals, accounts, allTransactions, params ->
        val (periodStart, periodEnd) = getDateRangeForFilter(params.filter, params.baseDate, params.startDate, params.endDate)
        
        // Goals-based savings
        val goalDestinations = goals.map { goal ->
            val periodContribution = allTransactions
                .filter { it.type == "savings" && it.goalId == goal.id && it.date in periodStart..periodEnd }
                .sumOf { it.amount }
            
            SavingsDestination(
                id = goal.id,
                name = goal.name,
                emoji = goal.emoji,
                amountSaved = periodContribution,
                percentage = 0f,
                targetAmount = goal.targetAmount,
                currentProgress = goal.currentAmount,
                periodContribution = periodContribution,
                isGoal = true
            )
        }
        
        // Account-based savings
        val savingsAccounts = accounts.filter { it.type.lowercase() == "savings" || it.type.lowercase() == "investment" }
        val accountDestinations = savingsAccounts.map { account ->
            val periodInflow = allTransactions
                .filter { (it.accountId == account.id || it.toAccountId == account.id) && it.date in periodStart..periodEnd }
                .filter { (it.type == "income" && it.accountId == account.id) || (it.type == "transfer" && it.toAccountId == account.id) }
                .sumOf { it.amount }
            
            val periodOutflow = allTransactions
                .filter { it.accountId == account.id && it.date in periodStart..periodEnd }
                .filter { it.type == "expense" || it.type == "transfer" }
                .sumOf { it.amount }
            
            val netPeriodSavings = (periodInflow - periodOutflow).coerceAtLeast(0.0)

            SavingsDestination(
                id = account.id,
                name = account.name,
                emoji = "💰",
                amountSaved = netPeriodSavings,
                percentage = 0f,
                targetAmount = 0.0,
                currentProgress = account.balance,
                periodContribution = netPeriodSavings,
                isGoal = false
            )
        }

        val allDestinations = (goalDestinations + accountDestinations)
            .filter { it.amountSaved > 0 || (it.isGoal && it.currentProgress > 0) }
            .sortedByDescending { it.amountSaved }
        
        val totalPeriodSavings = allDestinations.sumOf { it.amountSaved }
        
        allDestinations.map { 
            it.copy(percentage = if (totalPeriodSavings > 0) (it.amountSaved / totalPeriodSavings * 100).toFloat() else 0f)
        }
    }

    private val savingsStatsFlow: Flow<Triple<Double, Double, Double>> = combine(
        transactionRepository.getAllTransactions(),
        filterState
    ) { allTransactions, params ->
        val (periodStart, periodEnd) = getDateRangeForFilter(params.filter, params.baseDate, params.startDate, params.endDate)
        
        val currentPeriodSavings = allTransactions
            .filter { it.date in periodStart..periodEnd && (it.type == "savings" || it.type == "investment") }
            .sumOf { it.amount }
            
        val prevBaseDate = params.baseDate.clone() as Calendar
        prevBaseDate.add(Calendar.MONTH, -1)
        val (prevStart, prevEnd) = getDateRangeForFilter(params.filter, prevBaseDate, null, null)
        
        val prevPeriodSavings = allTransactions
            .filter { it.date in prevStart..prevEnd && (it.type == "savings" || it.type == "investment") }
            .sumOf { it.amount }
            
        val growth = if (prevPeriodSavings > 0) ((currentPeriodSavings - prevPeriodSavings) / prevPeriodSavings * 100) else 0.0
        
        val sixMonthsAgo = Calendar.getInstance().apply { add(Calendar.MONTH, -6) }.timeInMillis
        val lastSixMonthsTxs = allTransactions.filter { it.date >= sixMonthsAgo && (it.type == "savings" || it.type == "investment") }
        val avgMonthly = lastSixMonthsTxs.sumOf { it.amount } / 6.0
        
        Triple(currentPeriodSavings, growth, avgMonthly)
    }

    private val lendingDashboardSummaryFlow: Flow<LendingDashboardSummary> = combine(
        peerContactRepository.getAllPeers(),
        transactionRepository.getAllTransactions(),
        filterState
    ) { peers, allTransactions, params ->
        val (periodStart, periodEnd) = getDateRangeForFilter(params.filter, params.baseDate, params.startDate, params.endDate)
        
        val filteredTxs = allTransactions.filter { it.date in periodStart..periodEnd }
        
        val partnerSummaries = peers.map { peer ->
            val peerTxs = filteredTxs.filter { it.peerContactId == peer.id }
            
            val lent = peerTxs.filter { it.type == "lend" }.sumOf { it.amount }
            val received = peerTxs.filter { it.type == "receive" }.sumOf { it.amount }
            val borrowed = peerTxs.filter { it.type == "borrow" }.sumOf { it.amount }
            val repaid = peerTxs.filter { it.type == "repay" }.sumOf { it.amount }
            
            val isOwedToMe = (lent + received) > (borrowed + repaid) || (peer.totalGiven > peer.totalReceived)
            
            val principal = if (isOwedToMe) {
                peerTxs.filter { it.type == "lend" }.sumOf { it.amount }.let { if (it == 0.0) peer.totalGiven else it }
            } else {
                peerTxs.filter { it.type == "borrow" }.sumOf { it.amount }.let { if (it == 0.0) (peer.totalReceived - peer.totalGiven).coerceAtLeast(0.0) else it }
            }
            
            val settled = if (isOwedToMe) {
                peerTxs.filter { it.type == "receive" }.sumOf { it.amount }
            } else {
                peerTxs.filter { it.type == "repay" }.sumOf { it.amount }
            }
            
            val balance = if (isOwedToMe) {
                (peer.totalGiven - peer.totalReceived).coerceAtLeast(0.0)
            } else {
                (peer.totalReceived - peer.totalGiven).coerceAtLeast(0.0)
            }
            
            val progress = if (principal > 0) (settled / principal * 100).toFloat() else 0f
            
            val nextDue = peerTxs.filter { it.expectedReturnDate != null && it.expectedReturnDate!! > System.currentTimeMillis() }
                .minByOrNull { it.expectedReturnDate!! }?.expectedReturnDate

            PartnerSummary(
                peer = peer,
                principalAmount = principal,
                repaidAmount = settled,
                remainingBalance = balance,
                percentage = progress.coerceIn(0f, 100f),
                isOwedToMe = isOwedToMe,
                nextDueDate = nextDue
            )
        }.filter { it.principalAmount > 0 || it.remainingBalance > 0 }

        val owedToMeList = partnerSummaries.filter { it.isOwedToMe }.sortedByDescending { it.remainingBalance }
        val iOweList = partnerSummaries.filter { !it.isOwedToMe }.sortedByDescending { it.remainingBalance }
        
        val totalOwedToMe = owedToMeList.sumOf { it.remainingBalance }
        val totalIOwe = iOweList.sumOf { it.remainingBalance }
        
        LendingDashboardSummary(
            netPosition = totalOwedToMe - totalIOwe,
            totalOwedToMe = totalOwedToMe,
            totalIOwe = totalIOwe,
            partnersOwedToMe = owedToMeList,
            partnersIOwe = iOweList
        )
    }

    private fun calculatePeriodSummary(current: Double, prev: Double): PeriodSummary {
        val diff = current - prev
        val percent = if (prev != 0.0) (diff / prev) * 100 else if (current != 0.0) 100.0 else 0.0
        return PeriodSummary(current, prev, percent)
    }

    private val overviewFlow: Flow<DashboardUiState> = combine(
        transactionRepository.getAllTransactions(),
        filterState
    ) { allTransactions, params ->
        val (periodStart, periodEnd) = getDateRangeForFilter(params.filter, params.baseDate, params.startDate, params.endDate)
        
        val prevBaseDate = params.baseDate.clone() as Calendar
        when (params.filter) {
            TimeFilter.DAY -> prevBaseDate.add(Calendar.DAY_OF_YEAR, -1)
            TimeFilter.WEEK -> prevBaseDate.add(Calendar.WEEK_OF_YEAR, -1)
            TimeFilter.MONTH -> prevBaseDate.add(Calendar.MONTH, -1)
            TimeFilter.YEAR -> prevBaseDate.add(Calendar.YEAR, -1)
            else -> prevBaseDate.add(Calendar.MONTH, -1)
        }
        val (prevStart, prevEnd) = getDateRangeForFilter(params.filter, prevBaseDate, null, null)

        val currentTxs = allTransactions.filter { tx ->
            val inDate = tx.date in periodStart..periodEnd && !tx.isSplitChild
            val accountMatch = params.accountIds.isEmpty() || tx.accountId in params.accountIds || (tx.toAccountId != null && tx.toAccountId in params.accountIds)
            val categoryMatch = params.categoryIds.isEmpty() || tx.categoryId in params.categoryIds
            inDate && accountMatch && categoryMatch
        }
        val prevTxs = allTransactions.filter { tx ->
            val inDate = tx.date in prevStart..prevEnd && !tx.isSplitChild
            val accountMatch = params.accountIds.isEmpty() || tx.accountId in params.accountIds || (tx.toAccountId != null && tx.toAccountId in params.accountIds)
            val categoryMatch = params.categoryIds.isEmpty() || tx.categoryId in params.categoryIds
            inDate && accountMatch && categoryMatch
        }

        fun getMetrics(txs: List<TransactionEntity>): Map<String, Double> {
            val income = txs.filter { it.type == "income" }.sumOf { it.amount }
            val expense = txs.filter { it.type == "expense" }.sumOf { it.amount }
            val savings = txs.filter { it.type == "savings" || it.type == "investment" }.sumOf { it.amount }
            val lend = txs.filter { it.type == "lend" }.sumOf { it.amount }
            val receive = txs.filter { it.type == "receive" }.sumOf { it.amount }
            val borrow = txs.filter { it.type == "borrow" }.sumOf { it.amount } // Assuming "borrow" type exists or added
            val repay = txs.filter { it.type == "repay" }.sumOf { it.amount } // Assuming "repay" type exists or added
            
            // Lending Metric: money lent minus money received for new loans (implied by "receive")
            val netLending = lend - receive
            // Borrowing Metric: money borrowed minus money repaid for new loans
            // Since "borrow" and "repay" might not be in the enum yet, we check VALID_TYPES.
            // Let's assume for now "lend" is positive (outflow) and "receive" is negative (inflow) in effect.
            // Request said: Lending Tile: Total Money Lent (net, money lent minus money received).
            
            return mapOf(
                "income" to income,
                "expense" to expense,
                "savings" to savings,
                "lend" to lend,
                "receive" to receive,
                "borrow" to borrow,
                "repay" to repay,
                "netLending" to netLending,
                "netBorrowing" to (borrow - repay)
            )
        }

        val curM = getMetrics(currentTxs)
        val preM = getMetrics(prevTxs)

        DashboardUiState(
            totalIncome = curM["income"] ?: 0.0,
            totalExpense = curM["expense"] ?: 0.0,
            totalSavings = curM["savings"] ?: 0.0,
            totalLending = curM["netLending"] ?: 0.0,
            totalBorrowing = curM["borrow"] ?: 0.0,
            netBorrowing = curM["netBorrowing"] ?: 0.0,
            
            incomeSummary = calculatePeriodSummary(curM["income"] ?: 0.0, preM["income"] ?: 0.0),
            expenseSummary = calculatePeriodSummary(curM["expense"] ?: 0.0, preM["expense"] ?: 0.0),
            savingsSummary = calculatePeriodSummary(curM["savings"] ?: 0.0, preM["savings"] ?: 0.0),
            lendingSummary = calculatePeriodSummary(curM["netLending"] ?: 0.0, preM["netLending"] ?: 0.0),
            borrowingSummary = calculatePeriodSummary(curM["borrow"] ?: 0.0, preM["borrow"] ?: 0.0),
            netBorrowingSummary = calculatePeriodSummary(curM["netBorrowing"] ?: 0.0, preM["netBorrowing"] ?: 0.0)
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getTotalBalance(),
        filteredTransactions,
        transactionRepository.getRecentTransactions(10),
        accountRepository.getAllAccounts(),
        categoryRepository.getAllCategories(),
        filterState,
        selectedCategory,
        categoryTransactionsFlow,
        budgetsWithProgressFlow,
        upcomingRecurringFlow,
        expensesByCategory,
        incomesByCategory,
        averageIncomeFlow,
        dashboardType,
        preferencesManager.currency,
        showPercentages,
        carryOver,
        cumulativeTransactions,
        accountSummariesFlow,
        savingsDestinationsFlow,
        savingsStatsFlow,
        overviewFlow,
        budgetSummaryFlow,
        lendingDashboardSummaryFlow
    ) { values ->
        val accountBalance = values[0] as Double
        @Suppress("UNCHECKED_CAST")
        val transactions = values[1] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val recentTransactions = values[2] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val accounts = values[3] as List<AccountEntity>
        @Suppress("UNCHECKED_CAST")
        val categories = values[4] as List<com.moneymanager.data.entity.CategoryEntity>
        val filterParams = values[5] as FilterParams
        val category = values[6] as PieChartEntry?
        @Suppress("UNCHECKED_CAST")
        val categoryTransactions = values[7] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val budgets = values[8] as List<BudgetWithProgress>
        @Suppress("UNCHECKED_CAST")
        val recurring = values[9] as List<RecurringEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[10] as List<PieChartEntry>
        @Suppress("UNCHECKED_CAST")
        val incomes = values[11] as List<PieChartEntry>
        val avgIncome = values[12] as Double
        val dashType = values[13] as DashboardType
        val curr = values[14] as String
        val showPct = values[15] as Boolean
        val carry = values[16] as Boolean
        @Suppress("UNCHECKED_CAST")
        val cumTransactions = values[17] as List<TransactionEntity>
        @Suppress("UNCHECKED_CAST")
        val accountSummaries = values[18] as List<AccountPeriodSummary>
        @Suppress("UNCHECKED_CAST")
        val savingsDestinations = values[19] as List<SavingsDestination>
        @Suppress("UNCHECKED_CAST")
        val savingsStats = values[20] as Triple<Double, Double, Double>
        val overview = values[21] as DashboardUiState
        val budgetSummary = values[22] as BudgetSummary
        val lendingSummary = values[23] as LendingDashboardSummary

        val netWorth = accountBalance
        val monthTransactions = transactions.filter { !it.isSplitChild }
        val recentTx = recentTransactions.filter { !it.isSplitChild }
        
        val totalInflow = monthTransactions.filter { 
            it.type == "income" || it.type == "receive" || (it.type == "transfer" && it.toAccountId != null)
        }.sumOf { it.amount }
        
        val totalOutflow = monthTransactions.filter { 
            it.type == "expense" || it.type == "lend" || (it.type == "transfer" && it.toAccountId != null)
        }.sumOf { it.amount }

        val totalIncome = monthTransactions.filter { it.type == "income" }.sumOf { it.amount }
        val totalExpense = monthTransactions.filter { it.type == "expense" }.sumOf { it.amount }

        val periodBalance = if (carry) {
            val cumIncome = cumTransactions.filter { it.type == "income" && !it.isSplitChild }.sumOf { it.amount }
            val cumExpense = cumTransactions.filter { it.type == "expense" && !it.isSplitChild }.sumOf { it.amount }
            cumIncome - cumExpense
        } else {
            totalIncome - totalExpense
        }

        val savingsBreakdown = savingsDestinations.map { 
            PieChartEntry(
                label = it.name,
                value = it.amountSaved,
                color = com.moneymanager.app.ui.util.generateDistinctColor(it.id.toInt())
            )
        }

        DashboardUiState(
            netWorth = netWorth,
            periodBalance = periodBalance,
            totalIncome = if (dashType == DashboardType.OVERVIEW) overview.totalIncome else totalIncome,
            totalExpense = if (dashType == DashboardType.OVERVIEW) overview.totalExpense else totalExpense,
            totalSavings = if (dashType == DashboardType.OVERVIEW) overview.totalSavings else savingsStats.first,
            totalLending = overview.totalLending,
            totalBorrowing = overview.totalBorrowing,
            netBorrowing = overview.netBorrowing,
            incomeSummary = overview.incomeSummary,
            expenseSummary = overview.expenseSummary,
            savingsSummary = overview.savingsSummary,
            lendingSummary = overview.lendingSummary,
            borrowingSummary = overview.borrowingSummary,
            netBorrowingSummary = overview.netBorrowingSummary,
            savingsGrowth = savingsStats.second,
            avgMonthlySavings = savingsStats.third,
            savingsBreakdown = savingsBreakdown,
            savingsDestinations = savingsDestinations,
            recentTransactions = recentTx,
            accounts = accounts,
            categories = categories,
            expenseBreakdown = expenses,
            incomeBreakdown = incomes,
            averageIncome = avgIncome,
            dashboardType = dashType,
            accountSummaries = accountSummaries,
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            budgetSummary = budgetSummary,
            lendingDashboardSummary = lendingSummary,
            isLoading = false,
            selectedFilter = filterParams.filter,
            filterDisplayDate = getFilterDisplayDate(filterParams.filter, filterParams.baseDate),
            customStartDate = filterParams.startDate,
            customEndDate = filterParams.endDate,
            selectedAccountIds = filterParams.accountIds,
            selectedCategoryIds = filterParams.categoryIds,
            showPercentages = showPct,
            carryOver = carry,
            selectedCategory = category,
            categoryTransactions = categoryTransactions,
            budgetsWithProgress = budgets,
            upcomingRecurring = recurring,
            currency = curr
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

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

    fun applyFilters(
        filter: TimeFilter,
        accountIds: Set<Long>,
        categoryIds: Set<Long>,
        showPercentages: Boolean,
        carryOver: Boolean
    ) {
        selectedFilter.value = filter
        selectedAccountIds.value = accountIds
        selectedCategoryIds.value = categoryIds
        this.showPercentages.value = showPercentages
        this.carryOver.value = carryOver
    }

    fun setTimeFilter(filter: TimeFilter) {
        selectedFilter.value = filter
    }

    fun setDashboardType(type: DashboardType) {
        dashboardType.value = type
    }

    fun setCustomDateRange(startDate: Long, endDate: Long) {
        customStartDate.value = startDate
        customEndDate.value = endDate
    }

    fun selectCategory(entry: PieChartEntry?) {
        selectedCategory.value = entry
    }


    fun transferMoney(fromAccountId: Long, toAccountId: Long, amount: Double, note: String, date: Long) {
        viewModelScope.launch {
            val fromAccount = accountRepository.getAccountById(fromAccountId)
            val toAccount = accountRepository.getAccountById(toAccountId)
            
            if (fromAccount != null && toAccount != null) {
                // Dual-entry transfer: Create OUT transaction
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = fromAccountId,
                        toAccountId = toAccountId,
                        type = "transfer",
                        isTransfer = true,
                        amount = amount,
                        note = note.ifEmpty { "Transfer to ${toAccount.name}" },
                        date = date
                    )
                )
                // Dual-entry transfer: Create IN transaction
                transactionRepository.insertTransaction(
                    TransactionEntity(
                        accountId = toAccountId,
                        toAccountId = fromAccountId,
                        type = "transfer",
                        isTransfer = true,
                        amount = amount,
                        note = note.ifEmpty { "Transfer from ${fromAccount.name}" },
                        date = date
                    )
                )
                // accountRepository.updateAccountBalance(fromAccountId, -amount)
                // accountRepository.updateAccountBalance(toAccountId, amount)
            }
        }
    }
}
