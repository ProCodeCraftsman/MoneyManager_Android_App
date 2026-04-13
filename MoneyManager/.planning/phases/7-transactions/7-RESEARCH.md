# Phase 7: Core Transaction Features - Research

**Researched:** 2026-04-13
**Domain:** Android/Room/Jetpack Compose - Transaction Management
**Confidence:** HIGH

## Summary

Phase 7 implements 5 core transaction features: search, filters, tags, sub-categories, and transfer between accounts. The existing codebase has a solid foundation with TransactionEntity already supporting `tagIds` (comma-separated), `type` (including "transfer"), and `parentId` in CategoryEntity. Key gaps exist in the DAO queries and UI layer.

**Primary recommendation:** Implement database-layer filtering first (DAO queries), then upgrade the ViewModel state management to support multi-criteria filtering, and finally enhance the UI with filter chips and tag assignment dialogs.

---

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Room | 2.6.1 | Local SQLite persistence | Primary data layer |
| Hilt | 2.52 | Dependency injection | DI framework |
| Compose BOM | 2024.12.01 | UI toolkit | Current project standard |
| Navigation Compose | 2.8.5 | Screen navigation | Current project standard |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Coil | 2.7.0 | Image loading | For receipt thumbnails (future phase) |
| MPAndroidChart | v3.1.0 | Charts | For dashboard pie charts (Phase 2) |

---

## Feature Analysis

### 1. Transaction Search

#### Current State
- `TransactionDao.searchTransactions()` exists but only searches `note` field
- `TransactionsViewModel` has basic search that filters by note in-memory
- No full-text search across description, category, or account name

#### Gap
- Need to search across: note, category name, account name
- Current approach: in-memory filtering is inefficient
- Should use Room FTS (Full-Text Search) or JOIN queries

#### Implementation Approach

**Option A: Enhanced JOIN Query (Recommended)**
Add new DAO method with JOIN to search across related fields:

```kotlin
// In TransactionDao.kt
@Query("""
    SELECT t.* FROM transactions t
    LEFT JOIN categories c ON t.categoryId = c.id
    LEFT JOIN accounts a ON t.accountId = a.id
    WHERE t.note LIKE '%' || :query || '%'
    OR c.name LIKE '%' || :query || '%'
    OR a.name LIKE '%' || :query || '%'
    ORDER BY t.date DESC
""")
fun searchTransactionsFullText(query: String): Flow<List<TransactionEntity>>
```

**Option B: FTS4 Table**
Create FTS table for faster text search (more complex, better for large datasets).

**Recommendation:** Start with Option A - simpler to implement, sufficient for typical transaction volumes (<10,000).

#### Files to Modify
- `TransactionDao.kt` - Add search query method
- `TransactionRepositoryImpl.kt` - Implement repository method
- `TransactionRepository.kt` - Add interface method
- `TransactionsViewModel.kt` - Update search to use DAO method

#### New Files Needed
- None

---

### 2. Transaction Filters

#### Current State
- `TransactionsViewModel` has basic `_filterType` (in-memory)
- DAO has `getTransactionsByType()`, `getTransactionsByAccount()`, `getTransactionsByCategory()`
- No combined filters (e.g., "expense" + "Food" + "Cash account")

#### Gap
- Need to filter by: type, account, category, tag (multiple criteria)
- Current: single-filter queries, need combined multi-criteria
- Need filter chips in UI for easy toggling

#### Implementation Approach

**Database Layer:**
Add combined filter query in DAO:

```kotlin
// In TransactionDao.kt
@Query("""
    SELECT t.* FROM transactions t
    WHERE (:type IS NULL OR t.type = :type)
    AND (:accountId IS NULL OR t.accountId = :accountId)
    AND (:categoryId IS NULL OR t.categoryId = :categoryId)
    AND (:startDate IS NULL OR t.date >= :startDate)
    AND (:endDate IS NULL OR t.date <= :endDate)
    ORDER BY t.date DESC
""")
fun getFilteredTransactions(
    type: String?,
    accountId: Long?,
    categoryId: Long?,
    startDate: Long?,
    endDate: Long?
): Flow<List<TransactionEntity>>
```

**ViewModel Layer:**
Add filter state and combine with search:

