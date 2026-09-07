package com.moneymanager.app.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.moneymanager.app.ui.components.ScrollToTopBox
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.util.accountTypeIcon
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.data.entity.AccountEntity
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember(uiState.currencyCode) {
        CurrencyUtils.getCurrencyFormat(uiState.currencyCode)
    }
    val showAddDialog = remember { mutableStateOf(value = false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToArchive by remember { mutableStateOf<AccountEntity?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Archived
    val snackbarHostState = remember { SnackbarHostState() }
    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AccountEvent.Success -> snackbarHostState.showSnackbar(event.message)
                is AccountEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accounts", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddDialog.value = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account")
                }
            }
        },
    ) { padding ->
        ScrollToTopBox(lazyListState = lazyListState, modifier = Modifier.padding(padding)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    TotalAssetsHeader(
                        totalAssets = uiState.totalAssets,
                        currencyFormat = currencyFormat,
                        accountCount = uiState.activeAccounts.size
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                item {
                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Active Accounts (${uiState.activeAccounts.size})") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Archived (${uiState.archivedAccounts.size})") }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (selectedTab == 0) {
                    if (uiState.accountComparisonData.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                )
                            ) {
                                com.moneymanager.app.ui.components.AccountComparisonChart(
                                    data = uiState.accountComparisonData,
                                    currencyCode = uiState.currencyCode,
                                    selectedTypeFilter = uiState.selectedChartTypeFilter,
                                    onFilterSelected = { viewModel.setChartTypeFilter(it) }
                                )
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    val liquidAccounts = uiState.activeAccounts.filter { it.type != "credit" && it.type != "savings" && it.type != "peer" }
                    val creditAccounts = uiState.activeAccounts.filter { it.type == "credit" || it.type == "peer" }
                    val investmentAccounts = uiState.activeAccounts.filter { it.type == "savings" }

                    if (liquidAccounts.isNotEmpty()) {
                        item { AccountsSectionHeader(title = "Bank & Cash Accounts") }
                        items(liquidAccounts) { account ->
                            AccountCard(
                                account = account,
                                currencyFormat = currencyFormat,
                                onClick = { editingAccount = account },
                                onArchiveClick = { accountToArchive = account }
                            )
                        }
                    }

                    if (investmentAccounts.isNotEmpty()) {
                        item { AccountsSectionHeader(title = "Investment Platforms") }
                        items(investmentAccounts) { account ->
                            AccountCard(
                                account = account,
                                currencyFormat = currencyFormat,
                                onClick = { editingAccount = account },
                                onArchiveClick = { accountToArchive = account }
                            )
                        }
                    }

                    if (creditAccounts.isNotEmpty()) {
                        item { AccountsSectionHeader(title = "Credit Cards & Loans") }
                        items(creditAccounts) { account ->
                            AccountCard(
                                account = account,
                                currencyFormat = currencyFormat,
                                onClick = { editingAccount = account },
                                onArchiveClick = { accountToArchive = account }
                            )
                        }
                    }

                    if (uiState.activeAccounts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No active accounts. Tap + to add one.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Archived Accounts tab
                    if (uiState.archivedAccounts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No archived accounts.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        items(uiState.archivedAccounts) { account ->
                            AccountCard(
                                account = account,
                                currencyFormat = currencyFormat,
                                isArchivedView = true,
                                onClick = { editingAccount = account },
                                onReactivateClick = { viewModel.reactivateAccount(account.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showAddDialog.value) {
        AddEditAccountDialog(
            onDismiss = { showAddDialog.value = false },
            onConfirm = { name, type, balance ->
                viewModel.addAccount(name, type, "account_balance", "material", balance)
                showAddDialog.value = false
            }
        )
    }

    editingAccount?.let { account ->
        AddEditAccountDialog(
            account = account,
            onDismiss = { editingAccount = null },
            onConfirm = { name, type, balance ->
                viewModel.updateAccount(account.copy(name = name, type = type, balance = balance))
                editingAccount = null
            }
        )
    }

    accountToArchive?.let { account ->
        AlertDialog(
            onDismissRequest = { accountToArchive = null },
            title = { Text("Archive Account?") },
            text = { Text("Archiving '${account.name}' will hide it from new transaction entry lists while preserving all historical data and reports.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.archiveAccount(account.id)
                    accountToArchive = null
                }) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToArchive = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TotalAssetsHeader(
    totalAssets: Double,
    currencyFormat: NumberFormat,
    accountCount: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Total Assets",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currencyFormat.format(totalAssets),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = accountCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = if (accountCount == 1) "Active Acc" else "Active Accs",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun AccountsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

@Composable
private fun AccountCard(
    account: AccountEntity,
    currencyFormat: NumberFormat,
    isArchivedView: Boolean = false,
    onClick: () -> Unit,
    onArchiveClick: (() -> Unit)? = null,
    onReactivateClick: (() -> Unit)? = null
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = accountTypeIcon(account.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = account.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    if (isArchivedView) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        ) {
                            Text(
                                text = "Archived",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = account.type.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currencyFormat.format(account.balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (account.balance >= 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
            )
            if (isArchivedView && onReactivateClick != null) {
                IconButton(onClick = onReactivateClick) {
                    Icon(
                        Icons.Default.Unarchive,
                        contentDescription = "Reactivate Account",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else if (!isArchivedView && onArchiveClick != null) {
                IconButton(onClick = onArchiveClick) {
                    Icon(
                        Icons.Default.Archive,
                        contentDescription = "Archive Account",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 50.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountDialog(
    account: AccountEntity? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, balance: Double) -> Unit
) {
    var name by remember { mutableStateOf(account?.name ?: "") }
    var type by remember { mutableStateOf(account?.type ?: "bank") }
    var balance by remember { mutableStateOf(account?.balance?.toString() ?: "") }
    val types = listOf("bank", "cash", "credit", "savings")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (account == null) "Add Account" else "Edit Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Account Name") },
                    singleLine = true
                )
                var expanded by remember { mutableStateOf(value = false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = type.replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            accountTypeIcon(t),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(t.replaceFirstChar { it.uppercase() })
                                    }
                                },
                                onClick = {
                                    type = t
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it },
                    label = { Text("Balance") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bal = balance.toDoubleOrNull() ?: 0.0
                    onConfirm(name, type, bal)
                },
                enabled = name.isNotBlank()
            ) {
                Text(if (account == null) "Add" else "Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
