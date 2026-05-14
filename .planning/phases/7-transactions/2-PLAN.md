---
phase: 7-transactions
plan: 2
type: execute
wave: 1
depends_on: []
files_modified: []
autonomous: true
requirements:
  - TX-05
user_setup: []
must_haves:
  truths:
    - "User can create transfers between accounts (no income/expense recorded)"
    - "User can access transfer feature from main navigation"
  artifacts:
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt"
      provides: "Navigation with transfer route"
  key_links:
    - from: "TransferScreen"
      to: "TransferViewModel"
      via: "hiltViewModel()"
      pattern: "hiltViewModel<TransferViewModel>"
    - from: "Screen.Transfer"
      to: "MoneyManagerNavHost"
      via: "composable"
      pattern: "composable\\(Screen\\.Transfer"
gap_closure: true
---

<objective>
Close gaps from VERIFICATION.md for Phase 7: Core Transaction Features.
Gap 1: DashboardViewModel.transferMoney already uses type="transfer" (verified code is correct)
Gap 2: Transfer accessible from navigation - add Transfer to bottom nav or main menu
</objective>

<context>
@.planning/phases/7-transactions/VERIFICATION.md

Current state analysis from code:
- DashboardViewModel.kt lines 342, 351: Already uses type="transfer" ✓
- TransferDialog.kt: Correctly delegates to DashboardViewModel.transferMoney
- TransferScreen.kt: Exists and uses type="transfer"
- Screen.Transfer: Already defined in MoneyManagerNavHost.kt line 29
- Transfer route has composable: Lines ~108-113 in MoneyManagerNavHost.kt
</context>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<tasks>

<task type="auto">
  <name>Task 1: Add Transfer to Bottom Navigation</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt</files>
  <read_first>MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt</read_first>
  <action>
    Add TransferScreen.icon to the Screen class and include Transfer in the bottom navigation bar.

    1. Update Screen.Transfer at line 29 to include an icon:
       data object Transfer : Screen("transfer", "Transfer", Icons.Default.SwapHoriz)

    2. Add the import at the top:
       import androidx.compose.material.icons.filled.SwapHoriz

    3. Add Screen.Transfer to bottomNavScreens list (line 39-48):
       val bottomNavScreens = listOf(
           Screen.Dashboard,
           Screen.Accounts,
           Screen.Transactions,
           Screen.Budgets,
           Screen.Reports,
           Screen.Recurring,
           Screen.Goals,
           Screen.Transfer,  <-- Add this line
           Screen.Settings
       )
  </action>
  <acceptance_criteria>
    - grep -n "Screen.Transfer" MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt shows Transfer in bottomNavScreens
    - Build succeeds: ./gradlew assembleDebug
  </acceptance_criteria>
  <verify>
    <automated>grep -n "Screen.Transfer" MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt</automated>
  </verify>
  <done>Transfer accessible from bottom navigation bar</done>
</task>

<task type="auto">
  <name>Task 2: Verify Transfer Type Consistency</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt</files>
  <read_first>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt</read_first>
  <action>
    Verify and confirm DashboardViewModel.transferMoney uses type="transfer" (NOT "expense"/"income").

    Current code at lines 342 and 351:
    - Line 342: type = "transfer" (for source account)
    - Line 351: type = "transfer" (for destination account)

    This is already correct. No changes needed - just verify.
  </action>
  <acceptance_criteria>
    - grep 'type = "transfer"' MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt returns 2 matches
  </acceptance_criteria>
  <verify>
    <automated>grep -c 'type = "transfer"' MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/DashboardViewModel.kt</automated>
  </verify>
  <done>Transfer uses type="transfer" consistently</done>
</task>

</tasks>

<verification>
- [ ] Transfer screen appears in bottom navigation
- [ ] Bottom nav shows transfer icon (SwapHoriz)
- [ ] DashboardViewModel.transferMoney uses type="transfer" (verified)
- [ ] Build compiles successfully
</verification>

<success_criteria>
- User can navigate to Transfer from bottom nav bar
- Transfers are recorded with type="transfer" not income/expense
</success_criteria>

<output>
After completion, create `.planning/phases/7-transactions/2-SUMMARY.md`
</output>