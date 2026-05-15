# MoneyManager - Roadmap

## Milestones

| Version | Status | Date |
|---------|-------|------|
| [v1.0](milestones/v1.0-ROADMAP.md) | ✅ Shipped | 2026-04-14 |
| [v2.0](milestones/v2.0-ROADMAP.md) | ✅ Shipped | 2026-04-25 |
| [v2.1](milestones/v2.1-ROADMAP.md) | In Progress | 2026-04-25 |
| v2.2 | In Progress | 2026-04-28 |
| v3.0 | In Progress | 2026-05-15 |

## Next Milestone

v3.0: AI-Assisted Transaction Drafting — 54 requirements across 5 phases (32–36)

---

<!-- START: v2.1.milestone -->
# Milestone v2.1: Multiple Themes

**Started:** 2026-04-25
**Goal:** Add 5 selectable themes with light/dark variants, theme selection UI in settings, and consistent income/expense colors throughout the app.

## Phases

- [x] **Phase 14: Theme Infrastructure** - Material 3 theming, DataStore persistence, dark mode support ✅
- [x] **Phase 15: Complete Theme System** - All 5 themes with light and dark mode variants ✅
- [x] **Phase 16: Theme Settings UI** - Theme selector dropdown, dark mode toggle, immediate updates ✅
- [x] **Phase 17: Income/Expense Coloring** - Consistent color coding for income and expense ✅
- [x] **Phase 18: Split Transaction Expandable View** - Expandable split transactions with child items visible on demand ✅
- [x] **Phase 19: Transactions UI Fixes** - Visual corrections to TransactionsScreen (header, sticky date headers, search bar, filters) ✅
- [ ] **Phase 21: Transactions UI Polish** - Complete visual redesign of TransactionsScreen scroll behavior, search, surface discipline, and nav separation
- [x] **Phase 22: Transactions Header & Navigation Enhancements** - Time filter on right, search toggle, sticky nav, move-to-top, collapse/expand daily groups ✅
- [ ] **Phase 23: Transaction Code Refactoring** - Extract dialog subcomponents, fix validation duplication, remove magic strings
- [x] **Phase 24: Scroll-to-Top (Global)** - Global reusable scroll-to-top modifier for LazyColumn screens ✅
- [x] **Phase 25: Transaction Pagination** - Add lazy loading for large transaction lists
- [x] **Phase 26: Remove Reports Feature** - Remove ReportsScreen, ReportsViewModel, chart components, and navigation route ✅

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

### Phase 18: Split Transaction Expandable View
**Goal**: Users can view all child transactions within a split transaction with expandable/collapsible UI, defaulting to collapsed state

**Depends on**: Phase 17

**Requirements**: SPLIT-01, SPLIT-02, SPLIT-03

**Success Criteria** (what must be TRUE):
  1. Split parent transactions show expand icon by default (collapsed)
  2. Tapping expand icon reveals all child transactions below the parent
  3. Tapping collapse icon hides child transactions
  4. Each child transaction displays its own category, amount, and note
  5. Total of children equals parent amount

**Plans**: 
- [x] 18-01-PLAN.md — Expandable split transactions with child items visible on demand

**UI hint**: yes

### Phase 19: Transactions UI Fixes
**Goal**: Visual/UI corrections to TransactionsScreen — header status bar, sticky date headers, search bar placeholder, and filter components

**Depends on**: Phase 18

**Requirements**: TXUI-01, TXUI-02, TXUI-03, TXUI-04, TXUI-05

**Success Criteria** (what must be TRUE):
  1. Status bar color matches header background color
  2. Sticky date headers have visible elevation (shadow + spacing)
  3. Search placeholder text is centered vertically
  4. Filter chips height matches text field height
  5. Active filter indicator has lightweight styling (30% opacity)

**Plans**:
- [x] 19-01-PLAN.md — UI corrections to TransactionsScreen

**UI hint**: yes

### Phase 20: Transactions UI Enhancement
**Goal**: UI/UX enhancements to TransactionsScreen — collapsible header on scroll, bottom gap fix, theme-aware colors with elevation separation

**Depends on**: Phase 19

**Requirements**: None (pure UI improvements)

**Success Criteria** (what must be TRUE):
  1. Header collapses when scrolled down (scroll-based collapse)
  2. Header expands when scrolled to top
  3. Bottom gap above navigation removed
  4. Theme-aware colors used throughout (MaterialTheme.colorScheme)
  5. Elevation provides component separation

