package com.moneymanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.moneymanager.data.entity.CategoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val expandedCategories: Set<Long> = emptySet(),
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: com.moneymanager.domain.repository.CategoryRepository,
) : androidx.lifecycle.ViewModel() {

    private val _expandedCategories = MutableStateFlow<Set<Long>>(emptySet())

    val uiState: StateFlow<CategoriesUiState> = combine(
        categoryRepository.getAllCategories(),
        _expandedCategories
    ) { categories, expanded ->
        CategoriesUiState(
            categories = categories,
            isLoading = false,
            expandedCategories = expanded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState()
    )

    fun toggleExpanded(categoryId: Long) {
        _expandedCategories.update { current ->
            if (categoryId in current) {
                current - categoryId
            } else {
                current + categoryId
            }
        }
    }

    fun addCategory(name: String, emoji: String, type: String, parentId: Long?) {
        viewModelScope.launch {
            categoryRepository.insertCategory(
                CategoryEntity(
                    name = name,
                    emoji = emoji,
                    type = type,
                    parentId = parentId
                )
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(viewModel: CategoriesViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteConfirmCategory by remember { mutableStateOf<CategoryEntity?>(null) }

    // Group categories by type
    val expenseCategories = uiState.categories.filter { it.type == "expense" }
    val incomeCategories = uiState.categories.filter { it.type == "income" }
    val savingsCategories = uiState.categories.filter { it.type == "savings" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Category")
            }
        }
    ) { padding ->
        if (uiState.categories.isEmpty()) {
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
                        text = "No categories yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Create categories to organize your transactions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Expense section
                item {
                    CategorySection(
                        title = "Expense",
                        categories = expenseCategories,
                        expandedCategories = uiState.expandedCategories,
                        onToggleExpand = { viewModel.toggleExpanded(it) },
                        onEdit = { editingCategory = it },
                        onDelete = { deleteConfirmCategory = it }
                    )
                }

                // Income section
                item {
                    CategorySection(
                        title = "Income",
                        categories = incomeCategories,
                        expandedCategories = uiState.expandedCategories,
                        onToggleExpand = { viewModel.toggleExpanded(it) },
                        onEdit = { editingCategory = it },
                        onDelete = { deleteConfirmCategory = it }
                    )
                }

                // Savings section
                item {
                    CategorySection(
                        title = "Savings",
                        categories = savingsCategories,
                        expandedCategories = uiState.expandedCategories,
                        onToggleExpand = { viewModel.toggleExpanded(it) },
                        onEdit = { editingCategory = it },
                        onDelete = { deleteConfirmCategory = it }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        CategoryDialog(
            existingCategories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onSave = { name, emoji, type, parentId ->
                viewModel.addCategory(name, emoji, type, parentId)
                showAddDialog = false
            }
        )
    }

    editingCategory?.let { category ->
        CategoryDialog(
            category = category,
            existingCategories = uiState.categories,
            onDismiss = { editingCategory = null },
            onSave = { name, emoji, type, parentId ->
                viewModel.updateCategory(category.copy(name = name, emoji = emoji, type = type, parentId = parentId))
                editingCategory = null
            }
        )
    }

    // Delete confirmation
    deleteConfirmCategory?.let { category ->
        val hasSubCategories = uiState.categories.any { it.parentId == category.id }
        AlertDialog(
            onDismissRequest = { deleteConfirmCategory = null },
            title = { Text("Delete Category") },
            text = {
                if (hasSubCategories) {
                    Text("This category has sub-categories. Deleting it will also delete all sub-categories. Are you sure?")
                } else {
                    Text("Are you sure you want to delete \"${category.name}\"?")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(category)
                        deleteConfirmCategory = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmCategory = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CategorySection(
    title: String,
    categories: List<CategoryEntity>,
    expandedCategories: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit
) {
    val parentCategories = categories.filter { it.parentId == null }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        parentCategories.forEach { parent ->
            val subCategories = categories.filter { it.parentId == parent.id }
            val isExpanded = parent.id in expandedCategories
            val hasSubCategories = subCategories.isNotEmpty()

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = parent.emoji,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Column {
                                Text(
                                    text = parent.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                if (hasSubCategories) {
                                    Text(
                                        text = "${subCategories.size} sub-categories",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Row {
                            if (hasSubCategories) {
                                IconButton(onClick = { onToggleExpand(parent.id) }) {
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                                    )
                                }
                            }
                            IconButton(onClick = { onEdit(parent) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDelete(parent) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Sub-categories
                    if (isExpanded && hasSubCategories) {
                        Divider()
                        subCategories.forEach { subCategory ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .padding(start = 32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = subCategory.emoji,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = subCategory.name,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = { onEdit(subCategory) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDelete(subCategory) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDialog(
    category: CategoryEntity? = null,
    existingCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, type: String, parentId: Long?) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var emoji by remember { mutableStateOf(category?.emoji ?: "📁") }
    var type by remember { mutableStateOf(category?.type ?: "expense") }
    var parentId by remember { mutableStateOf(category?.parentId) }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showParentDropdown by remember { mutableStateOf(false) }

    // Get parent categories of the same type (for sub-categories)
    val parentCategories = existingCategories.filter { 
        it.parentId == null && it.type == type && it.id != category?.id 
    }

    val emojiOptions = listOf(
        "🍔", "🚗", "🛒", "🏠", "💊", "📱", "🎬", "✈️", "👕", "💄",
        "💰", "💼", "🎁", "🏋️", "📚", "🎵", "🐶", "🌿", "⚡", "💡",
        "🏦", "📈", "🎯", "🏆", "🎓", "💻", "📺", "🛠️", "🚿", "🧸"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Emoji picker
                Text(
                    text = "Icon",
                    style = MaterialTheme.typography.labelLarge
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(emojiOptions) { e ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .then(
                                    if (emoji == e) {
                                        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { emoji = e },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = e, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                // Type selector
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelLarge
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("expense", "income", "savings").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                parentId = null // Reset parent when type changes
                            },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                // Parent category (for sub-categories)
                Text(
                    text = "Parent Category (Optional)",
                    style = MaterialTheme.typography.labelLarge
                )
                ExposedDropdownMenuBox(
                    expanded = showParentDropdown,
                    onExpandedChange = { showParentDropdown = it }
                ) {
                    val parentCategory = existingCategories.find { it.id == parentId }
                    OutlinedTextField(
                        value = parentCategory?.name ?: "None (Top-level)",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showParentDropdown) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = showParentDropdown,
                        onDismissRequest = { showParentDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("None (Top-level)") },
                            onClick = {
                                parentId = null
                                showParentDropdown = false
                            }
                        )
                        parentCategories.forEach { parent ->
                            DropdownMenuItem(
                                text = { Text("${parent.emoji} ${parent.name}") },
                                onClick = {
                                    parentId = parent.id
                                    showParentDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name, emoji, type, parentId) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}