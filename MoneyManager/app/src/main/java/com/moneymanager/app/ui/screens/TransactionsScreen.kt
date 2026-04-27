package com.moneymanager.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.R
import com.moneymanager.app.ui.components.SplitTransactionCard
import com.moneymanager.app.ui.components.TransactionCardDense
import com.moneymanager.app.ui.components.TransactionFilterSheet
import com.moneymanager.app.ui.dialogs.AddEditTransactionDialog
import com.moneymanager.app.ui.dialogs.SplitRowData
import com.moneymanager.app.ui.components.TransferDialog
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    viewModel: TransactionsViewModel,
    initialType: String? = null,
    initialAccountId: Long? = null,
    initialStartDate: Long? = null,
    initialEndDate: Long? = null,
    initialGoalId: Long? = null,
    initialCategoryId: Long? = null,
    initialPeerId: Long? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val totalIncome = remember(uiState.transactions) {
        uiState.transactions.filter { it.type == "income" || it.type == "receive" }.sumOf { it.amount }
    }
    val totalExpense = remember(uiState.transactions) {
        uiState.transactions.filter { it.type == "expense" || it.type == "lend" || it.type == "savings" }.sumOf { it.amount }
    }
    val transactionCount = uiState.transactions.size

    LaunchedEffect(initialType, initialAccountId, initialStartDate, initialEndDate, initialGoalId, initialCategoryId, initialPeerId) {
        if (initialType != null) viewModel.setTypeFilter(initialType)
        if (initialAccountId != null) viewModel.setAccountFilter(initialAccountId)
        if (initialGoalId != null) viewModel.setGoalFilter(initialGoalId)
        if (initialCategoryId != null) viewModel.setCategoryFilter(initialCategoryId)
        if (initialPeerId != null) viewModel.setPeerFilter(initialPeerId)
        if (initialStartDate != null || initialEndDate != null) {
            viewModel.setDateRangeFilter(initialStartDate, initialEndDate)
        }
    }
    val currencyFormat = remember(uiState.currency) {
        CurrencyUtils.getCurrencyFormat(uiState.currency)
    }
    var collapsedDates by rememberSaveable { mutableStateOf(setOf<Long>()) }
    var expandedSplitIds by rememberSaveable { mutableStateOf(setOf<Long>()) }
    val lazyListState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 50 }
    }
    val showScrollToTop by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 ||
            lazyListState.firstVisibleItemScrollOffset > 300
        }
    }
    val animatedHeaderHeight by animateDpAsState(
        targetValue = if (isScrolled) 48.dp else 56.dp,
        label = "headerHeight"
    )
    var showAddDialog by remember { mutableStateOf(initialType != null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var searchText by remember { mutableStateOf("") }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var preselectedType by remember { mutableStateOf(initialType) }
    var timeFilter by remember { mutableStateOf("Month") }
    var currentPeriodStart by remember { mutableStateOf<Long?>(null) }
    var currentPeriodEnd by remember { mutableStateOf<Long?>(null) }
    var showTimeDropdown by remember { mutableStateOf(false) }

    val timeFilterOptions = listOf("All", "Day", "Week", "Month", "Year", "Custom")

    fun updatePeriodBasedOnFilter(filter: String, baseCal: Calendar? = null) {
        val cal = baseCal ?: Calendar.getInstance()
        when (filter) {
            "Day" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                currentPeriodEnd = cal.timeInMillis
            }
            "Week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = cal.timeInMillis
                cal.add(Calendar.WEEK_OF_YEAR, 1)
                cal.add(Calendar.MILLISECOND, -1)
                currentPeriodEnd = cal.timeInMillis
            }
            "Month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                currentPeriodEnd = cal.timeInMillis
            }
            "Year" -> {
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                currentPeriodStart = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                currentPeriodEnd = cal.timeInMillis
            }
        }
        viewModel.setDateRangeFilter(currentPeriodStart, currentPeriodEnd)
    }

    fun toggleSplitExpand(transactionId: Long) {
        expandedSplitIds = if (expandedSplitIds.contains(transactionId)) {
            expandedSplitIds - transactionId
        } else {
            expandedSplitIds + transactionId
        }
    }

    fun navigatePrevious() {
        val baseCal = Calendar.getInstance().apply {
            timeInMillis = currentPeriodStart ?: System.currentTimeMillis()
        }
        when (timeFilter) {
            "Day" -> baseCal.add(Calendar.DAY_OF_MONTH, -1)
            "Week" -> baseCal.add(Calendar.WEEK_OF_YEAR, -1)
            "Month" -> baseCal.add(Calendar.MONTH, -1)
            "Year" -> baseCal.add(Calendar.YEAR, -1)
        }
        updatePeriodBasedOnFilter(timeFilter, baseCal)
    }

    fun navigateNext() {
        val baseCal = Calendar.getInstance().apply {
            timeInMillis = currentPeriodStart ?: System.currentTimeMillis()
        }
        when (timeFilter) {
            "Day" -> baseCal.add(Calendar.DAY_OF_MONTH, 1)
            "Week" -> baseCal.add(Calendar.WEEK_OF_YEAR, 1)
            "Month" -> baseCal.add(Calendar.MONTH, 1)
            "Year" -> baseCal.add(Calendar.YEAR, 1)
        }
        updatePeriodBasedOnFilter(timeFilter, baseCal)
    }

    LaunchedEffect(Unit) {
        updatePeriodBasedOnFilter("Month")
    }

    val activeFilterCount = listOfNotNull(
        uiState.filterType.takeIf { it.isNotEmpty() && it != "All" },
        uiState.filterAccountId,
        uiState.filterCategoryId,
        uiState.filterTagId,
        uiState.filterGoalId,
        uiState.filterStartDate,
        uiState.filterEndDate
    ).size

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 2.dp,
                shadowElevation = 2.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(animatedHeaderHeight)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Transactions",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box {
                                TextButton(
                                    onClick = { showTimeDropdown = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = timeFilter,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showTimeDropdown,
                                    onDismissRequest = { showTimeDropdown = false }
                                ) {
                                    timeFilterOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                timeFilter = option
                                                showTimeDropdown = false
                                                if (option == "Custom") {
                                                    showFilterSheet = true
                                                } else if (option != "All") {
                                                    updatePeriodBasedOnFilter(option)
                                                } else {
                                                    viewModel.setDateRangeFilter(null, null)
                                                }
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    isSearchVisible = !isSearchVisible
                                    if (!isSearchVisible) {
                                        searchText = ""
                                        viewModel.setSearchQuery("")
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = if (isSearchVisible) "Close search" else "Search",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (activeFilterCount > 0) {
                                BadgedBox(badge = {
                                    Badge { Text(activeFilterCount.toString()) }
                                }) {
                                    IconButton(onClick = { showFilterSheet = true }) {
                                        Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(20.dp))
                                    }
                                }
                            } else {
                                IconButton(onClick = { showFilterSheet = true }) {
                                    Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isSearchVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it; viewModel.setSearchQuery(it) },
                                    modifier = Modifier
                                        .weight(1f),
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier.fillMaxHeight(),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            if (searchText.isEmpty()) {
                                                Text(
                                                    "Search by note, category or amount...",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                if (searchText.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchText = ""; viewModel.setSearchQuery("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, "Clear", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showScrollToTop) {
                    SmallFloatingActionButton(
                        onClick = {
                            CoroutineScope(Dispatchers.Main).launch {
                                lazyListState.animateScrollToItem(0)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll to top",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            val groupedTransactions = remember(uiState.transactions) {
                uiState.transactions.groupBy { tx ->
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
            }
            val headerDateFormat = remember { SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()) }

            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 56.dp)
            ) {
                if (timeFilter != "All" && timeFilter != "Custom") {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { navigatePrevious() }) {
                                    Icon(
                                        Icons.Default.ChevronLeft,
                                        contentDescription = "Previous",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = when (timeFilter) {
                                            "Day" -> SimpleDateFormat("EEE, MMM d", Locale.getDefault())
                                                .format(Date(currentPeriodStart ?: System.currentTimeMillis()))
                                            "Week" -> "Week ${Calendar.getInstance().apply {
                                                timeInMillis = currentPeriodStart ?: System.currentTimeMillis()
                                            }.get(Calendar.WEEK_OF_YEAR)}"
                                            "Month" -> SimpleDateFormat("MMM yyyy", Locale.getDefault())
                                                .format(Date(currentPeriodStart ?: System.currentTimeMillis()))
                                            "Year" -> Calendar.getInstance().apply {
                                                timeInMillis = currentPeriodStart ?: System.currentTimeMillis()
                                            }.get(Calendar.YEAR).toString()
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                val isAtCurrentDate = remember(currentPeriodStart) {
                                    val cal = Calendar.getInstance()
                                    cal.set(Calendar.HOUR_OF_DAY, 0)
                                    cal.set(Calendar.MINUTE, 0)
                                    cal.set(Calendar.SECOND, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    val currentStart = currentPeriodStart ?: System.currentTimeMillis()
                                    currentStart >= cal.timeInMillis
                                }
                                IconButton(
                                    onClick = { navigateNext() },
                                    enabled = !isAtCurrentDate
                                ) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Next",
                                        modifier = Modifier.size(24.dp),
                                        tint = if (!isAtCurrentDate) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (uiState.transactions.isEmpty()) {
                    item {
                        Box(Modifier.fillParentMaxSize(), Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inbox, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    if (activeFilterCount > 0 || searchText.isNotEmpty()) "No matching results"
                                    else "No transactions recorded yet",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    groupedTransactions.forEach { (dateMillis, transactions) ->
                        val isCollapsed = collapsedDates.contains(dateMillis)
                        item {
                            val dailyTotal = transactions.sumOf {
                                when(it.type) {
                                    "income", "receive" -> it.amount
                                    "expense", "lend", "savings" -> -it.amount
                                    else -> 0.0
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 2.dp,
                                tonalElevation = 1.dp,
                                onClick = {
                                    collapsedDates = if (isCollapsed) {
                                        collapsedDates - dateMillis
                                    } else {
                                        collapsedDates + dateMillis
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = headerDateFormat.format(Date(dateMillis)),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = if (dailyTotal >= 0) {
                                                "${stringResource(R.string.income)}: ${currencyFormat.format(dailyTotal)}"
                                            } else {
                                                "${stringResource(R.string.expense)}: ${currencyFormat.format(-dailyTotal)}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = if (dailyTotal >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Icon(
                                            imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (!isCollapsed) {
                            items(transactions.filter { !it.isSplitChild }, key = { it.id }) { tx ->
                                if (tx.isSplitParent) {
                                    val splitChildren by viewModel.getSplitChildren(tx.id).collectAsStateWithLifecycle(initialValue = emptyList())
                                    val isExpanded = expandedSplitIds.contains(tx.id)

                                    SplitTransactionCard(
                                        parentTransaction = tx,
                                        splitChildren = splitChildren,
                                        accounts = uiState.allAccounts,
                                        categories = uiState.allCategories,
                                        peers = uiState.allPeers,
                                        currencyFormat = currencyFormat,
                                        isExpanded = isExpanded,
                                        onToggleExpand = { toggleSplitExpand(tx.id) },
                                        onEdit = { editingTransaction = it },
                                        onDelete = { viewModel.deleteTransaction(it) }
                                    )
                                } else {
                                    TransactionItem(
                                        transaction = tx,
                                        accounts = uiState.allAccounts,
                                        categories = uiState.allCategories,
                                        peers = uiState.allPeers,
                                        currencyFormat = currencyFormat,
                                        onEdit = { editingTransaction = it },
                                        onDelete = { viewModel.deleteTransaction(it) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddEditTransactionDialog(
            transaction = null,
            currency = uiState.currency,
            categories = uiState.allCategories,
            tags = uiState.allTags,
            accounts = uiState.allAccounts,
            peers = uiState.allPeers,
            goals = uiState.allGoals,
            initialType = preselectedType,
            onDismiss = { showAddDialog = false },
            onConfirm = { tx, children ->
                if (children != null) viewModel.addSplitTransaction(tx, children)
                else viewModel.addTransaction(tx)
                showAddDialog = false
            },
            onSaveAndDuplicate = { tx, children ->
                if (children != null) viewModel.addSplitTransaction(tx, children)
                else viewModel.addTransaction(tx)
                preselectedType = tx.type
            },
            onCreateTag = { name -> viewModel.createTag(name) }
        )
    }

    editingTransaction?.let { editing ->
        val splitChildren by if (editing.isSplitParent) {
            viewModel.getSplitChildren(editing.id).collectAsStateWithLifecycle(initialValue = emptyList())
        } else {
            remember { mutableStateOf(emptyList<TransactionEntity>()) }
        }

        AddEditTransactionDialog(
            transaction = editing,
            currency = uiState.currency,
            splitChildren = splitChildren,
            categories = uiState.allCategories,
            tags = uiState.allTags,
            accounts = uiState.allAccounts,
            peers = uiState.allPeers,
            goals = uiState.allGoals,
            initialType = editing.type,
            onDismiss = { editingTransaction = null },
            onConfirm = { tx, children ->
                viewModel.updateTransaction(editing, tx, children)
                editingTransaction = null
            },
            onSaveAndDuplicate = { tx, children ->
                viewModel.updateTransaction(editing, tx, children)
                editingTransaction = null
            },
            onCreateTag = { name -> viewModel.createTag(name) }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            accounts = uiState.allAccounts,
            onDismiss = { showTransferDialog = false },
            onTransfer = { from, to, amount, note, date ->
                viewModel.addTransfer(from, to, amount, note, date)
                showTransferDialog = false
            }
        )
    }

    if (showFilterSheet) {
        TransactionFilterSheet(
            accounts = uiState.allAccounts,
            categories = uiState.allCategories,
            tags = uiState.allTags,
            selectedType = uiState.filterType,
            selectedAccountId = uiState.filterAccountId,
            selectedCategoryId = uiState.filterCategoryId,
            selectedTagId = uiState.filterTagId,
            selectedStartDate = uiState.filterStartDate,
            selectedEndDate = uiState.filterEndDate,
            onTypeSelected = { viewModel.setTypeFilter(it) },
            onAccountSelected = { viewModel.setAccountFilter(it) },
            onCategorySelected = { viewModel.setCategoryFilter(it) },
            onTagSelected = { viewModel.setTagFilter(it) },
            onStartDateSelected = { viewModel.setDateRangeFilter(it, uiState.filterEndDate) },
            onEndDateSelected = { viewModel.setDateRangeFilter(uiState.filterStartDate, it) },
            onClearFilters = { viewModel.clearAllFilters() },
            onApply = {},
            onDismiss = { showFilterSheet = false }
        )
    }
}