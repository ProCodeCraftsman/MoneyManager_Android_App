# Features Research — Insights Dashboard

**Domain:** Financial Insights / Summary Screens in Personal Finance Mobile App
**Researched:** 2026-04-28
**Milestone:** v2.2 — 3-screen swipeable Insights Dashboard (STATUS, RISKS, TRENDS)
**Confidence:** MEDIUM-HIGH (codebase analysis + UX principles from training data; web search unavailable)

---

## Screen 1: STATUS

### Table Stakes

Behaviors users absolutely expect. Their absence makes the screen feel broken or incomplete.

| Feature | Why Expected | Complexity | Data Dependency |
|---------|--------------|------------|-----------------|
| Hero number (Net Position) at the top, visually dominant | Users open a finance app to answer "Am I ahead or behind this month?" — the hero number must be the first thing scanned, not buried | Low | `SUM(income) - SUM(expense)` for current month, filter `isSplitChild = false` |
| Positive/negative color on hero number | Green for positive, red for negative is a universal finance convention — users scan color before reading the number itself | Low | Sign of net position maps to `MaterialTheme.colorScheme.primary` vs `error` |
| Current month label visibly adjacent to the hero | Without the time label, users cannot tell if the number is all-time, YTD, or monthly — they will not trust it | Low | `SimpleDateFormat("MMMM yyyy", Locale.getDefault())` on current calendar |
| Four supporting figures: Income, Expense, Savings, Net Cash Flow | Users need to see how the hero was computed — a single number without breakdown is not actionable | Low | `type IN ('income', 'expense', 'savings')`, same month boundary as hero |
| All figures share the exact same time boundary | If one card shows MTD and another shows YTD, experienced users notice immediately and lose trust in all numbers | Low | Pass same `startDate`/`endDate` pair to every aggregate |
| Currency symbol on all amounts | App supports multi-currency (INR default, 10+ options) — users with non-default currency must see the symbol | Low | `PreferencesManager.currency` already flows through `DashboardUiState.currency` |
| Deterministic empty state when no current-month transactions exist | A screen showing "0 / 0 / 0 / 0" looks like a crash; an explicit "No activity for April" message communicates intent | Low | Guard: check `currentMonthTxs.isEmpty()` before rendering figures |

### Differentiators

Features that are not universally expected but meaningfully improve the experience.

| Feature | Value Proposition | Complexity | Data Dependency |
|---------|-------------------|------------|-----------------|
| Month-to-month delta below each supporting figure | "Expenses: 8,400 (+12% vs last month)" contextualizes whether the number is normal or alarming | Low | `PeriodSummary` pattern already in `DashboardViewModel.calculatePeriodSummary()` — direct reuse |
| Savings rate (savings / income as %) | Personal finance literacy centers on savings rate as a key KPI; users who track this at all care about it significantly | Low | `(totalSavings / totalIncome * 100)` — guard divide-by-zero when income = 0 |
| Net Cash Flow distinguished from Net Position | Net Cash Flow = `income - expense - savings`; Net Position = `income - expense`. Clarifying these are different helps financially literate users | Low-Medium | Requires explicit formula decision and labeling in UI |

### Anti-Features

Things that look useful but confuse or mislead users. Do not build these.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Tappable figures that open an edit form | STATUS is a derived read-only summary; any figure being "editable" violates the mental model that insights are computed, not entered | If drill-down is desired, navigate to TransactionsScreen filtered by type |
| Progress bars toward budget targets | Project constraint is explicit: source of truth is transactions only, no budgets dependency. Adding budget bars means STATUS silently breaks when no budgets exist | Keep STATUS purely transaction-derived; Budget view stays on main Dashboard |
| All-time or YTD figures mixed with current-month figures | Extremely common finance app mistake, consistently cited in user reviews as confusing ("I don't know what period these numbers are for") | Lock all STATUS figures strictly to current month; show month label prominently |
| Animated number count-up on screen load | Looks impressive in demos; in daily use it delays cognitive recognition — users who check their balance daily already have an approximate mental model, animation interrupts it | Show final value immediately; use shimmer skeleton while ViewModel is loading |
| Net Worth as the hero number | Net Worth = sum of all account balances (all-time accumulated) — this is already displayed on the main Dashboard. The STATUS screen's purpose is the current-month flow story | Use Net Position (monthly income - expense) as hero |
| Forecasted or projected figures | Any projection requires assumptions the app cannot verify — projections on a STATUS screen get treated as fact by non-expert users | Show only actuals |

