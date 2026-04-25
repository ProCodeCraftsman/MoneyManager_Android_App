# MoneyManager - Roadmap

## Milestones

| Version | Status | Date |
|---------|-------|------|
| [v1.0](milestones/v1.0-ROADMAP.md) | ✅ Shipped | 2026-04-14 |
| [v2.0](milestones/v2.0-ROADMAP.md) | ✅ Shipped | 2026-04-25 |
| [v2.1](milestones/v2.1-ROADMAP.md) | In Progress | 2026-04-25 |

## Next Milestone

v2.1: Multiple Themes — 25 requirements across 4 phases

---

<!-- START: v2.1.milestone -->
# Milestone v2.1: Multiple Themes

**Started:** 2026-04-25
**Goal:** Add 5 selectable themes with light/dark variants, theme selection UI in settings, and consistent income/expense colors throughout the app.

## Phases

- [x] **Phase 14: Theme Infrastructure** - Material 3 theming, DataStore persistence, dark mode support ✅
- [x] **Phase 15: Complete Theme System** - All 5 themes with light and dark mode variants ✅
- [ ] **Phase 16: Settings UI** - Theme selector dropdown, dark mode toggle, immediate updates
- [ ] **Phase 17: Income/Expense Coloring** - Consistent color usage throughout app

## Phase Details

### Phase 14: Theme Infrastructure
**Goal**: Users experience consistent theming with Material 3, their preferences persist across sessions

**Depends on**: Phase 13 (v2.0 completion)

**Requirements**: THEM-01, THEM-02, THEM-03, THEM-04, THEM-05

**Success Criteria** (what must be TRUE):
  1. App uses Jetpack Compose Material 3 dynamic color theming system
  2. Theme colors defined in Theme.kt with ColorScheme extension
  3. User preference (theme selection + dark mode) persisted to DataStore
  4. Dark mode toggle applies to currently selected theme
  5. App applies theme colors on startup before first frame

**Plans**: 758ceda ✅

**UI hint**: yes

### Phase 15: Complete Theme System
**Goal**: Users can select from 5 complete themes with both light and dark variants

**Depends on**: Phase 14

**Requirements**: THEM-06, THEM-07, THEM-08, THEM-09, THEM-10, THEM-11, THEM-12, THEM-13, THEM-14, THEM-15

**Success Criteria** (what must be TRUE):
  1. Soft Neutral (Default) theme available in light mode
  2. Soft Neutral theme available in dark mode
  3. Warm Finance theme available in light mode
  4. Warm Finance theme available in dark mode
  5. Cool Blue Finance theme available in light mode
  6. Cool Blue Finance theme available in dark mode
  7. Minimal Green Ledger theme available in light mode
  8. Minimal Green Ledger theme available in dark mode
  9. Modern Muted theme available in light mode
  10. Modern Muted theme available in dark mode

**Plans**:
- [ ] 15-01-PLAN.md — All 5 themes with light/dark color schemes

**Plans**: 1 plan

**UI hint**: yes

### Phase 16: Theme Settings UI
**Goal**: Users can easily select their preferred theme and dark mode in settings

**Depends on**: Phase 15

**Requirements**: UI-01, UI-02, UI-03, UI-04, UI-05

**Success Criteria** (what must be TRUE):
  1. Settings screen shows theme selection dropdown with 5 theme options
  2. Settings screen shows dark mode toggle below theme selection
  3. Theme selection displays current theme name highlighted
  4. Changing theme immediately updates the UI
  5. Default theme is Soft Neutral for new users

**Plans**:
- [ ] 16-01-PLAN.md — Theme selection dropdown and dark mode toggle in Settings

**UI hint**: yes

### Phase 17: Income/Expense Coloring
**Goal**: Users see consistent color coding for income and expense throughout the app

**Depends on**: Phase 16

**Requirements**: COL-01, COL-02, COL-03, COL-04

**Success Criteria** (what must be TRUE):
  1. All income amounts display in theme's income color throughout the app
  2. All expense amounts display in theme's expense color throughout the app
  3. All income labels/icons use theme's income color
  4. All expense labels/icons use theme's expense color

**Plans**: TBD

**UI hint**: yes

---

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 14. Theme Infrastructure | 1/1 | ✅ Complete | 2026-04-25 |
| 15. Complete Theme System | 1/1 | ✅ Complete | 2026-04-25 |
| 16. Theme Settings UI | 0/1 | Not started | - |
| 17. Income/Expense Coloring | 0/1 | Not started | - |

---

## Milestone v2.1 Progress

**Requirements:** 25 total | **Phases:** 4 | **Mapped:** 25/25 ✓

<!-- END: v2.1.milestone -->