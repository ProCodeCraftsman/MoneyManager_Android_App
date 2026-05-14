---
phase: 7-transactions
plan: 1
type: execute
wave: 1
depends_on: []
files_modified: []
autonomous: true
requirements:
  - TX-01
  - TX-02
  - TX-03
  - TX-04
  - TX-05
user_setup: []
must_haves:
  truths:
    - "User can search transactions by description, note, or amount"
    - "User can filter transactions by type, account, category, and tags"
    - "User can create, edit, delete tags with custom colors"
    - "User can assign multiple tags to a transaction"
    - "User can create categories with sub-categories (parent/child hierarchy)"
    - "User can create transfers between accounts (no income/expense recorded)"
  artifacts:
    - path: "MoneyManager/app/src/main/java/com/moneymanager/data/dao/TransactionDao.kt"
      provides: "Search and filter queries"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/data/dao/TagDao.kt"
      provides: "Tag CRUD operations"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/data/dao/CategoryDao.kt"
      provides: "Sub-category queries"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt"
      provides: "Transaction business logic"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/domain/repository/CategoryRepository.kt"
      provides: "Category and tag business logic"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsViewModel.kt"
      provides: "Transaction screen state management"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt"
      provides: "Transaction list UI with search/filters"
  key_links:
    - from: "TransactionsScreen.kt"
      to: "TransactionsViewModel"
      via: "collectAsStateWithLifecycle"
      pattern: "viewModel.uiState"
    - from: "TransactionsViewModel"
      to: "TransactionRepository"
      via: "transactionRepository"
      pattern: "transactionRepository\\..*"
    - from: "TransactionRepository"
      to: "TransactionDao"
      via: "transactionDao"
      pattern: "transactionDao\\..*"
---

<objective>
Implement Phase 7: Core Transaction Features including Transaction Search, Filters, Tags, Sub-categories, and Transfer Between Accounts.
</objective>

<context>
@.planning/ROADMAP.md

Current codebase analysis:
- TransactionEntity: has type (income/expense/savings/transfer), tagIds (comma-separated), categoryId, accountId
- CategoryEntity: has parentId for sub-categories support
- TagEntity: exists with name and color
- TransactionDao: has searchTransactions(query), getTransactionsByType, getTransactionsByAccount, getTransactionsByCategory
- CategoryDao: has getParentCategories(), getSubCategories(parentId)
- TagDao: exists with basic CRUD
- TransactionsViewModel: has basic search and type filtering in memory
- TransactionsScreen: has basic search UI
</context>

<must_haves>
  truths:
    - "User can search transactions by description, note, or amount"
    - "User can filter transactions by type, account, category, and tags"
    - "User can create, edit, delete tags with custom colors"
    - "User can assign multiple tags to a transaction"
    - "User can create categories with sub-categories (parent/child hierarchy)"
    - "User can create transfers between accounts (no income/expense recorded)"
  artifacts:
    - path: "MoneyManager/app/src/main/java/com/moneymanager/data/dao/TransactionDao.kt"
      provides: "Search and filter queries"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/data/dao/TagDao.kt"
      provides: "Tag CRUD operations"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/data/dao/CategoryDao.kt"
      provides: "Sub-category queries"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt"
      provides: "Transaction business logic"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/domain/repository/CategoryRepository.kt"
      provides: "Category and tag business logic"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsViewModel.kt"
      provides: "Transaction screen state management"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt"
      provides: "Transaction list UI with search/filters"
  key_links:
    - from: "TransactionsScreen.kt"
      to: "TransactionsViewModel"
      via: "collectAsStateWithLifecycle"
      pattern: "viewModel.uiState"
    - from: "TransactionsViewModel"
      to: "TransactionRepository"
      via: "transactionRepository"
      pattern: "transactionRepository\\..*"
    - from: "TransactionRepository"
      to: "TransactionDao"
      via: "transactionDao"
      pattern: "transactionDao\\..*"
</must_haves>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<tasks>