---

## Screen 2: RISKS

### Table Stakes

| Feature | Why Expected | Complexity | Data Dependency |
|---------|--------------|------------|-----------------|
| Each alert rendered as: icon + title + 1-2 line explanation | Users must understand what triggered the alert without leaving the screen — "Overspending detected" without context causes anxiety, not action | Low | Alert struct needs: `icon`, `title: String`, `body: String` with embedded computed values |
| Negative alerts (problems) ordered before positive alerts | Users opening a Risks screen expect bad news first — showing "Great savings!" before "You're overspent" feels evasive and erodes trust | Low | Sort by `severity: Enum(WARNING, INFO)` descending |
| Explicit "No issues detected" state when zero alerts fire | A blank Risks screen looks like an error or unimplemented feature — users need confirmation the system evaluated data and found nothing concerning | Low | Guard: `if (alerts.isEmpty()) show PositiveEmptyState` with reassuring text |
| Maximum 3 alerts displayed | Beyond 3 alerts on mobile, users enter triage fatigue and stop reading — the cap forces the rule engine to prioritize ruthlessly | Low | `alerts.sortedByDescending { it.severity }.take(3)` |
| Fully deterministic output — same data produces same alerts | Users who navigate away and return to RISKS must see the same alerts — any non-determinism (time-of-day sensitivity, random tie-breaking) destroys trust | Low | All rules compare only monthly aggregates; no randomness |

### Differentiators

| Feature | Value Proposition | Complexity | Data Dependency |
|---------|-------------------|------------|-----------------|
| Alert body contains the triggering number | "Spending is up 34% vs last month (was 8,200, now 11,000)" is far more actionable than "Spending spike detected" | Low | Embed computed values into message string at rule evaluation time, not in UI layer |
| Severity-tinted card background | Warm red/amber card tint communicates urgency before the user reads text — reduces cognitive load for quick scanning | Low | Map `WARNING` to `MaterialTheme.colorScheme.errorContainer`, `INFO` to `tertiaryContainer` |
| Single positive "savings improvement" alert when no negatives exist | Positive reinforcement when savings improved is the one "good news" case worth surfacing — all other positive states should be silent | Low | Rule: `currentSavings > prevSavings AND noNegativeAlerts` |

### Anti-Features

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Swipeable-to-dismiss on alert cards | Users expect rule-derived alerts to be persistent status indicators, not notifications. A dismissed alert reappears on next open because the data hasn't changed — this confuses users who think they "handled" it | Alerts are always derived fresh; no dismiss state; no persistence needed |
| Push notifications for risk alerts | Requires background WorkManager, notification permission, scheduling logic, and battery management — substantial complexity for uncertain value in a local app | Surface alerts only when the user navigates to Insights |
| "Fix this" / CTA buttons on alert cards | Implies the app knows the correct remediation action — finance apps that do this are frequently wrong, and users who follow bad suggestions lose trust permanently | Show information only; let the user decide |
| More than 5 rule types | Each additional rule increases false positive rate and maintenance surface — 5 well-calibrated rules outperform 12 mediocre ones | Implement the 5 defined rules exactly; do not add speculative rules in this milestone |
| Rules that depend on categories, budgets, or goals | `PROJECT.md` explicitly constrains the source of truth to transaction records only — rules requiring budget thresholds break silently when no budgets exist | All rules use only `type`, `amount`, `date` from `TransactionEntity` |

**The 5 rules mapped to available transaction data:**

