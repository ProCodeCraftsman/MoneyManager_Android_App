package com.moneymanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import com.moneymanager.app.ui.components.CategoryIcon
import com.moneymanager.app.ui.components.ScrollToTopBox
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.moneymanager.app.ui.util.CurrencyUtils
import com.moneymanager.app.ui.util.MaterialIconProvider
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
    var deleteConfirmGoal by remember { mutableStateOf<GoalWithProgress?>(null) }
    val lazyListState = rememberLazyListState()

    // Group: in-progress vs completed
    val activeGoals = uiState.goals.filter { it.totalAmount < it.goal.targetAmount }
    val completedGoals = uiState.goals.filter { it.totalAmount >= it.goal.targetAmount }

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
        if (uiState.goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "No savings goals yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create goals to track your savings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            ScrollToTopBox(lazyListState = lazyListState, modifier = Modifier.padding(padding)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Active goals section header
                    item {
                        GoalsSectionHeader(title = "In Progress")
                    }

                    if (activeGoals.isEmpty()) {
                        item {
                            Text(
                                text = "All goals completed!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                            )
                        }
                    } else {
                        items(activeGoals) { goalWithProgress ->
                            GoalListRow(
                                goalWithProgress = goalWithProgress,
                                currencyFormat = currencyFormat,
                                onDelete = { deleteConfirmGoal = goalWithProgress },
                                showDivider = goalWithProgress != activeGoals.last()
                            )
                        }
                    }

                    // Completed goals section
                    if (completedGoals.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(7.dp))
                        }
                        item {
                            GoalsSectionHeader(title = "Completed")
                        }
                        items(completedGoals) { goalWithProgress ->
                            GoalListRow(
                                goalWithProgress = goalWithProgress,
                                currencyFormat = currencyFormat,
                                onDelete = { deleteConfirmGoal = goalWithProgress },
                                showDivider = goalWithProgress != completedGoals.last()
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog.value) {
        AddGoalDialog(
            onDismiss = { showAddDialog.value = false }
        ) { name, emoji, iconType, target, deadline ->
            viewModel.addGoal(name, emoji, iconType, target, deadline)
            showAddDialog.value = false
        }
    }

    deleteConfirmGoal?.let { goalWithProgress ->
        AlertDialog(
            onDismissRequest = { deleteConfirmGoal = null },
            title = { Text("Delete Goal") },
            text = { Text("Are you sure you want to delete \"${goalWithProgress.goal.name}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGoal(goalWithProgress.goal)
                        deleteConfirmGoal = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmGoal = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun GoalsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

@Composable
private fun GoalListRow(
    goalWithProgress: GoalWithProgress,
    currencyFormat: NumberFormat,
    onDelete: () -> Unit,
    showDivider: Boolean = true
) {
    val goal = goalWithProgress.goal
    val isCompleted = goalWithProgress.totalAmount >= goal.targetAmount
    val progress = if (goal.targetAmount > 0) (goalWithProgress.totalAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f

    val now = System.currentTimeMillis()
    val deadlineText = goal.deadline?.let { deadline ->
        val daysRemaining = ((deadline - now) / (1000 * 60 * 60 * 24)).toInt()
        when {
            daysRemaining > 0 -> "$daysRemaining days left"
            daysRemaining == 0 -> "Due today!"
            else -> "${-daysRemaining} days overdue"
        }
    }
    val isOverdue = goal.deadline?.let { ((it - now) / (1000 * 60 * 60 * 24)).toInt() < 0 } ?: false
    val monthlyNeededText = if (!isCompleted && goal.deadline != null && goal.deadline > now) {
        val remainingAmount = goal.targetAmount - goalWithProgress.totalAmount
        val monthsRemaining = ((goal.deadline - now) / (1000L * 60 * 60 * 24 * 30)).coerceAtLeast(1L)
        val monthly = remainingAmount / monthsRemaining
        "Need ${currencyFormat.format(monthly)}/mo"
    } else null

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    emoji = goal.emoji,
                    iconType = goal.iconType,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(14.dp))

            // Left column (main info)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${currencyFormat.format(goalWithProgress.totalAmount)} / ${currencyFormat.format(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // linkedAmount removed
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Right column: deadline + monthly needed
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(max = 110.dp)
            ) {
                if (deadlineText != null) {
                    Text(
                        text = deadlineText,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (monthlyNeededText != null) {
                    Text(
                        text = monthlyNeededText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Delete button only (no Add button)
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete goal",
                    tint = MaterialTheme.colorScheme.error
                )
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

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, String, String, Double, Long?) -> Unit) {
    // ... (unchanged, same as previous correct version)
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("flag") }
    var iconType by remember { mutableStateOf("material") }
    var target by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var customEmoji by remember { mutableStateOf("") }
    var showCustomEmojiInput by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf("") }
    var iconSearchQuery by remember { mutableStateOf("") }

    val emojiOptions = listOf(
        "🎯", "💰", "🏠", "🚗", "🏖️", "🎓", "💍", "📱", "💻", "🚲"
    )

    val materialIconsList = remember {
        MaterialIconProvider.allIcons.entries.toList()
    }
    val filteredIcons = remember(iconSearchQuery, materialIconsList) {
        if (iconSearchQuery.isBlank()) materialIconsList
        else materialIconsList.filter { it.key.contains(iconSearchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("material" to "Icons", "emoji" to "Emoji", "image" to "URL").forEach { (value, label) ->
                        FilterChip(
                            selected = iconType == value,
                            onClick = { iconType = value },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                when (iconType) {
                    "material" -> {
                        OutlinedTextField(
                            value = iconSearchQuery,
                            onValueChange = { iconSearchQuery = it },
                            placeholder = { Text("Search icons...") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(6),
                            modifier = Modifier.heightIn(max = 150.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            @Suppress("UNUSED_EXPRESSION")
                            items(filteredIcons.size) { index ->
                                val (name, _) = filteredIcons[index]
                                val isSelected = emoji == name && iconType == "material"
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .then(
                                            if (isSelected) Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.primary,
                                                RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                        .clickable {
                                            emoji = name
                                            iconType = "material"
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryIcon(emoji = name, iconType = "material", fontSize = 18.sp)
                                }
                            }
                        }
                    }
                    "emoji" -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(emojiOptions) { e ->
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (emoji == e && iconType == "emoji" && !showCustomEmojiInput) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable {
                                                emoji = e
                                                iconType = "emoji"
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
                                    .background(
                                        if (showCustomEmojiInput || (iconType == "emoji" && !emojiOptions.contains(emoji) && emoji.isNotBlank()))
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant
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
                                            iconType = "emoji"
                                        }
                                    }
                                },
                                label = { Text("Enter Emoji") },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Paste an emoji here") }
                            )
                        }
                    }
                    "image" -> {
                        OutlinedTextField(
                            value = imageUrl,
                            onValueChange = {
                                imageUrl = it
                                if (it.isNotBlank()) {
                                    emoji = it
                                    iconType = "image"
                                }
                            },
                            label = { Text("Image URL") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Paste an image URL") }
                        )
                        if (emoji.isNotBlank() && iconType == "image") {
                            Box(
                                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                coil.compose.AsyncImage(model = emoji, contentDescription = null, modifier = Modifier.size(60.dp))
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Target Amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                val context = LocalContext.current
                val calendar = Calendar.getInstance()
                deadline?.let { calendar.timeInMillis = it }
                val dateLabel = if (deadline == null) "Optional Target Date" else {
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(deadline!!))
                }

                OutlinedButton(
                    onClick = {
                        android.app.DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                Calendar.getInstance().apply { set(year, month, dayOfMonth) }.also { deadline = it.timeInMillis }
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
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
                    if (name.isNotBlank() && (amt > 0)) {
                        val finalEmoji = when (iconType) {
                            "material" -> if (emoji.isNotBlank()) emoji else "flag"
                            "emoji" -> if (emoji.isNotBlank()) emoji else "🎯"
                            "image" -> emoji
                            else -> emoji
                        }
                        onConfirm(name, finalEmoji, iconType, amt, deadline)
                    }
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