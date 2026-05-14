---
status: complete
phase: 27-data-layer-calculations
source: 27-01-SUMMARY.md, 27-02-SUMMARY.md
started: 2026-04-29T00:00:00Z
updated: 2026-04-29T00:30:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Project Builds Without Errors
expected: In Android Studio, run Build → Rebuild Project (or `./gradlew assembleDebug`). The project compiles with no errors in InsightsCalculator.kt, InsightsUiState.kt, and InsightsViewModel.kt.
result: pass

### 2. InsightsCalculator: No Android Runtime Imports
expected: Open InsightsCalculator.kt. The import block should only contain `com.moneymanager.data.entity.TransactionEntity`, `java.util.Calendar`, `java.text.SimpleDateFormat`, and `java.util.Locale` — no `android.*` imports anywhere in the file.
result: pass

### 3. InsightsCalculator: Net Position Formula
expected: In InsightsCalculator.kt around line 60, the netPosition formula should be `income - expense - savings + borrowing - lending` (NOT a simpler income - expense). This matches the Q2 resolution.
result: pass

### 4. InsightsCalculator: Split Children Filtered First
expected: At the top of the `compute()` function, the very first lines should filter both `currentMonthTxs` and `previousMonthTxs` with `filter { !it.isSplitChild }` BEFORE any amount aggregations happen. All 7 monetary figures use the filtered `current` list.
result: pass

### 5. InsightsCalculator: Risk Alert Priority + Cap
expected: The alerts list is sorted so WARNING alerts come before INFO, then `.take(3)` caps at 3. The RSK-03 (expense increase) and RSK-06 (savings improvement) alerts are guarded with `if (hasEnoughHistory && ...)` so they won't fire when there's no previous-month data.
result: pass

### 6. InsightsUiState: Four Data Contracts
expected: InsightsUiState.kt defines exactly 4 data classes: `InsightsUiState` (top-level with isLoading, hasEnoughHistory, currency), `StatusUiState` (7 monetary fields + monthLabel + hasTransactions), `RisksUiState` (alerts list + hasTransactions), and `TrendsUiState` (dominantType, dominantAmount, dailyIncome, dailyExpense, hasTransactions). Plus a `RiskAlert` data class with icon, title, explanation, triggeringValue, severity.
result: pass

### 7. InsightsViewModel: Uses Only Date-Range Queries
expected: InsightsViewModel.kt should call `transactionRepository.getTransactionsByDateRange()` twice (once for current month, once for previous month) and NEVER call `getAllTransactions()`. The `currentMonthDates` and `previousMonthDates` are computed using Calendar.
result: pass

### 8. InsightsViewModel: StateFlow with Currency
expected: The `uiState` property is a `StateFlow<InsightsUiState>` that `combine()`s three flows: currentMonthTxs, previousMonthTxs, and `preferencesManager.currency`. Inside the combine block it calls `InsightsCalculator.compute()` and sets `currency = currency` in the resulting `InsightsUiState`. Uses `SharingStarted.WhileSubscribed(5000)` and `initialValue = InsightsUiState()`.
result: pass

## Summary

total: 8
passed: 8
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps

[none]
