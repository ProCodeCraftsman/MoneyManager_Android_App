package com.moneymanager.app.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.components.ScrollToTopBox
import com.moneymanager.app.ui.theme.AppTheme
import com.moneymanager.data.repository.ExportType
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
    onNavigateToPeers: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToGoals: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToAiHistory: () -> Unit = {},
    onNavigateToAiModels: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var showCsvTypeDialog by remember { mutableStateOf(false) }
    var showThemeDropdown by remember { mutableStateOf(false) }
    var showAutoLockDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var pendingCsvAction by remember { mutableStateOf<String?>(null) }
    var selectedCsvType by remember { mutableStateOf<ExportType?>(null) }
    val lazyListState = rememberLazyListState()

    val currencies = listOf("INR", "USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "BRL")
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    val csvTypes = listOf(
        "Accounts" to ExportType.ACCOUNTS,
        "Transactions" to ExportType.TRANSACTIONS,
        "Categories" to ExportType.CATEGORIES,
        "Budgets" to ExportType.BUDGETS,
        "Goals" to ExportType.GOALS,
        "Tags" to ExportType.TAGS,
        "Peers" to ExportType.PEERS,
        "Recurring" to ExportType.RECURRING,
        "All Data" to ExportType.ALL
    )

    val jsonPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.importFromJson(it) }
    }

    val csvPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { selectedCsvType?.let { type -> viewModel.importFromCsv(type, it) } }
    }

    val createCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        uri?.let { selectedCsvType?.let { type -> viewModel.exportToCsv(type, it) } }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        uri?.let { viewModel.exportToJson(it) }
    }

    LaunchedEffect(uiState.importResult, uiState.exportResult) {
        if (uiState.importResult != null || uiState.exportResult != null) {
        }
    }

    fun onCsvExportClick() {
        showCsvTypeDialog = true
        pendingCsvAction = "export"
    }

    fun onCsvImportClick() {
        showCsvTypeDialog = true
        pendingCsvAction = "import"
    }

    fun onCsvTypeSelected(type: ExportType) {
        selectedCsvType = type
        showCsvTypeDialog = false
        if (pendingCsvAction == "export") {
            createCsvLauncher.launch("money_manager_${type.name.lowercase()}.csv")
        } else {
            csvPicker.launch(arrayOf("text/csv", "text/comma-separated-values", "application/csv", "text/plain"))
        }
        pendingCsvAction = null
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.clearResults() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        ScrollToTopBox(lazyListState = lazyListState, modifier = Modifier.padding(padding)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item {
                    ProfileHeader(
                        isSignedIn = uiState.isSignedIn,
                        userName = uiState.userName,
                        userEmail = uiState.userEmail,
                        userPhone = uiState.userPhone,
                        syncStatus = uiState.syncStatus,
                        lastSyncTime = uiState.lastSyncTime,
                        dateFormat = dateFormat,
                        onSignInClick = { showSignInDialog = true },
                        onSignOutClick = { viewModel.signOut() },
                        onSyncClick = { viewModel.triggerSync() }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionTile(
                            icon = Icons.Default.AccountBalance,
                            label = "Accounts",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToAccounts
                        )
                        QuickActionTile(
                            icon = Icons.Default.LocalOffer,
                            label = "Categories",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCategories
                        )
                        QuickActionTile(
                            icon = Icons.Default.PieChart,
                            label = "Budgets",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToBudgets
                        )
                        QuickActionTile(
                            icon = Icons.Default.Flag,
                            label = "Goals",
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToGoals
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    SettingsSectionHeader(title = "Master Data")
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.LocalOffer,
                        title = "Tags",
                        subtitle = "Manage labels for transactions",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onNavigateToTags
                    )
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.People,
                        title = "Peers",
                        subtitle = "Manage lending/borrowing partners",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onNavigateToPeers
                    )
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.Repeat,
                        title = "Recurring Transactions",
                        subtitle = "Manage scheduled payments",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onNavigateToRecurring
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                item {
                    SettingsSectionHeader(title = "AI Features")
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.AutoAwesome,
                        title = "AI Models",
                        subtitle = "Download, manage, and configure AI models",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onNavigateToAiModels
                    )
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.History,
                        title = "AI Conversation History",
                        subtitle = "Review prompts and responses from AI Fill",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = onNavigateToAiHistory
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                item {
                    SettingsSectionHeader(title = "Security")
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "PIN Lock",
                        subtitle = "Require a 4-digit PIN to open the app",
                        trailing = {
                            Switch(
                                checked = uiState.pinEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled && !uiState.pinEnabled && uiState.pinHash == null) {
                                        showPinSetupDialog = true
                                    } else {
                                        viewModel.setPinEnabled(enabled)
                                    }
                                }
                            )
                        }
                    )
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.Fingerprint,
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
                    SettingsRow(
                        icon = Icons.Default.Schedule,
                        title = "Auto-lock",
                        subtitle = if (uiState.autoLockMinutes <= 0) "Off"
                                   else "$uiState.autoLockMinutes min",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { showAutoLockDialog = true }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                item {
                    SettingsSectionHeader(title = "Appearance")
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = uiState.selectedTheme.displayName,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.selectedTheme.displayName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        onClick = { showThemeDropdown = true }
                    )
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.DarkMode,
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
                    SettingsRow(
                        icon = Icons.Default.AttachMoney,
                        title = "Currency",
                        subtitle = uiState.currency,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.currency,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        onClick = { showCurrencyDialog = true },
                        showDivider = false
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                item {
                    SettingsSectionHeader(title = "Data Management")
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.Upload,
                        title = "Export JSON Backup",
                        subtitle = "Save your data to a file",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { createJsonLauncher.launch("money_manager_backup.json") }
                    )
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.Download,
                        title = "Import JSON Backup",
                        subtitle = "Restore from a backup file",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { jsonPicker.launch(arrayOf("application/json", "text/plain")) }
                    )
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.Upload,
                        title = "Export CSV",
                        subtitle = "Export data to CSV by type",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onCsvExportClick() }
                    )
                }

                item {
                    SettingsRow(
                        icon = Icons.Default.Download,
                        title = "Import CSV",
                        subtitle = "Import data from CSV by type",
                        trailing = {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        onClick = { onCsvImportClick() },
                        showDivider = false
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(7.dp))
                }
            }
        }
    }

    if (showThemeDropdown) {
        AlertDialog(
            onDismissRequest = { showThemeDropdown = false },
            title = { Text("Choose Theme") },
            text = {
                Column {
                    AppTheme.entries.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setSelectedTheme(theme)
                                    showThemeDropdown = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = theme.displayName,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (theme == uiState.selectedTheme) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showThemeDropdown = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Select Currency") },
            text = {
                Column {
                    currencies.forEach { currency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setCurrency(currency)
                                    showCurrencyDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            if (currency == uiState.currency) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

    if (showCsvTypeDialog) {
        AlertDialog(
            onDismissRequest = {
                showCsvTypeDialog = false
                pendingCsvAction = null
            },
            title = { Text(if (pendingCsvAction == "export") "Select Export Type" else "Select Import Type") },
            text = {
                Column {
                    csvTypes.forEach { (label, type) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCsvTypeSelected(type) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = {
                    showCsvTypeDialog = false
                    pendingCsvAction = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAutoLockDialog) {
        val options = listOf(0 to "Off", 1 to "1 min", 5 to "5 min", 15 to "15 min", 30 to "30 min")
        AlertDialog(
            onDismissRequest = { showAutoLockDialog = false },
            title = { Text("Auto-lock Timer") },
            text = {
                Column {
                    options.forEach { (minutes, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setAutoLockMinutes(minutes)
                                    showAutoLockDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                            if (minutes == uiState.autoLockMinutes) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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

    if (showPinSetupDialog) {
        var setupPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var setupError by remember { mutableStateOf<String?>(null) }
        var setupStep by remember { mutableIntStateOf(0) }
        val maxPinLength = 4

        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text(if (setupStep == 0) "Create PIN" else "Confirm PIN") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (setupStep == 0) "Enter a 4-digit PIN" else "Re-enter your PIN",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = if (setupStep == 0) setupPin else confirmPin,
                        onValueChange = { value ->
                            if (value.length <= maxPinLength && value.all { it.isDigit() }) {
                                if (setupStep == 0) {
                                    setupPin = value
                                    if (value.length == maxPinLength) {
                                        setupStep = 1
                                    }
                                } else {
                                    confirmPin = value
                                }
                                setupError = null
                            }
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.width(200.dp)
                    )
                    if (setupError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = setupError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (setupStep == 1 && confirmPin.length == maxPinLength) {
                            if (setupPin == confirmPin) {
                                viewModel.createAndEnablePin(confirmPin)
                                showPinSetupDialog = false
                            } else {
                                setupError = "PINs don't match. Try again."
                                setupPin = ""
                                confirmPin = ""
                                setupStep = 0
                            }
                        }
                    },
                    enabled = (setupStep == 0 && setupPin.length == maxPinLength) ||
                              (setupStep == 1 && confirmPin.length == maxPinLength)
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPinSetupDialog = false
                    viewModel.setPinEnabled(false)
                }) {
                    Text("Cancel")
                }
            }
        )
    }

}

@Composable
private fun ProfileHeader(
    isSignedIn: Boolean,
    userName: String?,
    userEmail: String?,
    userPhone: String?,
    syncStatus: SyncStatus,
    lastSyncTime: Long?,
    dateFormat: SimpleDateFormat,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onSyncClick: () -> Unit,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            if (isSignedIn) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (userName?.firstOrNull()?.uppercase() ?: "?"),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        if (userEmail != null) {
                            Text(
                                text = userEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                        if (userPhone != null) {
                            Text(
                                text = userPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            )
                        }
                    }
                    TextButton(
                        onClick = onSignOutClick,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Sign Out", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(7.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
                Spacer(modifier = Modifier.height(7.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (syncStatus) {
                            is SyncStatus.Offline -> Icons.Default.CloudOff
                            is SyncStatus.Syncing -> Icons.Default.CloudSync
                            is SyncStatus.Success, is SyncStatus.Idle -> Icons.Default.CloudDone
                            is SyncStatus.Error -> Icons.Default.CloudOff
                        },
                        contentDescription = null,
                        tint = when (syncStatus) {
                            is SyncStatus.Error -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        },
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (syncStatus) {
                                is SyncStatus.Idle -> if (isSignedIn) "Ready to sync" else "Not signed in"
                                is SyncStatus.Syncing -> "Syncing..."
                                is SyncStatus.Success -> "Synced"
                                is SyncStatus.Error -> "Sync failed"
                                is SyncStatus.Offline -> "Offline"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        if (lastSyncTime != null && syncStatus !is SyncStatus.Error) {
                            Text(
                                text = "Last sync: ${dateFormat.format(Date(lastSyncTime))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                            )
                        }
                    }
                    if (syncStatus is SyncStatus.Syncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        FilledTonalIconButton(
                            onClick = onSyncClick,
                            modifier = Modifier.size(32.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = "Sync now",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Guest",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Sign in to sync your data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        )
                    }
                    OutlinedButton(
                        onClick = onSignInClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f))
                    ) {
                        Text("Sign In", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick)
                    else Modifier
                )
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
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 50.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        }
    }
}
