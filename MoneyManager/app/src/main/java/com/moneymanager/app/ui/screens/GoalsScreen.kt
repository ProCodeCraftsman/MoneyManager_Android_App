package com.moneymanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.util.CurrencyUtils
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
    val currencyFormat = remember(uiState.currencyCode) { 
        CurrencyUtils.getCurrencyFormat(uiState.currencyCode)
    }
    val showAddDialog = remember { mutableStateOf(value = false) }
    var goalForContribution by remember { mutableStateOf<GoalWithProgress?>(null) }

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
                items(uiState.goals) { goalWithProgress ->
                    GoalCard(
                        goalWithProgress = goalWithProgress,
                        currencyFormat = currencyFormat,
                        onAddMoney = { goalForContribution = goalWithProgress }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog.value) {
        AddGoalDialog(
            onDismiss = { showAddDialog.value = false }
        ) { name, emoji, target, deadline ->
            viewModel.addGoal(name, emoji, target, deadline)
            showAddDialog.value = false
        }
    }

    if (goalForContribution != null) {
        AddContributionDialog(
            goalWithProgress = goalForContribution!!,
            currencyFormat = currencyFormat,
            onDismiss = { goalForContribution = null },
            onConfirm = { amount ->
                viewModel.addContribution(goalForContribution!!.goal.id, amount)
                goalForContribution = null
            }
        )
    }
}

@Composable
fun GoalCard(goalWithProgress: GoalWithProgress, currencyFormat: NumberFormat, onAddMoney: () -> Unit) {
    val goal = goalWithProgress.goal
    val isCompleted = goalWithProgress.totalAmount >= goal.targetAmount
    val progress = if (goal.targetAmount > 0) (goalWithProgress.totalAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    // Calculate deadline countdown and monthly needed
    val now = System.currentTimeMillis()
    val deadlineText = goal.deadline?.let { deadline ->
        val daysRemaining = ((deadline - now) / (1000 * 60 * 60 * 24)).toInt()
        when {
            daysRemaining > 0 -> "$daysRemaining days remaining"
            daysRemaining == 0 -> "Due today!"
            else -> "${-daysRemaining} days overdue"
        }
    }
    
    val monthlyNeededText = if (!isCompleted && goal.deadline != null && goal.deadline > now) {
        val remainingAmount = goal.targetAmount - goalWithProgress.totalAmount
        val monthsRemaining = ((goal.deadline - now) / (1000L * 60 * 60 * 24 * 30)).coerceAtLeast(1L)
        val monthly = remainingAmount / monthsRemaining
        "Need ${currencyFormat.format(monthly)}/mo"
    } else null

    val isOverdue = goal.deadline?.let { ((it - now) / (1000 * 60 * 60 * 24)).toInt() < 0 } ?: false

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isCompleted) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)) 
                 else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = goal.emoji, style = MaterialTheme.typography.headlineMedium)
                    if (isCompleted) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(24.dp).align(Alignment.TopEnd).offset(x = 8.dp, y = (-8).dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                    Text(
                        text = goal.name, 
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (isCompleted) {
                        Text(
                            "Goal reached!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "${currencyFormat.format(goalWithProgress.totalAmount)} / ${currencyFormat.format(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (goalWithProgress.linkedAmount > 0) {
                        Text(
                            text = "Linked: ${currencyFormat.format(goalWithProgress.linkedAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF795548) // Brown per spec
                        )
                    }
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
                modifier = Modifier.fillMaxWidth(),
                color = if (isCompleted) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(progress * 100).toInt()}% saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.End) {
                    if (deadlineText != null) {
                        Text(
                            text = deadlineText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOverdue) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (monthlyNeededText != null) {
                        Text(
                            text = monthlyNeededText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, String, Double, Long?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("🎯") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var customEmoji by remember { mutableStateOf("") }
    var showCustomEmojiInput by remember { mutableStateOf(false) }

    val emojiOptions = listOf(
        "🎯", "💰", "🏠", "🚗", "🏖️", "🎓", "💍", "📱", "💻", "🚲"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Emoji picker
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(emojiOptions) { e ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (emoji == e && !showCustomEmojiInput) {
                                            Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable {
                                        emoji = e
                                        showCustomEmojiInput = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = e, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .then(
                                if (showCustomEmojiInput || (!emojiOptions.contains(emoji) && emoji.isNotBlank())) {
                                    Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                }
                            )
                            .clickable { showCustomEmojiInput = !showCustomEmojiInput },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (showCustomEmojiInput) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Custom Emoji"
                        )
                    }
                }

                if (showCustomEmojiInput) {
                    OutlinedTextField(
                        value = customEmoji,
                        onValueChange = {
                            if (it.length <= 2) {
                                customEmoji = it
                                if (it.isNotBlank()) {
                                    emoji = it
                                }
                            }
                        },
                        label = { Text("Enter Emoji") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Paste an emoji here") }
                    )
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date Picker for deadline
                val context = LocalContext.current
                val calendar = Calendar.getInstance()
                deadline?.let { calendar.timeInMillis = it }
                val dateLabel = if (deadline == null) "Optional Target Date" else {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(deadline!!))
                }

                OutlinedButton(
                    onClick = {
                        val datePickerDialog = android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val selectedCalendar = Calendar.getInstance()
                                selectedCalendar.set(year, month, dayOfMonth)
                                deadline = selectedCalendar.timeInMillis
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePickerDialog.show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(dateLabel)
                    if (deadline != null) {
                        IconButton(onClick = { deadline = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear date")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = target.toDoubleOrNull() ?: return@TextButton
                    if (name.isNotBlank() && (amt > 0)) onConfirm(name, emoji, amt, deadline)
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
    goalWithProgress: GoalWithProgress,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val goal = goalWithProgress.goal
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Money to ${goal.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Current: ${currencyFormat.format(goalWithProgress.totalAmount)}",
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