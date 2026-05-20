package com.moneymanager.app.ui.transactions

import android.speech.SpeechRecognizer
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.moneymanager.app.R
import com.moneymanager.app.ui.components.ScrollToTopBox
import com.moneymanager.app.ui.components.SplitTransactionCard
import com.moneymanager.app.ui.components.TransactionDetailSheet
import com.moneymanager.app.ui.components.TransactionsFilterControlsSheet
import com.moneymanager.app.ui.dialogs.AddEditTransactionDialog
import com.moneymanager.app.ui.transactions.components.AiDownloadConsentDialog
import com.moneymanager.app.ui.transactions.components.DownloadProgressBanner
import com.moneymanager.app.ui.transactions.components.TransactionItem
import com.moneymanager.app.ui.dialogs.SplitRowData
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.PeerContact
import com.moneymanager.data.entity.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    initialPeerId: Long? = null,
    isAiAssistAvailable: Boolean = false,
    onNavigateToAiDraftSms: () -> Unit = {},
    onNavigateToAiDraftReceipt: () -> Unit = {},
    onNavigateToAiDraftVoice: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Download consent + progress state (Phase 40 — additive)
    val showDownloadConsent by viewModel.showDownloadConsentDialog.collectAsStateWithLifecycle()
    val isDownloading by viewModel.isDownloading.collectAsStateWithLifecycle()
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadProgressCaption by viewModel.downloadProgressCaption.collectAsStateWithLifecycle()
    val downloadProgressPercent by viewModel.downloadProgressPercent.collectAsStateWithLifecycle()
    val downloadError by viewModel.downloadError.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect paginated transactions as LazyPagingItems
    val pagingTransactions = viewModel.transactionsPagingData.collectAsLazyPagingItems()

    // Snapshot of currently loaded items for summary calculations and grouping
    val visibleTransactions = remember(pagingTransactions.itemSnapshotList) {
        pagingTransactions.itemSnapshotList.filterNotNull()
            .filter { !it.isSplitChild }
    }

    var currentPeriodStart by remember { mutableStateOf<Long?>(null) }
    var currentPeriodEnd by remember { mutableStateOf<Long?>(null) }

    val periodTransactions = remember(visibleTransactions, currentPeriodStart, currentPeriodEnd) {
        val start = currentPeriodStart
        val end = currentPeriodEnd
        if (start != null && end != null) {
            visibleTransactions.filter { it.date in start..end }
        } else {
            visibleTransactions
        }
    }
    val periodIncome = remember(periodTransactions) {
        periodTransactions.filter { it.type == "income" }.sumOf { it.amount }
    }
    val periodExpense = remember(periodTransactions) {
        periodTransactions.filter { it.type == "expense" }.sumOf { it.amount }
    }
    val periodCount = periodTransactions.size

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
    var aiDraftExpanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(initialType != null) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var viewingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var searchText by remember { mutableStateOf("") }
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    // BUG-03 fix: showTransferDialog removed — TransferDialog was never triggered and is dead
    // code. Transfer type is handled by AddEditTransactionDialog (type = "transfer").
    var preselectedType by remember { mutableStateOf(initialType) }
    var timeFilter by remember { mutableStateOf("Month") }
    
    val currentPeriodName = remember(timeFilter, currentPeriodStart) {
        when (timeFilter) {
            "Day" -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                .format(Date(currentPeriodStart ?: System.currentTimeMillis()))
            "Week" -> {
                val start = Calendar.getInstance().apply { timeInMillis = currentPeriodStart ?: System.currentTimeMillis() }
                val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 6) }
                "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(start.time)} - ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(end.time)}"
            }
            "Month" -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                .format(Date(currentPeriodStart ?: System.currentTimeMillis()))
            "Year" -> SimpleDateFormat("yyyy", Locale.getDefault())
                .format(Date(currentPeriodStart ?: System.currentTimeMillis()))
            "All" -> "All Time"
            "Custom" -> "Custom Range"
            else -> timeFilter
        }
    }

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
            // BUG-04 fix: "All" must explicitly clear the date range in the ViewModel.
            "All" -> {
                currentPeriodStart = null
                currentPeriodEnd = null
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

    LaunchedEffect(showAddDialog) {
        if (showAddDialog) aiDraftExpanded = false
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

    // Show download error as long-duration snackbar (Phase 40)
    LaunchedEffect(downloadError) {
        downloadError?.let { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long) }
    }

    // Consent dialog — shown as sibling to Scaffold (not inside it)
    if (showDownloadConsent) {
        AiDownloadConsentDialog(
            onDownload = { viewModel.onDownloadConsented() },
            onMaybeLater = { viewModel.onDownloadPromptSuppressed() }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                tonalElevation = 1.dp,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                    // Title and Main Actions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Transactions",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { 
                                isSearchVisible = !isSearchVisible
                                if (!isSearchVisible) {
                                    searchText = ""
                                    viewModel.setSearchQuery("")
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Box {
                                if (activeFilterCount > 0) {
                                    BadgedBox(badge = { Badge { Text(activeFilterCount.toString()) } }) {
                                        IconButton(onClick = { showFilterSheet = true }) {
                                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                                        }
                                    }
                                } else {
                                    IconButton(onClick = { showFilterSheet = true }) {
                                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                                    }
                                }
                            }
                        }
                    }

                    // Search Bar
                    AnimatedVisibility(visible = isSearchVisible) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                BasicTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it; viewModel.setSearchQuery(it) },
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodyLarge,
                                    singleLine = true,
                                    decorationBox = { innerTextField ->
                                        if (searchText.isEmpty()) {
                                            Text("Search transactions...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = { searchText = ""; viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            }
                        }
                    }

                    // Centered Month Navigation
                    if (timeFilter != "All" && timeFilter != "Custom") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { navigatePrevious() }) {
                                Icon(Icons.Default.ChevronLeft, "Previous", tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = currentPeriodName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(onClick = { navigateNext() }) {
                                Icon(Icons.Default.ChevronRight, "Next", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Summary Metrics Row
                    if (uiState.showSummary && timeFilter != "All") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SummaryMetricCompact(
                                label = "Income",
                                value = currencyFormat.format(periodIncome),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryMetricCompact(
                                label = "Expense",
                                value = currencyFormat.format(periodExpense),
                                color = MaterialTheme.colorScheme.error
                            )
                            Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outlineVariant)
                            SummaryMetricCompact(
                                label = "Count",
                                value = "$periodCount",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 50.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (isAiAssistAvailable) {
                    AnimatedVisibility(
                        visible = aiDraftExpanded,
                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallFloatingActionButton(
                                onClick = { onNavigateToAiDraftSms(); aiDraftExpanded = false },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(Icons.Default.Sms, contentDescription = "AI Draft from SMS")
                            }
                            SmallFloatingActionButton(
                                onClick = { onNavigateToAiDraftReceipt(); aiDraftExpanded = false },
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = "AI Draft from Receipt")
                            }
                            if (SpeechRecognizer.isRecognitionAvailable(LocalContext.current)) {
                                SmallFloatingActionButton(
                                    onClick = { onNavigateToAiDraftVoice(); aiDraftExpanded = false },
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ) {
                                    Icon(Icons.Default.Mic, contentDescription = "AI Draft from Voice")
                                }
                            }
                        }
                    }
                    FloatingActionButton(
                        onClick = { aiDraftExpanded = !aiDraftExpanded },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = CircleShape
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "AI Draft Options")
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
            // Group visible (loaded) transactions by calendar day for date headers
            val groupedTransactions = remember(visibleTransactions) {
                visibleTransactions.groupBy { tx ->
                    val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                }
            }
            val allDateMillis = groupedTransactions.keys

            LaunchedEffect(uiState.isAllExpanded, allDateMillis) {
                collapsedDates = if (uiState.isAllExpanded) {
                    emptySet()
                } else {
                    allDateMillis
                }
            }
            val headerDateFormat = remember { SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()) }

            // Determine current refresh load state
            val refreshState = pagingTransactions.loadState.refresh
            val isRefreshing = refreshState is LoadState.Loading
            val refreshError = refreshState as? LoadState.Error

            if (isRefreshing && visibleTransactions.isEmpty()) {
                // Show full-screen spinner only during initial load when no data is shown yet
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                ScrollToTopBox(lazyListState = lazyListState) {
                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {

                        // Phase 40: download progress banner as first LazyColumn item (sticky=false, scrolls away)
                        item(key = "download_banner") {
                            DownloadProgressBanner(
                                isVisible = isDownloading,
                                progress = downloadProgress,
                                captionText = downloadProgressCaption,
                                percentText = downloadProgressPercent
                            )
                        }

                        if (visibleTransactions.isEmpty() && !isRefreshing) {
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
                                            "income" -> it.amount
                                            "expense" -> -it.amount
                                            else -> 0.0
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                collapsedDates = if (isCollapsed) {
                                                    collapsedDates - dateMillis
                                                } else {
                                                    collapsedDates + dateMillis
                                                }
                                            }
                                            .padding(horizontal = 20.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = headerDateFormat.format(Date(dateMillis)),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = when {
                                                    kotlin.math.abs(dailyTotal) < 0.01 -> currencyFormat.format(0.0)
                                                    dailyTotal > 0 -> " Total + ${currencyFormat.format(dailyTotal)}"
                                                    else -> "Total - ${currencyFormat.format(-dailyTotal)}"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
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
                                                onClickChild = { viewingTransaction = it }
                                            )
                                        } else {
                                            TransactionItem(
                                                transaction = tx,
                                                accounts = uiState.allAccounts,
                                                categories = uiState.allCategories,
                                                peers = uiState.allPeers,
                                                currencyFormat = currencyFormat,
                                                showCategory = uiState.showCategories,
                                                onClick = { viewingTransaction = it }
                                            )
                                        }
                                    }
                                }
                            }

                            // Trigger paging: access item at the edge of the loaded window
                            item {
                                LaunchedEffect(pagingTransactions.itemCount) {
                                    if (pagingTransactions.itemCount > 0) {
                                        // Access an item just beyond visible to trigger prefetch
                                        pagingTransactions[pagingTransactions.itemCount - 1]
                                    }
                                }
                                // Show append load state indicator
                                when (val appendState = pagingTransactions.loadState.append) {
                                    is LoadState.Loading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                    is LoadState.Error -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Error loading more transactions",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                } // ScrollToTopBox
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
            categoryUsageCounts = uiState.categoryUsageCounts,
            onDismiss = { showAddDialog = false },
            onConfirm = { tx, children ->
                if (children != null) viewModel.addSplitTransaction(tx, children)
                else viewModel.addTransaction(tx)
                showAddDialog = false
            },
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
            categoryUsageCounts = uiState.categoryUsageCounts,
            onDismiss = { editingTransaction = null },
            onConfirm = { tx, children ->
                viewModel.updateTransaction(editing, tx, children)
                editingTransaction = null
            },
        )
    }

    viewingTransaction?.let { viewing ->
        TransactionDetailSheet(
            transaction = viewing,
            accounts = uiState.allAccounts,
            categories = uiState.allCategories,
            tags = uiState.allTags,
            peers = uiState.allPeers,
            goals = uiState.allGoals,
            currency = uiState.currency,
            onDismiss = { viewingTransaction = null },
            onEdit = {
                viewingTransaction = null
                editingTransaction = viewing
            },
            onDelete = {
                viewingTransaction = null
                viewModel.deleteTransaction(viewing)
            }
        )
    }

    if (showFilterSheet) {
        TransactionsFilterControlsSheet(
            onDismiss = { showFilterSheet = false },
            currentPeriodName = currentPeriodName,
            onPrevPeriod = { navigatePrevious() },
            onNextPeriod = { navigateNext() },
            onPeriodTypeSelected = { 
                timeFilter = it.replace("This ", "").replace("Last ", "")
                if (it == "Last Month") {
                    val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                    updatePeriodBasedOnFilter("Month", cal)
                } else if (it == "This Month") {
                    updatePeriodBasedOnFilter("Month")
                } else {
                    updatePeriodBasedOnFilter(timeFilter)
                }
            },
            selectedPeriodType = when {
                timeFilter == "Month" -> "This Month"
                else -> timeFilter
            },
            isAllExpanded = uiState.isAllExpanded,
            onToggleExpand = { viewModel.toggleAllExpanded() },
            sortBy = uiState.sortBy,
            onSortBySelected = { viewModel.setSortBy(it) },
            showSummary = uiState.showSummary,
            onToggleSummary = { viewModel.setShowSummary(it) },
            showCategories = uiState.showCategories,
            onToggleCategories = { viewModel.setShowCategories(it) },
            selectedAccountName = uiState.allAccounts.find { it.id == uiState.filterAccountId }?.name ?: "All Accounts",
            onSelectAccount = { /* Logic to show account picker if needed */ },
            selectedCategoryName = uiState.allCategories.find { it.id == uiState.filterCategoryId }?.name ?: "All Categories",
            onSelectCategory = { /* Logic to show category picker */ },
            selectedTransactionType = uiState.filterType.ifEmpty { "All" },
            onSelectTransactionType = { /* Logic to show type picker */ },
            selectedTagsLabel = "All",
            onSelectTags = { },
            onResetAll = { viewModel.clearAllFilters() },
            onApply = { showFilterSheet = false }
        )
    }
}

@Composable
fun SummaryMetricCompact(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