**Plans**:
- [x] 20-01-PLAN.md — UI enhancements (collapsible header, bottom gap, theme colors)

**UI hint**: yes

### Phase 21: Transactions UI Polish
**Goal**: Complete the TransactionsScreen visual redesign — proper header-summary scroll choreography, single-surface theme discipline, hidden search, and clean bottom navigation separation

**Depends on**: Phase 20

**Requirements**: None (pure UI polish)

**Success Criteria** (what must be TRUE):
  1. Header and summary row scroll together with correct choreography
  2. Search is hidden by default, revealed via button near filters
  3. Single background surface with elevation-only separation throughout
  4. Bottom navigation area has clean, gapless separation
  5. All surfaces use MaterialTheme.colorScheme with no hardcoded colors

**Plans**: 3 plans

**Plans**:
- [x] 21-01-PLAN.md — Restructure topBar (compact header only) and add summary panel + time nav as LazyColumn items
- [ ] 21-02-PLAN.md — Hidden search behavior (icon in header, AnimatedVisibility bar) and remove inline filter pill
- [ ] 21-03-PLAN.md — Single-surface discipline (remove alpha hacks) and WindowInsets.navigationBars contentPadding

**UI hint**: yes

### Phase 22: Transactions Header & Navigation Enhancements
**Goal**: UI/UX enhancements to TransactionsScreen — time filter on right, search toggle, sticky navigation, move-to-top button, collapsible daily groups

**Depends on**: Phase 21

**Requirements**: None (pure UI enhancements)

**Success Criteria** (what must be TRUE):
  1. Time filter dropdown positioned on right side of header, after search icon
  2. Search bar hidden by default, toggled by search icon in header
  3. Navigation bar (period display) uses stickyHeader and sticks while scrolling
  4. Move-to-top button appears after scrolling past threshold
  5. Daily date groups are collapsible/expandable via tap
  6. Statistics calculate from filtered transactions (based on time period)

**Plans**: 3 plans

**Plans**:
- [ ] 22-01-PLAN.md — Header layout (search icon, time filter on right) + AnimatedVisibility search bar
- [ ] 22-02-PLAN.md — Sticky navigation bar + move-to-top button
- [ ] 22-03-PLAN.md — Collapsible daily groups + statistics following time filter

**UI hint**: yes

### Phase 23: Transaction Code Refactoring
**Goal**: Refactor AddEditTransactionDialog to use extracted components, remove duplicate validation logic, and fix fragile code patterns

**Depends on**: Phase 22

**Requirements**: None (refactoring from code analysis)

**Success Criteria** (what must be TRUE):
  1. AddEditTransactionDialog reduced to under 600 lines
  2. Amount validation extracted to reusable function
  3. Transfer balance heuristic replaced with explicit field
  4. Form state grouped into data class

**Plans**: 4 plans

**Plans**:
- [ ] 23-01-PLAN.md — Extract AmountKeypad and AmountDisplay to AmountEntrySection component
- [ ] 23-02-PLAN.md — Extract category selection to CategorySelector component
- [ ] 23-03-PLAN.md — Fix transfer balance heuristic and extract validation
- [ ] 23-04-PLAN.md — Group form state into FormState data class

**UI hint**: no

---

### Phase 24: Scroll-to-Top (Global)
**Goal**: Global reusable scroll-to-top modifier for all LazyColumn screens

**Depends on**: Phase 23

**Requirements**: None (global UI enhancement)

**Success Criteria** (what must be TRUE):
  1. ScrollToTop modifier works with any LazyColumn via lazyListState injection
  2. Button appears after scrolling past 50dp threshold
  3. Positioned center-bottom with 16dp margin
  4. Applied to all 13 scrollable screens

**Plans**:
- [x] 24-01-PLAN.md — Global scroll-to-top modifier for all LazyColumn screens

**UI hint**: yes

---

### Phase 25: Transaction Pagination
**Goal**: Add lazy loading for large transaction lists using Android Paging 3 to minimize loading time

**Depends on**: Phase 24

**Requirements**: None (performance optimization)

**Success Criteria** (what must be TRUE):
  1. Transactions load with pagination (50 items per page)
  2. Scrolling to bottom automatically loads next page
  3. Loading indicator shows when fetching next page
  4. Filters (type, account, category, date) work with pagination
  5. Search functionality preserved (uses separate non-paginated flow)
  6. Initial load time improved (only first page loaded)

