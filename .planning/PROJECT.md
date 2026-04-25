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
## Current State

Phase 20 complete — Transactions UI enhancements (collapsible header, nav gap fix, theme-aware surfaces)

## Current Milestone: v2.1 Multiple Themes

**Goal:** Add 5 selectable themes with light/dark variants, each with consistent color palettes for background, cards, text, income, expense, and accent colors.

**Target features:**

*Themes:*
- Theme 1 — Soft Neutral (Default): Professional, calm, maximum readability
- Theme 2 — Warm Finance: Friendly, less clinical, human feel
- Theme 3 — Cool Blue Finance: Classic banking tone, trust-focused
- Theme 4 — Minimal Green Ledger: Financial focus, clean and disciplined
- Theme 5 — Modern Muted: Premium feel, paid-product illusion

*Each theme includes:*
- Light mode variant
- Dark mode variant (tied to existing dark mode toggle)
- Complete color palette: Background, Card, Primary Text, Secondary Text, Income, Expense, Accent, Divider (where applicable)

*Settings UI:*
- Theme selection dropdown (shows theme names with visual preview)
- Dark mode toggle works with selected theme
- Default: Soft Neutral Theme

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

*Last updated: 2026-04-25 — Phase 20 complete*