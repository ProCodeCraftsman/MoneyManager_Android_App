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

v2.2 Insights Dashboard phases 27–30 complete (Data Layer, Navigation Shell, Status Pane, Risks Pane). Phase 31 Trends Pane pending. v3.0 AI-Assisted Transaction Drafting defined — research and roadmap in progress.

## Previous Milestone: v2.2 Insights Dashboard

**Goal:** Add a 3-screen swipeable Insights section that derives financial summaries purely from transaction records — no budgets, categories, or AI assumptions.

**Completed:** Phases 27–30 (Data Layer, Navigation, Status Pane, Risks Pane). Phase 31 (Trends Pane) pending at v2.2 close.

## Current Milestone: v3.0 AI-Assisted Transaction Drafting

**Goal:** Integrate Gemini Nano (via Android AICore) as an opt-in drafting assistant so users can create transaction drafts from SMS, receipt images, or voice memos — with the AI suggesting a pre-filled form the user reviews and confirms. App remains 100% functional without AI.

**Target features:**

*AI Infrastructure:*
- `DeviceCapabilityManager`: checks AICore availability on first launch, caches `isAiAssistAvailable` Boolean in DataStore
- `GenAiClient` interface (domain) + `NanoAiClient` implementation (data/ai/)
- `TransactionDraft` domain model aligned to existing entity IDs (categoryId, accountId, peerContactId)
- `GenerateDraftFromTextUseCase`: nullable AI client injection via Hilt — returns failure when AI unavailable
- Dynamic prompt builder: injects user's live master data (categories by type, account names, peer names, tags, transaction types from registry) — extensible as new types are added

*User-facing flows:*
- SMS picker: select financial SMS → "AI Fill" → draft populated in existing AddEditTransactionDialog
- Receipt OCR: camera/gallery capture → unbundled ML Kit OCR → "AI Fill" → draft review
- Voice memo: offline SpeechRecognizer (EXTRA_PREFER_OFFLINE) → "AI Fill" → draft review

*Graceful degradation:*
- All "AI Fill" buttons hidden when AICore unavailable (Flow<Boolean> drives visibility)
- Runtime AI errors show Snackbar; form stays editable manually
- Manual entry always primary — AI never required

*Code standards:*
- New packages: `data/ai/`, `domain/ai/`, `ui/aidraft/`
- Hilt `AiModule` with conditional `GenAiClient?` provision
- Transaction types resolved from a centralized `TransactionType` registry (not hardcoded in prompts)

**Key constraints:**
- Zero APK bloat — AICore is system-managed (Snapdragon 8 Gen 3 NPU acceleration automatic)
- 100% offline, privacy-preserving (Gemini Nano runs in Private Compute Core)
- Min SDK 26, Clean Architecture / MVVM / Hilt / Room / DataStore patterns unchanged
- All existing features unchanged — AI is additive only

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

*Last updated: 2026-05-15 — Milestone v3.0 started*