**Plans**: 2 plans

**Plans**:
- [x] 25-01-PLAN.md — Add Paging 3 dependencies, refactor DAO and Repository
- [x] 25-02-PLAN.md — Refactor ViewModel and TransactionsScreen for LazyPagingItems

**UI hint**: yes

### Phase 26: Remove Reports Feature
**Goal**: Remove ReportsScreen, ReportsViewModel, chart components, and navigation routes from the app to reduce complexity

**Depends on**: Phase 25

**Requirements**: None (removal/cleanup)

**Success Criteria** (what must be TRUE):
  1. ReportsScreen.kt and ReportsViewModel.kt removed from codebase
  2. Reports route removed from MoneyManagerNavHost.kt
  3. Chart components (TrendLineChart, ExpensePieChart, CategoryBarChart) removed ONLY if not used elsewhere
  4. All references to ReportsViewModel removed from dependency graph
  5. Build succeeds after removal

**Plans**: 1 plan

**Plans**:
- [x] 26-01-PLAN.md — Remove Reports feature files and navigation

**UI hint**: yes

---

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 14. Theme Infrastructure | 1/1 | ✅ Complete | 2026-04-25 |
| 15. Complete Theme System | 1/1 | ✅ Complete | 2026-04-25 |
| 16. Theme Settings UI | 1/1 | ✅ Complete | 2026-04-25 |
| 17. Income/Expense Coloring | 1/1 | ✅ Complete | 2026-04-25 |
| 18. Split Transaction Expandable | 1/1 | In Progress | — |
| 19. Transactions UI Fixes | 1/1 | Complete    | 2026-04-25 |
| 20. Transactions UI Enhancement | 1/1 | Complete    | 2026-04-25 |
| 21. Transactions UI Polish | 3/3 | ✅ Complete | 2026-04-27 |
| 22. Header & Navigation | 3/3 | ✅ Complete | 2026-04-27 |
| 23. Transaction Code Refactoring | 0/4 | Pending | — |
| 24. Scroll-to-Top (Global) | 1/1 | Complete    | 2026-04-27 |
| 25. Transaction Pagination | 2/2 | Complete    | 2026-04-27 |
| 26. Remove Reports Feature | 1/1 | ✅ Complete | — |

---

## Milestone v2.1 Progress

**Requirements:** 28 + 5 = 33 total | **Phases:** 7 | **Mapped:** 28/33 ✓

<!-- END: v2.1.milestone -->

<!-- START: v2.2.milestone -->
# Milestone v2.2: Insights Dashboard

**Started:** 2026-04-28
**Goal:** Add a 3-screen swipeable Insights section (STATUS / RISKS / TRENDS) that derives financial summaries purely from transaction records — no budgets, categories, or AI assumptions.

## Phases

- [ ] **Phase 27: Data Layer & Calculations** - InsightsCalculator pure object, InsightsUiState data contracts, InsightsViewModel wiring Room flows
- [x] **Phase 28: Navigation & Screen Shell** - Insights bottom nav entry, HorizontalPager shell with tab indicator, swipe between 3 panes (completed 2026-04-30)
- [ ] **Phase 29: Status Pane** - Net Position hero, Net Cash Flow, Income/Expense/Savings/Lending/Borrowing totals, month label, empty state
- [ ] **Phase 30: Risks Pane** - Up to 3 rule-based financial alerts, severity ordering, icon+title+explanation cards, empty state
- [ ] **Phase 31: Trends Pane** - Dominant activity card, daily income/expense line chart (Canvas), empty state

## Phase Details

### Phase 27: Data Layer & Calculations
**Goal**: All financial aggregation and risk logic is computable and unit-testable before any screen is built

**Depends on**: Phase 26

**Requirements**: None (infrastructure — contracts and wiring that enable phases 28–31)

**Success Criteria** (what must be TRUE):
  1. InsightsCalculator.compute() accepts two List<TransactionEntity> inputs (current month, previous month) and returns InsightsData without any Android runtime dependency
  2. Split child transactions (isSplitChild=true) are excluded from every aggregation — verified by unit test
  3. All seven monetary figures (Net Position, Net Cash Flow, Income, Expense, Savings, Lending, Borrowing) are computed from the same current-month date window
  4. InsightsViewModel emits StateFlow<InsightsUiState> using getTransactionsByDateRange() — never getAllTransactions()
  5. hasEnoughHistory flag is false when no previous-month transactions exist, suppressing all comparison-based risk rules

