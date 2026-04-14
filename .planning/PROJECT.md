# MoneyManager - Android App Project

## Overview
- **Type**: Native Android Personal Finance App
- **Goal**: Convert HTML web app to Android and publish on Google Play
- **Source**: `D:\Android projec\MoneyManager.html`

## Source Analysis
Comprehensive money management web app with:
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
## Current Milestone: v2.0 Categories + Dashboard

**Goal:** Add comprehensive default categories with sub-categories and build the complete dashboard with time filters, stat cards, charts, budget widget, and recurring reminders.

**Target features:**

*Categories:*
- 11 Expense Categories (Food, Transport, Shopping, Bills, Health, Entertainment, Travel, Education, Home, Personal Care, Other Expense)
- 5 Income Categories (Salary, Freelance, Investment, Gift, Other Income)
- 2 Savings & Investment Categories (Mutual Funds, Fixed Deposit) with sub-categories
- Default categories cannot be deleted but can be archived/unarchived
- Users can add custom categories

*Dashboard:*
- Time filter bar (Day, Week, Month, Year, All, Custom)
- Stat cards (Net Worth, Income, Expenses, Net)
- Spending by category doughnut chart with drill-down
- Recent transactions (last 8)
- Budget widget (current month)
- Recurring reminders banner

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

*Last updated: 2026-04-14*