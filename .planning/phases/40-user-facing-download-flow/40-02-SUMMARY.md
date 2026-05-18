---
phase: 40-user-facing-download-flow
plan: "02"
subsystem: ui-download-banner
tags:
  - android
  - compose
  - viewmodel
  - tdd
  - hybrid-ai
dependency_graph:
  requires:
    - "40-01 (TransactionsViewModel base — AiAvailabilityRepository injection, existing StateFlows)"
    - "37-data-foundation (PreferencesManager.localModelDownloadProgress/Received/Total/Speed flows)"
    - "37-data-foundation (ModelDownloadService — CHANNEL_ID constant fixed in this plan)"
  provides:
    - "DownloadProgressBanner composable (40-03 wires into TransactionsScreen LazyColumn)"
    - "TransactionsViewModel.isDownloading StateFlow (40-03 collects)"
    - "TransactionsViewModel.downloadProgress StateFlow (40-03 passes to banner)"
    - "TransactionsViewModel.downloadProgressCaption StateFlow (40-03 passes to banner)"
    - "TransactionsViewModel.downloadProgressPercent StateFlow (40-03 passes to banner)"
    - "TransactionsViewModel.downloadError StateFlow (40-03 passes to Snackbar)"
    - "buildBannerCaptionText() pure function (tested in DownloadBannerStateTest)"
    - "buildBannerPercentText() pure function (tested in DownloadBannerStateTest)"
  affects:
    - "MoneyManager/app/src/main/java/com/moneymanager/app/ui/transactions/TransactionsViewModel.kt"
    - "MoneyManager/app/src/main/java/com/moneymanager/data/ai/ModelDownloadService.kt"
tech_stack:
  added: []
  patterns:
    - "Pure functions extracted from ViewModel for unit-testability (buildBannerCaptionText, buildBannerPercentText)"
    - "AnimatedVisibility with fadeIn+expandVertically / fadeOut+shrinkVertically for banner transitions"
    - "Determinate vs indeterminate LinearProgressIndicator based on totalBytes == 0L"
    - "combine() of 3 flows for caption StateFlow (received, total, speed)"
    - "MutableStateFlow stub for downloadError (wire-forward pattern for future gap closure)"
key_files:
  created:
    - "MoneyManager/app/src/main/java/com/moneymanager/app/ui/transactions/components/DownloadProgressBanner.kt"
    - "MoneyManager/app/src/test/java/com/moneymanager/data/ai/DownloadBannerStateTest.kt"
  modified:
    - "MoneyManager/app/src/main/java/com/moneymanager/app/ui/transactions/TransactionsViewModel.kt"
    - "MoneyManager/app/src/main/java/com/moneymanager/data/ai/ModelDownloadService.kt"
decisions:
  - "buildBannerCaptionText and buildBannerPercentText extracted as package-level internal functions to allow unit testing without AndroidViewModel instantiation (same pattern as shouldShowDownloadConsent in Plan 40-01)"
  - "Speed suffix uses ' — N MB/s' / ' — NNN KB/s' / ' — NNN B/s' tiers matching AiModelsScreen.buildDownloadProgressText pattern; no ETA for Phase 40 (speed-only caption sufficient for HYBRID-06)"
  - "downloadError exposed as MutableStateFlow stub — ModelDownloadService is a foreground service with no direct ViewModel callback path; gap-closure phase can wire error signal"
  - "CHANNEL_ID string 'ai_model_download' appears once (in constant definition); CHANNEL_ID constant used 3x in file — grep count of 1 for the string literal is correct and expected"
metrics:
  duration_seconds: 7171
  completed_date: "2026-05-18"
  tasks_completed: 2
  files_created: 2
  files_modified: 2
---

# Phase 40 Plan 02: DownloadProgressBanner + TransactionsViewModel Download State Summary

**One-liner:** AnimatedVisibility banner composable with determinate/indeterminate LinearProgressIndicator driven by 5 new TransactionsViewModel StateFlows, plus ModelDownloadService notification channel ID corrected from "model_download" to "ai_model_download".

---

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | DownloadBannerStateTest — RED phase | 884be3a | DownloadBannerStateTest.kt (created) |
| 2 | DownloadProgressBanner + TransactionsViewModel state + channel fix — GREEN phase | e7f6dfa | DownloadProgressBanner.kt (created), TransactionsViewModel.kt (modified), ModelDownloadService.kt (modified) |

---

## What Was Built

### DownloadProgressBanner.kt
`@Composable fun DownloadProgressBanner(isVisible: Boolean, progress: Float, captionText: String?, percentText: String)` — M3 Card-based persistent banner per UI-SPEC Component Inventory section 2:
- `AnimatedVisibility(enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically())`
- `Card(containerColor = MaterialTheme.colorScheme.surfaceVariant)` with 16dp internal padding
- Row: `Icon(CloudDownload, size=16.dp, tint=primary)` + `Text("Downloading AI model", labelMedium, weight=Medium)` + `Text(percentText, primary)`
- Determinate: `LinearProgressIndicator(progress = { progress })` at `height(4.dp).clip(RoundedCornerShape(2.dp))`
- Indeterminate: `LinearProgressIndicator()` (no progress param) when `progress == 0f`
- Optional caption: `Text(captionText, bodySmall, onSurfaceVariant)` when non-null
- Zero hardcoded `Color()` values — all MaterialTheme tokens