**Plans**: 2 plans

**Plans**:
- [x] 27-01-PLAN.md — Create InsightsCalculator + InsightsUiState contracts ✅
- [x] 27-02-PLAN.md — Create InsightsViewModel wired to TransactionRepository ✅

**UI hint**: no

### Phase 28: Navigation & Screen Shell
**Goal**: Users can reach the Insights section from the bottom navigation bar and swipe between the 3 panes

**Depends on**: Phase 27

**Requirements**: INF-01, INF-02, INF-03

**Success Criteria** (what must be TRUE):
  1. User can tap the Insights item in the bottom navigation bar to navigate to InsightsScreen
  2. User can swipe left and right between the 3 panes (Status, Risks, Trends)
  3. A tab indicator at the top of the screen shows which of the 3 panes is currently active and updates on swipe
  4. Navigating away and back to Insights returns the user to the previously viewed pane

**Plans**: 1 plan

**Plans**:
- [x] 28-01-PLAN.md — Insights bottom nav entry + InsightsScreen with TabRow + HorizontalPager shell ✅

**UI hint**: yes

### Phase 29: Status Pane
**Goal**: Users see a clear summary of their current month's financial position on the Status screen

**Depends on**: Phase 28

**Requirements**: STA-01, STA-02, STA-03, STA-04, STA-05, STA-06, STA-07, STA-08, STA-09

**Success Criteria** (what must be TRUE):
  1. Net Position is displayed as the largest, most visually prominent number on screen, colored green when positive and red when negative
  2. Net Cash Flow, Total Income, Total Expense, Total Savings, Total Lending, and Total Borrowing each appear as distinct labeled figures below the hero number
  3. The current month and year label (e.g. "April 2026") is visible on the screen adjacent to the hero number
  4. All monetary figures display the user's selected currency symbol
  5. When no transactions exist for the current month, the screen shows "No financial activity recorded yet" instead of all-zero figures

**Plans**: 3 plans

**Plans**:
- [x] 29-01-PLAN.md — Create stateless Status Pane composables (hero, figures, grid, empty state) ✅
- [x] 29-02-PLAN.md — Create StatusPaneScreen root composable and wire to ViewModel ✅
- [x] 29-03-PLAN.md — Integrate StatusPaneScreen into InsightsScreen as first pane ✅

**UI hint**: yes

### Phase 30: Risks Pane
**Goal**: Users see actionable financial alerts derived from their current month's transactions, with the most serious alerts shown first

**Depends on**: Phase 28

**Requirements**: RSK-01, RSK-02, RSK-03, RSK-04, RSK-05, RSK-06, RSK-07, RSK-08

**Success Criteria** (what must be TRUE):
  1. At most 3 alerts are shown; WARNING-severity alerts appear before INFO-severity alerts
  2. "Expenses exceed income this month" alert appears when total expense exceeds total income
  3. "Spending increased significantly" alert appears when expense is more than 20% higher than last month — and is absent when no previous-month data exists
  4. "Borrowing is high relative to income" alert appears when total borrowing exceeds 50% of total income
  5. "Savings improved compared to last month" positive alert appears when this month's savings exceed last month's — and is absent when no previous-month data exists
  6. Each alert card shows an icon, a short title, and a one-to-two line explanation that includes the triggering computed value
  7. When no transactions exist for the current month, the screen shows "No financial activity recorded yet"

**Plans**: 2 plans

**Plans**:
- [x] 030-01-PLAN.md — Create stateless Risks Pane composables (RisksAlertCard, RisksAlertList, RisksPaneHeader, RisksPaneEmptyState, RisksHistoryDisclaimer)
- [x] 030-02-PLAN.md — Create RisksPaneScreen root composable, wire to ViewModel, update InsightsScreen to use real Risks Pane

**UI hint**: yes

### Phase 31: Trends Pane
**Goal**: Users can see the dominant financial activity and a daily chart of income vs expense for the current month

**Depends on**: Phase 28

**Requirements**: TRD-01, TRD-02, TRD-03, TRD-04

**Success Criteria** (what must be TRUE):
  1. The transaction type with the highest total amount in the current month is shown with its type label and total amount
  2. A dual-series line chart (income line + expense line) is displayed, with each data point representing one calendar day of the current month
  3. When fewer than 2 daily data points exist, the chart is hidden and a message is shown in its place
  4. When no transactions exist for the current month, the screen shows "No transactions this month"

