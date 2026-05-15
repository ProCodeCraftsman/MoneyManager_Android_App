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

v3.0 AI-Assisted Transaction Drafting complete — all 5 phases (32–36) shipped. v3.1 Hybrid AI Backend in planning — extends AI layer from AICore-only to 3-tier: AICore, local Gemma 3 1B (user opt-in), or None.

## Previous Milestone: v3.0 AI-Assisted Transaction Drafting

**Goal:** Integrate Gemini Nano (via Android AICore) as an opt-in drafting assistant — SMS, receipt OCR, and voice memo flows all pre-fill AddEditTransactionDialog. App 100% functional without AI.

**Completed:** Phases 32–36 (Domain Foundation, Data AI, DI Wiring, Source Screens, Dialog Integration).

## Current Milestone: v3.1 Hybrid AI Backend

**Goal:** Extend the AI layer from AICore-only to a 3-tier system — AICore (preferred), Gemma 3 1B local model via MediaPipe (fallback for capable hardware, user opt-in download), and None (graceful degradation) — so devices like the Samsung Galaxy S24 Ultra without AICore can use AI transaction drafting.

**Target features:**

*Backend Detection & Routing:*
- 3-tier detection at startup: AICore (ML Kit API) → Local Model (≥6 GB RAM) → None
- `AiBackend` enum: `AICORE`, `LOCAL_MODEL`, `NONE`
- Extended PreferencesManager keys: `ai_backend`, `ai_availability`, `local_model_downloaded`, `local_model_path`, `user_opted_in_ai`
- Updated `DeviceCapabilityManager` — 3-tier detection, never uses PackageManager as AICore proxy

*Local Model Infrastructure:*
- `LocalModelAiClient` — wraps `com.google.mediapipe:tasks-genai:0.10.22`, NPU → GPU → CPU delegate cascade, temperature=0.0/topK=1 for deterministic JSON
- Lazy initialization: model loaded on first inference call, not at Hilt startup
- `close()` releases ~1.5 GB RAM — called on Activity.onStop(), onTrimMemory(CRITICAL), or 5-min idle

*Download Flow:*
- `ModelDownloadManager` — downloads ~529 MB Gemma 3 1B int4 .task file, stores in `filesDir/models/`, WiFi-only default
- Progress as `Flow<DownloadProgress>`, survives config changes/backgrounding
- User opt-in dialog: discloses 529 MB size + privacy assurance before any download begins
- Download progress via persistent notification or in-app indicator

*DI & Availability:*
- Updated `AiModule` — provides `GenAiClient?` based on cached backend + download state
- `AiModule` returns null (AI buttons hidden) when no backend available or model not yet downloaded

**Key constraints:**
- 100% offline inference — local model never touches network after download
- Zero APK bloat — model downloaded post-install; `tasks-genai` adds ~15 MB (acceptable)
- Min SDK 26, all existing Clean Architecture / MVVM / Hilt / Room / DataStore patterns unchanged
- All changes additive — existing manual entry and non-AI flows completely unaffected
- Explicit user opt-in for download — nothing auto-downloads

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

*Last updated: 2026-05-16 — Milestone v3.1 started*