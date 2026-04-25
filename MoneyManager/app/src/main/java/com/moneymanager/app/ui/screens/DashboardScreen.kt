package com.moneymanager.app.ui.screens
import androidx.compose.ui.text.style.TextOverflow
import com.moneymanager.app.ui.util.CurrencyUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.components.*
import com.moneymanager.data.entity.TransactionEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToTransactions: (type: String?, accountId: Long?, startDate: Long?, endDate: Long?, goalId: Long?, categoryId: Long?, peerId: Long?) -> Unit,
    onNavigateToBorrowLend: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember(uiState.currency) {
        CurrencyUtils.getCurrencyFormat(uiState.currency)
    }
    val dateFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    var showFabMenu by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showDisplayOptions by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { expanded = true }
                        ) {
                            Text(
                                text = when (uiState.dashboardType) {
                                    DashboardType.OVERVIEW -> "Main Overview"
                                    DashboardType.EXPENSE -> "Expense Summary"
                                    DashboardType.INCOME -> "Income Summary"
                                    DashboardType.ACCOUNTS -> "Accounts Summary"
                                    DashboardType.SAVINGS -> "Savings Summary"
                                    DashboardType.BUDGET -> "Budget Summary"
                                    DashboardType.LENDING -> "Lending Summary"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Main Overview") },
                                onClick = {
                                    onNavigateToTransactions(null, null, null, null, null, null, null)
                                    showFabMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Expense Summary") },
                                onClick = {
                                    viewModel.setDashboardType(DashboardType.EXPENSE)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Income Summary") },
                                onClick = {
                                    viewModel.setDashboardType(DashboardType.INCOME)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Accounts Summary") },
                                onClick = {
                                    viewModel.setDashboardType(DashboardType.ACCOUNTS)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Savings Summary") },
                                onClick = {
                                    viewModel.setDashboardType(DashboardType.SAVINGS)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Budget Summary") },
                                onClick = {
                                    viewModel.setDashboardType(DashboardType.BUDGET)
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Lending Summary") },
                                onClick = {
                                    viewModel.setDashboardType(DashboardType.LENDING)
                                    expanded = false
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { showDisplayOptions = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Display Options")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (showFabMenu) {
                    SmallFloatingActionButton(
                        onClick = {
                            onNavigateToAccounts()
                            showFabMenu = false
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = "Add Account")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    SmallFloatingActionButton(
                        onClick = {
                            onNavigateToTransactions(null, null, null, null, null, null, null)
                            showFabMenu = false
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = "Add Transaction")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    SmallFloatingActionButton(
                        onClick = {
                            showTransferDialog = true
                            showFabMenu = false
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer")
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    SmallFloatingActionButton(
                        onClick = {
                            onNavigateToBorrowLend()
                            showFabMenu = false
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Borrow/Lend")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                FloatingActionButton(
                    onClick = { showFabMenu = !showFabMenu }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    PeriodNavigation(
                        displayDate = uiState.filterDisplayDate,
                        onPrevious = { viewModel.navigatePeriod(-1) },
                        onNext = { viewModel.navigatePeriod(1) }
                    )
                }

                item {
                    when (uiState.dashboardType) {
                        DashboardType.OVERVIEW -> {
                            OverviewDashboard(
                                uiState = uiState,
                                currencyFormat = currencyFormat
                            )
                        }
                        DashboardType.INCOME -> {
                            IncomeHeader(
                                totalIncome = uiState.totalIncome,
                                averageIncome = uiState.averageIncome,
                                period = uiState.filterDisplayDate,
                                currencyFormat = currencyFormat
                            )
                        }
                        DashboardType.ACCOUNTS -> {
                            AccountsHeader(
                                netBalance = uiState.periodBalance,
                                totalInflow = uiState.totalInflow,
                                totalOutflow = uiState.totalOutflow,
                                period = uiState.filterDisplayDate,
                                currencyFormat = currencyFormat
                            )
                        }
                        DashboardType.EXPENSE -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                KPIItem(
                                    label = "EXPENSE",
                                    value = currencyFormat.format(uiState.totalExpense),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                                KPIItem(
                                    label = "INCOME",
                                    value = currencyFormat.format(uiState.totalIncome),
                                    color = Color(0xFF00C853),
                                    modifier = Modifier.weight(1f)
                                )
                                KPIItem(
                                    label = "BALANCE",
                                    value = currencyFormat.format(uiState.periodBalance),
                                    color = if (uiState.periodBalance >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        DashboardType.SAVINGS -> {
                            SavingsHeader(
                                totalSavings = uiState.totalSavings,
                                savingsGrowth = uiState.savingsGrowth,
                                avgMonthlySavings = uiState.avgMonthlySavings,
                                period = uiState.filterDisplayDate,
                                currencyFormat = currencyFormat
                            )
                        }
                        DashboardType.BUDGET -> {
                            BudgetSummaryKPIs(
                                summary = uiState.budgetSummary,
                                currencyFormat = currencyFormat
                            )
                        }
                        DashboardType.LENDING -> {
                            LendingSummaryKPIs(
                                summary = uiState.lendingDashboardSummary,
                                currencyFormat = currencyFormat
                            )
                        }
                    }
                }

                if (uiState.dashboardType == DashboardType.LENDING) {
                    item {
                        LendingPartnerList(
                            summary = uiState.lendingDashboardSummary,
                            currencyFormat = currencyFormat,
                            onPartnerClick = { partner ->
                                val (startDate, endDate) = viewModel.getDateRangeForFilter(
                                    uiState.selectedFilter,
                                    Calendar.getInstance(),
                                    uiState.customStartDate,
                                    uiState.customEndDate
                                )
                                // Assuming we can filter transactions by peerContactId in the future or via note search
                                // For now, navigate to transactions screen
                                onNavigateToTransactions(
                                    null,
                                    null,
                                    startDate,
                                    endDate,
                                    null,
                                    null,
                                    partner.peer.id
                                )
                            }
                        )
                    }
                }

                if (uiState.dashboardType == DashboardType.BUDGET) {
                    item {
                        BudgetDetailedList(
                            budgets = uiState.budgetsWithProgress,
                            currencyFormat = currencyFormat,
                            onBudgetClick = { budgetProgress ->
                                val (startDate, endDate) = viewModel.getDateRangeForFilter(
                                    uiState.selectedFilter,
                                    Calendar.getInstance(),
                                    uiState.customStartDate,
                                    uiState.customEndDate
                                )
                                onNavigateToTransactions(
                                    "expense",
                                    null,
                                    startDate,
                                    endDate,
                                    null,
                                    budgetProgress.budget.categoryId,
                                    null
                                )
                            }
                        )
                    }
                }

                if (uiState.dashboardType == DashboardType.OVERVIEW) {
                    item {
                        OverviewComparisonTable(
                            uiState = uiState,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                if (uiState.dashboardType == DashboardType.ACCOUNTS) {
                    item {
                        val (startDate, endDate) = viewModel.getDateRangeForFilter(
                            uiState.selectedFilter,
                            Calendar.getInstance(),
                            uiState.customStartDate,
                            uiState.customEndDate
                        )
                        AccountsSummaryWidget(
                            accountSummaries = uiState.accountSummaries,
                            currencyFormat = currencyFormat,
                            onAccountClick = { accountSummary ->
                                onNavigateToTransactions(
                                    null,
                                    accountSummary.account.id,
                                    startDate,
                                    endDate,
                                    null,
                                    null,
                                    null
                                )
                            }
                        )
                    }
                }

                if (uiState.dashboardType == DashboardType.SAVINGS) {
                    item {
                        val (startDate, endDate) = viewModel.getDateRangeForFilter(
                            uiState.selectedFilter,
                            Calendar.getInstance(),
                            uiState.customStartDate,
                            uiState.customEndDate
                        )
                        SavingsSummaryWidget(
                            destinations = uiState.savingsDestinations,
                            currencyFormat = currencyFormat,
                            onDestinationClick = { dest ->
                                onNavigateToTransactions(
                                    null,
                                    if (dest.isGoal) null else dest.id,
                                    startDate,
                                    endDate,
                                    if (dest.isGoal) dest.id else null,
                                    null,
                                    null
                                )
                            }
                        )
                    }
                }

                val breakdown = when (uiState.dashboardType) {
                    DashboardType.EXPENSE -> uiState.expenseBreakdown
                    DashboardType.INCOME -> uiState.incomeBreakdown
                    DashboardType.SAVINGS -> uiState.savingsBreakdown
                    else -> emptyList()
                }
                val totalAmount = when (uiState.dashboardType) {
                    DashboardType.EXPENSE -> uiState.totalExpense
                    DashboardType.INCOME -> uiState.totalIncome
                    DashboardType.SAVINGS -> uiState.totalSavings
                    else -> 0.0
                }
                val summaryTitle = when (uiState.dashboardType) {
                    DashboardType.EXPENSE -> "Expense Overview"
                    DashboardType.INCOME -> "Income Overview"
                    DashboardType.SAVINGS -> "Savings Allocation"
                    else -> ""
                }
                val centerLabel = when (uiState.dashboardType) {
                    DashboardType.EXPENSE -> "Expenses"
                    DashboardType.INCOME -> "Incomes"
                    DashboardType.SAVINGS -> "Savings"
                    else -> ""
                }

                if (breakdown.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = summaryTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                ExpensePieChart(
                                    entries = breakdown,
                                    currencyFormat = currencyFormat,
                                    onCategoryClick = { entry -> viewModel.selectCategory(entry) },
                                    centerLabel = centerLabel
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                // Prioritized vertical list of categories
                                breakdown.forEach { entry ->
                                    val category = uiState.categories.find { it.name == entry.label }
                                    CategoryProgressItem(
                                        entry = entry,
                                        total = totalAmount,
                                        currencyFormat = currencyFormat,
                                        showPercentage = uiState.showPercentages,
                                        icon = category?.emoji ?: "📁",
                                        isExpense = uiState.dashboardType == DashboardType.EXPENSE,
                                        onClick = { 
                                            viewModel.selectCategory(entry)
                                            // Navigation logic would go here if not using drilldown panel
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }

                if (uiState.budgetsWithProgress.isNotEmpty()) {
                    item {
                        BudgetWidget(
                            budgetsWithProgress = uiState.budgetsWithProgress,
                            currencyFormat = currencyFormat,
                            periodName = uiState.filterDisplayDate
                        )
                    }
                }

                if (uiState.upcomingRecurring.isNotEmpty()) {
                    item {
                        RemindersWidget(
                            upcomingRecurring = uiState.upcomingRecurring,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                item {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.recentTransactions.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No transactions yet",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentTransactions) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            dateFormat = dateFormat,
                            currencyFormat = currencyFormat
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = uiState.accounts,
            onDismiss = { showTransferDialog = false },
            onTransfer = { fromId, toId, amount, note, date ->
                viewModel.transferMoney(fromId, toId, amount, note, date)
                showTransferDialog = false
            }
        )
    }

    val selectedCategory = uiState.selectedCategory
    if (selectedCategory != null) {
        CategoryDrilldownPanel(
            categoryName = selectedCategory.label,
            categoryColor = selectedCategory.color,
            transactions = uiState.categoryTransactions,
            dateFormat = dateFormat,
            currencyFormat = currencyFormat,
            onDismiss = { viewModel.selectCategory(null) }
        )
    }

    if (showDisplayOptions) {
        DisplayOptionsSheet(
            selectedFilter = uiState.selectedFilter,
            accounts = uiState.accounts,
            selectedAccountIds = uiState.selectedAccountIds,
            categories = uiState.categories,
            selectedCategoryIds = uiState.selectedCategoryIds,
            showPercentages = uiState.showPercentages,
            carryOver = uiState.carryOver,
            onDismiss = { showDisplayOptions = false },
            onApply = { filter, accounts, cats, pct, carry ->
                viewModel.applyFilters(filter, accounts, cats, pct, carry)
                showDisplayOptions = false
            }
        )
    }
}

@Composable
fun PeriodNavigation(
    displayDate: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }
        Text(
            text = displayDate,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
    }
}

@Composable
fun OverviewDashboard(
    uiState: DashboardUiState,
    currencyFormat: NumberFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryTile(
                label = "Income",
                summary = uiState.incomeSummary,
                currencyFormat = currencyFormat,
                isPositiveGood = true,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label = "Expenses",
                summary = uiState.expenseSummary,
                currencyFormat = currencyFormat,
                isPositiveGood = false,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryTile(
                label = "Savings",
                summary = uiState.savingsSummary,
                currencyFormat = currencyFormat,
                isPositiveGood = true,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label = "Net Lending",
                summary = uiState.lendingSummary,
                currencyFormat = currencyFormat,
                isPositiveGood = true,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryTile(
                label = "Borrowing",
                summary = uiState.borrowingSummary,
                currencyFormat = currencyFormat,
                isPositiveGood = false,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                label = "Net Borrowing",
                summary = uiState.netBorrowingSummary,
                currencyFormat = currencyFormat,
                isPositiveGood = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SummaryTile(
    label: String,
    summary: PeriodSummary,
    currencyFormat: NumberFormat,
    isPositiveGood: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currencyFormat.format(summary.amount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            val isIncrease = summary.percentChange >= 0
            val color = if (isIncrease) {
                if (isPositiveGood) Color(0xFF00C853) else MaterialTheme.colorScheme.error
            } else {
                if (isPositiveGood) MaterialTheme.colorScheme.error else Color(0xFF00C853)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isIncrease) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = color
                )
                Text(
                    text = "${"%.1f".format(Locale.getDefault(), kotlin.math.abs(summary.percentChange))}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = " vs prev",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun OverviewComparisonTable(
    uiState: DashboardUiState,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Period Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Metric",
                    modifier = Modifier.weight(1.2f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Previous",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                Text(
                    text = "Current",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
            
            val metrics = listOf(
                "Income" to uiState.incomeSummary,
                "Expenses" to uiState.expenseSummary,
                "Savings" to uiState.savingsSummary,
                "Net Lending" to uiState.lendingSummary,
                "Borrowing" to uiState.borrowingSummary,
                "Net Borrowing" to uiState.netBorrowingSummary
            )
            
            metrics.forEach { (name, summary) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = name,
                        modifier = Modifier.weight(1.2f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = currencyFormat.format(summary.prevAmount),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                    Text(
                        text = currencyFormat.format(summary.amount),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun KPIItem(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun IncomeHeader(
    totalIncome: Double,
    averageIncome: Double,
    period: String,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.weight(1.5f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Total Income",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormat.format(totalIncome),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00C853) // Emerald Green
                )
                Text(
                    text = "for $period",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Average Income\n(Last 3 Months):",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormat.format(averageIncome),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CategoryProgressItem(
    entry: PieChartEntry,
    total: Double,
    currencyFormat: NumberFormat,
    showPercentage: Boolean,
    icon: String,
    isExpense: Boolean,
    onClick: () -> Unit
) {
    val percentage = if (total > 0) (entry.value / total).toFloat() else 0f
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(entry.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = icon, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = entry.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isExpense) "-${currencyFormat.format(entry.value)}" else "+${currencyFormat.format(entry.value)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpense) MaterialTheme.colorScheme.error else Color(0xFF00C853)
                )
                if (showPercentage) {
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f%%", percentage * 100),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        LinearProgressIndicator(
            progress = { percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = entry.color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun AccountsHeader(
    netBalance: Double,
    totalInflow: Double,
    totalOutflow: Double,
    period: String,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Net Balance",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = currencyFormat.format(netBalance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (netBalance >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
            )
            Text(
                text = "for $period",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Inflow",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(totalInflow),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Outflow",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(totalOutflow),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsHeader(
    totalSavings: Double,
    savingsGrowth: Double,
    avgMonthlySavings: Double,
    period: String,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Total Savings ($period)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = currencyFormat.format(totalSavings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MoM Growth",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (savingsGrowth >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (savingsGrowth >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${if (savingsGrowth >= 0) "+" else ""}${String.format(Locale.getDefault(), "%.1f", savingsGrowth)}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (savingsGrowth >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "6-Month Avg",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = currencyFormat.format(avgMonthlySavings),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsSummaryWidget(
    destinations: List<SavingsDestination>,
    currencyFormat: NumberFormat,
    onDestinationClick: (SavingsDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Savings Breakdown",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            destinations.forEach { destination ->
                SavingsDestinationItem(
                    destination = destination,
                    currencyFormat = currencyFormat,
                    onClick = { onDestinationClick(destination) }
                )
                if (destination != destinations.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsDestinationItem(
    destination: SavingsDestination,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = destination.emoji)
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = destination.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (destination.isGoal) {
                LinearProgressIndicator(
                    progress = { (destination.currentProgress / destination.targetAmount).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = currencyFormat.format(destination.amountSaved),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${String.format(Locale.getDefault(), "%.1f", destination.percentage)}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AccountsSummaryWidget(
    accountSummaries: List<AccountPeriodSummary>,
    currencyFormat: NumberFormat,
    onAccountClick: (AccountPeriodSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Accounts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))

            accountSummaries.forEach { summary ->
                AccountListItem(
                    summary = summary,
                    currencyFormat = currencyFormat,
                    onClick = { onAccountClick(summary) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun AccountListItem(
    summary: AccountPeriodSummary,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = summary.account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Balance: ${currencyFormat.format(summary.currentBalance)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val net = summary.inflow - summary.outflow
                Text(
                    text = if (net >= 0) "+${currencyFormat.format(net)}" else currencyFormat.format(net),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (net >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Stacked horizontal bar chart for Inflow vs Outflow
        val total = summary.inflow + summary.outflow
        if (total > 0) {
            val inflowWeight = (summary.inflow / total).toFloat()
            val outflowWeight = (summary.outflow / total).toFloat()
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (inflowWeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(inflowWeight)
                            .background(Color(0xFF00C853))
                    )
                }
                if (outflowWeight > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(outflowWeight)
                            .background(MaterialTheme.colorScheme.error)
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetSummaryKPIs(
    summary: BudgetSummary,
    currencyFormat: NumberFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.weight(1.2f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Overall Budget",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(summary.totalBudget),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier.weight(1.5f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overall Current Utilization",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = currencyFormat.format(summary.totalSpent),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "(${String.format(Locale.getDefault(), "%.0f", summary.utilization)}% utilization)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (summary.utilization > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { (summary.utilization / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                        color = if (summary.utilization > 100) MaterialTheme.colorScheme.error else Color(0xFFFF9800),
                        trackColor = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KPICard(
                label = "Active Budgets",
                value = summary.activeBudgets.toString(),
                modifier = Modifier.weight(1f)
            )
            KPICard(
                label = "Budgets Under Control",
                value = summary.underControl.toString(),
                modifier = Modifier.weight(1.2f)
            )
            KPICard(
                label = "Budget Overrun",
                value = summary.overrun.toString(),
                valueColor = if (summary.overrun > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Overall Balance Amount",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currencyFormat.format(summary.balance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00C853)
                )
            }
        }
    }
}

@Composable
fun KPICard(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.labelSmall.lineHeight
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
fun BudgetDetailedList(
    budgets: List<BudgetWithProgress>,
    currencyFormat: NumberFormat,
    onBudgetClick: (BudgetWithProgress) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        budgets.forEach { budgetProgress ->
            BudgetListItem(
                budgetProgress = budgetProgress,
                currencyFormat = currencyFormat,
                onClick = { onBudgetClick(budgetProgress) }
            )
        }
    }
}

@Composable
fun BudgetListItem(
    budgetProgress: BudgetWithProgress,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(budgetProgress.categoryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = budgetProgress.categoryEmoji,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = budgetProgress.categoryName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = currencyFormat.format(budgetProgress.budget.amount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                val balance = budgetProgress.budget.amount - budgetProgress.spent
                val isOverrun = balance < 0
                
                if (isOverrun) {
                    Text(
                        text = "Overrun: ${currencyFormat.format(kotlin.math.abs(balance))}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Balance: ${currencyFormat.format(balance)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853)
                    )
                }
                
                Text(
                    text = "${currencyFormat.format(budgetProgress.spent)} spent / ${String.format(Locale.getDefault(), "%.0f", budgetProgress.percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun LendingSummaryKPIs(
    summary: LendingDashboardSummary,
    currencyFormat: NumberFormat
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Net Position Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (summary.netPosition >= 0) Color(0xFF00C853).copy(alpha = 0.1f) else MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (summary.netPosition >= 0) "Net Owed to You" else "Net You Owe",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (summary.netPosition >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                )
                Text(
                    text = currencyFormat.format(kotlin.math.abs(summary.netPosition)),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (summary.netPosition >= 0) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Money Owed to Me
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF00C853).copy(alpha = 0.05f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Money Owed to Me",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(summary.totalOwedToMe),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00C853)
                    )
                }
            }

            // Money I Owe
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Money I Owe",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(summary.totalIOwe),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun LendingPartnerList(
    summary: LendingDashboardSummary,
    currencyFormat: NumberFormat,
    onPartnerClick: (PartnerSummary) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (summary.partnersOwedToMe.isNotEmpty()) {
            Text(
                text = "Owed to Me",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            summary.partnersOwedToMe.forEach { partner ->
                PartnerTile(
                    partner = partner,
                    currencyFormat = currencyFormat,
                    onClick = { onPartnerClick(partner) }
                )
            }
        }

        if (summary.partnersIOwe.isNotEmpty()) {
            Text(
                text = "I Owe",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            summary.partnersIOwe.forEach { partner ->
                PartnerTile(
                    partner = partner,
                    currencyFormat = currencyFormat,
                    onClick = { onPartnerClick(partner) }
                )
            }
        }
    }
}

@Composable
fun PartnerTile(
    partner: PartnerSummary,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar Placeholder
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (partner.peer.description.contains("bank", ignoreCase = true)) Icons.Default.Business else Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partner.peer.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Loan: ${currencyFormat.format(partner.principalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = currencyFormat.format(partner.remainingBalance),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (partner.isOwedToMe) Color(0xFF00C853) else MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress Bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { partner.percentage / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape),
                    color = if (partner.isOwedToMe) Color(0xFF00C853) else MaterialTheme.colorScheme.error,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${String.format(Locale.getDefault(), "%.0f", partner.percentage)}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${currencyFormat.format(partner.repaidAmount)} repaid",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (partner.nextDueDate != null) {
                    Text(
                        text = "Due: ${dateFormat.format(Date(partner.nextDueDate))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note.ifBlank { transaction.description }.ifBlank { transaction.type.replaceFirstChar { it.uppercase() } },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateFormat.format(Date(transaction.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = when (transaction.type) {
                    "income" -> "+${currencyFormat.format(transaction.amount)}"
                    "expense" -> "-${currencyFormat.format(transaction.amount)}"
                    else -> currencyFormat.format(transaction.amount)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when (transaction.type) {
                    "income" -> Color(0xFF00C853)
                    "expense" -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}