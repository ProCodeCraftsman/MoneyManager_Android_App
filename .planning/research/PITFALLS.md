# Pitfalls Research

**Domain:** Android Personal Finance App — Default Categories, Archive, Dashboard
**Researched:** 2026-04-14
**Confidence:** MEDIUM

## Critical Pitfalls

### Pitfall 1: Flat Category Schema - No Room for Hierarchy

**What goes wrong:**
Database schema only supports flat categories (single-level), making it impossible to add subcategories later without breaking changes or complex migrations. Transactions referencing categories break when attempting to restructure the hierarchy.

**Why it happens:**
Developers underestimate the complexity of hierarchical data and opt for simple "category_name" TEXT columns or single-level foreign key relationships. Initial MVP thinking: "We'll just have categories like Food, Transport, Utilities."

**How to avoid:**
- Design schema with recursive parent_id foreign key from day one: `parent_category_id REFERENCES categories(id)`
- Use adjacency list pattern (parent_id) with CTEs for querying, or materialised path for performance
- Plan for at least 3 levels of depth initially (Category > Subcategory > Merchant)

**Warning signs:**
- Any category design using only enum or single-level reference
- Discussions avoid "subcategories" or treat them as future feature
- No category display order field in schema

**Phase to address:**
Categories phase — schema must support hierarchy before data seeding

---

### Pitfall 2: Hard Delete of Categories With Existing Transactions

**What goes wrong:**
When deleting a category that has existing transaction references, the app either cascades delete (losing transaction history) or throws foreign key violation (app crash). No graceful handling leads to data integrity issues or user frustration.

**Why it happens:**
Soft delete not implemented from the start. Developers treat delete as permanent, but finance apps require audit trails. Transactions should never lose their original categorization.

**How to avoid:**
- Implement soft delete with `is_deleted BOOLEAN` or `deleted_at TIMESTAMP` on all data tables
- Create "Uncategorized" fallback category for orphaned transactions
- Add "Archive" category for soft-deleted categories that preserves transaction links
- Use database-level ON DELETE SET DEFAULT to fallback category

**Warning signs:**
- No discussion of archive/un-delete workflow
- Foreign key constraints using CASCADE DELETE
- Transaction table lacks soft-delete column

**Phase to address:**
Archive functionality phase — must implement soft-delete before category management

---

### Pitfall 3: Dashboard Full Recomposition on Every Data Change

**What goes wrong:**
Dashboard using simple StateFlow/LiveData triggers full UI recomposition every time ANY data changes. Charts flicker, KPI cards update unnecessarily, scroll position loses on background data refresh.

**Why it happens:**
Single mutable state object drives entire dashboard. No granular state management. Every transaction change triggers recompose of spending charts, balance displays, and all KPI cards.

**How to avoid:**
- Use derivedStateOf() for expensive calculations that shouldn't recompute on minor changes
- Split dashboard into independent widget states — each KPI card subscribes only to its needed data
- Implement debounce for frequent data updates (500ms for transaction additions)
- Use remember() with stable keys to preserve scroll position

**Warning signs:**
- DashboardViewModel has single StateFlow<TotalDashboardState>
- No mention of granular updates or derived states
- Performance testing not done on low-end devices

**Phase to address:**
Dashboard phase — requires stable state architecture before building widgets

---

### Pitfall 4: Category Deletion Breaks Transaction Totals

**What goes wrong:**
When user deletes or merges categories, historical totals calculated from transaction JOINs become inaccurate. Reports show different totals than the sum of actual transactions because deleted category transactions are excluded.

**Why it happens:**
Dashboard queries use current category table instead of transaction-time category snapshot. No audit trail of what category a transaction belonged to at creation time.

**How to avoid:**
- Denormalize category data into transaction table at creation: store category_id, category_name, category_color at transaction time
- Or use event sourcing: transactions table stores category snapshot, separate category_audit table tracks changes
- Build totals as computed/denormalized fields updated via triggers or async workers
- Never rely on JOIN for historical reports — pre-aggregate monthly totals

**Warning signs:**
- Reports calculate sums by JOINing current category table
- No discussion of historical accuracy vs current categorization
- Dashboard queries transactions directly without caching

**Phase to address:**
Dashboard phase — pre-aggregation strategy required for financial accuracy

---

### Pitfall 5: Floating-Point Currency Storage Causing Rounding Errors

**What goes wrong:**
Amount stored as DOUBLE or FLOAT leads to rounding errors (e.g., $99.9999999999 instead of $100). Over time, totals diverge from actual transaction sums. Users notice "missing pennies."

**Why it happens:**
Default to REAL/FLOAT for monetary values because "it's just money." SQLite has no DECIMAL type, so developers choose simpler option.

**How to avoid:**
- Store currency values as INTEGER cents (BigInteger equivalent): 100 cents = $1.00
- Or store as TEXT that parses to BigDecimal
- Apply rounding at display time only (round to 2 decimal places for display)
- Never do arithmetic on display-formatted strings
- For multi-currency: store currency_code separately, convert at presentation layer

**Warning signs:**
- Any schema with REAL/DOUBLE for amount columns
- No precision discussion in data model review
- Calculations done in UI/code layer before database

**Phase to address:**
Database schema phase — precision-first storage before any transaction features

---

