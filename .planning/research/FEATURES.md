# Feature Research

**Domain:** Personal Finance App — Default Categories & Dashboard
**Researched:** 2026-04-14
**Confidence:** HIGH

## Feature Landscape

### Table Stakes (Users Expect These)

Features users assume exist. Missing these = product feels incomplete.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Default Category Set** | Users expect 15-30 pre-built categories covering Income, Expenses (Housing, Food, Transport, Utilities, etc.), Transfers | LOW | Must be present on first launch; Monarch Money, YNAB, Mint all provide this |
| **Subcategory Hierarchy** | Users need granular tracking (e.g., "Groceries" > "Whole Foods") without creating top-level category explosion | MEDIUM | Most apps support 2-3 levels; subcategories inherit parent type |
| **Category Types** | Income vs Expense vs Transfer determines how transactions count in reports | LOW | Required for accurate reporting; Transfer excludes from spending |
| **Dashboard Overview** | Users expect to see their financial "score" immediately on app open | LOW | Net worth is the new standard — not just checking balance |
| **Account Balances** | Unified view of all accounts (checking, savings, credit cards, cash) | LOW | Users hate opening separate apps for each account |
| **Income vs Expense Summary** | Core question: "Am I spending more than I earn?" | LOW | Should show current period with comparison to prior period |
| **Spending by Category** | Breakdown of where money goes — pie/donut chart or bars | LOW | Must support drill-down to transactions |
| **Recent Transactions** | Quick access to latest activity across all accounts | LOW | Most-used dashboard widget |
| **Budget Progress** | Visual indicator of spent vs. allocated per category | MEDIUM | Progress bars; alerts when approaching/exceeding |
| **Date Range Selection** | Users need to view dashboard for different periods (week/month/year) | LOW | Must remember last selection |

### Differentiators (Competitive Advantage)

Features that set the product apart. Not required, but valuable.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Auto-Categorization Rules** | Users don't want to manually categorize every transaction — rules automate based on merchant/description patterns | HIGH | Requires rule engine, merchant matching, conflict resolution |
| **Actionable Insights** | "You spent $400 on restaurants" is data; "Restaurant spending is up 30% vs. last month" is insight | MEDIUM | Compare to prior period, budget, or user average |
| **Goal Progress Widget** | Shows savings goals with projected completion dates | MEDIUM | Connect to existing Goals feature; visual progress rings |
| **Bill/Recurring Payments** | Upcoming bills with due dates and reminders | MEDIUM | Connect to existing Recurring feature; show what's coming |
| **Net Worth Trend** | Shows net worth over time (assets minus liabilities) | MEDIUM | Requires multi-account aggregation; line chart |
| **Customizable Widgets** | Users pin most-relevant widgets to top of dashboard | LOW | Modular dashboard architecture; user preference storage |
| **Cash Flow Forecast** | Projected balance based on recurring transactions | HIGH | Uses existing Recurring data; predicts future income/expenses |

### Anti-Features (Commonly Requested, Often Problematic)

Features that seem good but create problems.

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| **Too Many Metrics** | Want to see all data at once | Creates cognitive overload; users ignore dashboard entirely | Progressive disclosure: 5-7 key metrics at top, drill-down below |
| **Real-time Everything** | "I want live balance updates" | Battery drain, API rate limits, stale data from bank's own timing | Periodic sync (every 15-30 min) or manual refresh option |
| **Cross-Account Context Missing** | Show each account separately | Defeats purpose of unified dashboard; user opens 3 apps anyway | Aggregate view by default, expandable per-account |
| **No Comparison Context** | "Show me my transactions" | Users don't know if $500 is good/bad without comparison | Show vs. prior period, vs. budget, vs. user average |
| **Delete vs. Deactivate** | "I don't need this category, delete it" | Orphaned transactions, broken historical reports | Deactivate only; preserve transaction history |

## Feature Dependencies

```
[Default Categories]
    └──requires──> [Category Types (Income/Expense/Transfer)]
                       └──requires──> [Category CRUD (Create/Read/Update/Deactivate)]

[Dashboard]
    └──requires──> [Account Balances]
    └──requires──> [Transactions List]
    └──requires──> [Date Range Selection]

[Spending by Category]
    └──requires──> [Categories with Transactions]

[Budget Progress]
    └──requires──> [Categories] + [Budgets feature]

[Goal Progress Widget]
    └──requires──> [Goals feature]

[Bill Widget]
    └──requires──> [Recurring feature]

[Auto-Categorization Rules]
    └──requires──> [Categories] + [Transactions]
                       └──requires──> [Merchant/Description Matching]

[Cash Flow Forecast]
    └──requires──> [Recurring] + [Historical transaction patterns]
```

### Dependency Notes

