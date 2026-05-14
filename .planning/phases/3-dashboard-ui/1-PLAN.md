<objective>
Add pie chart visualization to Dashboard and implement transfer modal
</objective>

<context>
Dashboard currently shows stats but lacks visual chart. Need pie chart for expense breakdown by category.
Also missing transfer modal for moving money between accounts.
</context>

<tasks>

## 1. Add Chart Library
- [x] Add Compose-compatible chart library (Vico or custom Canvas)
- [x] Update build.gradle dependencies (MPAndroidChart already present)

## 2. Expense Pie Chart Component
- [x] Create ExpensePieChart composable
- [x] Fetch transactions grouped by category (last 30 days)
- [x] Display with legend showing category names and percentages
- [x] Handle empty state gracefully

## 3. Integrate Chart into Dashboard
- [x] Add chart between stats and recent transactions
- [x] Ensure responsive layout
- [x] Add loading state during data fetch

## 4. Transfer Modal
- [x] Create TransferDialog composable
- [x] Fields: From Account, To Account, Amount, Note
- [x] Validate sufficient balance
- [x] Create two transactions (expense from source, income to destination)
- [x] Add "Transfer" button to Accounts screen FAB menu

## 5. Dashboard FAB Update
- [x] Update Dashboard FAB to show menu with:
  - Add Transaction
  - Transfer Money
  - Quick Add

</tasks>

<success_criteria>
- Pie chart displays expense breakdown by category
- Transfer modal creates balanced transactions
- Both features work on all screen sizes
</success_criteria>