<task type="database">
  <name>Task 1: Add Database Support for Multi-Criteria Transaction Filtering</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/data/dao/TransactionDao.kt</files>
  <action>
    Add the following queries to TransactionDao:
    1. getTransactionsByAccountAndType(accountId: Long, type: String) - Filter by account AND type
    2. getTransactionsByAccountAndCategory(accountId: Long, categoryId: Long) - Filter by account AND category
    3. getTransactionsByAccountAndDateRange(accountId: Long, startDate: Long, endDate: Long) - Filter by account AND date
    4. getTransactionsByTag(tagId: Long) - Get transactions with specific tag in tagIds comma-separated field
    5. getTransactionsWithFilters(accountId: Long?, type: String?, categoryId: Long?, tagId: Long?, startDate: Long?, endDate: Long?) - Combined filter query
    
    Note: For tag filtering, use LIKE query: "tagIds LIKE '%' || :tagId || '%'" since tags are stored as comma-separated string
  </action>
  <verify>
    <automated>grep -n "getTransactionsWithFilters\|getTransactionsByTag" MoneyManager/app/src/main/java/com/moneymanager/data/dao/TransactionDao.kt</automated>
  </verify>
  <done>TransactionDao has all required filter queries for multi-criteria filtering</done>
</task>

<task type="repository">
  <name>Task 2: Extend TransactionRepository with Filter Methods</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt</files>
  <action>
    Add to TransactionRepository interface:
    1. getTransactionsWithFilters(accountId: Long?, type: String?, categoryId: Long?, tagId: Long?, startDate: Long?, endDate: Long?): Flow<List<TransactionEntity>>
    2. getTransactionsByTag(tagId: Long): Flow<List<TransactionEntity>>
    
    Then implement in TransactionRepositoryImpl:
    - Add filter logic combining all parameters with null checks
  </action>
  <verify>
    <automated>grep -n "getTransactionsWithFilters\|getTransactionsByTag" MoneyManager/app/src/main/java/com/moneymanager/domain/repository/TransactionRepository.kt</automated>
  </verify>
  <done>TransactionRepository provides filter methods</done>
</task>

<task type="repository">
  <name>Task 3: Extend CategoryRepository with Sub-category Methods</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/domain/repository/CategoryRepository.kt</files>
  <action>
    Add to CategoryRepository interface:
    1. getParentCategories(): Flow<List<CategoryEntity>> - Get top-level categories (parentId = null)
    2. getSubCategories(parentId: Long): Flow<List<CategoryEntity>> - Get children of a category
    3. getCategoriesWithSubCategories(): Flow<List<CategoryEntity>> - Get all categories with hierarchy info
    
    Already exists in CategoryDao, just need to expose via repository
  </action>
  <verify>
    <automated>grep -n "getParentCategories\|getSubCategories" MoneyManager/app/src/main/java/com/moneymanager/domain/repository/CategoryRepository.kt</automated>
  </verify>
  <done>CategoryRepository supports sub-category operations</done>
</task>

<task type="database">
  <name>Task 4: Add Indexes for Transaction Search Performance</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/data/entity/TransactionEntity.kt</files>
  <action>
    Update TransactionEntity indices to improve search/filter performance:
    - Add Index("note") for full-text search
    - Add Index("tagIds") for tag filtering
    
    Current indices: [Index("accountId"), Index("categoryId"), Index("date")]
  </action>
  <verify>
    <automated>grep -n "Index" MoneyManager/app/src/main/java/com/moneymanager/data/entity/TransactionEntity.kt</automated>
  </verify>
  <done>TransactionEntity has indexes on searchable/filterable columns</done>
</task>

<task type="viewmodel">
  <name>Task 5: Enhance TransactionsViewModel with Full Filter Support</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsViewModel.kt</files>
  <action>
    Extend TransactionsUiState and TransactionsViewModel:
    1. Add filter state: filterAccountId: Long?, filterCategoryId: Long?, filterTagId: Long?, filterStartDate: Long?, filterEndDate: Long?
    2. Add methods:
       - setAccountFilter(accountId: Long?)
       - setCategoryFilter(categoryId: Long?)
       - setTagFilter(tagId: Long?)
       - setDateRangeFilter(startDate: Long?, endDate: Long?)
       - clearAllFilters()
    3. Update uiState combine to include all filters in filtering logic
    4. Add allTags: StateFlow<List<TagEntity>> - expose all tags for UI
    5. Add allCategories: StateFlow<List<CategoryEntity>> - expose categories for UI
  </action>
  <verify>
    <automated>grep -n "filterAccountId\|filterCategoryId\|filterTagId" MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsViewModel.kt</automated>
  </verify>
  <done>TransactionsViewModel manages all filter state and provides filter methods</done>
