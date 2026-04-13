<objective>
Implement Clean Architecture repository layer to replace direct DAO usage in ViewModels
</objective>

<context>
Audit revealed DAOs are used directly in ViewModels, violating Clean Architecture principles.
Repository pattern needed for data abstraction, testability, and Firebase sync preparation.
</context>

<tasks>

## 1. Create Repository Interfaces in domain layer
- [ ] `AccountRepository` - CRUD for accounts, balance operations
- [ ] `TransactionRepository` - CRUD, filtering, search, date range queries
- [ ] `CategoryRepository` - Category and tag management
- [ ] `BudgetRepository` - Budget CRUD, progress calculation
- [ ] `GoalRepository` - Goal CRUD, progress tracking
- [ ] `RecurringRepository` - Recurring transaction management
- [ ] `TemplateRepository` - Transaction templates

## 2. Implement Repository Classes in data layer
- [ ] `AccountRepositoryImpl` - wraps AccountDao
- [ ] `TransactionRepositoryImpl` - wraps TransactionDao with business logic
- [ ] `CategoryRepositoryImpl` - wraps CategoryDao, TagDao
- [ ] `BudgetRepositoryImpl` - wraps BudgetDao with progress calc
- [ ] `GoalRepositoryImpl` - wraps GoalDao with progress tracking
- [ ] `RecurringRepositoryImpl` - wraps RecurringDao
- [ ] `TemplateRepositoryImpl` - wraps TemplateDao

## 3. Update ViewModels to Use Repositories
- [ ] Refactor `AccountsViewModel` - use AccountRepository
- [ ] Refactor `TransactionsViewModel` - use TransactionRepository
- [ ] Refactor `DashboardViewModel` - use repositories
- [ ] Refactor `BudgetsViewModel` - use BudgetRepository
- [ ] Refactor `GoalsViewModel` - use GoalRepository
- [ ] Refactor `SettingsViewModel` - use repositories for preferences

## 4. Register in Hilt DI
- [ ] Add repository bindings in DatabaseModule
- [ ] Verify dependency graph compiles

## 5. Verify & Test
- [ ] Build succeeds
- [ ] Basic CRUD operations work in each screen
- [ ] No regression in existing functionality

</tasks>

<success_criteria>
- All ViewModels use Repository interfaces, not DAOs
- Clean separation between UI and data layers
- Code compiles and runs without errors
- Ready for Firebase repository implementations
</success_criteria>
