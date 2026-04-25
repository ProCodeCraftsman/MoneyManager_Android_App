package com.moneymanager.app.ui.screens
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.text.BasicTextField
import com.moneymanager.app.ui.util.CurrencyUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.components.TransactionFilterSheet
import com.moneymanager.app.ui.components.TransferDialog
import com.moneymanager.app.ui.util.FileHelper
import com.moneymanager.data.entity.*
import androidx.compose.ui.res.stringResource
import com.moneymanager.app.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import android.app.Activity
import androidx.core.view.WindowCompat

// Investment platforms for savings transactions
private val INVESTMENT_PLATFORMS = listOf(
    "Zerodha", "Groww", "Kite", "IndMoney", "Paytm Money",
    "ET Money", "Coin by Zerodha", "Angel One", "Upstox",
    "HDFC Securities", "ICICI Direct", "Other"
)

// UI Constants for Transaction Tile
private val TILE_PADDING_HORIZONTAL = 12.dp
private val TILE_PADDING_INNER = 12.dp
private val TILE_CORNER_RADIUS = 12.dp
private val TILE_ELEVATION = 2.dp
private val TILE_ICON_SIZE = 40.dp
private val TILE_ICON_CORNER_RADIUS = 10.dp
private val TILE_SPACING_VERTICAL = 8.dp

private const val ICON_SPLIT = "🔀"
private const val ICON_TRANSFER = "⇌"
private const val ICON_SAVINGS = "📈"
private const val ICON_DEFAULT = "💸"

private val COLOR_EXPENSE = Color(0xFFE57373)
private val COLOR_SAVINGS = Color(0xFFF4A460)
private val COLOR_TRANSFER = Color(0xFF5B6FB5)

