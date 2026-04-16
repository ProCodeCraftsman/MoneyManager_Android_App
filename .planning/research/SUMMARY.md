# Project Research Summary

**Project:** Android Finance App — Default Categories & Dashboard
**Domain:** Personal Finance Application
**Researched:** April 14, 2026
**Confidence:** HIGH (Stack/Features), MEDIUM (Pitfalls), MISSING (Architecture)

## Executive Summary

This research synthesizes findings for building an Android Personal Finance App focused on Default Categories, Category Archive functionality, and Dashboard features. The project already has a solid foundation using Kotlin, Jetpack Compose, Room, Hilt, and Firebase Auth — all at stable, production-ready versions.

**Key recommendation:** Leverage existing stack with Room's self-referential pattern for hierarchical categories. Use soft-delete for archive functionality to preserve transaction history. Dashboard should implement granular state management to avoid full recomposition on every data change.

**Main risks to mitigate:**
1. Flat category schema without hierarchy support (will break subcategories)
2. Hard deletes breaking transaction history (must use soft-delete)
3. Dashboard performance issues from thread blocking and over-recomposition

The feature set aligns with market expectations: ~25 default categories with subcategory support, unified account balances, spending breakdown charts, and date range filtering. Auto-categorization rules should be deferred to v2+.

## Key Findings

### Recommended Stack

**Core technologies (already in project):**
- **Kotlin** (1.9.x) — Language, already validated
- **Jetpack Compose** (BOM 2024.12.01) — UI Framework
- **Room** (2.6.1) — Local database with self-referential foreign keys
- **Hilt** (2.52) — Dependency injection
- **MPAndroidChart** (v3.1.0) — Charts via VueView wrapper
- **Firebase Auth** (BOM 33.1.2) — Authentication
- **WorkManager** (2.9.1) — Background scheduling for reminders

**New additions:**
- **YCharts** (2.1.0+) — Alternative Compose-native chart library if MPAndroidChart integration proves problematic

**Key pattern — Category hierarchy:**
```kotlin
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String,
    val type: CategoryType, // INCOME or EXPENSE
    val parentId: String? = null, // Self-referential foreign key
    val isArchived: Boolean = false,
    val isDefault: Boolean = true
)
```

### Expected Features

**Must have (table stakes):**
- **Default Category Set** — 25 pre-built categories on first launch (Income, Housing, Food, Transport, Utilities, etc.)
- **Subcategory Hierarchy** — 2-3 levels max (e.g., "Groceries" > "Whole Foods")
- **Category Types** — Income vs Expense vs Transfer
- **Dashboard Overview** — Net worth + income vs expense summary
- **Account Balances Widget** — All accounts in one view
- **Spending by Category** — Visual breakdown with drill-down
- **Recent Transactions** — Last N transactions quick view
- **Date Range Picker** — Week/Month/Year selection

**Should have (competitive differentiators):**
- **Budget Progress Widget** — Visual progress bars per category (P2)
- **Goal Progress Widget** — Savings goals with projected dates (P2)
- **Actionable Insights** — Contextual comparisons vs. last month/budget (P2)

**Defer (v2+):**
- **Auto-Categorization Rules** — High complexity, requires rule engine
- **Customizable Widget Layout** — User reordering, low initial value
- **Cash Flow Forecast** — High complexity, uses existing Recurring data
- **Category Sharing/Import** — Niche feature

### Architecture Approach

**Components identified:**

1. **CategoryRepository** — CRUD with soft-delete, hierarchy queries, archive support
2. **DefaultCategoriesSeeder** — Seeds ~25 default categories on first launch, checked via PreferencesManager flag
3. **DashboardViewModel** — Aggregates accounts, transactions, date ranges; needs update to use real category data instead of hardcoded mapping
4. **TransactionRepository** — With category snapshot for historical accuracy (denormalization)

**Data flow:**
1. **First Launch:** PreferencesManager checks flag → DefaultCategoriesSeeder seeds Room
2. **Category Access:** CategoriesViewModel → CategoryRepository + archive support
3. **Dashboard:** DashboardViewModel → TransactionRepository + CategoryRepository (real data)

**Build order:**
1. DefaultCategoriesSeeder + PreferencesManager first-launch check (data layer)
2. CategoriesViewModel triggers seeding on init (UI ↔ data boundary)
3. DashboardViewModel replaces hardcoded category names with real CategoryRepository queries (integration fix)

