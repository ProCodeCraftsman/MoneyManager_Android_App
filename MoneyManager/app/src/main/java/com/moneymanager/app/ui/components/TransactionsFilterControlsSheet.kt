package com.moneymanager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.window.Dialog
import com.moneymanager.app.ui.transactions.TransactionSort
import com.moneymanager.app.ui.util.accountTypeIcon
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.entity.TagEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsFilterControlsSheet(
    onDismiss: () -> Unit,
    // Date & Period
    currentPeriodName: String,
    onPrevPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    selectedPeriodGranularity: String,
    onGranularitySelected: (String) -> Unit,
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
    accounts: List<AccountEntity>,
    selectedAccountId: Long?,
    onSelectAccount: (Long?) -> Unit,
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onSelectCategory: (Long?) -> Unit,
    selectedTransactionType: String,
    onSelectTransactionType: (String) -> Unit,
    tags: List<TagEntity>,
    selectedTagId: Long?,
    onSelectTags: (Long?) -> Unit,
    // Actions
    onResetAll: () -> Unit,
    onApply: () -> Unit
) {
    var showAccountPicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showTagPicker by remember { mutableStateOf(false) }
    var showSortPicker by remember { mutableStateOf(false) }

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
                .padding(bottom = 24.dp)
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
                        text = "Customize your transaction view",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // ── Date & Period ──────────────────────────────────────────────
            FilterSectionHeader(title = "Date & Period")

            FilterRow(
                icon = Icons.Default.CalendarToday,
                title = "Period",
                subtitle = currentPeriodName,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPrevPeriod, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "Previous",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onNextPeriod, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Next",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                showDivider = false
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "View by",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Day", "Month", "Year").forEach { granularity ->
                    FilterChip(
                        selected = selectedPeriodGranularity == granularity,
                        onClick = { onGranularitySelected(granularity) },
                        label = { Text(granularity, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(7.dp))

            // ── View Options ───────────────────────────────────────────────
            FilterSectionHeader(title = "View Options")

            FilterRow(
                icon = Icons.Default.SwapVert,
                title = "Expand / Collapse",
                subtitle = if (isAllExpanded) "Groups expanded" else "Groups collapsed",
                trailing = {
                    Switch(
                        checked = isAllExpanded,
                        onCheckedChange = { onToggleExpand() }
                    )
                }
            )

            FilterRow(
                icon = Icons.Default.Sort,
                title = "Sort by",
                subtitle = when (sortBy) {
                    TransactionSort.NEWEST -> "Date (Newest first)"
                    TransactionSort.OLDEST -> "Date (Oldest first)"
                    TransactionSort.HIGHEST -> "Amount (Highest first)"
                    TransactionSort.LOWEST -> "Amount (Lowest first)"
                },
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showSortPicker = true }
            )

            FilterRow(
                icon = Icons.Outlined.Visibility,
                title = "Show Summary",
                subtitle = "Income, Expense & Count",
                trailing = {
                    Switch(checked = showSummary, onCheckedChange = onToggleSummary)
                }
            )

            FilterRow(
                icon = Icons.Outlined.Label,
                title = "Show Categories",
                subtitle = "Display category with icon",
                trailing = {
                    Switch(checked = showCategories, onCheckedChange = onToggleCategories)
                },
                showDivider = false
            )

            Spacer(modifier = Modifier.height(7.dp))

            // ── Filter Transactions ────────────────────────────────────────
            FilterSectionHeader(title = "Filter Transactions")

            FilterRow(
                icon = Icons.Outlined.AccountBalance,
                title = "Account",
                subtitle = accounts.find { it.id == selectedAccountId }?.name ?: "All Accounts",
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showAccountPicker = true }
            )

            FilterRow(
                icon = Icons.Outlined.Category,
                title = "Category",
                subtitle = categories.find { it.id == selectedCategoryId }?.name ?: "All Categories",
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showCategoryPicker = true }
            )

            FilterRow(
                icon = Icons.Outlined.SwapHoriz,
                title = "Type",
                subtitle = selectedTransactionType.replaceFirstChar { it.uppercase() }.ifEmpty { "All" },
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showTypePicker = true }
            )

            FilterRow(
                icon = Icons.Outlined.Notes,
                title = "Tag",
                subtitle = tags.find { it.id == selectedTagId }?.name ?: "All Tags",
                trailing = {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = { showTagPicker = true },
                showDivider = false
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onResetAll,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset All", color = Color.Red)
                }
                Button(
                    onClick = { onApply(); onDismiss() },
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Filters", color = Color.White)
                }
            }
        }
    }

    // ── Picker Dialogs ─────────────────────────────────────────────────────

    if (showAccountPicker) {
        FilterPickerDialog(
            title = "Select Account",
            onDismiss = { showAccountPicker = false }
        ) {
            item {
                PickerItem(
                    label = "All Accounts",
                    isSelected = selectedAccountId == null,
                    icon = Icons.Outlined.AccountBalance,
                    onClick = { onSelectAccount(null); showAccountPicker = false }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
            items(accounts) { account ->
                PickerItem(
                    label = account.name,
                    sublabel = account.type.replaceFirstChar { it.uppercase() },
                    isSelected = selectedAccountId == account.id,
                    icon = accountTypeIcon(account.type),
                    onClick = { onSelectAccount(account.id); showAccountPicker = false }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }

    if (showCategoryPicker) {
        FilterPickerDialog(
            title = "Select Category",
            showSearch = true,
            onDismiss = { showCategoryPicker = false }
        ) { query ->
            val filtered = if (query.isEmpty()) categories
            else categories.filter { it.name.contains(query, ignoreCase = true) }

            item {
                PickerItem(
                    label = "All Categories",
                    isSelected = selectedCategoryId == null,
                    icon = Icons.Outlined.Category,
                    onClick = { onSelectCategory(null); showCategoryPicker = false }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
            items(filtered) { cat ->
                val parentName = if (cat.parentId != null)
                    categories.find { it.id == cat.parentId }?.name else null
                PickerItemWithCategoryIcon(
                    label = cat.name,
                    sublabel = parentName,
                    isSelected = selectedCategoryId == cat.id,
                    emoji = cat.emoji,
                    iconType = cat.iconType,
                    colorIndex = cat.colorIndex,
                    onClick = { onSelectCategory(cat.id); showCategoryPicker = false }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }

    if (showTypePicker) {
        val transactionTypes = listOf("All", "income", "expense", "transfer", "savings", "lend", "borrow")
        FilterPickerDialog(
            title = "Transaction Type",
            onDismiss = { showTypePicker = false }
        ) {
            items(transactionTypes) { type ->
                val isSelected = type == "All" && selectedTransactionType.isEmpty() ||
                    type == selectedTransactionType
                PickerItem(
                    label = when (type) {
                        "income" -> "Income"
                        "expense" -> "Expense"
                        "transfer" -> "Transfer"
                        "savings" -> "Savings"
                        "lend" -> "Lending"
                        "borrow" -> "Borrowing"
                        else -> "All Types"
                    },
                    isSelected = isSelected,
                    icon = when (type) {
                        "income" -> Icons.AutoMirrored.Filled.TrendingUp
                        "expense" -> Icons.Default.AccountBalanceWallet
                        "transfer" -> Icons.Default.SwapHoriz
                        "savings" -> Icons.Default.Savings
                        "lend" -> Icons.Default.Handshake
                        "borrow" -> Icons.Default.SouthWest
                        else -> Icons.Outlined.Category
                    },
                    onClick = {
                        onSelectTransactionType(if (type == "All") "" else type)
                        showTypePicker = false
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }

    if (showTagPicker) {
        FilterPickerDialog(
            title = "Select Tag",
            showSearch = true,
            onDismiss = { showTagPicker = false }
        ) { query ->
            val filtered = if (query.isEmpty()) tags
            else tags.filter { it.name.contains(query, ignoreCase = true) }

            item {
                PickerItem(
                    label = "All Tags",
                    isSelected = selectedTagId == null,
                    icon = Icons.Default.LocalOffer,
                    onClick = { onSelectTags(null); showTagPicker = false }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
            items(filtered) { tag ->
                PickerItem(
                    label = tag.name,
                    isSelected = selectedTagId == tag.id,
                    icon = Icons.Default.LocalOffer,
                    onClick = { onSelectTags(tag.id); showTagPicker = false }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }

    if (showSortPicker) {
        FilterPickerDialog(
            title = "Sort Transactions",
            onDismiss = { showSortPicker = false }
        ) {
            items(TransactionSort.values().toList()) { sort ->
                PickerItem(
                    label = when (sort) {
                        TransactionSort.NEWEST -> "Newest First"
                        TransactionSort.OLDEST -> "Oldest First"
                        TransactionSort.HIGHEST -> "Highest Amount"
                        TransactionSort.LOWEST -> "Lowest Amount"
                    },
                    sublabel = when (sort) {
                        TransactionSort.NEWEST, TransactionSort.OLDEST -> "Sort by date"
                        TransactionSort.HIGHEST, TransactionSort.LOWEST -> "Sort by amount"
                    },
                    isSelected = sortBy == sort,
                    icon = when (sort) {
                        TransactionSort.NEWEST -> Icons.Default.ArrowDownward
                        TransactionSort.OLDEST -> Icons.Default.ArrowUpward
                        TransactionSort.HIGHEST -> Icons.Default.KeyboardArrowUp
                        TransactionSort.LOWEST -> Icons.Default.KeyboardArrowDown
                    },
                    onClick = { onSortBySelected(sort); showSortPicker = false }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

// ── Picker Dialog ──────────────────────────────────────────────────────────

@Composable
private fun FilterPickerDialog(
    title: String,
    showSearch: Boolean = false,
    onDismiss: () -> Unit,
    content: LazyListScope.(query: String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (showSearch) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    content(searchQuery)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

// ── Picker Item (icon-based) ───────────────────────────────────────────────

@Composable
private fun PickerItem(
    label: String,
    sublabel: String? = null,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Picker Item (category icon) ────────────────────────────────────────────

@Composable
private fun PickerItemWithCategoryIcon(
    label: String,
    sublabel: String? = null,
    isSelected: Boolean,
    emoji: String,
    iconType: String,
    colorIndex: Int,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
            contentAlignment = Alignment.Center
        ) {
            CategoryIcon(
                emoji = emoji,
                iconType = iconType,
                colorIndex = colorIndex,
                fontSize = 18.sp
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            if (sublabel != null) {
                Text(
                    text = sublabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Section Header ─────────────────────────────────────────────────────────

@Composable
private fun FilterSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
    )
}

// ── Filter Row ─────────────────────────────────────────────────────────────

@Composable
private fun FilterRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
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
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
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
