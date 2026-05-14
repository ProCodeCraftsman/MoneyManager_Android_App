# Phase 25: Transaction Pagination

**Gathered:** 2026-04-27
**Status:** Ready for planning

<domain>
## Phase Boundary

Implementation of **pagination for transaction lists** using Android Paging 3 library to minimize loading time and improve app performance. The current implementation loads ALL transactions at once via `Flow<List<TransactionEntity>>`, which causes performance degradation as the transaction list grows.

Key aspects:
1. Integrate Android Paging 3 library (`androidx.paging:paging-runtime-ktx`)
2. Add Room Paging support (`androidx.room:room-paging`)
3. Modify TransactionDao to return `PagingSource<Int, TransactionEntity>`
4. Update TransactionRepository and TransactionRepositoryImpl for Paging 3
5. Refactor TransactionsViewModel to use `Flow<PagingData<TransactionEntity>>`
6. Update TransactionsScreen to use `LazyPagingItems` with `items()` instead of `items(List)`

</domain>

<decisions>
## Implementation Decisions

### D-01: Paging Library Choice
- **Library:** Android Paging 3 (androidx.paging)
- **Reasoning:** Official Google library, seamless Room integration, built-in invalidation handling
- **Alternative considered:** Manual LIMIT/OFFSET pagination (rejected - more code, no lifecycle awareness)

### D-02: Page Size
- **Page size:** 50 transactions per page
- **Initial load size:** 50 (same as page size for consistency)
- **Prefetch distance:** 2 pages (load next page when user is 100 items from end)

### D-03: Room Integration
- **Approach:** Use Room's built-in PagingSource support via `room-paging` artifact
- **DAO modification:** Change `getAllTransactions()` to return `PagingSource<Int, TransactionEntity>` 
- **No RemoteMediator needed:** Local database only, no network source

### D-04: Filtering Strategy
- **Challenge:** Paging 3 doesn't work well with post-load filtering (can't filter a PagingSource)
- **Solution:** Create separate PagingSources for each filter combination in DAO
- **Filters become parameters:** Pass filter state to DAO query, Room handles the rest

### D-05: Search Handling
- **Approach:** Search will continue to use the existing `searchTransactions(query)` which returns `Flow<List<TransactionEntity>>`
- **Reasoning:** Search is typically low-volume, full-text search across all transactions
- **Alternative:** Could implement Paging for search later if needed

### D-06: UI State
- **Change:** `transactions: List<TransactionEntity>` → `transactions: LazyPagingItems<TransactionEntity>`
- **Loading state:** Use `pagingItems.loadState.refresh` to detect initial load
- **End of pagination:** Use `pagingItems.loadState.append` to detect when all data is loaded

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Primary Files
- `MoneyManager/app/src/main/java/com/moneymanager/data/dao/TransactionDao.kt` — Add PagingSource return type
- `MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt` — Update interface
- `MoneyManager/app/src/main/java/com/moneymanager/data/repository/TransactionRepositoryImpl.kt` — Update implementation
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsViewModel.kt` — Refactor to PagingData
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt` — Migrate to LazyPagingItems

### Key Implementation References
- Lines 67-129 in TransactionsViewModel.kt show current `uiState` Flow combining transactions with filters
- Lines 608-732 in TransactionsScreen.kt show current LazyColumn with `items(transactions)` 
- Line 149: `lazyListState` already exists for scroll detection (keeping this)

### Existing Components (for reference)
- `com.moneymanager.app.ui.components.TransactionCardDense` — Used for rendering transaction items
- `com.moneymanager.app.ui.components.SplitTransactionCard` — Used for split transaction rendering

### Prior Phase Context
- Phase 24 (Scroll-to-Top Global) — Still pending, but our pagination work is independent
- Phase 22 (Code Refactoring) — TransactionItem extracted to `screens/TransactionItem.kt`

</canonical_refs>

<code_context>
## Existing Code Insights

