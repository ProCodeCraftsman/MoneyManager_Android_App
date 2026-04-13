<objective>
Add pie chart visualization to Dashboard and implement transfer modal
</objective>

<context>
Dashboard currently shows stats but lacks visual chart. Need pie chart for expense breakdown by category.
Also missing transfer modal for moving money between accounts.
</context>

<tasks>

## 1. Add Chart Library
- [ ] Add Compose-compatible chart library (Vico or custom Canvas)
- [ ] Update build.gradle dependencies

## 2. Expense Pie Chart Component
- [ ] Create ExpensePieChart composable
- [ ] Fetch transactions grouped by category (last 30 days)
- [ ] Display with legend showing category names and percentages
- [ ] Handle empty state gracefully

## 3. Integrate Chart into Dashboard
- [ ] Add chart between stats and recent transactions
- [ ] Ensure responsive layout
- [ ] Add loading state during data fetch

## 4. Transfer Modal
- [ ] Create TransferDialog composable
- [ ] Fields: From Account, To Account, Amount, Note
- [ ] Validate sufficient balance
- [ ] Create two transactions (expense from source, income to destination)
- [ ] Add "Transfer" button to Accounts screen FAB menu

## 5. Dashboard FAB Update
- [ ] Update Dashboard FAB to show menu with:
  - Add Transaction
  - Transfer Money
  - Quick Add

</tasks>

<success_criteria>
- Pie chart displays expense breakdown by category
- Transfer modal creates balanced transactions
- Both features work on all screen sizes
</success_criteria>
