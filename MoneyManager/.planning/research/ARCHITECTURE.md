# Architecture Research

**Domain:** Android Finance App - Default Categories & Dashboard
**Researched:** 2026-04-14
**Confidence:** HIGH

## Existing Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      UI Layer (Compose)                      │
├─────────────────────────────────────────────────────────────┤
│  Screens: Dashboard, Categories, Transactions, Reports...   │
│  ViewModels: DashboardViewModel, CategoriesViewModel...     │
│  Components: PieChart, BudgetWidget, TimeFilterBar...       │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                   Domain Layer (Repository)                  │
├─────────────────────────────────────────────────────────────┤
│  CategoryRepository, TransactionRepository, AccountRepository
│  (Interfaces defining data access contracts)                │
└─────────────────────────────────────────────────────────────┘
                              ↓ ↑
┌─────────────────────────────────────────────────────────────┐
│                     Data Layer (Room + DI)                   │
├─────────────────────────────────────────────────────────────┤
│  DAOs: CategoryDao, TransactionDao, AccountDao...           │
│  Entities: CategoryEntity, TransactionEntity...            │
│  RepositoryImpls: CategoryRepositoryImpl...                  │
│  DI Modules: RepositoryModule, DatabaseModule...           │
└─────────────────────────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | Current Implementation | New Feature Impact |
|-----------|----------------|------------------------|---------------------|
| **CategoryEntity** | Category data model | Already has `parentId` for sub-categories | **No change needed** |
| **CategoryDao** | Database queries | Has `getParentCategories()`, `getSubCategories()` | **No change needed** |
| **CategoryRepository** | Category data access | Already has sub-category methods | **No change needed** |
| **CategoriesScreen** | Category UI | Already supports sub-category display | **Minor enhancements** |
| **DashboardViewModel** | Dashboard data aggregation | Uses hardcoded category names | **Requires update** |
| **MoneyManagerDatabase** | Room database | Includes all entities | **No change needed** |

## Integration Points

### 1. Categories with Sub-Categories

**Status:** ALREADY IMPLEMENTED - The foundation exists:
- `CategoryEntity.parentId: Long?` enables hierarchical categories
- `CategoryDao.getParentCategories()` and `getSubCategories()` queries exist
- `CategoriesScreen` already has expand/collapse UI for sub-categories

**What's NEW (to add):**
- Default category seeding on first app launch
- Category selection UI in transaction entry should show sub-categories

### 2. Dashboard Feature Integration

**Current State:**
- DashboardViewModel uses hardcoded `getCategoryNameFromId()` mapping
- Category colors are hardcoded in `categoryColors` map
- Doesn't query Room for category data

**What's NEW (to add):**
- Query actual category names from CategoryRepository
- Replace hardcoded color map with category-based colors or Room-stored colors
- Add category drilldown that queries real data

### 3. Default Category Seeding

**What's NEW (to add):**
- New `DefaultCategoriesSeeder` component
- Runs on first launch (via PreferencesManager flag check)
- Inserts pre-defined category hierarchy into Room

## Recommended Project Structure

```
src/main/java/com/moneymanager/
├── app/
│   ├── ui/screens/
│   │   ├── DashboardScreen.kt       # Existing
│   │   ├── DashboardViewModel.kt    # Existing - needs updates
│   │   ├── CategoriesScreen.kt     # Existing - minor updates
│   │   └── CategoriesViewModel.kt  # Existing
├── data/
│   ├── repository/
│   │   ├── CategoryRepository.kt    # Existing
│   │   ├── CategoryRepositoryImpl.kt
│   │   └── DefaultCategoriesSeeder.kt  # NEW
│   ├── dao/
│   │   ├── CategoryDao.kt          # Existing
│   │   └── ...
│   └── entity/
│       ├── CategoryEntity.kt        # Existing
│       └── ...
├── di/
│   ├── RepositoryModule.kt          # Existing
│   └── DatabaseModule.kt           # Existing
└── domain/repository/
    └── CategoryRepository.kt       # Existing
```

