# Phase 8: Dashboard Enhancements - Research

**Researched:** April 14, 2026
**Domain:** Android Dashboard UI + Room Data Layer
**Confidence:** HIGH

## Summary

Phase 8 implements 5 major dashboard enhancements: time filter pills, stats cards updates, spending pie chart with drill-down, budget widget, and reminders section. The codebase already has the `ExpensePieChart` component partially implemented, and the data layer already supports the necessary queries via TransactionDao, BudgetDao, and RecurringDao. Key gaps are in the UI layer (time filter component, drill-down functionality, budget widget on dashboard, reminders widget) and ViewModel state management.

## Current State Analysis

### What's Already Implemented

| Component | Location | Status |
|-----------|----------|--------|
| DashboardScreen | `app/src/main/java/com/moneymanager/ui/screens/DashboardScreen.kt` | Basic stats cards + pie chart + recent transactions |
| DashboardViewModel | `app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt` | Month-based aggregation |
| ExpensePieChart | `app/src/main/java/com/moneymanager/app/ui/components/ExpensePieChart.kt` | Basic donut chart with legend |
| TransactionDao | `app/src/main/java/com/moneymanager/data/dao/TransactionDao.kt` | Date range queries exist |
| BudgetDao | `app/src/main/java/com/moneymanager/data/dao/BudgetDao.kt` | Active budgets query exists |
| RecurringDao | `app/src/main/java/com/moneymanager/data/dao/RecurringDao.kt` | Active + due recurring queries exist |
| BudgetRepository | `app/src/main/java/com/moneymanager/domain/repository/BudgetRepository.kt` | Interface exists |
| BudgetsScreen | `app/src/main/java/com/moneymanager/app/ui/screens/BudgetsScreen.kt` | Basic budget display |

### What's Missing

1. **Time Filter Pills UI** - Filter buttons (Day/Week/Month/Year/All/Custom) not implemented in DashboardScreen
2. **Custom Date Range Picker** - From/To date inputs not implemented
3. **Stats Cards Dynamic Update** - Currently hardcoded to "month" - needs filter-aware recalculation
4. **Pie Chart Click Interaction** - No click handler for category drill-down
5. **Category Drill-Down Panel** - Not implemented (shows transaction list for selected category)
6. **Budget Dashboard Widget** - Budgets not shown on dashboard (only in BudgetsScreen)
7. **Reminders Dashboard Section** - Recurring items with upcoming dates not shown on dashboard

## User Constraints (from BRIDGE.md)

### Locked Decisions
- Time filter: Day/Week/Month/Year/All/Custom options
- Stats to show: Net Worth, Income, Expenses, Net
- Pie chart: Clickable for drill-down
- Budget widget: Current month budgets on dashboard

### Deferred Ideas (OUT OF SCOPE)
- Trend line charts (Phase 3 - Reports)
- Reports screen (Phase 3)

---

## Implementation Approach by Feature

### 1. Time Filter Pills

**Gap:** No filter UI in DashboardScreen
**Approach:** Add horizontal Row with FilterChip components

**UI Component - TimeFilterBar.kt (NEW):**
```kotlin
@Composable
fun TimeFilterBar(
    selectedFilter: TimeFilter,
    onFilterSelected: (TimeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TimeFilter.entries.forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) }
            )
        }
    }
}

enum class TimeFilter(val displayName: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year"),
    ALL("All"),
    CUSTOM("Custom")
}
```

**ViewModel changes:**
- Add `selectedFilter: TimeFilter` to DashboardUiState
- Add `customStartDate: Long?` and `customEndDate: Long?` for custom range
- Update `combine()` to react to filter changes
- Calculate date range based on selected filter

**Date Range Calculation:**
```kotlin
private fun getDateRangeForFilter(filter: TimeFilter): Pair<Long, Long> {
    val calendar = Calendar.getInstance()
    val endDate = calendar.timeInMillis
    
    val startDate = when (filter) {
        TimeFilter.DAY -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        TimeFilter.WEEK -> {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.timeInMillis
        }
        TimeFilter.MONTH -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        TimeFilter.YEAR -> {
            calendar.set(Calendar.DAY_OF_YEAR, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        TimeFilter.ALL -> 0L // Beginning of time
        TimeFilter.CUSTOM -> customStartDate ?: monthStart
    }
    return Pair(startDate, endDate)
}
```

**Files to Modify:**
- `DashboardViewModel.kt` - Add filter state + date range logic
- `DashboardScreen.kt` - Add TimeFilterBar UI component

**New Files Needed:**
- `app/src/main/java/com/moneymanager/app/ui/components/TimeFilterBar.kt`

---

### 2. Stats Cards Update

**Gap:** Currently hardcoded to month range only
**Approach:** Reactive state based on selected filter