```kotlin
// In TransactionsViewModel.kt
data class TransactionFilter(
    val type: String? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val tagIds: List<Long> = emptyList(),
    val dateRange: Pair<Long, Long>? = null
)
```

**UI Layer:**
Add filter bar with dropdowns for type, account, category:

```kotlin
// Row with:
// - SearchTextField (existing)
// - FilterChip: Type (All/Income/Expense/Savings/Transfer)
// - FilterChip: Account (dropdown)
// - FilterChip: Category (dropdown)
// - "Clear Filters" text button
```

#### Files to Modify
- `TransactionDao.kt` - Add combined filter query
- `TransactionRepositoryImpl.kt` - Add repository method
- `TransactionRepository.kt` - Add interface method
- `TransactionsViewModel.kt` - Add filter state and logic
- `TransactionsScreen.kt` - Add filter UI components

#### New Files Needed
- `FilterBar.kt` - New composable for filter row (optional, can be inline)

---

### 3. Transaction Tags

#### Current State
- `TagEntity` exists with `id`, `name`, `color`
- `TagDao` exists with basic CRUD
- `TransactionEntity` has `tagIds: String = ""` (comma-separated IDs)
- No UI to assign tags to transactions
- Tags are NOT linked to specific categories (TagDao query has bug: `WHERE :categoryId IS NOT NULL` always fails)

#### Gap
- Need UI to create/edit tags (name + color picker)
- Need UI to assign tags to transactions
- Need to filter transactions by tag
- TagDao query needs fix

#### Implementation Approach

**Fix TagDao Query:**
```kotlin
// In TagDao.kt - Fix the broken query
@Query("SELECT * FROM tags WHERE categoryId = :categoryId ORDER BY name ASC")
fun getTagsByCategory(categoryId: Long): Flow<List<TagEntity>>
```

**Add Tag Filtering:**
```kotlin
// In TransactionDao.kt
@Query("SELECT * FROM transactions WHERE tagIds LIKE '%' || :tagId || '%'")
fun getTransactionsByTag(tagId: Long): Flow<List<TransactionEntity>>
```

**UI Components:**

1. **Tag Input in AddTransactionDialog:**
```kotlin
// In AddTransactionDialog - add tag selection
var selectedTags by remember { mutableStateOf<List<TagEntity>>(emptyList()) }

// Use ExposedDropdownMenu or chips for selection
Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
    selectedTags.forEach { tag ->
        SuggestionChip(
            onClick = { /* remove */ },
            label = { Text(tag.name) },
            containerColor = Color(android.graphics.Color.parseColor(tag.color))
        )
    }
}
OutlinedButton(onClick = { showTagSelector = true }) {
    Icon(Icons.Default.Add, null)
    Text("Add Tag")
}
```

2. **Tag Selector Dialog:**
```kotlin
@Composable
fun TagSelectorDialog(
    availableTags: List<TagEntity>,
    selectedTags: List<TagEntity>,
    onTagsSelected: (List<TagEntity>) -> Unit
) {
    // List of tags with checkboxes/selection
    // "Create New Tag" button
    // Color picker (predefined colors + hex input)
}
```

3. **Edit Transaction (to change tags):**
- Long-press on transaction shows options including "Edit Tags"

4. **Filter by Tag:**
```kotlin
// In filter bar, add tag dropdown
var selectedTagFilter by remember { mutableStateOf<TagEntity?>(null) }
```

#### Files to Modify
- `TagDao.kt` - Fix getTagsByCategory query, add getTransactionsByTag
- `TransactionDao.kt` - Add getTransactionsByTag query
- `TransactionRepositoryImpl.kt` - Add tag methods
- `TransactionRepository.kt` - Add interface methods
- `TransactionsViewModel.kt` - Add tags state, load available tags
- `TransactionsScreen.kt` - Add TagSelectorDialog, update AddTransactionDialog

#### New Files Needed
- `TagSelectorDialog.kt` - Tag selection UI
- `TagColors.kt` - Predefined color palette (optional, can be constants)

---

### 4. Sub-categories

#### Current State
- `CategoryEntity` has `parentId: Long? = null` - already supports hierarchy
- `CategoryDao` has `getSubCategories(parentId: Long)` and `getParentCategories()`
- No UI to see/use sub-categories in transactions
- Categories are stored flat with parentId reference

