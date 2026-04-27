package com.moneymanager.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.moneymanager.data.entity.CategoryEntity

@Composable
fun CategoryGrid(
    categories: List<CategoryEntity>,
    selectedCategoryId: Long?,
    onCategoryClick: (CategoryEntity) -> Unit,
    onShowAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 4
        ) {
            val displayCategories = categories.take(7)
            displayCategories.forEach { category ->
                val isSelected = selectedCategoryId == category.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryClick(category) },
                    label = { Text(category.name) },
                    leadingIcon = { Text(category.emoji) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(48.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                )
            }
            FilterChip(
                selected = false,
                onClick = onShowAllClick,
                label = { Text("All") },
                leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(16.dp)) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.height(48.dp)
            )
        }
    }
}