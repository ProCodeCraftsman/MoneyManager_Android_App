# MoneyManager - Android App Project

## Overview
- **Type**: Native Android Personal Finance App
- **Goal**: Convert HTML web app to Android and publish on Google Play
- **Source**: `D:\Android projec\MoneyManager.html`

## Source Analysis
Comprehensive money management \ app with:
- 12 main sections: Dashboard, Accounts, Transactions, Recurring, Reports, Budgets, Goals, Templates, Tags, Categories, Settings
- PIN lock with biometric support
- Dark/light theme
- 10+ currency support
- LocalStorage-based data

## Tech Stack Decision
| Component | Choice |
|-----------|--------|
| UI | Jetpack Compose + Material Design 3 |
| Database | Room (local) + Firestore (cloud backup) |
| Auth | Google Sign-In (Firebase Auth) |
| Charts | MPAndroidChart |
| DI | Hilt |
| Architecture | MVVM + Clean Architecture |

## Configuration
- **App Name**: MoneyManager
- **Data Storage**: Both (Room local + Firebase cloud backup)
- **Monetization**: Free (all features)
## Current State

Phase 28 complete — Insights bottom nav entry wired (Screen.Insights, route "insights", ShowChart icon); InsightsScreen shell with TabRow + HorizontalPager (3 panes: Status, Risks, Trends); pager state persists across navigation via rememberSaveable. Compilation verified: BUILD SUCCESSFUL.

## Current Milestone: v2.2 Insights Dashboard

**Goal:** Add a 3-screen swipeable Insights section that derives financial summaries purely from transaction records — no budgets, categories, or AI assumptions.

**Target features:**

*Screen 1 — STATUS:*
- Net Position as hero number (large, prominent)
- Net Cash Flow, Total Income, Total Expense, Total Savings
- Current month label
- No charts, no scrolling

*Screen 2 — RISKS:*
- Up to 3 rule-based financial alerts, negative alerts prioritized
- Rules: overspending, spending spike (>20% vs prev month), negative position, high borrowing (>50% of income), savings improvement
- Each alert: icon + title + short explanation

*Screen 3 — TRENDS:*
- Expense change % vs previous month with direction indicator
- Dominant activity: transaction type with highest total + amount
- Daily income/expense line chart for the month

*Key constraints:*
- Source of truth: transaction records only (date, amount, type)
- All calculations deterministic (current month vs previous month)
- Failsafe empty state: "No financial activity recorded yet"

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---

*Last updated: 2026-04-28 — Milestone v2.2 started*