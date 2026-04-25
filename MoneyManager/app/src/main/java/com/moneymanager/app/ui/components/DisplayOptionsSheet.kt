package com.moneymanager.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.moneymanager.app.ui.screens.TimeFilter
import com.moneymanager.data.entity.AccountEntity
import com.moneymanager.data.entity.CategoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayOptionsSheet(
    selectedFilter: TimeFilter,
    accounts: List<AccountEntity>,
    selectedAccountIds: Set<Long>,
    categories: List<CategoryEntity>,
    selectedCategoryIds: Set<Long>,
    showPercentages: Boolean,
    carryOver: Boolean,
    onDismiss: () -> Unit,
    onApply: (TimeFilter, Set<Long>, Set<Long>, Boolean, Boolean) -> Unit
) {
    var filter by remember { mutableStateOf(selectedFilter) }
    var accountIds by remember { mutableStateOf(selectedAccountIds) }
    var categoryIds by remember { mutableStateOf(selectedCategoryIds) }
    var showPct by remember { mutableStateOf(showPercentages) }
    var carry by remember { mutableStateOf(carryOver) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Display options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                item {
                    Text("View mode", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TimeFilter.entries.filter { it != TimeFilter.CUSTOM && it != TimeFilter.ALL }.forEachIndexed { index, timeFilter ->
                            SegmentedButton(
                                selected = filter == timeFilter,
                                onClick = { filter = timeFilter },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 4)
                            ) {
                                Text(timeFilter.displayName)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Text("Accounts", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(accounts) { account ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                accountIds = if (account.id in accountIds) {
                                    accountIds - account.id
                                } else {
                                    accountIds + account.id
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = account.id in accountIds,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(account.name)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Categories", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                }
                items(categories) { category ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                categoryIds = if (category.id in categoryIds) {
                                    categoryIds - category.id
                                } else {
                                    categoryIds + category.id
                                }
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = category.id in categoryIds,
                            onCheckedChange = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(category.name)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Show percentages", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(checked = showPct, onCheckedChange = { showPct = it })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Carry over", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "With Carry over enabled, monthly surplus will be added to the next month.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = carry, onCheckedChange = { carry = it })
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            Button(
                onClick = { onApply(filter, accountIds, categoryIds, showPct, carry) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Save & Apply Filters")
            }
        }
    }
}