</task>

<task type="ui">
  <name>Task 6: Create Filter Bottom Sheet UI Component</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/TransactionFilterSheet.kt</files>
  <action>
    Create new file TransactionFilterSheet.kt with:
    1. ModalBottomSheet with filter options
    2. Type filter: Chip group (All, Income, Expense, Savings, Transfer)
    3. Account filter: Dropdown menu with accounts
    4. Category filter: Dropdown with parent categories, expandable to sub-categories
    5. Tag filter: LazyRow of tag chips (multi-select)
    6. Date range: Date pickers for start and end
    7. Clear filters button
    8. Apply filters button
    9. Active filter indicators (show badge count)
  </action>
  <verify>
    <automated>ls MoneyManager/app/src/main/java/com/moneymanager/app/ui/components/TransactionFilterSheet.kt</automated>
  </verify>
  <done>Filter bottom sheet component created with all filter options</done>
</task>

<task type="ui">
  <name>Task 7: Update TransactionsScreen with Filter UI and Chips</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt</files>
  <action>
    Update TransactionsScreen:
    1. Add filter icon button in TopAppBar (trailing)
    2. Show active filter chips below search bar (horizontal scroll)
    3. Tap chip to remove that filter
    4. Filter badge showing count of active filters
    5. Connect filter sheet to ViewModel state
    6. Show "No results" message when filters return empty
    
    Keep existing search bar functionality, integrate with new filters
  </action>
  <verify>
<automated>grep -n "FilterChip\|filterSheet\|activeFilters" MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt</automated>
  </verify>
  <done>TransactionsScreen has filter UI with chips and bottom sheet</done>
</task>

<task type="ui">
  <name>Task 8: Create Tag Management UI</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TagsScreen.kt</files>
  <action>
    Create new TagsScreen.kt:
    1. List all tags with name and color indicator
    2. FAB to add new tag
    3. Dialog for create/edit tag:
       - Name text field
       - Color picker (predefined colors or hex input)
       - Save/Cancel buttons
    4. Swipe to delete tag (with confirmation)
    5. Tap tag to edit
    
    Create TagsViewModel.kt:
    - allTags: StateFlow<List<TagEntity>>
    - addTag(name, color)
    - updateTag(tag)
    - deleteTag(tag)
  </action>
  <verify>
<automated>ls MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TagsScreen.kt</automated>
  </verify>
  <done>Tag management screen created with CRUD operations</done>
</task>

<task type="ui">
  <name>Task 9: Update AddTransactionDialog with Tag and Category Selection</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt</files>
  <action>
    Update AddTransactionDialog:
    1. Add category dropdown (shows parent > sub hierarchy)
    2. Add tag selection (multi-select chips)
    3. Show selected tags with remove option
    4. When saving, set tagIds as comma-separated string
    
    Update TransactionEntity creation to include:
    - categoryId (nullable)
    - tagIds (build from selected tags)
  </action>
  <verify>
<automated>grep -n "tagIds\|categoryId\|TagChip" MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransactionsScreen.kt</automated>
  </verify>
  <done>AddTransactionDialog supports category and tag assignment</done>
</task>

<task type="ui">
  <name>Task 10: Create Categories Management Screen with Sub-category Support</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/CategoriesScreen.kt</files>
  <action>
    Create new CategoriesScreen.kt:
    1. Display categories in expandable groups by type (expense/income/savings)
    2. Each parent category shows sub-category count badge
    3. Expand to show sub-categories indented
    4. FAB to add new category
    5. Dialog for create/edit:
       - Name field
       - Emoji picker
       - Type selector (expense/income/savings)
       - Parent category dropdown (null = top-level)
    6. Swipe to delete (warn if has sub-categories)
    
    Create CategoriesViewModel.kt:
    - parentCategories: StateFlow<List<CategoryEntity>>
    - subCategories(parentId): Flow<List<CategoryEntity>>
    - addCategory(name, emoji, type, parentId)
    - updateCategory(category)
    - deleteCategory(category)
  </action>
  <verify>
<automated>ls MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/CategoriesScreen.kt</automated>
  </verify>
  <done>Categories management screen with sub-category support</done>
</task>