| Rule | Trigger Condition | Alert Severity | Data Query |
|------|-------------------|----------------|------------|
| Overspending | `currentExpense > currentIncome` | WARNING | `SUM(type='expense') > SUM(type='income')`, current month |
| Spending spike | `currentExpense > prevExpense * 1.20` | WARNING | Compare same month sums, current vs. previous calendar month |
| Negative net position | `currentIncome - currentExpense < 0` | WARNING | Conceptually overlaps with overspending — implement as one rule, not two separate alerts |
| High borrowing | `SUM(type='borrow', current month) > currentIncome * 0.50` | WARNING | `borrow` is in `TransactionEntity.VALID_TYPES`; guard: skip rule if `currentIncome = 0` |
| Savings improvement | `currentSavings > prevSavings` AND no WARNING-level alerts active | INFO | `SUM(type='savings')` both months |

Implementation note on "overspending" vs "negative net position": these are the same signal expressed differently. Emit only one alert for this condition to avoid alert duplication within the 3-alert cap.

---

## Screen 3: TRENDS

### Table Stakes

| Feature | Why Expected | Complexity | Data Dependency |
|---------|--------------|------------|-----------------|
| Expense % change vs previous month with directional indicator (arrow up/down) | This is the minimum viable trend signal — users who open a Trends screen expect at least one comparison value | Low | `(currentExpense - prevExpense) / prevExpense * 100`; guard `prevExpense = 0` (show "N/A" or "first month") |
| Dominant activity: type name + total amount | Shows which transaction type drove the most monetary volume this month — one clear answer, not a ranked list | Low | Group current-month transactions by type, sum each group, find max; exclude `transfer`, `repay`, `receive` from competition |
| Chart must handle sparse data without crashing | Early-month (e.g., day 3) means 27+ days have zero values — the chart library must accept zero-value data points and single-data-point months without exceptions | Low-Medium | Ensure `MPAndroidChart LineChart` receives a complete 1-to-N day series where missing days are zero-valued `Entry` objects, not omitted |
| Month label as chart title or subtitle | Users need to know which month's data is displayed on the chart | Low | Same `SimpleDateFormat("MMMM yyyy")` as STATUS screen |

### Chart Type Decision

**Recommendation: Daily line chart for the within-month view.**

**Confidence: MEDIUM** (UX principle reasoning, not verified against live user research).

Rationale:

- The goal of TRENDS Screen 3 is to show the pattern of spending across a 28-31 day period. A line chart over 30 data points on a mobile screen (~360dp wide) gives each point approximately 12dp of horizontal space — readable without crowding.
- Weekly bar charts are better suited for multi-month comparison views (which is not this scope). Monthly bars reduce to only 2 bars (current vs. previous) — a pair of bars communicates no more than a single percentage number does, which is already shown above the chart.
- Dual series (income line + expense line on the same axes) gives the user an instant read of whether income crossed below expense at any point in the month — this is a high-signal visual.
- Granularity: use a continuous 1-to-N calendar day X-axis. Days with zero transactions plot as `Entry(day, 0.0f)`. Do NOT skip days with no activity — gaps in a line chart imply missing data to users, not zero activity.

**MPAndroidChart implementation specifics** (already in the stack per `PROJECT.md`):

- `LineChart` with two `LineDataSet` instances (income, expense)
- X-axis: integer day index 1 to `daysInMonth` using `IndexAxisValueFormatter`
- Y-axis: formatted currency values using `ValueFormatter`
- Filled area under each line (`setDrawFilled(true)`) aids readability on dark/light themes
- `OnChartValueSelectedListener` for tap-to-show-value marker

### Dominant Activity Display

**What "dominant activity" means:** the transaction type with the highest total amount in the current month, scoped to economically meaningful types.

Exclude from competition: `transfer` (internal movement, not a gain or loss), `repay` (settlement, not new spending), `receive` (settlement, not new income). Include: `income`, `expense`, `savings`, `lend`, `borrow`.

**Display anatomy:**

```
Dominant this month
[Icon]  Expenses              8,400
        42% of total activity
```