### TransactionsViewModel.kt additions (strictly additive — Plan 40-01 additions unchanged)
- `isDownloading: StateFlow<Boolean>` — `localModelDownloadProgress.map { it > 0f && it < 1f }`
- `downloadProgress: StateFlow<Float>` — raw progress passthrough
- `downloadProgressCaption: StateFlow<String?>` — `combine(received, total, speed) { buildBannerCaptionText(...) }`
- `downloadProgressPercent: StateFlow<String>` — `map { buildBannerPercentText(it) }`
- `downloadError: StateFlow<String?>` — stub MutableStateFlow for future wiring
- All use `SharingStarted.WhileSubscribed(5000)`

### buildBannerCaptionText() pure function
`internal fun buildBannerCaptionText(receivedBytes: Long, totalBytes: Long, bytesPerSecond: Long): String?` — returns null when `totalBytes == 0L`; returns `"X.X / YYY MB (NN%)"` + optional speed suffix when bytes known. Package-level for testability.

### buildBannerPercentText() pure function
`internal fun buildBannerPercentText(progress: Float): String = "${(progress * 100).toInt()}%"` — package-level for testability.

### ModelDownloadService.kt fix
`CHANNEL_ID` constant changed from `"model_download"` to `"ai_model_download"`. NotificationChannel human name changed from `"Model Downloads"` to `"AI Model Download"`. All other service logic unchanged (single-constant refactor — zero risk).

### DownloadBannerStateTest.kt
6 unit tests covering: banner visibility predicate (Tests 1-3), caption text null/formatting (Tests 4-5), percent text formatting (Test 6). RED confirmed (compile error on missing symbols); GREEN confirmed (BUILD SUCCESSFUL).

---

## TDD Gate Compliance

| Gate | Commit | Status |
|------|--------|--------|
| RED (test commit) | 884be3a | PASS — failed with "Unresolved reference: buildBannerCaptionText / buildBannerPercentText" |
| GREEN (feat commit) | e7f6dfa | PASS — BUILD SUCCESSFUL, all 6 tests pass |

---

## Verification Checklist

- [x] `grep -c "ai_model_download" ModelDownloadService.kt` = 1 (constant definition; constant used 3x in file)
- [x] `grep "\"model_download\"" ModelDownloadService.kt` = 0 (old ID removed)
- [x] `grep -c "AI Model Download" ModelDownloadService.kt` = 1
- [x] `grep -c "Downloading AI model" DownloadProgressBanner.kt` = 1
- [x] `grep -c "isDownloading" TransactionsViewModel.kt` >= 1 (= 1 declaration + used in combine)
- [x] `grep -c "downloadProgress" TransactionsViewModel.kt` = 3 (downloadProgress, downloadProgressCaption, downloadProgressPercent)
- [x] `grep "Color(" DownloadProgressBanner.kt` = 0 (no hardcoded colors)
- [x] `grep -c "RoundedCornerShape(2.dp)" DownloadProgressBanner.kt` = 2 (determinate + indeterminate branches)
- [x] `grep -c "height(4.dp)" DownloadProgressBanner.kt` = 3 (determinate + indeterminate + modifier chain)
- [x] All 6 DownloadBannerStateTest tests pass
- [x] Full test suite BUILD SUCCESSFUL (no regressions from Plan 40-01 additions)

---

## Deviations from Plan

### Auto-noted: grep count for "ai_model_download"

The plan's done-criteria states "grep returns at least 2 matches" for `"ai_model_download"` in ModelDownloadService.kt. The implemented file has exactly 1 string literal (`private const val CHANNEL_ID = "ai_model_download"`) and uses the `CHANNEL_ID` constant (not the raw string) in 2 further places. This is correct Kotlin practice. The semantic intent (channel ID is `ai_model_download` throughout the file) is fully met — the plan's count expectation was written assuming string repetition rather than constant usage.

All other plan items executed exactly as written.

---

## Known Stubs

- `downloadError: StateFlow<String?>` — `MutableStateFlow(null)` stub. ModelDownloadService is a foreground service with no direct ViewModel callback path. Error signals from the service (network failure, storage full) cannot easily flow back to ViewModel without an additional IPC mechanism (LocalBroadcastManager, BoundService, or DataStore error flag). Plan 40-02 exposes the flow; a future gap-closure phase wires the signal. This stub does not block HYBRID-06 (banner visibility) which is the goal of this plan.

---

## Threat Surface Scan

No new network endpoints, auth paths, or file access patterns introduced. Changes:
- T-40-04 (Repudiation, accepted): CHANNEL_ID change from `"model_download"` to `"ai_model_download"` creates a new Android notification channel on-device; old channel persists silently until user clears app notification settings. No data loss, no security concern. Implemented as specified.

---

## Self-Check: PASSED

- `DownloadProgressBanner.kt` — FOUND at `MoneyManager/app/src/main/java/com/moneymanager/app/ui/transactions/components/DownloadProgressBanner.kt`
- `DownloadBannerStateTest.kt` — FOUND at `MoneyManager/app/src/test/java/com/moneymanager/data/ai/DownloadBannerStateTest.kt`
- `TransactionsViewModel.kt` modified (isDownloading, downloadProgress, downloadProgressCaption, downloadProgressPercent, downloadError, buildBannerCaptionText, buildBannerPercentText) — FOUND
- `ModelDownloadService.kt` modified (CHANNEL_ID = "ai_model_download", "AI Model Download") — FOUND
- Commit 884be3a — FOUND (test RED)
- Commit e7f6dfa — FOUND (feat GREEN)