#### Gap
- Need UI to select category AND sub-category in transaction
- Need display of parent > sub category in transaction list
- Need filtering by sub-category

#### Implementation Approach

**Data Model (Already Complete):**
- CategoryEntity already has `parentId` - no changes needed

**DAO Enhancement:**
```kotlin
// In CategoryDao.kt - Add getSubCategoriesByType
@Query("SELECT * FROM categories WHERE parentId = :parentId AND type = :type ORDER BY name ASC")
fun getSubCategoriesByType(parentId: Long, type: String): Flow<List<CategoryEntity>>

// In TransactionDao.kt - Add filter by category including children
@Query("""
    SELECT * FROM transactions 
    WHERE categoryId = :categoryId 
    OR categoryId IN (SELECT id FROM categories WHERE parentId = :categoryId)
    ORDER BY date DESC
""")
fun getTransactionsByCategoryOrSubcategory(categoryId: Long): Flow<List<TransactionEntity>>
```

**UI - Add Transaction:**
```kotlin
// In AddTransactionDialog
var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
var selectedSubCategory by remember { mutableStateOf<CategoryEntity?>(null) }

// Two dropdowns:
// 1. Category dropdown (parent categories only)
// 2. Sub-category dropdown (filtered by selected category, enabled when category selected)

val parentCategories = categories.filter { it.parentId == null }
val subCategories = if (selectedCategory != null) 
    categories.filter { it.parentId == selectedCategory.id } 
else emptyList()
```

**UI - Transaction Display:**
```kotlin
// In TransactionCard - show parent > subcategory
val categoryName = if (subCategory != null) {
    "${parentCategory.name} > ${subCategory.name}"
} else {
    parentCategory?.name ?: "Uncategorized"
}
```

**Filter UI:**
- Same category filter dropdown shows grouped categories with sub-category indented

#### Files to Modify
- `CategoryDao.kt` - Add getSubCategoriesByType query
- `TransactionDao.kt` - Add getTransactionsByCategoryOrSubcategory
- `TransactionRepositoryImpl.kt` - Add new methods
- `TransactionRepository.kt` - Add interface methods
- `TransactionsViewModel.kt` - Add sub-category selection state
- `TransactionsScreen.kt` - Add AddTransactionDialog update with sub-category selection

#### New Files Needed
- None (updates to existing dialog)

---

### 5. Transfer Between Accounts