**Plans**: 2 plans

**Plans**:
- [ ] 31-01-PLAN.md — Create stateless Trends Pane composables (TrendsPaneHeader, TrendsDominanceCard, TrendsLineChart, TrendsPaneEmptyState)
- [ ] 31-02-PLAN.md — Create TrendsPaneScreen root composable, wire to ViewModel, update InsightsScreen to use real Trends Pane

**UI hint**: yes

---

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 27. Data Layer & Calculations | 2/2 | ✅ Complete | 2026-04-29 |
| 28. Navigation & Screen Shell | 1/1 | Complete    | 2026-04-30 |
| 29. Status Pane | 3/3 | ✅ Complete | 2026-04-30 |
| 30. Risks Pane | 2/2 | ✅ Complete | 2026-04-30 |
| 31. Trends Pane | 0/? | Not started | — |

---

## Milestone v2.2 Progress

**Requirements:** 24 total | **Phases:** 5 | **Mapped:** 24/24 ✓

<!-- END: v2.2.milestone -->

<!-- START: v3.0.milestone -->
# Milestone v3.0: AI-Assisted Transaction Drafting

**Started:** 2026-05-15
**Goal:** Integrate Gemini Nano (via Android AICore) as an opt-in drafting assistant. Users create transaction drafts from SMS, receipt images, or voice memos — AI suggests a pre-filled form, user reviews and confirms. App stays 100% functional without AI. All existing features unchanged.

## Phases

- [x] **Phase 32: Domain AI Foundation** - GenAiClient interface, TransactionDraft, TransactionType enum, PromptContextBuilder, GenerateDraftFromTextUseCase — zero Android imports ✅
- [x] **Phase 33: Data AI Implementation** - Gradle dependencies, PreferencesManager extension, PromptBuilder, DraftParser, NanoAiClient, DeviceCapabilityManager (4-state) ✅
- [ ] **Phase 34: DI Wiring & AI Availability** - AiModule nullable @Provides, MoneyManagerApp startup hook, Flow<Boolean> availability repository
- [ ] **Phase 35: AI Draft Source Screens** - AiDraftViewModel, SmsPickerScreen, ReceiptScanScreen, VoiceMemoScreen, shared UI state
- [ ] **Phase 36: Dialog Integration & FAB** - Expandable 3-source FAB, AddEditTransactionDialog initialDraft, source banner, AI field highlighting, nav routes

## Phase Details

### Phase 32: Domain AI Foundation
**Goal**: All AI domain contracts exist as pure Kotlin — zero Android runtime imports, fully unit-testable on JVM

**Depends on**: no previous phases/

**Requirements**: AIFND-03, AIFND-05, AIFND-06, AIFND-07, AIFND-08

**Success Criteria** (what must be TRUE):
  1. All domain/ai/ classes compile without any Android runtime imports — verified by import scan
  2. GenerateDraftFromTextUseCase returns Result.failure(AiUnavailableException) when GenAiClient is null — no silent no-op
  3. TransactionType.allIds() returns all current transaction type strings matching TransactionEntity.VALID_TYPES
  4. PromptContextBuilder builds a PromptContext from 3 mock repositories without touching Room or Android Context

**Plans**: 3 plans

Plans:
- [x] 32-01-PLAN.md — Create TransactionType enum, GenAiClient interface, AiUnavailableException ✅
- [x] 32-02-PLAN.md — Create TransactionDraft and PromptContext with entry projection types ✅
- [x] 32-03-PLAN.md — Create PromptContextBuilder and GenerateDraftFromTextUseCase ✅

**UI hint**: no

### Phase 33: Data AI Implementation
**Goal**: All data-layer AI components are implemented and Gradle sync succeeds with the 4 new dependencies

**Depends on**: Phase 32

**Requirements**: AIFND-01, AIFND-04, AIFND-09, AIFND-10

**Success Criteria** (what must be TRUE):
  1. Gradle sync succeeds with 4 new dependencies; APK size increase is 0 MB for genai-prompt + genai-common (system-managed model)
  2. DeviceCapabilityManager writes "READY", "NEVER", or "PENDING" to PreferencesManager — never a Boolean
  3. DraftParser correctly strips markdown fences and extracts valid TransactionDraft from 3 test JSON variants (clean, fenced, partial)
  4. NanoAiClient.generateDraft() returns Result.failure on AICore exception without crashing

**Plans**: 3 plans