<task type="ui">
  <name>Task 11: Create Transfer Between Accounts Feature</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransferScreen.kt</files>
  <action>
    Create TransferScreen.kt:
    1. From Account dropdown (all accounts)
    2. To Account dropdown (all accounts, different from "from")
    3. Amount field (numeric, validated > 0)
    4. Note field (optional)
    5. Date picker (default today)
    6. Transfer button
    
    Create TransferViewModel.kt:
    - executeTransfer(fromAccountId, toAccountId, amount, note, date)
    
    Business logic:
    1. Create TWO transactions:
       - From account: type="transfer", amount negative, note="Transfer to [Account Name]"
       - To account: type="transfer", amount positive, note="Transfer from [Account Name]"
    2. Both transactions linked via note or separate field
    3. Validate fromAccount has sufficient balance
    4. Update both account balances
    
    Validation:
    - Cannot transfer to same account
    - Amount must be > 0
    - From account balance >= transfer amount
  </action>
  <verify>
<automated>ls MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/TransferScreen.kt</automated>
  </verify>
  <done>Transfer between accounts feature with dual transaction creation</done>
</task>

<task type="ui">
  <name>Task 12: Add Transfer Button to Navigation and Home</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/navigation/Screen.kt</files>
  <action>
    1. Add Transfer to navigation routes (Screen.kt)
    2. Add Transfer button to:
       - Dashboard quick actions
       - Bottom navigation (optional, or under More menu)
    3. Add Transfer option in AddTransactionDialog type selector OR separate button
  </action>
  <verify>
<automated>grep -n "transfer\|Transfer" MoneyManager/app/src/main/java/com/moneymanager/app/ui/navigation/Screen.kt</automated>
  </verify>
  <done>Transfer accessible from navigation</done>
</task>

<task type="viewmodel">
  <name>Task 13: Add Account Repository Methods for Balance Updates</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/domain/repository/AccountRepository.kt</files>
  <action>
    Add to AccountRepository interface for transfer support:
    1. updateAccountBalance(accountId: Long, delta: Double) - Add/subtract from balance
    2. getAccountBalance(accountId: Long): Flow<Double> - Get current balance
    
    Implement in AccountRepositoryImpl using transaction in Room
  </action>
  <verify>
<automated>grep -n "updateAccountBalance\|getAccountBalance" MoneyManager/app/src/main/java/com/moneymanager/domain/repository/AccountRepository.kt</automated>
  </verify>
  <done>AccountRepository supports balance updates for transfers</done>
</task>

<task type="test">
  <name>Task 14: Add Basic Unit Tests for Filter Logic</name>
  <files>MoneyManager/app/src/test/java/com/moneymanager/TransactionsViewModelTest.kt</files>
  <action>
    Create basic test class:
    1. Test filter combination (type + account + category)
    2. Test tag filtering (parsing comma-separated)
    3. Test date range filtering
    4. Test clear filters restores all transactions
    
    Use mock repository or test repository implementation
  </action>
  <verify>
<automated>ls MoneyManager/app/src/test/java/com/moneymanager/TransactionsViewModelTest.kt</automated>
  </verify>
  <done>Basic filter logic tests exist</done>
</task>

</tasks>

<verification>
- [ ] TransactionDao has all filter queries
- [ ] TransactionRepository exposes filter methods
- [ ] CategoryRepository supports sub-categories
- [ ] TransactionsViewModel manages all filter state
- [ ] Filter bottom sheet UI component created
- [ ] TransactionsScreen shows filter chips and active filters
- [ ] TagsScreen for tag CRUD
- [ ] AddTransactionDialog includes category and tag selection
- [ ] CategoriesScreen with sub-category hierarchy
- [ ] TransferScreen with dual transaction creation
- [ ] Transfer accessible from navigation
- [ ] AccountRepository supports balance updates
</verification>

<success_criteria>
- User can search transactions by description/note and see results within 100ms
- User can filter by type, account, category, tag, and date range with AND logic
- User can create tags with custom colors and assign to transactions
- User can create categories with sub-categories in hierarchy
- User can transfer money between accounts (two transactions created, balances updated)
- All CRUD operations work correctly with no regression
</success_criteria>

<output>
After completion, create `.planning/phases/7-transactions/phase-1-SUMMARY.md`
</output>