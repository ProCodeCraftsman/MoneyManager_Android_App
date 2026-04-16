package com.moneymanager.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.moneymanager.data.entity.RecurringEntity
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringListScreen(
    viewModel: RecurringViewModel,
    navController: NavController? = null,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val coroutineScope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<RecurringEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recurring Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController?.navigate("recurring_form")
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (uiState.recurringList.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No recurring transactions",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.recurringList) { recurring ->
                    RecurringItem(
                        recurring = recurring,
                        currencyFormat = currencyFormat,
                        dateFormat = dateFormat,
                        onToggleActive = {
                            coroutineScope.launch {
                                viewModel.toggleActive(recurring)
                            }
                        },
                        onClick = {
                            navController?.navigate("recurring_form?recurringId=${recurring.id}")
                        },
                        onDelete = { showDeleteDialog = recurring }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    showDeleteDialog?.let { recurring ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Recurring") },
            text = { Text("Are you sure you want to delete this recurring transaction?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            viewModel.deleteRecurring(recurring)
                        }
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun RecurringItem(
    recurring: RecurringEntity,
    currencyFormat: NumberFormat,
    dateFormat: SimpleDateFormat,
    onToggleActive: () -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recurring.note.ifEmpty { "Recurring Transaction" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${recurring.type.replaceFirstChar { it.uppercase() }} - ${recurring.frequency}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = currencyFormat.format(recurring.amount),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (recurring.type == "income") MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Next: ${dateFormat.format(Date(recurring.nextDate))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = recurring.isActive,
                    onCheckedChange = { onToggleActive() }
                )
                TextButton(onClick = onDelete) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}