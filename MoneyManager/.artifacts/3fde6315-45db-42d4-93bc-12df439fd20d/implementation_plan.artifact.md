# Implementation Plan - Include Child Category Transactions in Filters

When a parent category is selected as a filter in the transaction screen, all transactions belonging to its child categories should also be included in the results.

## Proposed Changes

### Data Layer

#### [MODIFY] [TransactionDao.kt](file:///D:/Android%20projec/MoneyManager/app/src/main/java/com/moneymanager/data/dao/TransactionDao.kt)

Update the `WHERE` clause in the following methods to include transactions from subcategories when a category filter is applied:
- `getTransactionsWithFilters`
- `getTransactionsWithFiltersOldest`
- `getTransactionsWithFiltersHighest`
- `getTransactionsWithFiltersLowest`
- `getTransactionSummaryWithFilters`

The current filter:
```sql
AND (:categoryId IS NULL OR categoryId = :categoryId)
```
will be updated to:
```sql
AND (:categoryId IS NULL OR categoryId = :categoryId OR categoryId IN (SELECT id FROM categories WHERE parentId = :categoryId))
```

## Verification Plan

### Automated Tests
- I will check if there are existing tests for `TransactionDao` and run them to ensure no regressions.
- If no tests exist, I will rely on building the project to ensure syntax correctness.

### Manual Verification
- Verify the build finishes successfully.
- The user can verify by:
    1. Selecting a parent category in the transaction filter.
    2. Confirming that transactions from both the parent category and its child categories are displayed.
