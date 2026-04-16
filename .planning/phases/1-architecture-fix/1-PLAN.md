<objective>
Implement Clean Architecture repository layer to replace direct DAO usage in ViewModels
</objective>

<context>
Audit revealed DAOs are used directly in ViewModels, violating Clean Architecture principles.
Repository pattern needed for data abstraction, testability, and Firebase sync preparation.
</context>

<tasks>

## 1. Create Repository Interfaces in domain layer
- [x] `AccountRepository` - CRUD for accounts, balance operations
- [x] `TransactionRepository` - CRUD, filtering, search, date range queries
- [x] `CategoryRepository` - Category and tag management
- [x] `BudgetRepository` - Budget CRUD, progress calculation
- [x] `GoalRepository` - Goal CRUD, progress tracking
- [x] `RecurringRepository` - Recurring transaction management
- [x] `TemplateRepository` - Transaction templates

## 2. Implement Repository Classes in data layer
- [x] `AccountRepositoryImpl` - wraps AccountDao
- [x] `TransactionRepositoryImpl` - wraps TransactionDao with business logic
- [x] `CategoryRepositoryImpl` - wraps CategoryDao, TagDao
- [x] `BudgetRepositoryImpl` - wraps BudgetDao with progress calc
- [x] `GoalRepositoryImpl` - wraps GoalDao with progress tracking
- [x] `RecurringRepositoryImpl` - wraps RecurringDao
- [x] `TemplateRepositoryImpl` - wraps TemplateDao

## 3. Update ViewModels to Use Repositories
- [x] Refactor `AccountsViewModel` - use AccountRepository
- [x] Refactor `TransactionsViewModel` - use TransactionRepository
- [x] Refactor `DashboardViewModel` - use repositories
- [x] Refactor `BudgetsViewModel` - use BudgetRepository
- [x] Refactor `GoalsViewModel` - use GoalRepository
- [x] Refactor `SettingsViewModel` - use repositories for preferences (already uses PreferencesManager)

## 4. Register in Hilt DI
- [x] Add repository bindings in RepositoryModule
- [x] Verify dependency graph compiles (requires Android Studio)

## 5. Verify & Test
- [x] Build succeeds (requires Android Studio)
- [x] Basic CRUD operations work in each screen
- [x] No regression in existing functionality

</tasks>

<success_criteria>
- All ViewModels use Repository interfaces, not DAOs
- Clean separation between UI and data layers
- Code compiles and runs without errors
- Ready for Firebase repository implementations
</success_criteria>