### Critical Pitfalls

1. **Flat Category Schema** — Must use parentId from day one, or subcategories require breaking changes later. Use self-referential foreign key pattern.

2. **Hard Delete Breaking History** — Never use CASCADE DELETE on categories. Implement soft-delete with `isArchived` flag. Create "Uncategorized" fallback for orphaned transactions.

3. **Dashboard Full Recomposition** — Avoid single StateFlow for entire dashboard. Use derivedStateOf(), split into independent widget states, implement debounce (500ms).

4. **Floating-Point Currency Errors** — Store as INTEGER cents (100 = $1.00), never REAL/DOUBLE. Round at display time only.

5. **Dashboard Thread Blocking** — Always use Dispatchers.IO + produceOn(Dispatchers.Main). Pre-aggregate monthly summaries. Use skeleton loaders.

6. **Category Deletion Breaks Totals** — Denormalize category data into transaction table at creation time (store category_id, category_name, category_color).

## Implications for Roadmap

Based on research, suggested phase structure:

### Phase 1: Category Foundation
**Rationale:** All other features depend on category infrastructure. Must get schema right before seeding data.
**Delivers:**
- Hierarchical category schema with parentId
- Default category seeding (~25 categories)
- Subcategory CRUD
- Category types (Income/Expense/Transfer)
- Soft-delete / Archive functionality
**Avoids:** Pitfall #1 (flat schema), Pitfall #2 (hard delete)

### Phase 2: Dashboard Core
**Rationale:** Dashboard aggregates existing features. Needs proper async loading from the start.
**Delivers:**
- Account balances aggregation
- Income vs Expense summary
- Spending by category widget
- Recent transactions widget
- Date range selection
- Proper state management (derivedStateOf, granular updates)
**Avoids:** Pitfall #3 (recomposition), Pitfall #6 (thread blocking), Pitfall #4 (historical totals)

### Phase 3: Enhanced Features
**Rationale:** Adds competitive differentiation after core is proven.
**Delivers:**
- Budget progress widget
- Goal progress widget
- Net worth trend chart
- Actionable insights
**Avoids:** None specific — these add on existing foundation

### Phase 4: Advanced Features (v2+)
**Rationale:** Defer high-complexity features until product-market fit established.
**Delivers:**
- Auto-categorization rules
- Customizable dashboard widgets
- Cash flow forecast
- Category import/sharing

### Research Flags

**Needs deeper research:**
- **Phase 1 (Category Foundation):** Category ordering/display priority — how to handle manual reordering
- **Phase 2 (Dashboard Core):** Pre-aggregation strategy for monthly summaries — requires database design decisions

**Standard patterns (skip research-phase):**
- **Phase 1:** Room self-referential is well-documented pattern
- **Phase 2:** Compose state management follows standard patterns

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Technologies already in use, version-compatible |
| Features | HIGH | Clear competitor analysis, well-researched MVP |
| Architecture | HIGH | Verified existing schema supports subcategories, identified seeder as new component |
| Pitfalls | MEDIUM | Comprehensive pitfalls list, some inference on solutions |

**Overall confidence:** HIGH — All research areas complete

### Gaps to Address

1. **ARCHITECTURE.md missing** — Cannot assess component boundaries, data flow patterns, or architecture decisions. This is blocking for roadmap creation.

2. **Category display order** — Not covered in pitfalls or features. How do users reorder categories?

3. **Pre-aggregation strategy** — Dashboard performance section mentions it but no implementation detail.

4. **Test coverage strategy** — Not addressed in research. What's the testing approach?

## Sources

### Primary (HIGH confidence)
- Android Developers — Room Relationships documentation
- Android Developers — WorkManager documentation
- Room Releases — Stable version 2.8.4

### Secondary (MEDIUM confidence)
- YCharts GitHub — Compose-native chart library
- Space-O Technologies — Personal Finance App recommendations
- Maybe Finance (GitHub) — Open-source hierarchical category schema

### Tertiary (LOW confidence)
- Community blog posts on WorkManager patterns
- Medium articles on Dashboard recomposition

---

*Research completed: April 14, 2026*
*Ready for roadmap: YES*