**Current behavior (line 64-101 in DashboardViewModel.kt):**
```kotlin
val uiState: StateFlow<DashboardUiState> = combine(
    accountRepository.getTotalAssets(),
    accountRepository.getTotalDebt(),
    transactionRepository.getTransactionsByDateRange(monthStart, monthEnd),
    // ...
)
```

**Required changes:**
- Replace `monthStart/monthEnd` with dynamic date range from filter
- Net Worth should remain "all time" (sum of all accounts)
- Income/Expense/Net should respect selected time filter

**Data Model Changes:**
None needed - existing TransactionDao queries support date range.

**Files to Modify:**
- `DashboardViewModel.kt` - Update date range calculation
- `DashboardUiState.kt` - Add filter state fields

---

### 3. Spending Pie Chart (Clickable for Drill-down)

**Gap:** ExpensePieChart has no click interaction
**Approach:** Add clickable parameter + callback

**Current ExpensePieChart (line 26-114):**
```kotlin
@Composable
fun ExpensePieChart(
    entries: List<PieChartEntry>,
    modifier: Modifier = Modifier,
    currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
)
```

**Enhancement:**
```kotlin
@Composable
fun ExpensePieChart(
    entries: List<PieChartEntry>,
    modifier: Modifier = Modifier,
    currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US),
    onCategoryClick: ((PieChartEntry) -> Unit)? = null  // NEW
)
```

**Implementation:**
- Add clickable modifier to each legend item
- Show CategoryDrilldownPanel when category clicked

**CategoryDrilldownPanel.kt (NEW):**
```kotlin
@Composable
fun CategoryDrilldownPanel(
    category: PieChartEntry,
    transactions: List<TransactionEntity>,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            // Header with category name + close button
            // List of transactions for this category in selected period
        }
    }
}
```

**ViewModel changes:**
- Add `selectedCategory: PieChartEntry?` to DashboardUiState
- Add query to get transactions by category + date range

**Files to Modify:**
- `ExpensePieChart.kt` - Add onCategoryClick parameter
- `DashboardScreen.kt` - Add CategoryDrilldownPanel
- `DashboardViewModel.kt` - Add category transaction query

**New Files Needed:**
- `app/src/main/java/com/moneymanager/app/ui/components/CategoryDrilldownPanel.kt`

---

### 4. Budget Widget on Dashboard

**Gap:** Budgets shown only in BudgetsScreen, not Dashboard
**Approach:** Add budget section to Dashboard UI + load active budgets

**Current BudgetsScreen shows:**
- Budget cards with category, amount, linear progress indicator (hardcoded 50%)

**Dashboard budget widget should show:**
- Compact list of budget items with progress bars
- Color states: Green (<80%), Amber (80-100%), Red (>100%)
- Per BRIDGE.md: "Progress bars turn amber at 80% and red when over budget"

**Budget progress calculation (NEW in ViewModel):**
```kotlin
data class BudgetWithProgress(
    val budget: BudgetEntity,
    val categoryName: String,
    val spent: Double,
    val percentage: Float
)

// In ViewModel, for each budget:
// 1. Get current month expenses for that category
// 2. Calculate: spent / budget.amount * 100
// 3. Assign color state
```

**ViewModel changes:**
- Add `budgetsWithProgress: List<BudgetWithProgress>` to DashboardUiState
- Inject BudgetRepository
- Calculate progress from expense data

**UI Component - BudgetWidget.kt (NEW):**
```kotlin
@Composable
fun BudgetWidget(
    budgets: List<BudgetWithProgress>,
    currencyFormat: NumberFormat,
    onBudgetClick: (BudgetEntity) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Budgets - ${currentMonth}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            budgets.forEach { budgetWithProgress ->
                BudgetItemRow(
                    budgetWithProgress = budgetWithProgress,
                    currencyFormat = currencyFormat
                )
            }
        }
    }
}

@Composable
private fun BudgetItemRow(
    budgetWithProgress: BudgetWithProgress,
    currencyFormat: NumberFormat
) {
    val progressColor = when {
        budgetWithProgress.percentage > 100 -> MaterialTheme.colorScheme.error
        budgetWithProgress.percentage >= 80 -> Color(0xFFB8860B) // Amber
        else -> Color(0xFF2A6049) // Green
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = budgetWithProgress.categoryName,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${currencyFormat.format(budgetWithProgress.spent)} / ${currencyFormat.format(budgetWithProgress.budget.amount)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
    
    LinearProgressIndicator(
        progress = { (budgetWithProgress.percentage / 100).coerceIn(0f, 1f) },
        modifier = Modifier.fillMaxWidth(),
        color = progressColor
    )
}
```

