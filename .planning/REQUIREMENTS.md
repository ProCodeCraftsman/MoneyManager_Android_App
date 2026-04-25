# Requirements: MoneyManager v2.1

**Defined:** 2026-04-25
**Core Value:** Personal finance management with multiple theme options

## v2.1 Requirements

### Theme Infrastructure

- [ ] **THEM-01**: App uses Jetpack Compose Material 3 dynamic color theming system
- [ ] **THEM-02**: Theme colors are defined in Theme.kt with ColorScheme extension
- [ ] **THEM-03**: User preference (theme selection + dark mode) is persisted to DataStore
- [ ] **THEM-04**: Dark mode toggle applies to currently selected theme
- [ ] **THEM-05**: App applies theme colors on startup before first frame

### Theme 1: Soft Neutral (Default)

- [ ] **THEM-06**: Soft Neutral light mode with colors: Background #F6F7F9, Card #FFFFFF, Primary Text #1F2937, Secondary Text #6B7280, Income #16A34A, Expense #DC2626, Accent #2563EB, Divider #E5E7EB
- [ ] **THEM-07**: Soft Neutral dark mode with colors: Background #0F172A, Card #1E293B, Primary Text #E5E7EB, Secondary Text #94A3B8, Income #22C55E, Expense #F87171, Accent #3B82F6

### Theme 2: Warm Finance

- [ ] **THEM-08**: Warm Finance light mode with colors: Background #FAF7F2, Card #FFFFFF, Primary Text #2B2B2B, Secondary Text #7A7A7A, Income #2E7D32, Expense #C62828, Accent #F59E0B, Divider #E8E3DA
- [ ] **THEM-09**: Warm Finance dark mode with colors: Background #1C1917, Card #292524, Primary Text #F5F5F4, Secondary Text #A8A29E, Income #4ADE80, Expense #FB7185, Accent #F59E0B

### Theme 3: Cool Blue Finance

- [ ] **THEM-10**: Cool Blue Finance light mode with colors: Background #F4F8FF, Card #FFFFFF, Primary Text #1E3A8A, Secondary Text #64748B, Income #059669, Expense #DC2626, Accent #2563EB, Divider #DBEAFE
- [ ] **THEM-11**: Cool Blue Finance dark mode with colors: Background #020617, Card #0F172A, Primary Text #E2E8F0, Secondary Text #94A3B8, Income #34D399, Expense #F87171, Accent #3B82F6

### Theme 4: Minimal Green Ledger

- [ ] **THEM-12**: Minimal Green Ledger light mode with colors: Background #F3FBF6, Card #FFFFFF, Primary Text #064E3B, Secondary Text #6B7280, Income #16A34A, Expense #B91C1C, Accent #10B981, Divider #D1FAE5
- [ ] **THEM-13**: Minimal Green Ledger dark mode with colors: Background #022C22, Card #064E3B, Primary Text #ECFDF5, Secondary Text #A7F3D0, Income #4ADE80, Expense #F87171, Accent #10B981

### Theme 5: Modern Muted

- [ ] **THEM-14**: Modern Muted light mode with colors: Background #F8FAFC, Card #FFFFFF, Primary Text #0F172A, Secondary Text #64748B, Income #22C55E, Expense #EF4444, Accent #7C3AED, Divider #E2E8F0
- [ ] **THEM-15**: Modern Muted dark mode with colors: Background #020617, Card #111827, Primary Text #F1F5F9, Secondary Text #94A3B8, Income #4ADE80, Expense #FB7185, Accent #8B5CF6

### Settings UI

- [ ] **UI-01**: Settings screen shows theme selection dropdown with 5 theme options
- [ ] **UI-02**: Settings screen shows dark mode toggle below theme selection
- [ ] **UI-03**: Theme selection is highlighted with current theme name
- [ ] **UI-04**: Changing theme immediately updates the UI
- [ ] **UI-05**: Default theme is Soft Neutral (Theme 1) for new users

### Income/Expense Color Consistency

- [ ] **COL-01**: All income amounts display in theme's income color throughout the app
- [ ] **COL-02**: All expense amounts display in theme's expense color throughout the app
- [ ] **COL-03**: All income labels/icons use theme's income color
- [ ] **COL-04**: All expense labels/icons use theme's expense color

## v2 Requirements

Deferred themes for future consideration.

### Theme Presets

- **PRESET-01**: User can save current theme + dark mode as a preset
- **PRESET-02**: User can switch between saved presets quickly

### Auto Theme

- **AUTO-01**: App respects system theme by default
- **AUTO-02**: User can override system theme with manual selection

## Out of Scope

| Feature | Reason |
|---------|--------|
| Custom theme creator | High complexity, most users prefer curated options |
| Theme switching animations | Performance concern on lower-end devices |
| Per-account themes | Confusion, single theme for app consistency |
| AMOLED black themes | Special-purpose, not needed for v2.1 |
| Sync themes across devices | Cloud sync infrastructure not in scope |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| THEM-01 | Phase 14 | Pending |
| THEM-02 | Phase 14 | Pending |
| THEM-03 | Phase 14 | Pending |
| THEM-04 | Phase 14 | Pending |
| THEM-05 | Phase 14 | Pending |
| THEM-06 | Phase 15 | Pending |
| THEM-07 | Phase 15 | Pending |
| THEM-08 | Phase 15 | Pending |
| THEM-09 | Phase 15 | Pending |
| THEM-10 | Phase 15 | Pending |
| THEM-11 | Phase 15 | Pending |
| THEM-12 | Phase 15 | Pending |
| THEM-13 | Phase 15 | Pending |
| THEM-14 | Phase 15 | Pending |
| THEM-15 | Phase 15 | Pending |
| UI-01 | Phase 16 | Pending |
| UI-02 | Phase 16 | Pending |
| UI-03 | Phase 16 | Pending |
| UI-04 | Phase 16 | Pending |
| UI-05 | Phase 16 | Pending |
| COL-01 | Phase 17 | Pending |
| COL-02 | Phase 17 | Pending |
| COL-03 | Phase 17 | Pending |
| COL-04 | Phase 17 | Pending |

**Coverage:**
- v2.1 requirements: 25 total
- Mapped to phases: 25
- Unmapped: 0 ✓

---
*Requirements defined: 2026-04-25*
*Last updated: 2026-04-25 after initial definition*