package com.moneymanager.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.moneymanager.data.entity.CategoryEntity
import com.moneymanager.data.seed.CategorySeeder
import com.moneymanager.app.ui.util.MaterialIconProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val isLoading: Boolean = true,
    val expandedCategories: Set<Long> = emptySet(),
    val showArchived: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: com.moneymanager.domain.repository.CategoryRepository,
) : androidx.lifecycle.ViewModel() {

    private val _expandedCategories = MutableStateFlow<Set<Long>>(emptySet())
    private val _showArchived = MutableStateFlow(value = false)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<CategoriesUiState> = combine(
        categoryRepository.getAllCategoriesWithArchived(),
        _expandedCategories,
        _showArchived,
        _error
    ) { categories, expanded, showArchived, error ->
        val filtered = if (showArchived) categories else categories.filter { !it.isArchived }
        CategoriesUiState(
            categories = filtered,
            isLoading = false,
            expandedCategories = expanded,
            showArchived = showArchived,
            error = error
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CategoriesUiState()
    )

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategoriesWithArchived().first().let {
                if (it.isEmpty()) {
                    CategorySeeder.seed(categoryRepository)
                }
            }
        }
    }

    fun toggleExpanded(categoryId: Long) {
        _expandedCategories.update { current ->
            if (categoryId in current) {
                current - categoryId
            } else {
                current + categoryId
            }
        }
    }

    fun addCategory(name: String, emoji: String, iconType: String, type: String, parentId: Long?) {
        viewModelScope.launch {
            if (categoryRepository.categoryNameExists(name, type)) {
                _error.value = "Category name must be unique"
                return@launch
            }
            _error.value = null
            categoryRepository.insertCategory(
                CategoryEntity(
                    name = name,
                    emoji = emoji,
                    iconType = iconType,
                    type = type,
                    parentId = parentId,
                    isCustom = true
                )
            )
        }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch {
            val existing = categoryRepository.getCategoryByName(category.name, category.type)
            if (existing != null && (existing.id != category.id)) {
                _error.value = "Category name must be unique"
                return@launch
            }
            _error.value = null
            categoryRepository.updateCategory(category)
        }
    }

    fun archiveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category.copy(isArchived = true))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        if (!category.isCustom) return // Pre-configured categories cannot be deleted
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun toggleShowArchived() {
        _showArchived.update { !it }
    }

    fun unarchiveCategory(category: CategoryEntity) {
        viewModelScope.launch {
            categoryRepository.updateCategory(category.copy(isArchived = false))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var deleteConfirmCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var archiveConfirmCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    val lazyListState = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Group categories by type
    val expenseCategories = uiState.categories.filter { it.type == "expense" }
    val incomeCategories = uiState.categories.filter { it.type == "income" }
    val savingsCategories = uiState.categories.filter { it.type == "savings" }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowArchived() }) {
                        Icon(
                            if (uiState.showArchived) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (uiState.showArchived) "Hide Archived" else "Show Archived"
                        )
                    }
                }
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
            ScrollToTopBox(lazyListState = lazyListState, modifier = Modifier.padding(padding)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Expense section
                item {
                    CategorySection(
                        categories = expenseCategories,
                        expandedCategories = uiState.expandedCategories,
                        onToggleExpand = { viewModel.toggleExpanded(it) },
                        onEdit = { editingCategory = it },
                        onDelete = { deleteConfirmCategory = it },
                        onArchive = { archiveConfirmCategory = it },
                        onUnarchive = { viewModel.unarchiveCategory(it) },
                        sectionTitle = "Expense"
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                // Income section
                item {
                    CategorySection(
                        categories = incomeCategories,
                        expandedCategories = uiState.expandedCategories,
                        onToggleExpand = { viewModel.toggleExpanded(it) },
                        onEdit = { editingCategory = it },
                        onDelete = { deleteConfirmCategory = it },
                        onArchive = { archiveConfirmCategory = it },
                        onUnarchive = { viewModel.unarchiveCategory(it) },
                        sectionTitle = "Income"
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(7.dp))
                }

                // Savings section
                item {
                    CategorySection(
                        categories = savingsCategories,
                        expandedCategories = uiState.expandedCategories,
                        onToggleExpand = { viewModel.toggleExpanded(it) },
                        onEdit = { editingCategory = it },
                        onDelete = { deleteConfirmCategory = it },
                        onArchive = { archiveConfirmCategory = it },
                        onUnarchive = { viewModel.unarchiveCategory(it) },
                        sectionTitle = "Savings"
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
            } // ScrollToTopBox
        }
    }

    // Add Category Dialog
    if (showAddDialog) {
        CategoryDialog(
            existingCategories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onSave = { name, emoji, iconType, type, parentId ->
                viewModel.addCategory(name, emoji, iconType, type, parentId)
                showAddDialog = false
            }
        )
    }

    editingCategory?.let { category ->
        CategoryDialog(
            category = category,
            existingCategories = uiState.categories,
            onDismiss = { editingCategory = null },
            onSave = { name, emoji, iconType, type, parentId ->
                viewModel.updateCategory(category.copy(name = name, emoji = emoji, iconType = iconType, type = type, parentId = parentId))
                editingCategory = null
            }
        )
    }

    // Archive confirmation
    archiveConfirmCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { archiveConfirmCategory = null },
            title = { Text("Archive Category") },
            text = { Text("Are you sure you want to archive \"${category.name}\"? It will no longer appear in selection lists.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archiveCategory(category)
                        archiveConfirmCategory = null
                    }
                ) {
                    Text("Archive")
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveConfirmCategory = null }) {
                    Text("Cancel")
                }
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
    categories: List<CategoryEntity>,
    expandedCategories: Set<Long>,
    onToggleExpand: (Long) -> Unit,
    onEdit: (CategoryEntity) -> Unit,
    onDelete: (CategoryEntity) -> Unit,
    onArchive: (CategoryEntity) -> Unit,
    onUnarchive: (CategoryEntity) -> Unit = {},
    sectionTitle: String = ""
) {
    val parentCategories = categories.filter { it.parentId == null }

    Column(
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
        )

        parentCategories.forEachIndexed { index, parent ->
            val subCategories = categories.filter { it.parentId == parent.id }
            val isExpanded = parent.id in expandedCategories
            val hasSubCategories = subCategories.isNotEmpty()
            val isLastParent = index == parentCategories.size - 1

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (parent.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CategoryIcon(
                            emoji = parent.emoji,
                            iconType = parent.iconType,
                            fontSize = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = parent.name + if (parent.isArchived) " (Archived)" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasSubCategories) {
                                Text(
                                    text = "${subCategories.size} sub-categories",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!parent.isCustom) {
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "System",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
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
                        if (parent.isArchived) {
                            IconButton(onClick = { onUnarchive(parent) }) {
                                Icon(
                                    imageVector = Icons.Default.Unarchive,
                                    contentDescription = "Unarchive",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            IconButton(onClick = { onArchive(parent) }) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "Archive",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        if (parent.isCustom) {
                            IconButton(onClick = { onDelete(parent) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                if (isExpanded && hasSubCategories) {
                    subCategories.forEach { subCategory ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .padding(start = 50.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (subCategory.isArchived) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                CategoryIcon(
                                    emoji = subCategory.emoji,
                                    iconType = subCategory.iconType,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = subCategory.name + if (subCategory.isArchived) " (Archived)" else "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Row {
                                IconButton(onClick = { onEdit(subCategory) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                }
                                if (subCategory.isArchived) {
                                    IconButton(onClick = { onUnarchive(subCategory) }, modifier = Modifier.size(32.dp)) {
                                        Icon(imageVector = Icons.Default.Unarchive, contentDescription = "Unarchive", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                } else {
                                    IconButton(onClick = { onArchive(subCategory) }, modifier = Modifier.size(32.dp)) {
                                        Icon(imageVector = Icons.Default.Archive, contentDescription = "Archive", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
                                    }
                                }
                                if (subCategory.isCustom) {
                                    IconButton(onClick = { onDelete(subCategory) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 74.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }

                if (index < parentCategories.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 50.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
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
    onSave: (name: String, emoji: String, iconType: String, type: String, parentId: Long?) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    var emoji by remember { mutableStateOf(category?.emoji ?: "restaurant") }
    var iconType by remember { mutableStateOf(category?.iconType ?: "material") }
    var customEmoji by remember { mutableStateOf("") }
    var showCustomEmojiInput by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(category?.type ?: "expense") }
    var parentId by remember { mutableStateOf(category?.parentId) }
    var showParentDropdown by remember { mutableStateOf(false) }
    var iconSearchQuery by remember { mutableStateOf("") }

    val parentCategories = existingCategories.filter {
        it.parentId == null && it.type == type && it.id != category?.id
    }

    val emojiOptions = listOf(
        "🍔", "🚗", "🛒", "🏠", "💊", "📱", "🎬", "✈️", "👕", "💄",
        "💰", "💼", "🎁", "🏋️", "📚", "🎵", "🐶", "🌿", "⚡", "💡",
        "🏦", "📈", "🎯", "🏆", "🎓", "💻", "📺", "🛠️", "🚿", "🧸"
    )

    val materialIconsList = remember {
        MaterialIconProvider.allIcons.entries.toList()
    }

    val filteredIcons = remember(iconSearchQuery, materialIconsList) {
        if (iconSearchQuery.isBlank()) materialIconsList
        else materialIconsList.filter { it.key.contains(iconSearchQuery, ignoreCase = true) }
    }

    // Initialize state from existing category
    LaunchedEffect(category) {
        if (category != null) {
            emoji = category.emoji
            iconType = category.iconType
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category == null) "Add Category" else "Edit Category") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 500.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
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
                            modifier = Modifier.heightIn(max = 200.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredIcons) { (name, _) ->
                                val isSelected = emoji == name && iconType == "material"
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
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
                                    CategoryIcon(
                                        emoji = name,
                                        iconType = "material",
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }
                    "emoji" -> {
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
                            placeholder = { Text("Paste a Flaticon URL or image URL") }
                        )
                        if (emoji.isNotBlank() && iconType == "image") {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = emoji,
                                    contentDescription = null,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                    }
                }

                Text("Type", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("expense", "income", "savings").forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                parentId = null
                            },
                            label = { Text(t.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }

                Text("Parent Category (Optional)", style = MaterialTheme.typography.labelLarge)
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
                            .menuAnchor(MenuAnchorType.PrimaryEditable, true)
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
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CategoryIcon(emoji = parent.emoji, iconType = parent.iconType, fontSize = 16.sp)
                                        Text(parent.name)
                                    }
                                },
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
                onClick = {
                    if (name.isNotBlank()) {
                        val finalEmoji = when (iconType) {
                            "material" -> if (emoji.isNotBlank()) emoji else "category"
                            "emoji" -> if (emoji.isNotBlank()) emoji else "📁"
                            "image" -> if (emoji.isNotBlank()) emoji else ""
                            else -> emoji
                        }
                        onSave(name, finalEmoji, iconType, type, parentId)
                    }
                },
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