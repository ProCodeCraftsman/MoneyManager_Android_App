package com.moneymanager.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.moneymanager.data.entity.GoalEntity
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }
    val showAddDialog = remember { mutableStateOf(value = false) }
    var goalForContribution by remember { mutableStateOf<GoalEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Savings Goals", fontWeight = FontWeight.Bold) },
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
            FloatingActionButton(onClick = { showAddDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
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
            item { Spacer(modifier = Modifier.height(8.dp)) }

            if (uiState.goals.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No savings goals yet",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.goals) { goal ->
                    GoalCard(
                        goal = goal,
                        currencyFormat = currencyFormat,
                        onAddMoney = { goalForContribution = goal }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog.value) {
        AddGoalDialog(
            onDismiss = { showAddDialog.value = false }
        ) { name, target, deadline ->
            viewModel.addGoal(name, "🎯", target, deadline)
            showAddDialog.value = false
        }
    }

    if (goalForContribution != null) {
        AddContributionDialog(
            goal = goalForContribution!!,
            currencyFormat = currencyFormat,
            onDismiss = { goalForContribution = null },
            onConfirm = { amount ->
                viewModel.addContribution(goalForContribution!!.id, amount)
                goalForContribution = null
            }
        )
    }
}

@Composable
fun GoalCard(goal: GoalEntity, currencyFormat: NumberFormat, onAddMoney: () -> Unit) {
    val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    // Calculate deadline countdown
    val deadlineText = goal.deadline?.let { deadline ->
        val daysRemaining = ((deadline - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
        when {
            daysRemaining > 0 -> "$daysRemaining days remaining"
            daysRemaining == 0 -> "Due today!"
            else -> "${-daysRemaining} days overdue"
        }
    }
    val isOverdue = goal.deadline?.let { ((it - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt() < 0 } ?: false

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = goal.emoji, style = MaterialTheme.typography.headlineMedium)
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(text = goal.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "${currencyFormat.format(goal.currentAmount)} / ${currencyFormat.format(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onAddMoney) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Add money",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (deadlineText != null) {
                    Text(
                        text = deadlineText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, Double, Long?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf<Long?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = target.toDoubleOrNull() ?: return@TextButton
                    if (name.isNotBlank() && (amt > 0)) onConfirm(name, amt, deadline)
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddContributionDialog(
    goal: GoalEntity,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Money to ${goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current: ${currencyFormat.format(goal.currentAmount)}",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount to Add") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: return@TextButton
                    if (amt > 0) onConfirm(amt)
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}