package com.moneymanager.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moneymanager.app.ui.components.CategoryIcon
import com.moneymanager.app.ui.dialogs.SplitRowData
import com.moneymanager.data.entity.CategoryEntity
import java.util.Locale
import kotlin.math.abs

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
    val parentCategories = remember(allCategories, type) {
        allCategories.filter { it.parentId == null && (it.type == type || (type == "savings" && it.type == "expense")) }
    }
    val selectedParent = remember(row.categoryId, allCategories) {
        allCategories.find { it.id == row.categoryId }
    }
    val subCategories = remember(selectedParent, allCategories) {
        selectedParent?.let { parent -> allCategories.filter { it.parentId == parent.id } } ?: emptyList()
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(showDropdown, { if (it) onToggleDropdown() else onToggleDropdown() }, Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedParent?.let { it.name } ?: "Category",
                        onValueChange = {}, readOnly = true, label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showDropdown) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(showDropdown, onToggleDropdown) {
                        DropdownMenuItem(text = { Text("None") }, onClick = { onUpdate(row.copy(categoryId = null, subCategoryId = null)); onToggleDropdown() })
                        parentCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CategoryIcon(emoji = cat.emoji, iconType = cat.iconType, fontSize = 16.sp)
                                        Text(cat.name)
                                    }
                                },
                                onClick = { onUpdate(row.copy(categoryId = cat.id, subCategoryId = null)); onToggleDropdown() }
                            )
                        }
                    }
                }

                if (subCategories.isNotEmpty()) {
                    ExposedDropdownMenuBox(showSubDropdown, { showSubDropdown = it }, Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = allCategories.find { it.id == row.subCategoryId }?.name ?: "Sub-cat (Opt)",
                            onValueChange = {}, readOnly = true, label = { Text("Sub-category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(showSubDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(showSubDropdown, { showSubDropdown = false }) {
                            DropdownMenuItem(text = { Text("None") }, onClick = { onUpdate(row.copy(subCategoryId = null)); showSubDropdown = false })
                            subCategories.forEach { sub ->
                                DropdownMenuItem(
                                    text = { Text(sub.name) },
                                    onClick = { onUpdate(row.copy(subCategoryId = sub.id)); showSubDropdown = false }
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = row.description, onValueChange = { onUpdate(row.copy(description = it)) },
                    label = { Text("Description") }, modifier = Modifier.weight(1f), singleLine = true
                )
                OutlinedTextField(
                    value = row.amount, onValueChange = { onUpdate(row.copy(amount = it)) },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(100.dp), singleLine = true
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$currencySymbol${"%.2f".format(Locale.US, remainingAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (abs(remainingAmount) < 0.01) MaterialTheme.colorScheme.primary
                        else if (remainingAmount < 0) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "Remove split", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}