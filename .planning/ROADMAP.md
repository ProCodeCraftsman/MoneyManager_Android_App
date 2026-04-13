# MoneyManager - Roadmap

## Phase 1: Project Setup
- [x] Create Android Studio project with Kotlin + Jetpack Compose
- [x] Configure build.gradle dependencies
- [x] Set up project structure (Clean Architecture)
- [x] Create shell APK verification

## Phase 2: Data Layer
- [x] Define Room entities (Account, Transaction, Category, Budget, Goal, Recurring, Template, Tag)
- [x] Create DAOs and database
- [ ] Implement repositories ⚠️ **IN PROGRESS**
- [ ] Set up Firebase project for cloud backup ⚠️ **IN PROGRESS**
- [ ] Implement sync logic ⚠️ **IN PROGRESS**

## Phase 3: Core UI (Dashboard, Accounts, Transactions)
- [x] Dashboard screen with net worth, stats, transaction list
- [x] Accounts screen with cards, add/edit
- [x] Transactions screen with list, filters, add/edit form
- [ ] Pie chart ⚠️ **IN PROGRESS**
- [ ] Transfer modal ⚠️ **IN PROGRESS**

## Phase 4: Features
- [ ] Recurring transactions with auto-generate
- [x] Budgets with progress bars
- [x] Savings Goals with tracking
- [ ] Reports with charts ⚠️ **IN PROGRESS**
- [ ] Tags & Categories management
- [ ] Templates

## Phase 5: Settings & Security
- [x] Settings screen (dark mode, currency, data management)
- [ ] PIN lock screen ⚠️ **IN PROGRESS**
- [ ] Biometric authentication ⚠️ **IN PROGRESS**
- [x] Auto-lock timer

## Phase 6: Build & Publish
- [ ] JSON/CSV import/export
- [ ] Create Play Store listing
- [ ] Privacy Policy
- [ ] App icons and screenshots
- [ ] Upload and publish

## Critical Fixes (Active)
- [ ] Repository layer (Clean Architecture) - `.planning/phases/1-architecture-fix/`
- [ ] Firebase sync - `.planning/phases/2-firebase-sync/`
- [ ] Dashboard pie chart & transfer - `.planning/phases/3-dashboard-ui/`
- [ ] PIN/Biometric security - `.planning/phases/4-security/`
- [ ] Reports screen - `.planning/phases/5-reports/`
