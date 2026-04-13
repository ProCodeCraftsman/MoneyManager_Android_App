# Phase 9: Recurring Transactions & Reports - Research

**Researched:** 2026-04-14
**Domain:** Android Jetpack Compose, Room Database, Background Processing
**Confidence:** HIGH

## Summary

Phase 9 focuses on implementing recurring transactions (auto-generation) and enhancing the existing Reports screen. The app already has:
- **RecurringEntity** with complete data model (frequency, nextDate, isActive, reminderEnabled)
- **RecurringDao** with queries for due recurring items
- **RecurringRepository** with full CRUD operations
- **ReportsScreen** with Overview, Trends, Categories, Budgets tabs
- **TrendLineChart** and **ExpensePieChart** components already implemented
- **RemindersWidget** showing upcoming recurring transactions

**Primary recommendation:** Implement recurring auto-generation via WorkManager, create a Recurring management screen, and enhance category breakdown with horizontal bar chart.

---

## Current State (Already Implemented)

### Recurring Data Layer
| Component | Status | Details |
|-----------|--------|---------|
| RecurringEntity | ✅ Complete | Fields: id, accountId, type, amount, categoryId, note, frequency, nextDate, isActive, reminderEnabled, createdAt |
| RecurringDao | ✅ Complete | Includes `getDueRecurring(date)` for finding due items |
| RecurringRepository | ✅ Complete | Full interface + implementation |
| RepositoryModule | ✅ Bound | RecurringRepository already injected |

### Reports Screen
| Component | Status | Details |
|-----------|--------|---------|
| ReportsScreen.kt | ✅ Complete | 4 tabs: Overview, Trends, Categories, Budgets |
| ReportsViewModel | ✅ Complete | Calculates income/expense, trend data, category breakdown |
| TrendLineChart.kt | ✅ Complete | Line chart for income vs expenses over time |
| ExpensePieChart.kt | ✅ Complete | Donut chart for category breakdown |
| RemindersWidget.kt | ✅ Complete | Shows upcoming recurring on dashboard |

### Dashboard Integration
| Component | Status | Details |
|-----------|--------|---------|
| DashboardViewModel | ✅ Has recurring | Queries `recurringRepository.getActiveRecurring()` |
| Upcoming recurring display | ✅ Exists | RemindersWidget shows next 7 days |

---

## Gap Analysis

### What's Missing

| Gap | Priority | Impact |
|-----|----------|--------|
| **Recurring Auto-Generation** | HIGH | Recurring transactions never become actual transactions |
| **Recurring Management Screen** | HIGH | No UI to create/edit recurring items |
| **Horizontal Bar Chart** | MEDIUM | Category breakdown only shows pie chart, need bar chart too |
| **Category Bar Chart in Reports** | MEDIUM | Requirement from BRIDGE.md |

---

## Implementation Approach

### 1. Recurring Auto-Generation (WorkManager)

**Architecture:**
- Use **WorkManager** with **PeriodicWorkRequest** for scheduled processing
- Run daily to check for due recurring transactions
- On due: create TransactionEntity, update RecurringEntity.nextDate

**Worker Implementation:**
```kotlin
// New file: RecurringGenerationWorker.kt
class RecurringGenerationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val recurringDao = (applicationContext as MoneyManagerApp).database.recurringDao()
        val transactionDao = (applicationContext as MoneyManagerApp).database.transactionDao()
        
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }.timeInMillis

        val dueRecurring = recurringDao.getDueRecurring(today)
        
        for (recurring in dueRecurring) {
            // Create transaction
            val transaction = TransactionEntity(
                accountId = recurring.accountId,
                type = recurring.type,
                amount = recurring.amount,
                categoryId = recurring.categoryId,
                note = recurring.note,
                date = System.currentTimeMillis(),
                isRecurring = true,
                recurringId = recurring.id
            )
            transactionDao.insertTransaction(transaction)
            
            // Update nextDate based on frequency
            val nextDate = calculateNextDate(recurring.nextDate, recurring.frequency)
            recurringDao.updateRecurring(recurring.copy(nextDate = nextDate))
        }
        
        return Result.success()
    }
    
    private fun calculateNextDate(currentDate: Long, frequency: String): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
        when (frequency) {
            "daily" -> calendar.add(Calendar.DAY_OF_MONTH, 1)
            "weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
            "biweekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 2)
            "monthly" -> calendar.add(Calendar.MONTH, 1)
            "yearly" -> calendar.add(Calendar.YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
```

**Schedule:**
```kotlin
// In Application class or MainActivity
val workRequest = PeriodicWorkRequestBuilder<RecurringGenerationWorker>(
    1, TimeUnit.DAYS
).build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "recurring_generation",
    ExistingPeriodicWorkPolicy.KEEP,
    workRequest
)
```

### 2. Recurring Management Screen

**Navigation:** Add `Screen.Recurring` to MoneyManagerNavHost.kt

**Screens Needed:**
- `RecurringListScreen` - Show all recurring (active/inactive)
- `RecurringFormScreen` - Create/Edit recurring

**ViewModel:** RecurringViewModel with:
- List of all recurring transactions
- CRUD operations
- Toggle active/inactive

**Form Fields:**
- Amount (required)
- Type (income/expense/savings)
- Account (dropdown)
- Category (dropdown, nullable)
- Note (text)
- Frequency (daily/weekly/biweekly/monthly/yearly)
- Start Date (date picker)
- Reminder toggle

### 3. Category Horizontal Bar Chart

**New Component:** `CategoryBarChart.kt`

