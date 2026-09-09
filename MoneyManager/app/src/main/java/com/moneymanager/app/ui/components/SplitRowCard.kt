package com.moneymanager.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.dialogs.SplitRowData
import com.moneymanager.app.ui.dialogs.TransactionFormConfig
import com.moneymanager.data.entity.CategoryEntity
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitRowCard(
    row: SplitRowData,
    allCategories: List<CategoryEntity>,
    type: String,
    onUpdate: (SplitRowData) -> Unit,
    onRemove: () -> Unit,
    showDropdown: Boolean,
    onToggleDropdown: () -> Unit,
    remainingAmount: Double = 0.0,
    currencySymbol: String = "₹",
) {
    var showSubDropdown by remember { mutableStateOf(false) }
    val categoryFilter = remember(type) {
        TransactionFormConfig.resolveCategoryType(type)
    }
    val parentCategories = remember(allCategories, categoryFilter) {
        allCategories.filter { it.parentId == null && it.type == categoryFilter }
    }
    val selectedParent = remember(row.categoryId, allCategories, categoryFilter) {
        allCategories.find { it.id == row.categoryId && it.type == categoryFilter }
    }
    val subCategories = remember(selectedParent, allCategories, categoryFilter) {
        selectedParent?.let { parent ->
            allCategories.filter { it.parentId == parent.id && it.type == categoryFilter }
        } ?: emptyList()
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Category & Sub-Category Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Parent Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = { onToggleDropdown() },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedParent?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        leadingIcon = selectedParent?.let { parent ->
                            {
                                CategoryIcon(
                                    emoji = parent.emoji,
                                    iconType = parent.iconType,
                                    colorIndex = parent.colorIndex,
                                    fontSize = 16.sp
                                )
                            }
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = onToggleDropdown
                    ) {
                        DropdownMenuItem(
                            text = { Text("None", style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onUpdate(row.copy(categoryId = null, subCategoryId = null))
                                onToggleDropdown()
                            }
                        )
                        parentCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CategoryIcon(
                                            emoji = cat.emoji,
                                            iconType = cat.iconType,
                                            colorIndex = cat.colorIndex,
                                            fontSize = 18.sp
                                        )
                                        Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                },
                                onClick = {
                                    onUpdate(row.copy(categoryId = cat.id, subCategoryId = null))
                                    onToggleDropdown()
                                }
                            )
                        }
                    }
                }

                // Sub-category Dropdown (if present)
                if (subCategories.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = showSubDropdown,
                        onExpandedChange = { showSubDropdown = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        val selectedSub = remember(row.subCategoryId, subCategories) {
                            subCategories.find { it.id == row.subCategoryId }
                        }
                        OutlinedTextField(
                            value = selectedSub?.name ?: "Sub-category (Opt)",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubDropdown) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = showSubDropdown,
                            onDismissRequest = { showSubDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    onUpdate(row.copy(subCategoryId = null))
                                    showSubDropdown = false
                                }
                            )
                            subCategories.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        onUpdate(row.copy(subCategoryId = sub.id))
                                        showSubDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Description, Amount, Fill Balance Button, Remove Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = row.description,
                    onValueChange = { onUpdate(row.copy(description = it)) },
                    placeholder = { Text("Description", fontSize = 12.sp) },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                )

                OutlinedTextField(
                    value = row.amount,
                    onValueChange = { onUpdate(row.copy(amount = it)) },
                    placeholder = { Text("0.00", fontSize = 12.sp) },
                    prefix = { Text(currencySymbol, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(110.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                )

                if (remainingAmount > 0) {
                    Surface(
                        onClick = {
                            val currentAmt = row.amount.toDoubleOrNull() ?: 0.0
                            val newAmt = currentAmt + remainingAmount
                            onUpdate(row.copy(amount = "%.2f".format(Locale.US, newAmt)))
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Fill",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }

                Surface(
                    onClick = onRemove,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove split",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
