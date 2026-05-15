package com.moneymanager.app.ui.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.summary.components.*
import com.moneymanager.app.ui.constants.TimeFilter
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    viewModel: SummaryViewModel = hiltViewModel(),
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToTransactions: (String?, Long?, Long?, Long?, Long?, Long?, Long?) -> Unit = { _, _, _, _, _, _, _ -> },
    onNavigateToGoals: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }

    val (startDate, endDate) = viewModel.getCurrentDateRange()
    val scope = rememberCoroutineScope()
    
    val pagerState = rememberPagerState(
        initialPage = uiState.activeTab.ordinal,
        pageCount = { SummaryTab.entries.size }
    )

    // Sync pager with activeTab from ViewModel
    LaunchedEffect(uiState.activeTab) {
        if (pagerState.currentPage != uiState.activeTab.ordinal) {
            pagerState.animateScrollToPage(uiState.activeTab.ordinal)
        }
    }

    // Sync activeTab in ViewModel with pager
    LaunchedEffect(pagerState.currentPage) {
        viewModel.setActiveTab(SummaryTab.entries[pagerState.currentPage])
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        SummaryHeader(
            uiState = uiState,
            onTimeFilterChange = viewModel::setTimeFilter,
            onNavigatePeriod = viewModel::navigatePeriod,
            onCustomDateRange = viewModel::setCustomDateRange,
            onDateClick = { showDateRangePicker = true }
        )

        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.isEmpty -> {
                SummaryEmptyState(onAdd = onNavigateToAddTransaction)
            }
            else -> {
                Column(modifier = Modifier.weight(1f)) {
                    NetBalanceCard(
                        netBalance = uiState.netBalance,
                        income = uiState.totalIncome,
                        expense = uiState.totalExpense,
                        trendPercent = uiState.netBalanceTrendPercent,
                        currency = uiState.currency
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Custom Tab Row
                    SummaryTabRow(
                        selectedTab = SummaryTab.entries[pagerState.currentPage],
                        onTabSelected = { tab ->
                            scope.launch {
                                pagerState.animateScrollToPage(tab.ordinal)
                            }
                        }
                    )

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.Top
                    ) { pageIndex ->
                        val currentTab = SummaryTab.entries[pageIndex]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp)
                        ) {
                            when (currentTab) {
                                SummaryTab.EXPENSE -> {


                                    TopCategoriesCard(
                                        rows = uiState.topBudgetUtilization,
                                        currency = uiState.currency
                                    )

                                    ExpenseBreakdownCard(
                                        categoryEntries = uiState.expenseByCategory,
                                        accountEntries = uiState.expenseByAccount,
                                        totalExpense = uiState.totalExpense,
                                        currency = uiState.currency
                                    )
                                }
                                SummaryTab.INCOME -> {
                                    TopIncomeSourcesCard(
                                        rows = uiState.incomeByCategory,
                                        currency = uiState.currency
                                    )

                                    IncomeBreakdownCard(
                                        categoryEntries = uiState.incomeByCategoryPie,
                                        accountEntries = uiState.incomeByAccount,
                                        totalIncome = uiState.totalIncome,
                                        currency = uiState.currency
                                    )
                                }
                                SummaryTab.LENDING -> {
                                    LendingOverviewCard(
                                        totalLent = uiState.totalLent,
                                        totalBorrowed = uiState.totalBorrowed,
                                        lentPeopleCount = uiState.lentPeopleCount,
                                        borrowedPeopleCount = uiState.borrowedPeopleCount,
                                        netBalance = uiState.lendingNetBalance,
                                        settledAmount = uiState.settledAmount,
                                        settledCount = uiState.settledCount,
                                        currency = uiState.currency,
                                        onViewAllClick = {
                                            onNavigateToTransactions(
                                                null,
                                                null,
                                                startDate,
                                                endDate,
                                                null,
                                                null,
                                                null
                                            )
                                        }
                                    )

                                    LendingPeopleList(
                                        people = uiState.lendingPeople,
                                        currency = uiState.currency,
                                        onViewAllClick = { /* Navigate to people list */ },
                                        onPersonClick = { /* Navigate to person details */ }
                                    )
                                }
                                SummaryTab.TRANSFERS -> {
                                    TransferSummaryCard(
                                        totalTransfers = uiState.totalTransfersCount,
                                        totalAmount = uiState.totalTransferAmount,
                                        currency = uiState.currency
                                    )

                                    AccountWiseTransferSummary(
                                        accounts = uiState.accountTransfers,
                                        currency = uiState.currency
                                    )
                                }
                                SummaryTab.SAVINGS -> {
                                    SavingsOverviewCard(
                                        totalSavings = uiState.totalSavings,
                                        growthPercent = uiState.savingsGrowthPercent,
                                        growthPeriod = uiState.savingsGrowthPeriod,
                                        currency = uiState.currency
                                    )

                                    if (uiState.savingsGoals.isNotEmpty()) {
                                        SavingsGoalsList(
                                            goals = uiState.savingsGoals,
                                            currency = uiState.currency,
                                            onViewAllClick = onNavigateToGoals
                                        )
                                    }

                                    if (uiState.savingsAccounts.isNotEmpty()) {
                                        SavingsAccountsList(
                                            accounts = uiState.savingsAccounts,
                                            currency = uiState.currency,
                                            onViewAllClick = onNavigateToAccounts
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    // Pager Indicator (Dots)
                    Row(
                        Modifier
                            .height(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(SummaryTab.entries.size) { iteration ->
                            val color = if (pagerState.currentPage == iteration) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        DateRangePickerSheet(
            initialStartDate = uiState.customStartDate ?: startDate,
            initialEndDate = uiState.customEndDate ?: endDate,
            onApply = { start, end, filter ->
                if (filter != null && filter != TimeFilter.CUSTOM) {
                    viewModel.setTimeFilter(filter, start)
                } else {
                    viewModel.setCustomDateRange(start, end)
                }
                showDateRangePicker = false
            },
            onDismiss = { showDateRangePicker = false }
        )
    }
}

@Composable
fun SummaryTabRow(
    selectedTab: SummaryTab,
    onTabSelected: (SummaryTab) -> Unit
) {
    val tabs = listOf(
        TabItem("Expense", Icons.Default.PieChart, SummaryTab.EXPENSE),
        TabItem("Income", Icons.Default.AccountBalanceWallet, SummaryTab.INCOME),
        TabItem("Lending", Icons.Default.People, SummaryTab.LENDING),
        TabItem("Transfers", Icons.Default.SwapHoriz, SummaryTab.TRANSFERS),
        TabItem("Savings", Icons.Default.Savings, SummaryTab.SAVINGS)
    )

    TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        divider = {},
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                color = MaterialTheme.colorScheme.primary,
                height = 3.dp
            )
        }
    ) {
        tabs.forEach { item ->
            val isSelected = selectedTab == item.tab
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

data class TabItem(
    val label: String,
    val icon: ImageVector,
    val tab: SummaryTab
)

@Composable
private fun SummaryEmptyState(onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.AddCircle,
            contentDescription = "No transactions",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No transactions available",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onAdd) {
            Text("Add Transaction")
        }
    }
}