```kotlin
@Composable
fun CategoryBarChart(
    entries: List<PieChartEntry>,
    modifier: Modifier = Modifier,
    currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)
) {
    val total = entries.sumOf { it.value }
    
    Column(modifier = modifier) {
        entries.forEach { entry ->
            val percentage = if (total > 0) (entry.value / total * 100).toFloat() else 0f
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category name
                Text(
                    text = entry.label,
                    modifier = Modifier.width(100.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                // Bar
                LinearProgressIndicator(
                    progress = { percentage / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp),
                    color = entry.color,
                    trackColor = entry.color.copy(alpha = 0.2f)
                )
                
                // Amount
                Text(
                    text = currencyFormat.format(entry.value),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

**Integration:** Add to CategoriesTab in ReportsScreen.kt

---

## Files to Modify

| File | Modification |
|------|--------------|
| `app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt` | Add Recurring route |
| `app/src/main/java/com/moneymanager/app/ui/screens/ReportsScreen.kt` | Add horizontal bar chart |
| `app/build.gradle.kts` | Add WorkManager dependency |
| `app/src/main/java/com/moneymanager/app/MoneyManagerApp.kt` | Schedule recurring worker |

---

## New Files Needed

| File | Purpose |
|------|---------|
| `app/src/main/java/com/moneymanager/app/worker/RecurringGenerationWorker.kt` | Background worker for auto-generation |
| `app/src/main/java/com/moneymanager/app/ui/screens/RecurringViewModel.kt` | ViewModel for recurring list |
| `app/src/main/java/com/moneymanager/app/ui/screens/RecurringListScreen.kt` | List all recurring transactions |
| `app/src/main/java/com/moneymanager/app/ui/screens/RecurringFormScreen.kt` | Create/Edit recurring form |
| `app/src/main/java/com/moneymanager/app/ui/components/CategoryBarChart.kt` | Horizontal bar chart for categories |

---

## Standard Stack

### Dependencies Required
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| WorkManager | 2.9.1 | Background task scheduling | Android's recommended solution |
| Room | 2.6.1 | Already in use | — |
| Hilt | 2.52 | Already in use | — |

### Installation
```kotlin
// build.gradle.kts - add
implementation("androidx.work:work-runtime-ktx:2.9.1")
implementation("androidx.hilt:hilt-work:1.2.0")
ksp("androidx.hilt:hilt-compiler:1.2.0")
```

---

## Architecture Patterns

### Recurring Auto-Generation Flow
```
App Start → Schedule WorkManager (daily)
                ↓
Background Worker Runs
                ↓
Query: getDueRecurring(today)
                ↓
For each due:
  - Create TransactionEntity
  - Update RecurringEntity.nextDate
```

### Recurring Management Pattern
- List screen with FAB to add new
- Each item shows: name, amount, frequency, next date
- Swipe to delete, tap to edit
- Toggle active/inactive

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Background scheduling | Custom AlarmManager | WorkManager | Handles Doze mode, battery optimization |
| Date calculations | Manual Calendar logic | java.time (ThreeTenABP) or java.util.Calendar | Already available in Android |

---

## Common Pitfalls

### Pitfall 1: WorkManager Not Running on Emulator
**What goes wrong:** PeriodicWorkRequest doesn't trigger on emulator because time doesn't advance
**How to avoid:** Test with `setInitialDelay()` or manually trigger worker for testing
**Warning signs:** Logs show worker never runs

### Pitfall 2: Next Date Calculation Edge Cases
**What goes wrong:** Monthly/yearly frequencies don't account for month-end (e.g., Jan 31 → Feb 28)
**How to avoid:** Use `calendar.getActualMaximum(Calendar.DAY_OF_MONTH)` or accept shorter months
**Warning signs:** Users with monthly on 31st get skipped in shorter months

### Pitfall 3: Multiple Transactions Created
**What goes wrong:** Worker runs multiple times before nextDate updates
**How to avoid:** Use `getDueRecurring(date)` which filters by nextDate ≤ date
**Warning signs:** Duplicate transactions for same recurring

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Room | Data layer | ✓ | 2.6.1 | — |
| Hilt | DI | ✓ | 2.52 | — |
| WorkManager | Background tasks | ✗ | — | Add dependency |
| Compose | UI | ✓ | BOM 2024.12.01 | — |

**Missing dependencies with no fallback:**
- WorkManager - must be added to build.gradle.kts

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + Android Testing |
| Config file | none (defaults) |
| Quick run command | `./gradlew test` |
| Full suite command | `./gradlew testDebugUnitTests` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command |
|--------|----------|-----------|-------------------|
| REQ-9.1 | Auto-create transactions from recurring | unit | Test RecurringGenerationWorker |
| REQ-9.2 | Show recurring list screen | manual | UI test |
| REQ-9.3 | Create/Edit recurring form | manual | UI test |
| REQ-9.4 | Category bar chart in reports | unit | Test CategoryBarChart component |

---

## Sources

### Primary (HIGH confidence)
- AndroidX WorkManager Documentation - https://developer.android.com/guide/background
- Room Database Documentation - https://developer.android.com/jetpack/androidx/releases/room
- Jetpack Compose Charts - Custom Canvas implementation (existing code)

### Secondary (MEDIUM confidence)
- MPAndroidChart library (already in dependencies) - Could be alternative to custom charts

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - Android best practices
- Architecture: HIGH - Existing app patterns match
- Pitfalls: MEDIUM - Based on common Android issues

**Research date:** 2026-04-14
**Valid until:** 30 days (stable Android APIs)