**Wave 1**
- [x] 33-01-PLAN.md — Add 4 Gradle dependencies + extend PreferencesManager with ai_availability_status key ✅

**Wave 2** *(blocked on Wave 1 completion)*
- [x] 33-02-PLAN.md — Create DraftParser + PromptBuilder; replace placeholder in GenerateDraftFromTextUseCase ✅
- [x] 33-03-PLAN.md — Create NanoAiClient + DeviceCapabilityManager ✅

**Cross-cutting constraints:** DeviceCapabilityManager writes String "READY"/"NEVER"/"PENDING" — never Boolean; exactly 1 preferencesDataStore delegate throughout

**UI hint**: no

### Phase 34: DI Wiring & AI Availability
**Goal**: Full Hilt graph compiles cleanly with nullable GenAiClient — build gate passes before any UI is touched

**Depends on**: Phase 33

**Requirements**: AIFND-02, AIFND-11, AIFND-12

**Success Criteria** (what must be TRUE):
  1. Full Hilt graph compiles with nullable GenAiClient? — no KSP annotation processor errors
  2. On a non-AICore device (or emulator), GenAiClient resolves to null and the app starts without crash
  3. Flow<Boolean> from AiAvailabilityRepository emits false on non-AICore device and true on READY device

**Plans**: TBD

**UI hint**: no

### Phase 35: AI Draft Source Screens
**Goal**: Users can reach all three AI draft source screens; each flow reaches AddEditTransactionDialog with pre-filled fields on AICore devices and falls back to manual entry gracefully on non-AICore devices

**Depends on**: Phase 34

**Requirements**: SMS-01, SMS-02, SMS-03, SMS-04, SMS-05, SMS-06, SMS-07, SMS-08, SMS-09, SMS-10, OCR-01, OCR-02, OCR-03, OCR-04, OCR-05, OCR-06, OCR-07, OCR-08, OCR-09, VOICE-01, VOICE-02, VOICE-03, VOICE-04, VOICE-05, VOICE-06, VOICE-07, VOICE-08, VOICE-09, VOICE-10, STD-01, STD-02, STD-03

**Success Criteria** (what must be TRUE):
  1. User can paste SMS text and tap "AI Fill" to reach AddEditTransactionDialog with pre-filled fields
  2. User can capture or select a receipt image; OCR text appears in the scrollable pane; "AI Fill" pre-fills the dialog
  3. User can record a voice memo; transcription appears in editable field; "AI Fill" pre-fills the dialog
  4. On a non-AICore device all three screens are reachable; "AI Fill" is absent; manual entry works end-to-end
  5. VoiceMemoScreen is hidden entirely when SpeechRecognizer.isRecognitionAvailable() returns false

**Plans**: TBD

**UI hint**: yes

### Phase 36: Dialog Integration & FAB
**Goal**: The existing AddEditTransactionDialog accepts AI drafts transparently; all changes to existing files are strictly additive with null-default parameters

**Depends on**: Phase 35

**Requirements**: DRAFT-01, DRAFT-02, DRAFT-03, DRAFT-04, DRAFT-05, DRAFT-06, DRAFT-07, DRAFT-08, DRAFT-09, STD-04

**Success Criteria** (what must be TRUE):
  1. Existing + FAB opens AddEditTransactionDialog with no change in behavior (null initialDraft path unchanged)
  2. Opening dialog from SMS, OCR, or Voice flow shows source banner and AI field highlighting
  3. Editing an AI-highlighted field removes its tint and badge for that field only
  4. Dismissing and reopening the dialog shows blank form — clearDraft() fired on dismiss
  5. AI-drafted transaction saves successfully using the existing validation path with no separate AI save code path

**Plans**: TBD

**UI hint**: yes

---

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 32. Domain AI Foundation | 3/3 | ✅ Complete | 2026-05-15 |
| 33. Data AI Implementation | 3/3 | ✅ Complete | 2026-05-15 |
| 34. DI Wiring & AI Availability | 0/? | Not started | — |
| 35. AI Draft Source Screens | 0/? | Not started | — |
| 36. Dialog Integration & FAB | 0/? | Not started | — |

---

## Milestone v3.0 Progress

**Requirements:** 54 total (12 AIFND + 10 SMS + 9 OCR + 10 VOICE + 9 DRAFT + 4 STD) | **Phases:** 5 | **Mapped:** 54/54 ✓

<!-- END: v3.0.milestone -->