- **Default Categories requires Category CRUD:** Existing Categories UI must support create/edit before defaults can be fully managed
- **Dashboard requires Accounts + Transactions:** Must query existing features; dashboard is aggregation layer
- **Auto-Categorization Rules enhance Categories:** Not blocking for MVP but adds significant value post-launch
- **Spending by Category requires Categories + Transactions:** Uses existing transaction data, just aggregated differently

## MVP Definition

### Launch With (v1)

Minimum viable product — what's needed to validate the concept.

- [x] **Default Category Set** — Pre-seeded categories on first launch (handled in existing Categories UI)
- [x] **Subcategory Support** — Ability to create/edit child categories under parents
- [x] **Category Types** — Income/Expense/Transfer type per category
- [x] **Dashboard Overview** — Net worth + income vs expense summary cards
- [x] **Account Balances Widget** — All account balances in one view
- [x] **Spending by Category Widget** — Visual breakdown with drill-down
- [x] **Recent Transactions Widget** — Last N transactions quick view
- [x] **Date Range Picker** — Week/Month/Year selection

### Add After Validation (v1.x)

Features to add once core is working.

- [ ] **Budget Progress Widget** — Visual progress bars per budget category
- [ ] **Goal Progress Widget** — Savings goals with projected dates
- [ ] **Bill/Recurring Widget** — Upcoming payments
- [ ] **Net Worth Trend Chart** — Historical net worth line chart
- [ ] **Actionable Insights** — Contextual comparisons (vs. last month, vs. budget)
- [ ] **Auto-Categorization Rules** — Merchant-based automatic categorization

### Future Consideration (v2+)

Features to defer until product-market fit is established.

- [ ] **Customizable Widget Layout** — User reorders/pins dashboard widgets
- [ ] **Cash Flow Forecast** — Predicted balance based on recurring
- [ ] **Category Sharing/Import** — Import others' category setups (Fina-style)
- [ ] **Investment Category Enhancements** — Dividends, capital gains, buy/sell

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---------|------------|---------------------|----------|
| Default Category Set | HIGH | LOW | P1 |
| Subcategory Support | HIGH | MEDIUM | P1 |
| Category Types | HIGH | LOW | P1 |
| Dashboard Overview | HIGH | LOW | P1 |
| Account Balances Widget | HIGH | LOW | P1 |
| Spending by Category | HIGH | LOW | P1 |
| Recent Transactions | HIGH | LOW | P1 |
| Date Range Picker | HIGH | LOW | P1 |
| Budget Progress | MEDIUM | MEDIUM | P2 |
| Goal Progress | MEDIUM | MEDIUM | P2 |
| Bill Widget | MEDIUM | MEDIUM | P2 |
| Net Worth Trend | MEDIUM | MEDIUM | P2 |
| Actionable Insights | MEDIUM | HIGH | P2 |
| Auto-Categorization | HIGH | HIGH | P3 |
| Customizable Widgets | LOW | HIGH | P3 |
| Cash Flow Forecast | LOW | HIGH | P3 |

**Priority key:**
- P1: Must have for launch
- P2: Should have, add when possible
- P3: Nice to have, future consideration

## Competitor Feature Analysis

| Feature | Monarch Money | YNAB | Mint (Credit Karma) | Our Approach |
|---------|---------------|------|---------------------|---------------|
| Default Categories | Yes, 60+ across groups | Yes, 30+ starter | Yes, 20+ | Start with ~25 essentials, user adds more |
| Subcategories | Yes, 2 levels | Yes, unlimited | No | Yes, 2-3 levels max |
| Category Types | Income/Expense/Transfer | Income/Expense/Transfer | Income/Expense/Transfer | Same — required |
| Dashboard | Net worth, cash flow, trends | Spending by category, goals | Net worth, spending | Net worth + I/E + category breakdown |
| Auto-Categorize | Yes, rules engine | Yes, auto-import rules | Yes | P3 — defer |
| Budget on Dashboard | Progress bars | Direct in budget | Yes | P2 — add after MVP |
| Goal Tracking | Yes, linked to budgets | Yes | No (was goals feature) | P2 — widget showing existing goals |
| Bill Tracking | Yes, subscription detection | No | Yes | P2 — show existing Recurring |

## Sources

- Monarch Money Help: Default Categories structure (2025)
- Whistl Budget Guide: Custom categories and subcategories (2026)
- Fina Docs: Category areas, import, snapshots (2026)
- TheFrontkit: Finance Dashboard Templates (2026)
- Clarity App: Personal Finance Dashboard best practices (2026)
- DesignPixil: Fintech Dashboard Design Patterns (2026)
- WildnetEdge: Personal Finance Apps User Expectations 2026
- Blue Train Marketing: What Users Want in Personal Finance Apps (2025)
- Stan Vision: Fintech UX in 2026

---

*Feature research for: Android Finance App — Default Categories & Dashboard*
*Researched: 2026-04-14*