**Files to Modify:**
- `DashboardViewModel.kt` - Add budget loading + progress calculation
- `DashboardScreen.kt` - Add BudgetWidget to dashboard list

**New Files Needed:**
- `app/src/main/java/com/moneymanager/app/ui/components/BudgetWidget.kt`

---

### 5. Dashboard Reminders Section

**Gap:** Recurring not shown on dashboard
**Approach:** Query upcoming recurring items and display as reminder banner

**Current RecurringEntity has:**
- `nextDate: Long` - when next transaction is due
- `isActive: Boolean`
- `reminderEnabled: Boolean`
- `note`, `amount`, `type`, `frequency`

**HTML Reference (lines 182-186 in MoneyManager.html):**
```css
.reminder-banner{background:var(--gold-light);border:1px solid rgba(184,134,11,0.3);border-radius:12px;padding:14px 16px;margin-bottom:14px}
.reminder-title{font-family:'DM Mono',monospace;font-size:10px;letter-spacing:1.5px;text-transform:uppercase;color:var(--gold);margin-bottom:8px}
.reminder-item{display:flex;align-items:center;gap:10px;padding:6px 0;border-bottom:1px solid rgba(184,134,11,0.15)}
.reminder-item:last-child{border-bottom:none}
```

**UI Component - RemindersWidget.kt (NEW):**
```kotlin
@Composable
fun RemindersWidget(
    upcomingRecurring: List<RecurringEntity>,
    onAddTransaction: (RecurringEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (upcomingRecurring.isEmpty()) return
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5ECD0) // gold-light
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "UPCOMING",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFB8860B)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            upcomingRecurring.take(5).forEach { recurring ->
                ReminderItem(
                    recurring = recurring,
                    onClick = { onAddTransaction(recurring) }
                )
            }
        }
    }
}

@Composable
private fun ReminderItem(
    recurring: RecurringEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recurring.note.ifEmpty { "Recurring ${recurring.type}" },
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = formatNextDate(recurring.nextDate), // e.g., "Tomorrow" or "Apr 20"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = currencyFormat.format(recurring.amount),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
```

**ViewModel changes:**
- Add `upcomingRecurring: List<RecurringEntity>` to DashboardUiState
- Inject RecurringRepository
- Query: Get all active recurring where nextDate is within 7 days

**Query (in RecurringDao):**
```kotlin
@Query("SELECT * FROM recurring WHERE isActive = 1 AND nextDate <= :date ORDER BY nextDate ASC LIMIT 5")
fun getUpcomingRecurring(date: Long): Flow<List<RecurringEntity>>
```
Note: Use current time + 7 days as the cutoff date.

**Files to Modify:**
- `DashboardViewModel.kt` - Add recurring loading
- `DashboardScreen.kt` - Add RemindersWidget to dashboard list

**New Files Needed:**
- `app/src/main/java/com/moneymanager/app/ui/components/RemindersWidget.kt`

---

## Standard Stack

| Library | Version | Purpose |
|---------|---------|---------|
| Jetpack Compose BOM | 2024.12.01 | UI framework |
| Room | 2.6.1 | Local database |
| Hilt | 2.52 | Dependency injection |
| Kotlin Coroutines | (via Lifecycle 2.8.7) | Async operations |

**Already in build.gradle.kts:**
- `androidx.compose.material3:material3` - FilterChip, Card
- `androidx.compose.material:material-icons-extended` - Icons
- MPAndroidChart is included but NOT used (custom Canvas charts instead)

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Date range calculation | Custom calendar logic | java.util.Calendar (already used) | Standard API |
| Pie chart rendering | Use MPAndroidChart | Custom Canvas in ExpensePieChart | Already implemented, lightweight |
| Filter UI | Custom toggle buttons | Material3 FilterChip | Native Material3 component |

---

## Common Pitfalls

### Pitfall 1: Date Range Edge Cases
**What goes wrong:** Month boundaries, leap years, timezone issues
**How to avoid:** Use Calendar consistently, test with edge dates (Jan 1, Dec 31, Feb 29)
**Warning signs:** Stats jump unexpectedly at month boundaries

### Pitfall 2: Budget Progress Bar Colors
**What goes wrong:** Hardcoded 50% in BudgetsScreen (line 89)
**How to avoid:** Calculate actual spent percentage from transactions
**Warning signs:** All budgets show same "50% used" regardless of actual spending

### Pitfall 3: Large Transaction Lists
**What goes wrong:** Loading all transactions for pie chart calculations
**How to avoid:** Use aggregate SQL queries (SUM by category) instead of loading all rows
**Warning signs:** Dashboard feels slow with many transactions

### Pitfall 4: Missing Recurring Data
**What goes wrong:** RecurringRepository not injected in DashboardViewModel
**How to avoid:** Add to constructor @Inject parameters
**Error:** " lateinit property recurringRepository has not been initialized"