#### Current State
- `TransactionEntity` has `type: String` accepting "transfer" (from HTML BRIDGE.md)
- No explicit support for "transfer" type currently (AddTransactionDialog only has expense/income/savings)
- No `toAccountId` field in entity
- HTML shows transfer type with blue color (#5b6fb5)

#### Gap
- Need to allow transfer type selection in UI
- Need to select source AND destination account
- Transfer should NOT affect category (no category required for transfer type)
- Balance updates: source account decreases, destination account increases

#### Implementation Approach

**Data Model - Add toAccountId:**
```kotlin
// TransactionEntity.kt - Add toAccountId
@Entity(...)
data class TransactionEntity(
    ...
    val toAccountId: Long? = null,  // NEW: for transfers
    ...
)
```

**Add Transfer Type Chip:**
```kotlin
// In AddTransactionDialog
var type by remember { mutableStateOf("expense") }
// Update chip list:
listOf("income", "expense", "savings", "transfer").forEach { t ->
    FilterChip(
        selected = type == t,
        onClick = { type = t },
        label = { Text(t.replaceFirstChar { it.uppercase() }) }
    )
}
```

**Transfer-Specific UI:**
When type == "transfer", show second account dropdown:

```kotlin
if (type == "transfer") {
    // Source account (required)
    var fromAccountId by remember { mutableStateOf<Long?>(null) }
    // Destination account (required)
    var toAccountId by remember { mutableStateOf<Long?>(null) }
    
    OutlinedTextField(
        value = accounts.find { it.id == fromAccountId }?.name ?: "",
        onValueChange = { },
        label = { Text("From Account") },
        trailingIcon = { ExposedDropdownMenuAnchor(...) }
    )
    
    OutlinedTextField(
        value = accounts.find { it.id == toAccountId }?.name ?: "",
        onValueChange = { },
        label = { Text("To Account") },
        trailingIcon = { ExposedDropdownMenuAnchor(...) }
    )
    
    // Category NOT required for transfers
} else {
    // Show existing category dropdown
}
```

**Balance Update Logic:**
In repository or ViewModel, update account balances after transfer:

```kotlin
// When inserting a transfer transaction:
// 1. Insert transaction with type="transfer", fromAccountId, toAccountId
// 2. Update source account: balance = balance - amount
// 3. Update destination account: balance = balance + amount
// These should be done in a transaction or via Room @Transaction
```

**Display:**
```kotlin
// In TransactionCard
val displayText = when (transaction.type) {
    "transfer" -> {
        val fromName = accounts.find { it.id == transaction.accountId }?.name
        val toName = accounts.find { it.id == transaction.toAccountId }?.name
        "Transfer: $fromName → $toName"
    }
    else -> transaction.note.ifEmpty { transaction.type.replaceFirstChar { it.uppercase() } }
}
```

**Transfer Filter:**
Add "transfer" to type filter options.

#### Files to Modify
- `TransactionEntity.kt` - Add `toAccountId` field
- `TransactionDao.kt` - Add query for toAccountId, update indices
- `TransactionRepositoryImpl.kt` - Add balance update logic with @Transaction
- `TransactionRepository.kt` - Add interface method for transfer insert
- `TransactionsViewModel.kt` - Add transfer handling with balance updates
- `TransactionsScreen.kt` - Add transfer UI to AddTransactionDialog
- `AccountDao.kt` - Check for balance update method
- `AccountRepository.kt` - Check for balance update interface

#### New Files Needed
- None

---

## Architecture Patterns

### Recommended Project Structure
```
src/main/java/com/moneymanager/
├── data/
│   ├── dao/
│   │   ├── TransactionDao.kt     # Add filter queries
│   │   ├── CategoryDao.kt         # Add sub-category queries
│   │   └── TagDao.kt             # Fix queries
│   ├── entity/
│   │   ├── TransactionEntity.kt  # Add toAccountId
│   │   └── TagEntity.kt          # No change needed
│   └── repository/
│       ├── TransactionRepositoryImpl.kt  # Add transfer logic
│       └── CategoryRepositoryImpl.kt        # Add tag methods
├── domain/
│   └── repository/
│       ├── TransactionRepository.kt       # Add interface methods
│       └── CategoryRepository.kt          # Add interface methods
└── app/
    └── ui/
        └── screens/
            ├── TransactionsViewModel.kt   # Add filters, tags, transfer
            ├── TransactionsScreen.kt     # Add filter UI, dialogs
            └── components/
                ├── TagSelectorDialog.kt   # NEW
                └── FilterBar.kt          # NEW (optional)
```

### Pattern: Multi-Criteria Filter State

**State:**
```kotlin
data class TransactionsFilterState(
    val query: String = "",
    val type: String? = null,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val tagIds: List<Long> = emptyList(),
    val dateRange: Pair<Long, Long>? = null
)

val hasActiveFilters: Boolean
    get() = query.isNotEmpty() || type != null || accountId != null || 
            categoryId != null || tagIds.isNotEmpty() || dateRange != null
```

**Flow:**
1. User edits any filter
2. ViewModel constructs DAO query with non-null criteria
3. DAO returns filtered results
4. UI updates reactively

### Pattern: Tag Assignment UI

**Flow:**
1. Add/Edit transaction dialog shows "Tags" section
2. User clicks "Add Tag" → TagSelectorDialog opens
3. User selects existing tags or creates new
4. Tags stored as comma-separated IDs in TransactionEntity.tagIds

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Text search | Custom search with multiple LIKE queries | Single JOIN query (Option A) | Simpler, sufficient for standard volumes |
| Filter combination | In-memory filtering of all results | Room WHERE clause with multiple optional params | More efficient, less data transferred |
| Color selection | Custom color picker from scratch | Predefined palette + Material3 ColorPicker | Consistent UI, less code |

---

## Common Pitfalls

### Pitfall 1: TagDao Query Always Returns Empty
**What goes wrong:** `WHERE :categoryId IS NOT NULL` in getTagsByCategory always evaluates to true (literal) because the bind param is never NULL in SQL - the condition is evaluated before binding.
**How to avoid:** Use proper parameter binding or remove the condition and filter in Kotlin.
**Fix:** Change query to `WHERE categoryId = :categoryId` or remove categoryId filter entirely for global tags.

### Pitfall 2: Transfer Creates Orphan Balances
**What goes wrong:** Insert transfer without updating account balances in both accounts.
**How to avoid:** Use Room @Transaction for atomic balance updates, or perform balance updates in ViewModel/repository before/after insert.
**Warning signs:** Account balances don't match transaction totals.

### Pitfall 3: Sub-category Without Parent
**What goes wrong:** User selects sub-category but parent category is null in UI.
**How to avoid:** Make category dropdown mandatory before showing sub-category dropdown. Clear sub-category when category changes.

### Pitfall 4: Filter State Not Reset
**What goes wrong:** Clearing search query doesn't clear other active filters, leading to confusing results.
**How to avoid:** Add "Clear All Filters" button that resets entire filter state. Show badge on filter icon when filters are active.

---

## Code Examples

### Filter Query (TransactionDao)
```kotlin
// Source: Custom implementation based on current patterns
@Query("""
    SELECT t.* FROM transactions t
    LEFT JOIN categories c ON t.categoryId = c.id
    LEFT JOIN accounts a ON t.accountId = a.id
    WHERE (:query IS NULL OR :query = '' OR t.note LIKE '%' || :query || '%' OR c.name LIKE '%' || :query || '%' OR a.name LIKE '%' || :query || '%')
    AND (:type IS NULL OR t.type = :type)
    AND (:accountId IS NULL OR t.accountId = :accountId)
    AND (:categoryId IS NULL OR t.categoryId = :categoryId OR c.parentId = :categoryId)
    AND (:startDate IS NULL OR t.date >= :startDate)
    AND (:endDate IS NULL OR t.date <= :endDate)
    ORDER BY t.date DESC
""")
fun getFilteredTransactions(
    query: String?,
    type: String?,
    accountId: Long?,
    categoryId: Long?,
    startDate: Long?,
    endDate: Long?
): Flow<List<TransactionEntity>>
```

### ViewModel Filter State
```kotlin
// Source: Custom implementation
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _filterState = MutableStateFlow(TransactionsFilterState())
    val filterState: StateFlow<TransactionsFilterState> = _filterState.asStateFlow()

    val uiState: StateFlow<TransactionsUiState> = combine(
        _filterState.flatMapLatest { filter ->
            transactionRepository.getFilteredTransactions(
                query = filter.query.takeIf { it.isNotEmpty() },
                type = filter.type,
                accountId = filter.accountId,
                categoryId = filter.categoryId,
                startDate = filter.dateRange?.first,
                endDate = filter.dateRange?.second
            )
        },
        _filterState
    ) { transactions, filter ->
        // If tag filtering needed, do in-memory filter (fewer results after primary filter)
        val filteredByTags = if (filter.tagIds.isNotEmpty()) {
            transactions.filter { tx ->
                val txTagIds = tx.tagIds.split(",").mapNotNull { it.toLongOrNull() }
                filter.tagIds.any { it in txTagIds }
            }
        } else transactions

        TransactionsUiState(
            transactions = filteredByTags,
            isLoading = false,
            filterState = filter,
            hasActiveFilters = filter.hasActiveFilters
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TransactionsUiState()
    )

    fun updateFilter(filter: TransactionsFilterState.() -> TransactionsFilterState) {
        _filterState.update { it.filter() }
    }

    fun clearFilters() {
        _filterState.value = TransactionsFilterState()
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| In-memory search | Database JOIN search | This phase | Better performance for large datasets |
| Single filter only | Multi-criteria filter | This phase | Better UX, more powerful |
| Tags not used | Tags with transactions | This phase | Enable organization by tags |
| Categories flat | Parent > Sub categories | This phase | Better category organization |
| No transfer type | Transfer type | This phase | Move money between accounts |

**Deprecated/outdated:**
- In-memory search (replaced by JOIN query)
- Single-filter approach (replaced by multi-criteria)

---

## Open Questions

1. **Tags per Category or Global?**
   - What we know: Tags exist but not linked to categories in current spec
   - What's unclear: Should tags be category-specific or global (any transaction can have any tag)?
   - Recommendation: Start with global tags (simpler), add category-specific later if needed

2. **Split Transactions?**
   - What we know: BRIDGE.md mentions split transactions as Priority #3 in Gap Analysis, but NOT in Phase 1
   - What's unclear: Should split be in Phase 1 or deferred?
   - Recommendation: Defer to later phase - split requires significant UI complexity

3. **Tag Filtering Performance?**
   - What we know: Tags stored as comma-separated string, LIKE query works
   - What's unclear: With 1000+ transactions, is tag filtering fast enough in-memory?
   - Recommendation: Filter by tags in-memory after primary database filter - acceptable for now

---

## Environment Availability

> Step 2.6: SKIPPED (no external dependencies identified)

All required libraries are already in the project (Room, Hilt, Compose).

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4.13.2 + AndroidX Test |
| Config file | None (default) |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew test` |

### Phase Requirements → Test Map
| Feature | Behavior | Test Type | Automated Command |
|--------|----------|-----------|-----------------|
| Search | Full-text search across note/category/account | Unit | `TransactionDaoTest.searchTransactionsFullText()` |
| Filters | Multi-criteria filter returns correct results | Unit | `TransactionDaoTest.getFilteredTransactions()` |
| Tags | Tags assignable and filterable | Unit | `TagDaoTest.getTagsByCategory()`, `TransactionDaoTest.getTransactionsByTag()` |
| Sub-categories | Parent/sub category selection and display | Unit | `CategoryDaoTest.getSubCategories()` |
| Transfer | Transfer type updates both account balances | Unit | `TransactionRepositoryImplTest.insertTransfer()` |

### Wave 0 Gaps
- [ ] Test files for TransactionDao - covers all new queries
- [ ] Test files for CategoryDao - covers sub-category queries  
- [ ] Test files for TagDao - covers fixed queries

*(If no gaps: "Existing test infrastructure - add tests for new DAO methods when implementation starts")*

---

## Implementation Order

### Phase 7 Feature Dependencies

```
Order 1: Database Layer (Foundation)
├── 1.1 TransactionDao - Add filter queries
├── 1.2 TransactionDao - Add search JOIN query
├── 1.3 TransactionDao - Add getTransactionsByTag
├── 1.4 CategoryDao - Add getSubCategoriesByType
├── 1.5 TagDao - Fix getTagsByCategory query
├── 1.6 AccountDao - Verify balance update method exists
└── 1.7 TransactionEntity - Add toAccountId field

Order 2: Repository Layer
├── 2.1 TransactionRepository - Add filter/search/tag methods
├── 2.2 CategoryRepository - Add sub-category methods
├── 2.3 TransactionRepository - Add transfer with balance update

Order 3: ViewModel Layer
├── 3.1 TransactionsViewModel - Add filter state
├── 3.2 TransactionsViewModel - Add tags state
├── 3.3 TransactionsViewModel - Add transfer handling

Order 4: UI Layer  
├── 4.1 TransactionsScreen - Enhance filter UI with chips
├── 4.2 AddTransactionDialog - Add sub-category selection
├── 4.3 AddTransactionDialog - Add transfer type + toAccountId
├── 4.4 AddTransactionDialog - Add tag selection
├── 4.5 TagSelectorDialog - New dialog component
└── 4.6 TransactionCard - Update display for sub-categories/transfers
```

**Critical Path:**
- Filter queries in DAO → Filter in ViewModel → Filter UI (1→2→3)
- TagDao fix is prerequisite for tag UI
- toAccountId added to entity before transfer UI

---

## Sources

### Primary (HIGH confidence)
- AndroidX Room documentation - Query methods, Flow support
- Current codebase - TransactionEntity, CategoryEntity patterns

### Secondary (MEDIUM confidence)
- MoneyManager.html - UI patterns for tags, search, filters, transfer
- Hilt documentation - ViewModel injection patterns

### Tertiary (LOW confidence)
- MPAndroidChart docs - For pie charts (Phase 2)

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Already in project
- Architecture: HIGH - Follows existing patterns
- Pitfalls: MEDIUM - Identified via code review

**Research date:** 2026-04-13
**Valid until:** 2026-05-13 (30 days - stable domain)