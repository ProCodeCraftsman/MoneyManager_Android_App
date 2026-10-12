package com.moneymanager.app.ui.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.app.ui.util.Permissions
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.domain.ai.TransactionDraft
import com.moneymanager.domain.capture.PendingCapture
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The SMS draft inbox (F07 E7). Rows are grouped by day. "Save N ready" approves only fully deterministic rows, and
 * undo is offered after any approve. Tapping a row opens the full editor; the category chip is a one-tap fix.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureInboxScreen(
    onNavigateBack: () -> Unit,
    onEditDraft: (TransactionDraft) -> Unit,
    viewModel: CaptureInboxViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    var categoryFor by remember { mutableStateOf<PendingCapture?>(null) }
    var choices by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionResult(granted)
    }
    val onScan = {
        if (Permissions.isGranted(context, Permissions.READ_SMS)) viewModel.scan()
        else permissionLauncher.launch(Permissions.READ_SMS)
    }

    // Scan on open: the inbox is only useful once it has read the newest messages.
    LaunchedEffect(Unit) { onScan() }

    LaunchedEffect(state.messageId) {
        val message = state.message ?: return@LaunchedEffect
        val result = snackbarHost.showSnackbar(
            message = message,
            actionLabel = if (state.canUndo) "Undo" else null,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) viewModel.undo()
    }

    LaunchedEffect(categoryFor?.ledgerId) {
        choices = categoryFor?.let { viewModel.categoryChoices(it.draft.typeId) }.orEmpty()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SMS inbox") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = onScan, enabled = !state.scanning) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan now")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            if (state.readyCount > 0) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = viewModel::approveReady,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text("Save ${state.readyCount} ready") }
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (state.scanning) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp))
                    Text("Reading new messages", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.permissionDenied) {
                Text(
                    "SMS access was refused, so new messages cannot be read.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            if (state.rows.isEmpty() && !state.scanning) {
                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Nothing to review. Bank and card messages show up here once they are read.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            } else {
                InboxList(
                    rows = state.rows,
                    currency = state.currency,
                    onEdit = { row -> onEditDraft(row.draft.copy(captureId = row.ledgerId)) },
                    onApprove = viewModel::approve,
                    onReject = viewModel::reject,
                    onMerge = viewModel::merge,
                    onKeepBoth = viewModel::keepBoth,
                    onPickCategory = { categoryFor = it },
                )
            }
        }
    }

    categoryFor?.let { row ->
        CategoryChoiceDialog(
            choices = choices,
            currency = state.currency,
            onPick = { category ->
                viewModel.setCategory(row, category)
                categoryFor = null
            },
            onDismiss = { categoryFor = null },
        )
    }
}

@Composable
private fun InboxList(
    rows: List<PendingCapture>,
    currency: String,
    onEdit: (PendingCapture) -> Unit,
    onApprove: (PendingCapture) -> Unit,
    onReject: (PendingCapture) -> Unit,
    onMerge: (PendingCapture) -> Unit,
    onKeepBoth: (PendingCapture) -> Unit,
    onPickCategory: (PendingCapture) -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val byDay = rows.groupBy { Instant.ofEpochMilli(it.messageDate).atZone(zone).toLocalDate() }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        byDay.forEach { (day, dayRows) ->
            item(key = "day-$day") {
                Text(
                    dayLabel(day),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }
            items(dayRows, key = { it.ledgerId }) { row ->
                InboxRow(
                    row = row,
                    currency = currency,
                    onEdit = { onEdit(row) },
                    onApprove = { onApprove(row) },
                    onReject = { onReject(row) },
                    onMerge = { onMerge(row) },
                    onKeepBoth = { onKeepBoth(row) },
                    onPickCategory = { onPickCategory(row) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun InboxRow(
    row: PendingCapture,
    currency: String,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onMerge: () -> Unit,
    onKeepBoth: () -> Unit,
    onPickCategory: () -> Unit,
) {
    val draft = row.draft
    val format = CurrencyUtils.getCurrencyFormat(currency)
    val time = Instant.ofEpochMilli(row.messageDate).atZone(ZoneId.systemDefault()).toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        draft.merchantHint ?: draft.description ?: "Transaction",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "${draft.accountName ?: "No account"} · $time",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    format.format(draft.amount ?: 0.0),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryChip(draft.categoryName, onClick = onPickCategory)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onReject) { Icon(Icons.Default.Close, contentDescription = "Reject") }
                IconButton(onClick = onApprove) { Icon(Icons.Default.Check, contentDescription = "Save") }
            }
            row.maybeDuplicateOf?.let {
                HorizontalDivider()
                Text(
                    "Looks like a spend already saved, same amount nearby. Is it the same?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onMerge) { Text("Merge") }
                    TextButton(onClick = onKeepBoth) { Text("Keep both") }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(name: String?, onClick: () -> Unit) {
    // "Missing" is shown in the error colour so an unfilled category stands out in the list.
    val label = name ?: "Missing category"
    TextButton(onClick = onClick) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (name == null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun CategoryChoiceDialog(
    choices: List<CategoryEntity>,
    currency: String,
    onPick: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Category") },
        text = {
            Column {
                if (choices.isEmpty()) {
                    Text("No categories for this type yet. Open the draft to add one.", style = MaterialTheme.typography.bodyMedium)
                }
                choices.forEach { category ->
                    TextButton(onClick = { onPick(category) }, modifier = Modifier.fillMaxWidth()) {
                        Text(category.name, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun dayLabel(day: LocalDate): String {
    val today = LocalDate.now()
    return when (day) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> day.format(DateTimeFormatter.ofPattern("EEE d MMM"))
    }
}