### Current Performance Problem
- `transactionRepository.getAllTransactions()` returns `Flow<List<TransactionEntity>>`
- All transactions loaded into memory at once via `combine()` in ViewModel (line 67-129)
- Filtering happens in-memory after all data is loaded (lines 88-104)
- For large transaction lists (1000+), this causes slow initial load and high memory usage

### Reusable Assets
- Hilt DI already set up for Repository pattern
- Room database with TransactionEntity already has proper indices
- `derivedStateOf` pattern for scroll detection (line 150-152) can be preserved

### Integration Points
- Paging 3 integrates with Room via `PagingSource` 
- ViewModel uses `cachedIn(viewModelScope)` for state persistence across configuration changes
- Compose UI uses `LazyColumn` + `items()` — will change to `items(LazyPagingItems)`

### Build Configuration
- `build.gradle.kts` uses `room-ktx:2.8.4` — needs `room-paging` addition
- Kotlin version: `2.3.20` via plugin
- Min SDK: 26, Target SDK: 36

</code_context>

<specifics>
## Specific Implementation Details

### DAO Changes (TransactionDao.kt)
```kotlin
// Add room-paging import
import androidx.paging.PagingSource

// Change from:
@Query("SELECT * FROM transactions ORDER BY date DESC")
fun getAllTransactions(): Flow<List<TransactionEntity>>

// Change to:
@Query("SELECT * FROM transactions WHERE (:accountId IS NULL OR accountId = :accountId) AND ... ORDER BY date DESC")
fun getTransactionsPaged(
    accountId: Long?,
    type: String?,
    categoryId: Long?,
    goalId: Long?,
    tagId: Long?,
    startDate: Long?,
    endDate: Long?
): PagingSource<Int, TransactionEntity>
```

### ViewModel Changes (TransactionsViewModel.kt)
```kotlin
// Add imports
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.map

// Change from:
val uiState: StateFlow<TransactionsUiState> = combine(
    transactionRepository.getAllTransactions(),
    ...
) { array -> ... }

// Change to:
val transactionsPagingData: Flow<PagingData<TransactionEntity>> = 
    _filters.flatMapLatest { filters ->
        Pager(
            config = PagingConfig(
                pageSize = 50,
                enablePlaceholders = false,
                initialLoadSize = 50,
                prefetchDistance = 100
            )
        ) {
            transactionDao.getTransactionsPaged(
                accountId = filters.accountId,
                type = filters.type.takeIf { it.isNotEmpty() && it != "All" },
                categoryId = filters.categoryId,
                goalId = filters.goalId,
                tagId = filters.tagId,
                startDate = filters.startDate,
                endDate = filters.endDate
            )
        }.flow
    }.cachedIn(viewModelScope)
```

### UI Changes (TransactionsScreen.kt)
```kotlin
// Add imports
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.items

// In composable:
val pagingTransactions: LazyPagingItems<TransactionEntity> = 
    viewModel.transactionsPagingData.collectAsLazyPagingItems()

// Replace LazyColumn items:
LazyColumn(...) {
    items(
        items = pagingTransactions,
        key = { it.id }
    ) { tx ->
        // Render transaction item (tx can be null during load)
        tx?.let { transaction ->
            // Existing rendering logic
        }
    }
    
    // Handle load states
    item {
        when (pagingTransactions.loadState.append) {
            is LoadState.Loading -> CircularProgressIndicator()
            is LoadState.Error -> Text("Error loading more")
            else -> {}
        }
    }
}
```

### Build Configuration (app/build.gradle.kts)
```kotlin
dependencies {
    // Add Paging 3
    implementation("androidx.paging:paging-runtime-ktx:3.3.0")
    implementation("androidx.room:room-paging:2.8.4")
}
```

</specifics>

<deferred>
## Deferred Ideas

- **Search pagination:** Keep search as full list for now (typically low volume)
- **Swipe-to-refresh:** Could add later with Paging 3's refresh mechanism
- **Placeholder items:** `enablePlaceholders = false` for now (simpler implementation)

</deferred>

---

*Phase: 25-transaction-pagination*
*Context gathered: 2026-04-27*
