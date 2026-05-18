---
phase: 40-user-facing-download-flow
plan: "01"
subsystem: ui-consent-dialog
tags:
  - android
  - compose
  - viewmodel
  - tdd
  - hybrid-ai
dependency_graph:
  requires:
    - "39-backend-detection-di (AiAvailabilityRepository.aiBackendTier Flow)"
    - "37-data-foundation (PreferencesManager.userOptedInAi, isLocalModelDownloaded)"
    - "37-data-foundation (ModelDownloadService.start companion)"
  provides:
    - "AiDownloadConsentDialog composable (40-03 wires into TransactionsScreen)"
    - "TransactionsViewModel.showDownloadConsentDialog StateFlow (40-03 collects)"
    - "TransactionsViewModel.onDownloadConsented() (40-03 passes as lambda)"
    - "TransactionsViewModel.onDownloadPromptSuppressed() (40-03 passes as lambda)"
    - "shouldShowDownloadConsent() pure function (tested in DownloadConsentStateTest)"
  affects:
    - "MoneyManager/app/src/main/java/com/moneymanager/app/ui/transactions/TransactionsViewModel.kt"
tech_stack:
  added: []
  patterns:
    - "Pure function extracted from ViewModel for unit-testability (shouldShowDownloadConsent)"
    - "MutableStateFlow for in-memory session flag (not DataStore)"
    - "combine() of 4 flows stateIn(WhileSubscribed(5000)) for dialog trigger"
    - "M3 AlertDialog with icon/title/text/confirmButton/dismissButton slots"
key_files:
  created:
    - "MoneyManager/app/src/main/java/com/moneymanager/app/ui/transactions/components/AiDownloadConsentDialog.kt"
    - "MoneyManager/app/src/test/java/com/moneymanager/data/ai/DownloadConsentStateTest.kt"
  modified:
    - "MoneyManager/app/src/main/java/com/moneymanager/app/ui/transactions/TransactionsViewModel.kt"
decisions:
  - "shouldShowDownloadConsent extracted as package-level internal function to allow unit testing without AndroidViewModel instantiation"
  - "AiAvailabilityRepository added to TransactionsViewModel constructor as new Hilt-injected dependency"
  - "_isDownloadPromptSuppressedForSession kept as MutableStateFlow (not plain var) so combine() reacts reactively — avoids Pitfall 1 (dialog reappears on recomposition)"
  - "setUserOptedInAi(true) called before ModelDownloadService.start() per T-40-01 threat mitigation"
metrics:
  duration_seconds: 451
  completed_date: "2026-05-18"
  tasks_completed: 2
  files_created: 2
  files_modified: 1
---

# Phase 40 Plan 01: Consent Dialog + ViewModel State Summary

**One-liner:** M3 AlertDialog consent composable with exact UI-SPEC copy and TransactionsViewModel StateFlow combining 4 inputs to drive HYBRID-05 dialog visibility.

---

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | DownloadConsentStateTest — RED phase | b6f7c41 | DownloadConsentStateTest.kt (created) |
| 2 | AiDownloadConsentDialog + TransactionsViewModel state — GREEN phase | 4017756 | AiDownloadConsentDialog.kt (created), TransactionsViewModel.kt (modified) |

---

## What Was Built

### AiDownloadConsentDialog.kt
`@Composable fun AiDownloadConsentDialog(onDownload: () -> Unit, onMaybeLater: () -> Unit)` — M3 AlertDialog per UI-SPEC Component Inventory section 1. Exact copy strings from the Copywriting Contract:
- Title: "Enable On-Device AI?"
- Privacy line: "All AI processing happens entirely on your device. Your financial data is never sent to any server."
- Size line: "Requires a one-time 529 MB download. Wi-Fi recommended."
- Confirm button: "Download (529 MB)"
- Dismiss button: "Maybe Later"
- `onDismissRequest = onMaybeLater` (scrim/back treated as Maybe Later per T-40-02)
- No hardcoded `Color()` values — all `MaterialTheme.colorScheme` tokens

### TransactionsViewModel.kt additions (strictly additive)
- `AiAvailabilityRepository` added to `@Inject constructor`
- `_isDownloadPromptSuppressedForSession: MutableStateFlow<Boolean>` (in-memory session flag)
- `showDownloadConsentDialog: StateFlow<Boolean>` — `combine()` of `aiBackendTier`, `isLocalModelDownloaded`, `userOptedInAi`, `_isDownloadPromptSuppressedForSession` via `shouldShowDownloadConsent()`
- `onDownloadConsented()`: `setUserOptedInAi(true)` then `ModelDownloadService.start()`
- `onDownloadPromptSuppressed()`: sets `_isDownloadPromptSuppressedForSession.value = true` only

### shouldShowDownloadConsent() pure function
`internal fun shouldShowDownloadConsent(tier, downloaded, optedIn, suppressed): Boolean` — package-level in `TransactionsViewModel.kt`, testable without AndroidViewModel. Condition: `tier == AiBackend.LOCAL_MODEL && !downloaded && !optedIn && !suppressed`.

### DownloadConsentStateTest.kt
7 unit tests covering all trigger conditions. RED confirmed (compile error on missing symbol), GREEN confirmed (BUILD SUCCESSFUL).

---

## TDD Gate Compliance

| Gate | Commit | Status |
|------|--------|--------|
| RED (test commit) | b6f7c41 | PASS — tests failed with "Unresolved reference: shouldShowDownloadConsent" |
| GREEN (feat commit) | 4017756 | PASS — BUILD SUCCESSFUL, all 7 tests pass |

---

## Verification Checklist

- [x] `grep -c "Maybe Later" AiDownloadConsentDialog.kt` = 3 (>= 1)
- [x] `grep -c "529 MB" AiDownloadConsentDialog.kt` = 3 (>= 2)
- [x] `grep -c "on your device" AiDownloadConsentDialog.kt` = 1 (>= 1)
- [x] `grep -c "showDownloadConsentDialog" TransactionsViewModel.kt` = 1 (>= 1)
- [x] `grep -c "setUserOptedInAi" TransactionsViewModel.kt` = 2 (>= 1)
- [x] `grep -c "Color(" AiDownloadConsentDialog.kt` = 0 (no hardcoded colors)
- [x] All 7 DownloadConsentStateTest tests pass

---

## Deviations from Plan

None — plan executed exactly as written.

---

## Known Stubs

None. The dialog composable accepts callbacks (`onDownload`, `onMaybeLater`) — these are wired in Plan 40-03 (TransactionsScreen integration). The StateFlow is ready; the consumer is added in Plan 40-03.

---

## Threat Surface Scan

No new network endpoints, auth paths, or file access patterns introduced. Threat mitigations per plan:
- T-40-01 (Repudiation): `setUserOptedInAi(true)` called before `ModelDownloadService.start()` — implemented as specified.
- T-40-02 (Elevation of Privilege): `onDismissRequest = onMaybeLater` — scrim/back is Maybe Later; no download initiated.

---

## Self-Check: PASSED

- `AiDownloadConsentDialog.kt` — FOUND
- `DownloadConsentStateTest.kt` — FOUND
- `TransactionsViewModel.kt` modified — FOUND
- Commit b6f7c41 — FOUND (test RED)
- Commit 4017756 — FOUND (feat GREEN)