- Row 1: section label "Dominant this month"
- Row 2: type icon (reuse existing type-to-icon mapping from app), human-readable type label, formatted total
- Row 3 (optional): percentage of total across all included types

**Complexity:** Low. In-memory: `currentMonthTxs.groupBy { it.type }.mapValues { it.value.sumOf { tx -> tx.amount } }.maxByOrNull { it.value }`.

### Differentiators

| Feature | Value Proposition | Complexity | Data Dependency |
|---------|-------------------|------------|-----------------|
| Dual-line chart (income + expense on same axes) | Shows the crossover moment — when expense line crosses above income line — far more powerfully than two separate numbers | Low-Medium | Two `Entry` lists, same X-axis, in single `LineData` |
| Chart value marker on tap | Standard chart interaction users expect — tapping a data point shows that day's value | Low | `OnChartValueSelectedListener` + custom `MarkerView` in MPAndroidChart |
| Running cumulative net line as third series | Shows whether the user is accumulating or depleting through the month — highly predictive of end-of-month position | Medium | Cumulative sum of `(income - expense)` per day, requires daily bucketing pass |

### Anti-Features

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Pie chart on TRENDS | Pie charts show composition at a single point in time — they answer "what category did I spend most on?" not "how did spending change over time?" The Dashboard already has a pie/donut chart for category breakdown | Keep daily line chart; the pie is already on Dashboard's Expense tab |
| Percentage change expressed as decimal (0.34 instead of 34%) | This is a common implementation bug — `PeriodSummary.percentChange` is stored as a raw ratio in the existing ViewModel; always multiply by 100 and format before display | `String.format("%+.0f%%", percentChange)` — note the `+` flag for explicit sign |
| "Average daily spend" as a headline metric | Sounds analytical but is not actionable — users cannot meaningfully change their "average daily spend" as a lever; what they can act on is the direction (up/down vs last month) | Show directional percent change and the chart; skip the average |
| Stacked bar chart with categories | Would require category data dependency, violating the project constraint of transaction-only source of truth. Also, stacked bars at daily granularity over 30 days are illegible on a 360dp mobile screen | Line chart on raw type aggregates |
| Month navigation (prev/next month) on TRENDS | Trends is defined as current month; adding navigation multiplies data query complexity (both current AND a reference previous month are always needed regardless of which month the user navigates to) and adds scope without clear v1 value | Lock to current month; defer multi-month navigation to a future milestone |
| All-zero chart as substitute for empty state | An all-zero line chart looks like a broken chart, not an intentional design choice | When no current-month transactions exist, hide the chart and show an explicit empty state message |

---

## UX Patterns

### Swipe Navigation Between the Three Screens

**Pattern: `HorizontalPager` + page indicator dots**

- Three pages in order: STATUS (index 0), RISKS (index 1), TRENDS (index 2)
- Swipe gesture is the primary navigation — no tap required
- Page indicator (3 dots) positioned below the pager content, above the bottom nav bar
- Optional: `TabRow` or `ScrollableTabRow` as a secondary navigation row above the pager — this adds a title to each page and enables tap-to-jump

**Implementation:** Use `androidx.compose.foundation.pager.HorizontalPager` (stable since Compose 1.4 — no Accompanist dependency needed) + `PagerState`. `rememberPagerState(initialPage = 0)`.

### Navigation Entry Point

Insights should be a distinct bottom navigation item, not a sub-tab embedded within Dashboard. Rationale:

1. It has a distinct ViewModel with its own data scope (always current month, no filters)
2. The 3-screen sub-structure requires its own internal pager navigation
3. Embedding it under Dashboard creates a nested navigation hierarchy that is confusing to users

Suggested icon: `Icons.Default.Insights` (Material Design) or `Icons.Default.TrendingUp`.

### Empty States — Per Screen

Each screen fails independently and must have its own empty state.

| Screen | Condition | Message |
|--------|-----------|---------|
| STATUS | No transactions in current month | "No activity recorded for [Month]. Add your first transaction to see your financial status." |
| RISKS | No transactions in current month (no data to evaluate rules) | "Nothing to flag yet. Add transactions to get personalized insights." |
| TRENDS | No transactions in current month | "No data available for [Month] yet." Chart is hidden — do not show an all-zero chart |

