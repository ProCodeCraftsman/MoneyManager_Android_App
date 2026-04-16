# Architecture Research

**Domain:** Android Personal Finance App
**Researched:** 2026-04-14
**Confidence:** HIGH

## Integration Points

### Existing Architecture (Verified)

The app follows Clean Architecture with MVVM pattern:

- **Data Layer:** Room database with repositories
- **Domain Layer:** Use cases and business logic
- **UI Layer:** Jetpack Compose screens with ViewModels
- **DI:** Hilt for dependency injection

### Key Findings

1. **Sub-category architecture already exists** - CategoryEntity has `parentId`, CategoryDao has `getParentCategories()` and `getSubCategories()`, CategoriesScreen has expand/collapse UI
2. **Dashboard needs real category data** - DashboardViewModel currently uses hardcoded `getCategoryNameFromId()` mapping that must be replaced with actual CategoryRepository queries
3. **Default categories require new seeder component** - Need `DefaultCategoriesSeeder` that seeds Room on first launch (checked via PreferencesManager flag)
4. **Minimal database changes needed** - Existing schema supports everything; only new component is the seeder

## New Components

| Component | Purpose | Location |
|-----------|---------|----------|
| DefaultCategoriesSeeder | Seeds default categories on first launch | data/seeder |
| CategoryRepository enhancement | Support for archive/unarchive, custom categories | data/repository |
| DashboardViewModel update | Uses real category data instead of hardcoded | ui/dashboard |

## Data Flow Changes

1. **First Launch:** PreferencesManager checks flag → DefaultCategoriesSeeder seeds Room
2. **Category Access:** CategoriesViewModel → CategoryRepository (existing) + archive support
3. **Dashboard:** DashboardViewModel → TransactionRepository + CategoryRepository (real data)

## Build Order

1. **Phase 1:** DefaultCategoriesSeeder + PreferencesManager first-launch check (data layer)
2. **Phase 2:** CategoriesViewModel triggers seeding on init (UI ↔ data boundary)
3. **Phase 3:** DashboardViewModel replaces hardcoded category names with real CategoryRepository queries (integration fix)

## Open Questions

- Whether to add `color` field to CategoryEntity for persistent category colors (future scaling consideration)
- Whether default categories should include pre-defined sub-categories (e.g., Food → Restaurants, Groceries, Delivery)

---

*Architecture research for: Default Categories and Dashboard*
*Researched: 2026-04-14*