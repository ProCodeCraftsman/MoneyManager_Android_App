package com.moneymanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.screens.TransactionSort
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TagEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsFilterControlsSheet(
    onDismiss: () -> Unit,
    // Date & Period
    currentPeriodName: String,
    onPrevPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onPeriodTypeSelected: (String) -> Unit,
    selectedPeriodType: String,
    // View Options
    isAllExpanded: Boolean,
    onToggleExpand: () -> Unit,
    sortBy: TransactionSort,
    onSortBySelected: (TransactionSort) -> Unit,
    showSummary: Boolean,
    onToggleSummary: (Boolean) -> Unit,
    showCategories: Boolean,
    onToggleCategories: (Boolean) -> Unit,
    // Filters
    selectedAccountName: String,
    onSelectAccount: () -> Unit,
    selectedCategoryName: String,
    onSelectCategory: () -> Unit,
    selectedTransactionType: String,
    onSelectTransactionType: () -> Unit,
    selectedTagsLabel: String,
    onSelectTags: () -> Unit,
    // Actions
    onResetAll: () -> Unit,
    onApply: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filters & Controls",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Customize the view of your transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Date & Period
            SectionHeader(title = "Date & Period")
            
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BoxBorder(),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { /* Show period picker */ }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = currentPeriodName, style = MaterialTheme.typography.bodyLarge)
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 8.dp))
                        IconButton(onClick = onPrevPeriod) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                        }
                        Text("Prev", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Next", style = MaterialTheme.typography.labelLarge)
                        IconButton(onClick = onNextPeriod) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val periods = listOf("This Month", "Last Month", "This Quarter", "This Year", "Custom")
                periods.forEach { period ->
                    FilterChip(
                        selected = selectedPeriodType == period,
                        onClick = { onPeriodTypeSelected(period) },
                        label = { Text(period, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // View Options
            SectionHeader(title = "View Options")
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                ControlItem(
                    icon = Icons.Default.SwapVert,
                    title = "Expand / Collapse",
                    subtitle = "Toggle date group sections",
                    trailing = {
                        Switch(
                            checked = isAllExpanded,
                            onCheckedChange = { onToggleExpand() }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    ControlItem(
                        icon = Icons.Default.Sort,
                        title = "Sort by",
                        subtitle = when(sortBy) {
                            TransactionSort.NEWEST -> "Date (Newest first)"
                            TransactionSort.OLDEST -> "Date (Oldest first)"
                            TransactionSort.HIGHEST -> "Amount (Highest first)"
                            TransactionSort.LOWEST -> "Amount (Lowest first)"
                        },
                        trailing = {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        },
                        onClick = { showSortMenu = true }
                    )
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        TransactionSort.values().forEach { sort ->
                            DropdownMenuItem(
                                text = { 
                                    Text(when(sort) {
                                        TransactionSort.NEWEST -> "Newest First"
                                        TransactionSort.OLDEST -> "Oldest First"
                                        TransactionSort.HIGHEST -> "Highest Amount"
                                        TransactionSort.LOWEST -> "Lowest Amount"
                                    }) 
                                },
                                onClick = { 
                                    onSortBySelected(sort)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ControlItem(
                    icon = Icons.Outlined.Visibility,
                    title = "Show Summary",
                    subtitle = "Income, Expense & Count",
                    trailing = {
                        Switch(
                            checked = showSummary,
                            onCheckedChange = onToggleSummary
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ControlItem(
                    icon = Icons.Outlined.Label,
                    title = "Show Categories",
                    subtitle = "With icons",
                    trailing = {
                        Switch(
                            checked = showCategories,
                            onCheckedChange = onToggleCategories
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Filter Transactions
            SectionHeader(title = "Filter Transactions")
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            ) {
                ControlItem(
                    icon = Icons.Outlined.AccountBalance,
                    title = "Accounts",
                    subtitle = selectedAccountName,
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    onClick = onSelectAccount
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ControlItem(
                    icon = Icons.Outlined.Category,
                    title = "Categories",
                    subtitle = selectedCategoryName,
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    onClick = onSelectCategory
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ControlItem(
                    icon = Icons.Outlined.SwapHoriz,
                    title = "Transaction Type",
                    subtitle = selectedTransactionType,
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    onClick = onSelectTransactionType
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                ControlItem(
                    icon = Icons.Outlined.Notes,
                    title = "Notes / Tags",
                    subtitle = selectedTagsLabel,
                    trailing = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                    onClick = onSelectTags
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onResetAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset All", color = Color.Red)
                }
                Button(
                    onClick = {
                        onApply()
                        onDismiss()
                    },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Dark Green
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Filters", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
private fun ControlItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing()
    }
}

@Composable
private fun BoxBorder() = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