### Pitfall 6: Dashboard Blocking Main Thread with Heavy Queries

**What goes wrong:**
Dashboard loads all transaction history synchronously. On slow devices, UI freezes for seconds. ANR (Application Not Responding) warnings in Play Console.

**Why it happens:**
ViewModel loads data in init block or onStart(), runs on Dispatchers.Main by default. No background loading. Complex queries with JOINs blocking UI thread.

**How to avoid:**
- Always load dashboard data with Dispatchers.IO + produceOn(Dispatchers.Main)
- Use Room with @Transaction annotation for complex queries
- Pre-aggregate data daily/weekly into summary tables
- Implement skeleton loaders and use cached data while refreshing

**Warning signs:**
- Any database query without explicit coroutine dispatcher
- init block or LaunchedEffect contains database calls
- Play Console shows ANR on Dashboard screen

**Phase to address:**
Dashboard phase — async-first loading before performance testing

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Use category ID only in transactions | Simpler schema | Cannot rename categories without updates | Never — breaks historical accuracy |
| Delete categories as CASCADE | Simpler delete logic | Lost transaction history, audit failures | Never for finance apps |
| One category type enum | Easy dropdown | Cannot extend to custom categories | Only in throwaway MVP |
| Calculate totals on-demand | No aggregation maintenance | Slow dashboard on large data | Never past 1000 transactions |
| Load all transactions to dashboard | Simple pagination | Memory issues, slow load | When caching pre-aggregated data |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Room Database | Not using @Transaction for multi-table reads | Add @Transaction to all query methods reading related tables |
| Chart Library | Not releasing chart resources on detach | Implement LifecycleObserver to release on ViewModel cleared |
| DataStore | Using synchronous getPreferences() | Use DataStore.data Flow, never blocking reads |
| Main Thread | Running analytics queries on Main | Move all database work to IO dispatcher, use flow.produceOn |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Category query without index | 500ms+ category list load | Add index on (user_id, parent_id, is_deleted) | 50+ categories |
| Dashboard full scan | 3+ seconds to load | Pre-aggregate monthly summaries | 5000+ transactions |
| Chart image decoding | OOM on large transactions | Downsample images, use Coil/Glide | 100+ transactions with merchant logos |
| LiveData observation chain | Memory leak, stale data | Explicitly remove observers in onCleared | Long dashboard sessions |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Local storage without encryption | Device compromise exposes financial data | Use EncryptedSharedPreferences for sensitive values |
| No biometric lock | Unauthorized access to finances | Implement BiometricPrompt on app resume |
| Export includes sensitive data | Data leakage on cloud backup | Exclude sensitive fields from backup/restore |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| Category picker flat list | Hard to find category in 50+ list | Grouped by parent, searchable, recent picks |
| No category icons | Hard to visually identify | Color + Icon per category, customizable |
| Dashboard shows all time | No period context | Default to "This Month", allow custom range |
| No spending context | Users don't know if $500 is good/bad | Show vs budget, vs last month, trend arrow |
| Merge categories loses history | Can't reconcile past months | Show "Merged from X" in transaction details |

## "Looks Done But Isn't" Checklist

- [ ] **Categories:** Often missing category reassignment flow when deleting categories — verify orphaned transactions handled
- [ ] **Subcategories:** Often missing display order management — verify manual reordering works
- [ ] **Archive:** Often missing archive list UI/filtering — verify archived items can be found and restored
- [ ] **Dashboard:** Often missing empty state — verify shows helpful message when no transactions
- [ ] **Dashboard:** Often missing loading state — verify skeleton loaders work
- [ ] **Currency:** Often missing edge case rounding — verify 0.01 + 0.02 displays correctly

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Lost category transactions | HIGH | Requires migration script to restore from backup + audit log |
| Hard delete cascade | HIGH | Need backup before delete — otherwise manual transaction edits |
| Dashboard ANR | LOW | Add background loading + skeleton states, hotfix release |
| Floating point errors | MEDIUM | New calculation layer, recalculate all totals from transactions |
| Full recomposition jank | LOW | Refactor to granular states, hotfix release |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Flat category schema | Categories | Schema review with parent_id field |
| Hard delete breakage | Archive/Soft Delete | Delete flow test with existing transactions |
| Recomposition jank | Dashboard | Device profiling on low-end device |
| Historical totals mismatch | Dashboard | Verify totals = sum(transactions) |
| Currency precision | Database Schema | Schema uses INTEGER cents |
| Thread blocking | Dashboard | ANR console check |
| Category orphans | Categories | Delete category with transactions test |

## Sources

- Space-O Technologies: "Build Personal Finance App" (2025) — category structure recommendations
- Maybe Finance (GitHub): Open-source schema with hierarchical categories, parent_id pattern
- Budget Bee (Mintlify): Category hierarchy planning, RLS patterns
- EzBookkeeping: Multi-level category model with type classification
- Android Performance articles (Medium, 2025-2026): Dashboard recomposition pitfalls
- Groovy Web: Fintech app compliance and security mistakes
- SQLite financial app mistakes (MoldStud, 2025): Normalization, data types
- Android Vitals: ANR definitions and thresholds

---

*Pitfalls research for: Android Finance App — Categories, Archive, Dashboard*
*Researched: 2026-04-14*