---

## Code Examples

### Using FilterChip (Material3)
```kotlin
// Source: AndroidX Compose Material3 documentation
FilterChip(
    selected = selectedFilter == TimeFilter.MONTH,
    onClick = { viewModel.setFilter(TimeFilter.MONTH) },
    label = { Text("Month") },
    leadingIcon = if (selectedFilter == TimeFilter.MONTH) {
        { Icon(Icons.Default.Check, contentDescription = null) }
    } else null
)
```

### Date Range Query in ViewModel
```kotlin
// Source: DashboardViewModel.kt line 67
transactionRepository.getTransactionsByDateRange(monthStart, monthEnd)
```

### Loading Budgets with Progress
```kotlin
// BudgetRepository already has: getActiveBudgets() -> Flow<List<BudgetEntity>>
// Need: Calculate spent from transaction data
combine(
    budgetRepository.getActiveBudgets(),
    transactionRepository.getTransactionsByDateRange(monthStart, monthEnd)
) { budgets, transactions ->
    budgets.map { budget ->
        val spent = transactions
            .filter { it.type == "expense" && it.categoryId == budget.categoryId }
            .sumOf { it.amount }
        BudgetWithProgress(
            budget = budget,
            categoryName = getCategoryName(budget.categoryId),
            spent = spent,
            percentage = (spent / budget.amount * 100).toFloat()
        )
    }
}
```

---

## Open Questions

1. **Custom Date Range UI**: Should custom dates use a date picker dialog or inline TextField with DatePicker?
   - Recommendation: DatePickerDialog (Material3) for better UX on mobile

2. **Budget Widget Placement**: Should it appear above or below the pie chart?
   - HTML reference shows budgets below the charts at line 465
   - Recommendation: Below pie chart, above recent transactions

3. **Drill-down Navigation**: Should clicking a category open a modal or navigate to filtered transactions?
   - HTML reference shows inline panel (lines 327-331 CSS)
   - Recommendation: Modal bottom sheet for inline drill-down

4. **Reminders Threshold**: How far in advance to show upcoming recurring?
   - HTML doesn't specify - uses "upcoming" in general
   - Recommendation: 7 days (next week) - balance between useful and noisy

---

## Environment Availability

| Dependency | Required By | Available | Version |
|------------|------------|-----------|---------|
| Android SDK | Build | ✓ | API 35 |
| Room Database | Data layer | ✓ | 2.6.1 |
| Hilt | DI | ✓ | 2.52 |
| Compose BOM | UI | ✓ | 2024.12.01 |

No missing dependencies.

---

## Files to Modify Summary

| File | Changes |
|------|---------|
| `DashboardViewModel.kt` | Add TimeFilter state, custom date range, budget loading, recurring loading |
| `DashboardScreen.kt` | Add TimeFilterBar, BudgetWidget, RemindersWidget, CategoryDrilldownPanel |
| `DashboardUiState` (inline in ViewModel) | Add filter state fields, budget list, recurring list |

## New Files to Create

1. `app/src/main/java/com/moneymanager/app/ui/components/TimeFilterBar.kt`
2. `app/src/main/java/com/moneymanager/app/ui/components/BudgetWidget.kt`
3. `app/src/main/java/com/moneymanager/app/ui/components/RemindersWidget.kt`
4. `app/src/main/java/com/moneymanager/app/ui/components/CategoryDrilldownPanel.kt`

---

## Test Approach

**Unit Tests:**
- TimeFilter date range calculation correctness
- Budget progress calculation (spent / budget * 100)
- Recurring filter (within 7 days)

**Compose Tests:**
- TimeFilterBar renders all 6 filter options
- BudgetWidget shows correct color for progress states
- RemindersWidget shows nothing when list empty

**Manual Testing:**
- Filter by "Day" shows only today's transactions
- Filter by "Week" shows last 7 days
- Budget shows 80%+ as amber, >100% as red
- Pie chart click opens drill-down panel

---

## Sources

### Primary (HIGH confidence)
- `DashboardScreen.kt` - Current implementation
- `DashboardViewModel.kt` - Current state management
- `TransactionDao.kt` - Date range queries
- `BudgetDao.kt` - Budget queries
- `RecurringDao.kt` - Recurring queries
- BRIDGE.md - Phase 8 requirements

### Secondary (MEDIUM confidence)
- MoneyManager.html - HTML reference for UI patterns
- Material3 Compose documentation for FilterChip, Card components

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Using existing Room + Compose stack
- Architecture: HIGH - MVVM pattern already in use
- Pitfalls: HIGH - Known issues documented (budget 50% hardcode)

**Research date:** April 14, 2026
**Valid until:** July 14, 2026 (stable features, no major changes expected)