### Structure Rationale

- **DefaultCategoriesSeeder.kt:** Placed in `data/repository/` as it's a data initialization concern that uses Repository layer
- **DashboardViewModel updates:** Stay in existing file, just inject CategoryRepository and use real data

## Data Flow

### Default Categories Seeding Flow

```
App First Launch
       ↓
Check PreferencesManager.isFirstLaunch()
       ↓ (true)
DefaultCategoriesSeeder.seedDefaultCategories()
       ↓
CategoryRepository.insertCategory() x N
       ↓
Set PreferencesManager.isFirstLaunch(false)
       ↓
App continues normal operation
```

### Dashboard Category Display Flow (New)

```
DashboardViewModel init
       ↓
CategoryRepository.getAllCategories()
       ↓ (Flow)
Combine with transactions
       ↓
DashboardUiState with real category names
       ↓
DashboardScreen displays actual data
```

## New vs Modified Components

| Component | Type | Action | Build Order |
|-----------|------|--------|-------------|
| `DefaultCategoriesSeeder` | **NEW** | Create | 1 |
| `PreferencesManager` | **MODIFIED** | Add `isFirstLaunch()` | 1 (concurrent) |
| `CategoryRepository` | **EXISTING** | No changes needed | N/A |
| `CategoryDao` | **EXISTING** | No changes needed | N/A |
| `CategoryEntity` | **EXISTING** | No changes needed | N/A |
| `CategoriesViewModel` | **EXISTING** | Minor enhancement (seed on first launch) | 2 |
| `DashboardViewModel` | **MODIFIED** | Use CategoryRepository for real data | 3 |
| `DashboardScreen` | **EXISTING** | Already displays correctly | 3 (concurrent) |

## Build Order

1. **Create DefaultCategoriesSeeder** - Core data seeding
2. **Update PreferencesManager** - Add first launch check
3. **Update CategoriesViewModel** - Trigger seeding on init
4. **Update DashboardViewModel** - Query real category data
5. **Update DashboardScreen** (if needed) - Display improvements

## Anti-Patterns

### Anti-Pattern 1: Hardcoding Category IDs in ViewModels

**What people do:** Using hardcoded mappings like `getCategoryNameFromId()` in DashboardViewModel

**Why it's wrong:** 
- Breaks when categories are added/modified
- Doesn't reflect user's custom categories
- Requires code changes to update category names

**Do this instead:** Query actual categories from CategoryRepository and join with transactions

### Anti-Pattern 2: Duplicating Category Data

**What people do:** Creating separate "default categories" list separate from Room

**Why it's wrong:** 
- Two sources of truth for categories
- Sync issues between defaults and Room data
- Harder to maintain

**Do this instead:** Seed Room with defaults once, then let Room be the single source of truth

### Anti-Pattern 3: Seeding on Every Launch

**What people do:** Inserting default categories on every app start

**Why it's wrong:** 
- Wastes resources
- Creates duplicates (unless using ON CONFLICT REPLACE, which overwrites user changes)
- Poor UX

**Do this instead:** Check first launch flag, seed only once, respect user's custom categories

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 0-1k users | Current architecture is sufficient |
| 1k-100k users | Add category color persistence to Room; consider caching Flow |
| 100k+ users | Add pagination to category queries; consider offline-first sync |

### Scaling Priorities

1. **First consideration:** Add `color` field to CategoryEntity for persistent colors (not hardcoded)
2. **Second consideration:** Add category icon persistence beyond emoji string
3. **Third consideration:** Consider Firebase Cloud Firestore sync for cross-device

## Integration with Firebase Auth

**Note:** The app uses Firebase Auth but data is local-first with Room. Default categories should:
- Be seeded locally only
- NOT sync to Firebase (user-specific data)
- Respect user's modifications after seeding

## Sources

- Code analysis of existing project structure
- Room database patterns for Android
- Clean Architecture with MVVM on Android

---

*Architecture research for: Default Categories and Dashboard enhancement*
*Researched: 2026-04-14*