Do not show zero-value charts as a substitute for an empty state. A chart showing a flat zero line looks like a rendering bug.

### Loading State

Use shimmer/skeleton placeholders while the ViewModel computes. The screen layout is fixed and known in advance, so skeleton shapes matching the actual layout are straightforward to define. Reuse the `isLoading` flag pattern from `DashboardUiState` — the Insights ViewModel should expose an identical `isLoading: Boolean` in its own UI state.

Do NOT use a full-screen spinner — it hides the layout structure and makes the transition jarring.

### Time Scope: Fixed Current Month, No Filter Controls

The Insights Dashboard screens are deliberately locked to the current month. Do not add a `TimeFilterBar` to these screens. Rationale:

- The main Dashboard already provides filtered historical views across Day / Week / Month / Year / Custom
- Insights answers "right now, this month, how am I doing?" — the fixed scope is a feature, not a limitation
- Adding filter controls would make Insights a duplicate of Dashboard with fewer features and no differentiating purpose

---

## Data Model Compatibility

All Insights calculations are achievable from the existing `TransactionDao` and `TransactionEntity` without new DAO methods.

**Reusable from existing code:**

| Existing Asset | How Insights Uses It |
|----------------|----------------------|
| `calculatePeriodSummary(current, prev)` in `DashboardViewModel` | Extract to a shared utility or duplicate into InsightsViewModel — computes `PeriodSummary(amount, prevAmount, percentChange)` |
| `getDateRangeForFilter(TimeFilter.MONTH, Calendar.getInstance(), null, null)` | Call this to get current month `startDate`/`endDate` bounds |
| Previous month bounds pattern in `overviewFlow` | Clone current calendar, `prevBaseDate.add(Calendar.MONTH, -1)` — identical pattern |
| `TransactionEntity.VALID_TYPES` | Canonical type list for dominant activity grouping |
| `PeriodSummary` data class | Reuse directly for TRENDS expense % change display |
| `getTransactionsByDateRange(start, end)` DAO method | Primary query for both current and previous month — all Insights derives from two calls to this |
| `isSplitChild` filtering pattern | All Insights aggregates must filter `!isSplitChild` — same as existing `overviewFlow` |

**What requires new computation (no existing query needed, in-memory only):**

| New Computation | Where | Notes |
|----------------|-------|-------|
| Daily bucketing of transactions for line chart | InsightsViewModel | `groupBy { Calendar.DAY_OF_MONTH from tx.date }`, fill missing days with 0.0 |
| Rule evaluation logic (5 rules) | InsightsViewModel or dedicated `RiskEvaluator` class | Pure function: takes two sets of aggregates (current month, previous month), returns `List<RiskAlert>` |
| Dominant activity calculation | InsightsViewModel | In-memory groupBy + maxByOrNull on filtered type set |
| Savings rate calculation | InsightsViewModel | `totalSavings / totalIncome * 100`; guard divide-by-zero |

**No new Room DAO queries are required for this milestone.** All data comes from `getTransactionsByDateRange` called twice (once for current month, once for previous month), with all aggregation done in ViewModel memory. This keeps the implementation self-contained.

---

## Sources

- Codebase analysis: `TransactionEntity.kt`, `DashboardViewModel.kt`, `TransactionDao.kt`, `PROJECT.md`, `MoneyManagerNavHost.kt` — HIGH confidence (direct file inspection)
- UX pattern reasoning: Material Design 3 guidelines, Compose `HorizontalPager` API (Compose 1.4+), MPAndroidChart `LineChart` patterns — MEDIUM confidence (training data, not verified against live docs in this session)
- Finance app UX conventions (hero number, color coding, alert UX): MEDIUM confidence (training data, widely consistent across multiple sources in training corpus)
- Chart type recommendation (daily line vs. weekly bars): MEDIUM confidence (UX first-principles reasoning; not validated against user research data)