private const val SEPARATOR_HYPHEN = " - "
private const val SEPARATOR_DOT = " • "

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


    // Calculate totals for the current view
    val totalIncome = remember(uiState.transactions) {
        uiState.transactions.filter { it.type == "income" || it.type == "receive" }.sumOf { it.amount }
    }
    val totalExpense = remember(uiState.transactions) {
        uiState.transactions.filter { it.type == "expense" || it.type == "lend" || it.type == "savings" }.sumOf { it.amount }
    }
    val transactionCount = uiState.transactions.size

    // Apply initial filters if provided
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
    val animatedHeaderHeight by animateDpAsState(
        targetValue = if (isScrolled) 48.dp else 56.dp,
        label = "headerHeight"
    )
    var showAddDialog by remember { mutableStateOf(initialType != null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var searchText by remember { mutableStateOf("") }
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
                // Custom Compact Header
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
                        Box {
                            TextButton(
                                onClick = { showTimeDropdown = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = timeFilter,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                    }

                    // Filter button with count
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Streamlined Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .clickable { /* focus search */ }
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
                    // Task 3: Center search placeholder text vertically
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it; viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.CenterVertically),
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
                    if (activeFilterCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$activeFilterCount filter${if (activeFilterCount > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.clearAllFilters() },
                                    modifier = Modifier.size(12.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear filters",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
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

            // Transaction list with Sticky Headers
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
                // Item 0 — Summary panel (always shown, scrolls with list)
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            SummaryItem("Spent", totalExpense, MaterialTheme.colorScheme.error, currencyFormat)
                            SummaryItem("Income", totalIncome, MaterialTheme.colorScheme.secondary, currencyFormat)
                            SummaryItem("Items", transactionCount.toDouble(), MaterialTheme.colorScheme.primary, null)
                        }
                    }
                }

                // Item 1 — Time navigation (conditional, scrolls with list)
                if (timeFilter != "All" && timeFilter != "Custom") {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navigatePrevious() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous", modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = when (timeFilter) {
                                    "Day" -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(currentPeriodStart ?: System.currentTimeMillis()))
                                    "Week" -> "Week ${Calendar.getInstance().apply { timeInMillis = currentPeriodStart ?: System.currentTimeMillis() }.get(Calendar.WEEK_OF_YEAR)}"
                                    "Month" -> SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(currentPeriodStart ?: System.currentTimeMillis()))
                                    "Year" -> Calendar.getInstance().apply { timeInMillis = currentPeriodStart ?: System.currentTimeMillis() }.get(Calendar.YEAR).toString()
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { navigateNext() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next", modifier = Modifier.size(20.dp))
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
                        stickyHeader {
                            val dailyTotal = transactions.sumOf {
                                when(it.type) {
                                    "income", "receive" -> it.amount
                                    "expense", "lend", "savings" -> -it.amount
                                    else -> 0.0
                                }
                            }

                            // Task 2: Add elevation and vertical padding to sticky date headers
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
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
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
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
                            HorizontalDivider(thickness = TILE_SPACING_VERTICAL, color = Color.Transparent)
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

    // Add dialog
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

    // Edit dialog
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

    // Transfer dialog
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

    // Filter sheet
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

// ---------------------------------------------------------------------------
// Transaction Item (Dense Refactor)
// ---------------------------------------------------------------------------

@Composable
fun SummaryItem(label: String, amount: Double, color: Color, format: NumberFormat?) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = format?.format(amount) ?: amount.toInt().toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun CompactFilterChip(label: String, onClear: () -> Unit) {
    Surface(
        modifier = Modifier.height(30.dp),
        shape = RoundedCornerShape(15.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Icon(
                Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp).clickable { onClear() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    peers: List<PeerContact> = emptyList(),
    currencyFormat: NumberFormat,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
) {
    // 1. State for the confirmation dialog
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    // 2. Instead of deleting, show the dialog
                    showDeleteConfirm = true
                    false // Return false so the item doesn't disappear yet
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit(transaction)
                    false
                }
                else -> false
            }
        }
    )

    // 3. Confirmation Dialog Logic
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirm = false
            },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this record? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(transaction)
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val color = when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.secondary
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }
            Box(
                Modifier.fillMaxSize().padding(horizontal = TILE_PADDING_HORIZONTAL).clip(RoundedCornerShape(TILE_CORNER_RADIUS)).background(color).padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Edit else Icons.Default.Delete,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    ) {
        Column {
            TransactionCardDense(
                transaction = transaction,
                accounts = accounts,
                categories = categories,
                peers = peers,
                currencyFormat = currencyFormat,
                onClick = { onEdit(transaction) }
            )
            Spacer(Modifier.height(TILE_SPACING_VERTICAL))
        }
    }
}

@Composable
fun TransactionCardDense(
    transaction: TransactionEntity,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    peers: List<PeerContact> = emptyList(),
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val account = remember(transaction.accountId, accounts) { accounts.find { it.id == transaction.accountId } }
    val category = remember(transaction.categoryId, categories) { categories.find { it.id == transaction.categoryId } }
    val subcategory = remember(transaction.subCategoryId, categories) {
        transaction.subCategoryId?.let { id -> categories.find { it.id == id } }
    }
    val toAccount = remember(transaction.toAccountId, accounts) {
        transaction.toAccountId?.let { id -> accounts.find { it.id == id } }
    }
    val peer = remember(transaction.peerContactId, peers) {
        transaction.peerContactId?.let { id -> peers.find { it.id == id } }
    }

    val typeColor = when (transaction.type) {
        "income", "receive" -> MaterialTheme.colorScheme.secondary
        "expense", "lend" -> COLOR_EXPENSE
        "savings" -> COLOR_SAVINGS
        "transfer" -> COLOR_TRANSFER
        else -> MaterialTheme.colorScheme.onSurface
    }

    val typeIcon = when {
        transaction.isSplitParent -> ICON_SPLIT
        transaction.isTransfer || transaction.type == "transfer" -> ICON_TRANSFER
        transaction.type == "savings" -> ICON_SAVINGS
        else -> category?.emoji ?: ICON_DEFAULT
    }

    val amountText = when (transaction.type) {
        "income", "receive" -> "+${currencyFormat.format(transaction.amount)}"
        "expense", "savings", "lend" -> "-${currencyFormat.format(transaction.amount)}"
        else -> currencyFormat.format(transaction.amount)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(TILE_CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = TILE_ELEVATION,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TILE_PADDING_HORIZONTAL)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(TILE_PADDING_INNER),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                Modifier
                    .size(TILE_ICON_SIZE)
                    .clip(RoundedCornerShape(TILE_ICON_CORNER_RADIUS))
                    .background(typeColor.copy(alpha = 0.12f)),
                Alignment.Center
            ) {
                Text(typeIcon, fontSize = 20.sp)
            }

            Spacer(Modifier.width(12.dp))

            // Middle Column: Category & Info
            Column(Modifier.weight(1f)) {
                // Row 1: Title (Category) - Subtitle ([Icon] subcategory)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {

                    Text(
                        text = if (transaction.type == "transfer")
                            stringResource(R.string.transfer)
                        else
                            (category?.name ?: transaction.type.replaceFirstChar { it.uppercase() }),

                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (subcategory != null) {

                        Text(
                            text = " - ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Icon(
                            imageVector = Icons.Default.LocalOffer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        // subcategory name
                        Text(
                            text = subcategory.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Row 2: Account(s) - Description
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (transaction.type == "transfer" && toAccount != null) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = account?.name ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = COLOR_TRANSFER
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = toAccount.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = COLOR_TRANSFER
                        )
                    } else if ((transaction.type == "lend" || transaction.type == "receive") && peer != null) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = COLOR_EXPENSE
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = account?.name ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = COLOR_EXPENSE
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            text = peer.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = COLOR_EXPENSE
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = account?.name ?: stringResource(R.string.app_name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (transaction.note.isNotBlank() || transaction.description.isNotBlank()) {
                        val desc = transaction.note.ifBlank { transaction.description }
                        Text(SEPARATOR_DOT, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right Column: Amount & Time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = typeColor
                )
                Text(
                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun SplitTransactionCard(
    parentTransaction: TransactionEntity,
    splitChildren: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    categories: List<CategoryEntity>,
    peers: List<PeerContact> = emptyList(),
    currencyFormat: NumberFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit,
) {
    val isCurrentlyExpanded = isExpanded

    Column {
        // Parent transaction with expand/collapse indicator
        Surface(
            onClick = onToggleExpand,
            shape = RoundedCornerShape(TILE_CORNER_RADIUS),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = TILE_ELEVATION,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TILE_PADDING_HORIZONTAL)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TILE_PADDING_INNER),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with expand indicator
                Box(
                    Modifier
                        .size(TILE_ICON_SIZE)
                        .clip(RoundedCornerShape(TILE_ICON_CORNER_RADIUS))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                    Alignment.Center
                ) {
                    Text(ICON_SPLIT, fontSize = 20.sp)
                }

                Spacer(Modifier.width(12.dp))

                // Middle Column: Title + child count
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Split Transaction",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${splitChildren.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Right Column: Amount + expand icon
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(parentTransaction.amount),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (parentTransaction.type) {
                            "income", "receive" -> MaterialTheme.colorScheme.secondary
                            "expense", "lend" -> COLOR_EXPENSE
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Icon(
                        imageVector = if (isCurrentlyExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isCurrentlyExpanded) "Collapse" else "Expand",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Expanded: Show child transactions indented below parent
        if (isCurrentlyExpanded) {
            Column(
                modifier = Modifier.padding(start = 24.dp),
                verticalArrangement = Arrangement.spacedBy(TILE_SPACING_VERTICAL)
            ) {
                splitChildren.forEach { child ->
                    TransactionCardDense(
                        transaction = child,
                        accounts = accounts,
                        categories = categories,
                        peers = peers,
                        currencyFormat = currencyFormat,
                        onClick = { onEdit(child) }
                    )
                }
            }
        }

        Spacer(Modifier.height(TILE_SPACING_VERTICAL))
    }
}


@Composable
fun AccountBadge(account: AccountEntity?, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            Alignment.Center
        ) {
            Text(account?.emoji ?: "💰", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.width(4.dp))
        Text(
            text = account?.name ?: "Unknown",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------------------------------------------------------------------------
// Add / Edit Transaction Dialog
// ---------------------------------------------------------------------------

data class SplitRowData(
    val localId: Int,
    val categoryId: Long? = null,
    val subCategoryId: Long? = null,
    val description: String = "",
    val amount: String = ""
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategoryClick: (CategoryEntity) -> Unit,
    onShowAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            // Task 4: Match filter chip height to text field and fix indicator styling
            val displayCategories = categories.take(7)
            displayCategories.forEach { category ->
                val isSelected = selectedCategoryId == category.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryClick(category) },
                    label = { Text(category.name) },
                    leadingIcon = { Text(category.emoji) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(48.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
            FilterChip(
                selected = false,
                onClick = onShowAllClick,
                label = { Text("All") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(48.dp)
            )
        }
    }
}

@Composable
fun NumericKeypad(
    onNumberClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onClearClick: () -> Unit,
    onEvaluate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf("7", "8", "9", "/"),
        listOf("4", "5", "6", "*"),
        listOf("1", "2", "3", "-"),
        listOf(".", "0", "=", "+")
    )

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keys.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { key ->
                    val isOperator = key in listOf("/", "*", "-", "+", "=")

                    Button(
                        onClick = {
                            if (key == "=") onEvaluate() else onNumberClick(key)
                        },
                        modifier = Modifier.weight(1f).aspectRatio(if (isOperator) 1.5f else 2f),
                        contentPadding = PaddingValues(0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = if (isOperator) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                        else ButtonDefaults.filledTonalButtonColors()
                    ) {
                        Text(key, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        // Bottom row for backspace and clear
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onClearClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("CLEAR")
            }
            OutlinedButton(
                onClick = onDeleteClick,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Backspace, "Backspace")
            }
        }
    }
}

private fun evaluateExpression(expression: String): Double {
    if (expression.isEmpty()) return 0.0

    // Simple expression evaluator for +, -, *, /
    // This is a basic implementation. For more complex ones, use a library or a better parser.
    return try {
        val tokens = mutableListOf<String>()
        var current = ""
        for (char in expression) {
            if (char in "+-*/") {
                if (current.isNotEmpty()) tokens.add(current)
                tokens.add(char.toString())
                current = ""
            } else {
                current += char
            }
        }
        if (current.isNotEmpty()) tokens.add(current)

        if (tokens.isEmpty()) return 0.0

        // Handle * and / first
        val pass1 = mutableListOf<String>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            if (token == "*" || token == "/") {
                val prev = pass1.removeAt(pass1.size - 1).toDouble()
                val next = tokens[i + 1].toDouble()
                val res = if (token == "*") prev * next else prev / next
                pass1.add(res.toString())
                i += 2
            } else {
                pass1.add(token)
                i++
            }
        }

        // Handle + and -
        var result = pass1[0].toDouble()
        i = 1
        while (i < pass1.size) {
            val op = pass1[i]
            val next = pass1[i + 1].toDouble()
            result = if (op == "+") result + next else result - next
            i += 2
        }
        result
    } catch (e: Exception) {
        expression.toDoubleOrNull() ?: 0.0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionDialog(
    transaction: TransactionEntity?,
    splitChildren: List<TransactionEntity> = emptyList(),
    currency: String,
    categories: List<CategoryEntity>,
    tags: List<TagEntity>,
    accounts: List<AccountEntity>,
    peers: List<PeerContact> = emptyList(),
    goals: List<GoalEntity>,
    initialType: String?,
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, List<TransactionEntity>?) -> Unit,
    onSaveAndDuplicate: (TransactionEntity, List<TransactionEntity>?) -> Unit,
    onCreateTag: (String) -> Unit,
) {
    val isEdit = transaction != null
    var type by remember { mutableStateOf(transaction?.type ?: initialType ?: "expense") }
    var amount by remember { mutableStateOf(transaction?.amount?.let { if (it < 0) (-it).toString() else it.toString() } ?: "") }
    var selectedAccountId by remember { mutableStateOf(transaction?.accountId ?: accounts.firstOrNull()?.id) }
    var selectedToAccountId by remember { mutableStateOf(transaction?.toAccountId) }
    var showToAccountDropdown by remember { mutableStateOf(false) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(transaction?.categoryId) }
    var selectedPeerId by remember { mutableStateOf<Long?>(transaction?.peerContactId) }
    var peerName by remember { mutableStateOf(peers.find { it.id == transaction?.peerContactId }?.displayName ?: "") }
    var selectedDate by remember { mutableStateOf(transaction?.date ?: System.currentTimeMillis()) }
    var description by remember { mutableStateOf(transaction?.note ?: "") }
    var selectedGoalId by remember { mutableStateOf<Long?>(transaction?.goalId) }
    var selectedPlatform by remember { mutableStateOf<String?>(transaction?.investmentPlatform) }
    var splitEnabled by remember { mutableStateOf(transaction?.isSplitParent == true) }
    var splitRows by remember {
        mutableStateOf(
            if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) {
                splitChildren.mapIndexed { index, child ->
                    val parentCatId = if (categories.find { it.id == child.categoryId }?.parentId != null) {
                        categories.find { it.id == child.categoryId }?.parentId
                    } else {
                        child.categoryId
                    }
                    val subCatId = if (categories.find { it.id == child.categoryId }?.parentId != null) {
                        child.categoryId
                    } else {
                        null
                    }
                    SplitRowData(index, parentCatId, subCatId, child.note, child.amount.toString())
                }
            } else {
                listOf(SplitRowData(0), SplitRowData(1))
            }
        )
    }
    var tagQuery by remember { mutableStateOf("") }
    var showTagDropdown by remember { mutableStateOf(false) }
    var selectedTagIds by remember {
        mutableStateOf(
            if (transaction?.tagIds?.isNotEmpty() == true)
                transaction.tagIds.split(",").mapNotNull { it.trim().toLongOrNull() }.toSet()
            else emptySet<Long>()
        )
    }
    var receiptData by remember { mutableStateOf<String?>(transaction?.receiptPath) }
    var showReceiptPreview by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showAccountDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var showPeerDropdown by remember { mutableStateOf(false) }
    var expectedReturnDate by remember { mutableStateOf<Long?>(transaction?.expectedReturnDate) }
    var showExpectedReturnDatePicker by remember { mutableStateOf(false) }

    var showTypeDropdown by remember { mutableStateOf(false) }

    // Visibility states for Progressive Disclosure
    var showNoteInput by remember { mutableStateOf(transaction?.note?.isNotEmpty() == true) }
    var showTagInput by remember { mutableStateOf(transaction?.tagIds?.isNotEmpty() == true) }
    var showReceiptInput by remember { mutableStateOf(transaction?.receiptPath != null) }
    var showSplitInput by remember { mutableStateOf(transaction?.isSplitParent == true) }

    var showSubCategoryDropdown by remember { mutableStateOf(false) }
    var showGoalDropdown by remember { mutableStateOf(false) }
    var showPlatformDropdown by remember { mutableStateOf(false) }
    var splitIdCounter by remember { mutableStateOf(2) }
    var showSplitCategoryDropdown by remember { mutableStateOf<Int?>(null) }

    val context = LocalContext.current
    val dateDisplayFormat = remember { SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()) }

    val parentCategories = remember(categories, type) {
        categories.filter { it.parentId == null && (it.type == type || (type == "savings" && it.type == "expense")) }
    }
    val selectedParentCategory = remember(selectedCategoryId, categories) {
        val cat = categories.find { it.id == selectedCategoryId }
        if (cat?.parentId != null) categories.find { it.id == cat.parentId } else cat
    }
    val selectedSubCategory = remember(selectedCategoryId, categories) {
        categories.find { it.id == selectedCategoryId }?.takeIf { it.parentId != null }
    }
    val subCategories = remember(selectedParentCategory, categories) {
        selectedParentCategory?.let { parent -> categories.filter { it.parentId == parent.id } } ?: emptyList()
    }

    val filteredTags = remember(tagQuery, tags) {
        if (tagQuery.isEmpty()) tags else tags.filter { it.name.contains(tagQuery, ignoreCase = true) }
    }
    val selectedTags = remember(selectedTagIds, tags) { tags.filter { it.id in selectedTagIds } }

    // Update splitRows if splitChildren changes (useful when editing and children are loaded asynchronously)
    LaunchedEffect(splitChildren) {
        if (transaction?.isSplitParent == true && splitChildren.isNotEmpty()) {
            splitRows = splitChildren.mapIndexed { index, child ->
                val parentCatId = if (categories.find { it.id == child.categoryId }?.parentId != null) {
                    categories.find { it.id == child.categoryId }?.parentId
                } else {
                    child.categoryId
                }
                val subCatId = if (categories.find { it.id == child.categoryId }?.parentId != null) {
                    child.categoryId
                } else {
                    null
                }
                SplitRowData(index, parentCatId, subCatId, child.note, child.amount.toString())
            }
        }
    }

    // Auto-select newly created tag
    var pendingTagName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(tags) {
        pendingTagName?.let { name ->
            val newTag = tags.find { it.name.equals(name, ignoreCase = true) }
            if (newTag != null) {
                selectedTagIds = selectedTagIds + newTag.id
                pendingTagName = null
                tagQuery = ""
            }
        }
    }

    // Receipt picker
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { receiptData = FileHelper.saveReceipt(context, it) }
    }

    val mainAmount = amount.toDoubleOrNull() ?: 0.0
    val splitTotal = splitRows.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val splitRemaining = mainAmount - splitTotal

    fun buildTransaction(): TransactionEntity? {
        val amt = amount.toDoubleOrNull() ?: return null
        if (amt <= 0 || selectedAccountId == null) return null
        val effectiveCategoryId = selectedSubCategory?.id ?: selectedParentCategory?.id
        return TransactionEntity(
            id = transaction?.id ?: 0,
            accountId = selectedAccountId!!,
            type = type,
            amount = amt,
            categoryId = effectiveCategoryId,
            peerContactId = if (type == "lend" || type == "receive") selectedPeerId else null,
            expectedReturnDate = if (type == "lend" || type == "receive") expectedReturnDate else null,
            goalId = selectedGoalId,
            tagIds = selectedTagIds.joinToString(","),
            date = selectedDate,
            note = description,
            description = description,
            receiptPath = receiptData,
            isRecurring = transaction?.isRecurring ?: false,
            recurringId = transaction?.recurringId,
            isSplitParent = splitEnabled,
            isTransfer = type == "transfer",
            toAccountId = if (type == "transfer") selectedToAccountId else null,
            investmentPlatform = if (type == "savings") selectedPlatform else null,
            createdAt = transaction?.createdAt ?: System.currentTimeMillis()
        )
    }

    fun buildSplitChildren(parentId: Long): List<TransactionEntity> {
        return splitRows.mapNotNull { row ->
            val rowAmt = row.amount.toDoubleOrNull() ?: return@mapNotNull null
            if (rowAmt <= 0) return@mapNotNull null
            TransactionEntity(
                accountId = selectedAccountId!!,
                type = type,
                amount = rowAmt,
                categoryId = row.subCategoryId ?: row.categoryId,
                tagIds = "",
                date = selectedDate,
                note = row.description,
                description = row.description,
                isSplitChild = true,
                isTransfer = type == "transfer",
                parentTransactionId = parentId
            )
        }
    }

    // Date/Time pickers
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance().apply { timeInMillis = millis }
                        val timeCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                        cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                        cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                        selectedDate = cal.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE)
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
                    cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(Calendar.MINUTE, timePickerState.minute)
                    selectedDate = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Cancel") } }
        )
    }

    if (showExpectedReturnDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = expectedReturnDate ?: System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showExpectedReturnDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expectedReturnDate = datePickerState.selectedDateMillis
                    showExpectedReturnDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    expectedReturnDate = null
                    showExpectedReturnDatePicker = false
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Receipt full-size preview
    if (showReceiptPreview && receiptData != null) {
        Dialog(onDismissRequest = { showReceiptPreview = false }) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    receiptData?.let { data ->
                        if (data.startsWith("data:image")) {
                            val bitmap = remember(data) {
                                runCatching {
                                    val b64 = data.substringAfter("base64,")
                                    val bytes = Base64.decode(b64, Base64.DEFAULT)
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                }.getOrNull()
                            }
                            bitmap?.let {
                                Image(it.asImageBitmap(), "Receipt", Modifier.fillMaxWidth())
                            }
                        } else {
                            Icon(Icons.Default.PictureAsPdf, null, Modifier.size(64.dp).align(Alignment.CenterHorizontally))
                            Text("PDF Receipt", Modifier.align(Alignment.CenterHorizontally))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showReceiptPreview = false }, Modifier.align(Alignment.End)) { Text("Close") }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(Modifier.fillMaxSize()) {
                // 1. Header: Contextual Dropdown
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        TextButton(
                            onClick = { showTypeDropdown = true },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "New ${type.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        DropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false }
                        ) {
                            val types = listOf("expense", "income", "savings", "transfer", "lend", "receive")
                            types.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(t.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        type = t
                                        selectedCategoryId = null
                                        selectedToAccountId = null
                                        selectedPlatform = null
                                        selectedGoalId = null
                                        selectedPeerId = null
                                        showTypeDropdown = false
                                    },
                                    leadingIcon = {
                                        val icon = when (t) {
                                            "expense" -> Icons.Default.MoneyOff
                                            "income" -> Icons.Default.AttachMoney
                                            "savings" -> Icons.AutoMirrored.Filled.TrendingUp
                                            "transfer" -> Icons.Default.SwapHoriz
                                            "lend" -> Icons.Default.Handshake
                                            "receive" -> Icons.AutoMirrored.Filled.CallReceived
                                            else -> Icons.Default.Edit
                                        }
                                        Icon(icon, null)
                                    }
                                )
                            }
                        }
                    }
                }

                // Scrollable body
                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 2. Hero Area: Amount & Integrated Keypad
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Text(
                                text = CurrencyUtils.getCurrencySymbol(currency),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (amount.isEmpty()) "0.00" else amount,
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (amount.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.clickable { /* Tap to focus/edit if needed */ }
                            )
                        }

                        NumericKeypad(
                            onNumberClick = { num ->
                                if (num == ".") {
                                    if (amount.isEmpty()) amount = "0."
                                    else if (!amount.contains(".")) amount += "."
                                } else if (num in listOf("+", "-", "*", "/")) {
                                    if (amount.isNotEmpty() && !amount.last().isDigit()) {
                                        amount = amount.dropLast(1) + num
                                    } else if (amount.isNotEmpty()) {
                                        amount += num
                                    }
                                } else {
                                    if (amount == "0") amount = num
                                    else amount += num
                                }
                            },
                            onDeleteClick = {
                                if (amount.isNotEmpty()) amount = amount.dropLast(1)
                            },
                            onClearClick = { amount = "" },
                            onEvaluate = {
                                try {
                                    val result = evaluateExpression(amount)
                                    amount = if (result % 1.0 == 0.0) result.toInt().toString() else "%.2f".format(Locale.US, result)
                                } catch (e: Exception) {
                                    // Keep as is or show error
                                }
                            }
                        )
                    }

                    // 3. Smart Bar: Date, Time | Account
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()).format(Date(selectedDate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { showDatePicker = true }
                                )
                                Text(" | ", color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(selectedDate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { showTimePicker = true }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                VerticalDivider(Modifier.height(16.dp).padding(horizontal = 8.dp))
                                if (type == "transfer") {
                                    Text(
                                        text = accounts.find { it.id == selectedAccountId }?.name ?: "From",
                                        modifier = Modifier.clickable { showAccountDropdown = true },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(12.dp).padding(horizontal = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = accounts.find { it.id == selectedToAccountId }?.name ?: "To",
                                        modifier = Modifier.clickable { showToAccountDropdown = true },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    val account = accounts.find { it.id == selectedAccountId }
                                    Text(
                                        text = account?.name ?: "Select Account",
                                        modifier = Modifier.clickable { showAccountDropdown = true },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }

                    // Account Dropdowns
                    Box {
                        DropdownMenu(
                            expanded = showAccountDropdown,
                            onDismissRequest = { showAccountDropdown = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.emoji} ${acc.name}") },
                                    onClick = { selectedAccountId = acc.id; showAccountDropdown = false }
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showToAccountDropdown,
                            onDismissRequest = { showToAccountDropdown = false }
                        ) {
                            accounts.forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("${acc.emoji} ${acc.name}") },
                                    onClick = { selectedToAccountId = acc.id; showToAccountDropdown = false }
                                )
                            }
                        }
                    }

                    // 4. Main Grid: Categories
                    if (type != "transfer" && !splitEnabled) {
                        if (type == "lend" || type == "receive") {
                            // Peer selection for lending/receiving
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Lending Partner", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showPeerDropdown = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(peerName.ifBlank { "Select Person" })
                                        Icon(Icons.Default.Person, null)
                                    }
                                }
                                Box {
                                    DropdownMenu(expanded = showPeerDropdown, onDismissRequest = { showPeerDropdown = false }) {
                                        if (peers.isEmpty()) {
                                            DropdownMenuItem(text = { Text("No partners found") }, onClick = { showPeerDropdown = false })
                                        }
                                        peers.forEach { peer ->
                                            DropdownMenuItem(
                                                text = { Text(peer.displayName) },
                                                onClick = {
                                                    selectedPeerId = peer.id
                                                    peerName = peer.displayName
                                                    showPeerDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Text("Expected Return Date", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showExpectedReturnDatePicker = true },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(expectedReturnDate?.let { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(it)) } ?: "No return date")
                                        Icon(Icons.Default.DateRange, null)
                                    }
                                }
                            }
                        } else if (selectedCategoryId == null || (selectedParentCategory == null && selectedSubCategory == null)) {
                            CategoryGrid(
                                categories = parentCategories,
                                selectedCategoryId = null,
                                onCategoryClick = { cat ->
                                    selectedCategoryId = cat.id
                                },
                                onShowAllClick = { showCategoryDropdown = true }
                            )
                        } else {
                            // Single selected category view
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val displayCat = selectedParentCategory ?: selectedSubCategory
                                    FilterChip(
                                        selected = true,
                                        onClick = { /* Do nothing, click Change to clear */ },
                                        label = { Text(displayCat?.name ?: "") },
                                        leadingIcon = { Text(displayCat?.emoji ?: "") },
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.height(48.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        )
                                    )
                                    TextButton(onClick = {
                                        selectedCategoryId = null
                                    }) {
                                        Text("Change")
                                    }
                                }

                                if (subCategories.isNotEmpty()) {
                                    Text("Sub-category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(subCategories) { sub ->
                                            FilterChip(
                                                selected = selectedCategoryId == sub.id,
                                                onClick = { selectedCategoryId = sub.id },
                                                label = { Text(sub.name) },
                                                leadingIcon = { Text(sub.emoji) },
                                                shape = RoundedCornerShape(16.dp),
                                                modifier = Modifier.height(48.dp),
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Category Dropdown (Search/All)
                        Box {
                            DropdownMenu(
                                expanded = showCategoryDropdown,
                                onDismissRequest = { showCategoryDropdown = false }
                            ) {
                                categories.filter { it.type == type || (type == "savings" && it.type == "expense") }.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text("${if (cat.parentId != null) "  ↳ " else ""}${cat.emoji} ${cat.name}") },
                                        onClick = { selectedCategoryId = cat.id; showCategoryDropdown = false }
                                    )
                                }
                            }
                        }
                    }

                    // Savings specific fields
                    if (type == "savings") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Savings Details", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            // Platform selection
                            OutlinedCard(
                                onClick = { showPlatformDropdown = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AccountBalance, null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(12.dp))
                                        Text(selectedPlatform ?: "Select Platform", style = MaterialTheme.typography.bodyLarge)
                                    }
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }

                            // Goal selection
                            if (goals.isNotEmpty()) {
                                OutlinedCard(
                                    onClick = { showGoalDropdown = true },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Row(
                                        Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(Modifier.width(12.dp))
                                            Text(goals.find { it.id == selectedGoalId }?.name ?: "Link to Goal", style = MaterialTheme.typography.bodyLarge)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, null)
                                    }
                                }
                            }
                        }

                        DropdownMenu(expanded = showPlatformDropdown, onDismissRequest = { showPlatformDropdown = false }) {
                            INVESTMENT_PLATFORMS.forEach { platform ->
                                DropdownMenuItem(text = { Text(platform) }, onClick = { selectedPlatform = platform; showPlatformDropdown = false })
                            }
                        }
                        DropdownMenu(expanded = showGoalDropdown, onDismissRequest = { showGoalDropdown = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { selectedGoalId = null; showGoalDropdown = false })
                            goals.forEach { goal ->
                                DropdownMenuItem(text = { Text(goal.name) }, onClick = { selectedGoalId = goal.id; showGoalDropdown = false })
                            }
                        }
                    }

                    // 5. Action Bar: Optional feature toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showNoteInput = !showNoteInput }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Notes,
                                "Note",
                                tint = if (showNoteInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showTagInput = !showTagInput }) {
                            Icon(
                                Icons.Default.LocalOffer,
                                "Tag",
                                tint = if (showTagInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showReceiptInput = !showReceiptInput }) {
                            Icon(
                                Icons.Default.AttachFile,
                                "Receipt",
                                tint = if (showReceiptInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showSplitInput = !showSplitInput; splitEnabled = !splitEnabled }) {
                            Icon(
                                Icons.AutoMirrored.Filled.CallSplit,
                                "Split",
                                tint = if (showSplitInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Progressive Disclosure Sections
                    if (showNoteInput) {
                        OutlinedTextField(
                            value = description, onValueChange = { description = it },
                            label = { Text("Note") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )
                    }

                    if (showTagInput) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (selectedTags.isNotEmpty()) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    selectedTags.forEach { tag ->
                                        InputChip(
                                            selected = true,
                                            onClick = { selectedTagIds = selectedTagIds - tag.id },
                                            label = { Text(tag.name) },
                                            trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                                        )
                                    }
                                }
                            }
                            Box {
                                OutlinedTextField(
                                    value = tagQuery, onValueChange = { tagQuery = it; showTagDropdown = true },
                                    label = { Text("Search or create tag") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                DropdownMenu(
                                    expanded = showTagDropdown && tagQuery.isNotEmpty(),
                                    onDismissRequest = { showTagDropdown = false },
                                    properties = PopupProperties(focusable = false)
                                ) {
                                    if (filteredTags.isEmpty() && tagQuery.isNotEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("Create tag \"$tagQuery\"") },
                                            onClick = {
                                                onCreateTag(tagQuery)
                                                pendingTagName = tagQuery
                                                showTagDropdown = false
                                            }
                                        )
                                    }
                                    filteredTags.forEach { tag ->
                                        DropdownMenuItem(
                                            text = { Text(tag.name) },
                                            onClick = {
                                                selectedTagIds = selectedTagIds + tag.id
                                                tagQuery = ""
                                                showTagDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showReceiptInput) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (receiptData != null) {
                                Box(
                                    Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { showReceiptPreview = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (receiptData!!.startsWith("data:image")) {
                                        val bitmap = remember(receiptData) {
                                            runCatching {
                                                val b64 = receiptData!!.substringAfter("base64,")
                                                val bytes = Base64.decode(b64, Base64.DEFAULT)
                                                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            }.getOrNull()
                                        }
                                        if (bitmap != null) {
                                            Image(bitmap.asImageBitmap(), "Receipt", modifier = Modifier.fillMaxSize())
                                        } else {
                                            Icon(Icons.Default.Image, null)
                                        }
                                    } else {
                                        Icon(Icons.Default.PictureAsPdf, null)
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { filePicker.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.AddAPhoto, null)
                                Spacer(Modifier.width(8.dp))
                                Text(if (receiptData == null) "Add Receipt" else "Change")
                            }
                        }
                    }

                    if (showSplitInput) {
                        // Split Summary Header
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (kotlin.math.abs(splitRemaining) < 0.01) MaterialTheme.colorScheme.primaryContainer
                                else if (splitRemaining < 0) MaterialTheme.colorScheme.errorContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Amount", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, mainAmount)}",
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Allocated", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, splitTotal)}",
                                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Remaining", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        "${CurrencyUtils.getCurrencySymbol(currency)}${"%.2f".format(Locale.US, splitRemaining)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (kotlin.math.abs(splitRemaining) < 0.01) MaterialTheme.colorScheme.primary
                                        else if (splitRemaining < 0) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            splitRows.forEachIndexed { index, row ->
                                SplitRowCard(
                                    row = row,
                                    allCategories = categories,
                                    type = type,
                                    onUpdate = { updated -> splitRows = splitRows.toMutableList().also { it[index] = updated } },
                                    onRemove = { splitRows = splitRows.toMutableList().also { it.removeAt(index) } },
                                    showDropdown = showSplitCategoryDropdown == row.localId,
                                    onToggleDropdown = { showSplitCategoryDropdown = if (showSplitCategoryDropdown == row.localId) null else row.localId },
                                    remainingAmount = splitRemaining,
                                    currencySymbol = CurrencyUtils.getCurrencySymbol(currency)
                                )
                            }
                            TextButton(onClick = { splitRows = splitRows + SplitRowData(splitIdCounter++) }) {
                                Icon(Icons.Default.Add, null)
                                Text(" Add Split")
                            }
                        }
                    }

                    Spacer(Modifier.height(80.dp)) // Extra space for FAB/Bottom bar
                }

                // 6. Footer: Save Button
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }
                            Button(
                                onClick = {
                                    val tx = buildTransaction() ?: return@Button
                                    if (splitEnabled) {
                                        onConfirm(tx, buildSplitChildren(tx.id))
                                    } else {
                                        onConfirm(tx, null)
                                    }
                                },
                                modifier = Modifier.weight(2f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = (amount.toDoubleOrNull() ?: try { evaluateExpression(amount) } catch(e:Exception) { null }) != null && selectedAccountId != null &&
                                        (!splitEnabled || kotlin.math.abs(splitRemaining) < 0.001) &&
                                        (type != "lend" && type != "receive" || selectedPeerId != null)
                            ) {
                                Text(if (isEdit) "UPDATE" else "SAVE", fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!isEdit) {
                            Button(
                                onClick = {
                                    val tx = buildTransaction() ?: return@Button
                                    if (splitEnabled) {
                                        onSaveAndDuplicate(tx, buildSplitChildren(tx.id))
                                    } else {
                                        onSaveAndDuplicate(tx, null)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                enabled = (amount.toDoubleOrNull() ?: try { evaluateExpression(amount) } catch(e:Exception) { null }) != null && selectedAccountId != null &&
                                        (!splitEnabled || kotlin.math.abs(splitRemaining) < 0.001) &&
                                        (type != "lend" && type != "receive" || selectedPeerId != null)
                            ) {
                                Text("SAVE & DUPLICATE", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SplitRowCard(
    row: SplitRowData,
    allCategories: List<CategoryEntity>,
    type: String,
    onUpdate: (SplitRowData) -> Unit,
    onRemove: () -> Unit,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    remainingAmount: Double = 0.0,
    currencySymbol: String = "₹",
) {
    var showSubDropdown by remember { mutableStateOf(false) }
    val parentCategories = remember(allCategories, type) {
        allCategories.filter { it.parentId == null && (it.type == type || (type == "savings" && it.type == "expense")) }
    }
    val selectedParent = remember(row.categoryId, allCategories) {
        allCategories.find { it.id == row.categoryId }
    }
    val subCategories = remember(selectedParent, allCategories) {
        selectedParent?.let { parent -> allCategories.filter { it.parentId == parent.id } } ?: emptyList()
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Category Dropdown
                ExposedDropdownMenuBox(showDropdown, { if (it) onToggleDropdown() else onToggleDropdown() }, Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedParent?.let { "${it.emoji} ${it.name}" } ?: "Category",
                        onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showDropdown) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                    )
                    ExposedDropdownMenu(showDropdown, onToggleDropdown) {
                        DropdownMenuItem(text = { Text("None") }, onClick = { onUpdate(row.copy(categoryId = null, subCategoryId = null)); onToggleDropdown() })
                        parentCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text("${cat.emoji} ${cat.name}") },
                                onClick = { onUpdate(row.copy(categoryId = cat.id, subCategoryId = null)); onToggleDropdown() }
                            )
                        }
                    }
                }

                // Sub-category Dropdown
                if (subCategories.isNotEmpty()) {
                    ExposedDropdownMenuBox(showSubDropdown, { showSubDropdown = it }, Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = allCategories.find { it.id == row.subCategoryId }?.name ?: "Sub-cat (Opt)",
                            onValueChange = {}, readOnly = true, label = { Text("Sub-category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showSubDropdown) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable, true).fillMaxWidth()
                        )
                        ExposedDropdownMenu(showSubDropdown, { showSubDropdown = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { onUpdate(row.copy(subCategoryId = null)); showSubDropdown = false })
                            subCategories.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name) },
                                    onClick = { onUpdate(row.copy(subCategoryId = sub.id)); showSubDropdown = false }
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.description, onValueChange = { onUpdate(row.copy(description = it)) },
                    label = { Text("Description") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = row.amount, onValueChange = { onUpdate(row.copy(amount = it)) },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(100.dp), singleLine = true
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$currencySymbol${"%.2f".format(Locale.US, remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (kotlin.math.abs(remainingAmount) < 0.01) MaterialTheme.colorScheme.primary
                        else if (remainingAmount < 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove split", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

// ---------------------------------------------------------------------------
@Composable
fun ReceiptPreviewDialog(path: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Receipt Preview") },
        text = {
            val context = LocalContext.current
            val bitmap = remember(path) {
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Receipt",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                )
            } else {
                Text("Could not load image")
            }
        }
    )
}

