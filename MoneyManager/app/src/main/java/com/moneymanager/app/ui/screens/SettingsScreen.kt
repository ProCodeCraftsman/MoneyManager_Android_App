package com.moneymanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.data.sync.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToTags: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToTemplates: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }

    val currencies = listOf("USD", "EUR", "GBP", "JPY", "INR", "CAD", "AUD", "CHF", "CNY", "BRL")
    val autoLockOptions = listOf("Never" to 0, "1 minute" to 1, "5 minutes" to 5, "15 minutes" to 15, "30 minutes" to 30)
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Cloud Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SyncStatusCard(
                    isSignedIn = uiState.isSignedIn,
                    userEmail = uiState.userEmail,
                    userName = uiState.userName,
                    syncStatus = uiState.syncStatus,
                    lastSyncTime = uiState.lastSyncTime,
                    dateFormat = dateFormat,
                    onSignInClick = { showSignInDialog = true },
                    onSignOutClick = { viewModel.signOut() },
                    onSyncClick = { viewModel.triggerSync() }
                )
            }

            item {
                Text(
                    text = "Master Data",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsClickableCard(
                    title = "Accounts",
                    subtitle = "Manage your bank accounts and cash",
                    onClick = onNavigateToAccounts
                )
            }

            item {
                SettingsClickableCard(
                    title = "Categories",
                    subtitle = "Expense and income categories",
                    onClick = onNavigateToCategories
                )
            }

            item {
                SettingsClickableCard(
                    title = "Tags",
                    subtitle = "Manage labels for transactions",
                    onClick = onNavigateToTags
                )
            }

            item {
                SettingsClickableCard(
                    title = "Budgets",
                    subtitle = "Set monthly spending limits",
                    onClick = onNavigateToBudgets
                )
            }

            item {
                SettingsClickableCard(
                    title = "Goals",
                    subtitle = "Track your savings goals",
                    onClick = onNavigateToGoals
                )
            }

            item {
                SettingsClickableCard(
                    title = "Recurring Transactions",
                    subtitle = "Manage scheduled payments",
                    onClick = onNavigateToRecurring
                )
            }

            item {
                SettingsClickableCard(
                    title = "Templates",
                    subtitle = "Quick transaction entry templates",
                    onClick = onNavigateToTemplates
                )
            }

            item {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsCard(
                    title = "Dark Mode",
                    subtitle = "Switch to dark color scheme",
                    trailing = {
                        Switch(
                            checked = uiState.darkMode,
                            onCheckedChange = { viewModel.setDarkMode(it) }
                        )
                    }
                )
            }

            item {
                Text(
                    text = "Privacy & Security",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsCard(
                    title = "PIN Lock",
                    subtitle = "Require a 4-digit PIN to open the app",
                    trailing = {
                        Switch(
                            checked = uiState.pinEnabled,
                            onCheckedChange = { viewModel.setPinEnabled(it) }
                        )
                    }
                )
            }

            item {
                SettingsCard(
                    title = "Biometric Unlock",
                    subtitle = "Use Face ID / Fingerprint",
                    trailing = {
                        Switch(
                            checked = uiState.biometricEnabled,
                            onCheckedChange = { viewModel.setBiometricEnabled(it) }
                        )
                    }
                )
            }

            item {
                SettingsClickableCard(
                    title = "Auto-lock",
                    subtitle = when (uiState.autoLockMinutes) {
                        0 -> "Never"
                        1 -> "1 minute"
                        else -> "$uiState.autoLockMinutes minutes"
                    },
                    onClick = { showAutoLockDialog = true }
                )
            }

            item {
                Text(
                    text = "Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsClickableCard(
                    title = "Display Currency",
                    subtitle = uiState.currency,
                    onClick = { showCurrencyDialog = true }
                )
            }

            item {
                Text(
                    text = "Data Management",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingsCard(
                    title = "Export JSON Backup",
                    subtitle = "Save your data to a file",
                    trailing = {
                        IconButton(onClick = { /* TODO: Export */ }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                )
            }

            item {
                SettingsCard(
                    title = "Import JSON Backup",
                    subtitle = "Restore from a backup file",
                    trailing = {
                        IconButton(onClick = { /* TODO: Import */ }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                )
            }

            item {
                SettingsCard(
                    title = "Export CSV",
                    subtitle = "For spreadsheet analysis",
                    trailing = {
                        IconButton(onClick = { /* TODO: Export CSV */ }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    currencies.forEach { currency ->
                        TextButton(
                            onClick = {
                                viewModel.setCurrency(currency)
                                showCurrencyDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(currency, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAutoLockDialog) {
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text("Auto-lock After") },
            text = {
                Column {
                    autoLockOptions.forEach { (label, minutes) ->
                        TextButton(
                            onClick = {
                                viewModel.setAutoLockMinutes(minutes)
                                showAutoLockDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.weight(1f))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAutoLockDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSignInDialog) {
        AlertDialog(
            onDismissRequest = { showSignInDialog = false },
            title = { Text("Sign in Required") },
            text = { Text("Please sign in with Google to enable cloud backup and sync across devices.") },
            confirmButton = {
                TextButton(onClick = { showSignInDialog = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignInDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SyncStatusCard(
    isSignedIn: Boolean,
    userEmail: String?,
    userName: String?,
    syncStatus: SyncStatus,
    lastSyncTime: Long?,
    dateFormat: SimpleDateFormat,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSyncClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (syncStatus) {
                            is SyncStatus.Offline -> Icons.Default.CloudOff
                            is SyncStatus.Syncing -> Icons.Default.CloudSync
                            is SyncStatus.Success, is SyncStatus.Idle -> Icons.Default.Cloud
                            is SyncStatus.Error -> Icons.Default.CloudOff
                        },
                        contentDescription = null,
                        tint = when (syncStatus) {
                            is SyncStatus.Offline -> MaterialTheme.colorScheme.error
                            is SyncStatus.Syncing -> MaterialTheme.colorScheme.primary
                            is SyncStatus.Success, is SyncStatus.Idle -> MaterialTheme.colorScheme.primary
                            is SyncStatus.Error -> MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Cloud Backup",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (syncStatus) {
                                is SyncStatus.Idle -> if (isSignedIn) "Ready to sync" else "Not signed in"
                                is SyncStatus.Syncing -> "Syncing..."
                                is SyncStatus.Success -> "Synced"
                                is SyncStatus.Error -> "Sync failed"
                                is SyncStatus.Offline -> "Offline"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (syncStatus) {
                                is SyncStatus.Error -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                if (isSignedIn) {
                    IconButton(onClick = onSyncClick, enabled = syncStatus !is SyncStatus.Syncing) {
                        if (syncStatus is SyncStatus.Syncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync now")
                        }
                    }
                }
            }

            if (isSignedIn && userEmail != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (lastSyncTime != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Last sync: ${dateFormat.format(Date(lastSyncTime))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (isSignedIn) {
                    TextButton(onClick = onSignOutClick) {
                        Text("Sign Out")
                    }
                } else {
                    Button(onClick = onSignInClick) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing()
        }
    }
}

@Composable
fun SettingsClickableCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
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
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}