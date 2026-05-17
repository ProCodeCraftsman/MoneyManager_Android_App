# Phase 40: User-Facing Download Flow — Research

**Researched:** 2026-05-18
**Domain:** Android Jetpack Compose UI — opt-in download dialog, in-app download progress banner, foreground service integration
**Confidence:** HIGH

---

## Summary

Phase 40 is the final phase of the v3.1 Hybrid AI Backend milestone. All infrastructure built in Phases 37–39 is already in place: `ModelDownloadService` (foreground service), `PreferencesManager` keys (`user_opted_in_ai`, `local_model_download_progress`, `ai_backend_tier`, `isLocalModelDownloaded`), `AiAvailabilityRepository` (reactive `Flow<Boolean>` keyed on tier + download state), and `AiClientRouter`. Phase 40 adds only the user-facing consent dialog and the in-app progress banner — no new data layer work is required.

The consent dialog (`AiDownloadConsentDialog`) triggers from `MoneyManagerApp` startup via a new check in the `TransactionsScreen` composable (or a dedicated ViewModel collecting `aiBackendTier == LOCAL_DOWNLOADABLE && !userOptedInAi && !sessionSuppressed`). The in-app progress banner (`DownloadProgressBanner`) sits at the top of the `TransactionsScreen` LazyColumn and collects from `PreferencesManager.localModelDownloadProgress` and related speed/ETA keys that are already written by `ModelDownloadService`. When `isLocalModelDownloaded` becomes `true`, `AiAvailabilityRepository.isAiAvailable` emits `true`, and the existing `AnimatedVisibility` FAB group in `TransactionsScreen` appears automatically with no new FAB UI needed.

The critical integration gap is that `AiModule.providePreferredGenAiClient` is `@Singleton` — it reads `isLocalModelDownloaded` exactly once at Hilt graph construction time. After download completes, the singleton is stale. Phase 40 must address this either by routing through `AiClientRouter` (which already checks `modelManager.isModelDownloaded()` at call time) or by triggering a process restart. `AiClientRouter` is already the canonical `GenAiClient` binding and does a live `isModelDownloaded()` check — so the FAB auto-reveal works via `AiAvailabilityRepository` without needing a restart, and inference calls work because they route through `AiClientRouter`.

**Primary recommendation:** New files are `AiDownloadConsentDialog.kt` (composable) and a small `DownloadProgressBanner` composable. The ViewModel concern is lightweight — extend `TransactionsViewModel` (or use `AiDraftViewModel` which is already in scope on the TransactionsScreen composable block in NavHost) to expose download-phase state. The session-suppress flag lives in the ViewModel as a plain `Boolean` (not persisted).

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Opt-in dialog display logic | Frontend (Composable / ViewModel) | — | UI state: tier == LOCAL_DOWNLOADABLE, not opted in, not session-suppressed |
| Session-suppress flag | Frontend (ViewModel in-memory) | — | Must not survive process death; ViewModel scope is correct |
| `user_opted_in_ai` persistence | Data (PreferencesManager) | — | Already has key + setter; no new work |
| Download initiation | Data (ModelDownloadService foreground service) | — | Already wired; Phase 40 calls `ModelDownloadService.start()` |
| Download progress reporting | Data (ModelDownloadService → PreferencesManager) | — | Already writes 4 progress keys on each `onProgress` callback |
| In-app progress banner | Frontend (TransactionsScreen LazyColumn item) | — | Collects from PreferencesManager flows via ViewModel |
| System notification progress | Data (ModelDownloadService notification) | — | Already implemented; Phase 40 may refine notification channel ID to match UI-SPEC |
| AI FAB auto-reveal on completion | Frontend (existing AnimatedVisibility) | — | AiAvailabilityRepository already reactive; no new FAB code needed |
| GenAiClient availability after download | Data (AiClientRouter live check) | — | Singleton concern mitigated; router checks isModelDownloaded() live |

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| HYBRID-05 | User sees opt-in dialog disclosing 529 MB size + privacy assurance before any download; "Maybe Later" suppresses for session and re-prompts next launch; "Download (529 MB)" initiates download | `user_opted_in_ai` key exists in PreferencesManager; no download byte is sent until `ModelDownloadService.start()` is called; session flag is ViewModel in-memory Boolean |
| HYBRID-06 | User sees download progress (persistent notification or in-app indicator) while download runs; app remains fully usable; AI features become available automatically on completion | `ModelDownloadService` already writes progress keys every `onProgress` callback; `AiAvailabilityRepository.isAiAvailable` is reactive and emits `true` when `isLocalModelDownloaded` flips; existing AnimatedVisibility FAB group handles reveal |
</phase_requirements>

---

## Standard Stack

### Core (all already on classpath — no new Gradle lines needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.compose.material3:material3` | BOM 2024.12.01 | AlertDialog, Card, LinearProgressIndicator, AnimatedVisibility | Already used throughout; M3 AlertDialog is the UI-SPEC prescribed component [VERIFIED: codebase grep] |
| `androidx.datastore:datastore-preferences` | 1.1.1 | `user_opted_in_ai` read/write | Already in `build.gradle.kts`; key + Flow already in PreferencesManager [VERIFIED: codebase read] |
| `androidx.work:work-runtime-ktx` | 2.11.2 | WorkManager already on classpath — NOT used for download (foreground service is the actual mechanism) | On classpath from prior phases [VERIFIED: `build.gradle.kts` line 140] |
| `com.moneymanager.data.ai.ModelDownloadService` | — | Existing foreground service that downloads, writes progress, fires notification | Already registered in AndroidManifest; `foregroundServiceType="dataSync"` [VERIFIED: AndroidManifest.xml line 72] |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.compose.animation` | BOM 2024.12.01 | `AnimatedVisibility`, `fadeIn`, `expandVertically` for banner enter/exit | Already imported in TransactionsScreen.kt |
| `kotlinx.coroutines.flow` | 1.9.0 (test) | Collect `localModelDownloadProgress`, `isLocalModelDownloaded` as StateFlow | Already used throughout ViewModels |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Foreground service (current) | WorkManager DownloadWorker | WorkManager would require refactoring existing `ModelDownloadService`; UI-SPEC accepts foreground service; REQUIREMENTS.md says "foreground service OR WorkManager" — foreground service is already done [ASSUMED: WorkManager approach not investigated because existing service is sufficient] |
| In-app banner only | System notification only | In-app banner satisfies HYBRID-06 without POST_NOTIFICATIONS permission; system notification is bonus (already implemented by `ModelDownloadService`) [VERIFIED: UI-SPEC section 2 rationale] |

**No new Gradle dependencies required for Phase 40.** [VERIFIED: build.gradle.kts — all required libraries present]

---

## Architecture Patterns

### System Architecture Diagram

```
App Launch
    │
    ▼
MoneyManagerApp.onCreate()
    │ async on Dispatchers.IO
    ▼
DeviceCapabilityManager.checkAndCacheAvailability()
    │ writes aiBackendTier = "local_model", aiAvailabilityStatus = "LOCAL_DOWNLOADABLE"
    ▼
PreferencesManager.aiBackendTier Flow emits "local_model"
    │
    ├──► AiAvailabilityRepository.isAiAvailable (combines tier + isLocalModelDownloaded)
    │        emits false (not yet downloaded) → TransactionsScreen FAB hidden
    │
    └──► TransactionsScreen composable collects aiBackendTier + userOptedInAi + sessionSuppressed
             │ tier == LOCAL_DOWNLOADABLE && !optedIn && !sessionSuppressed
             ▼
         AiDownloadConsentDialog shown
             │
             ├─ "Maybe Later" → sessionSuppressed = true (ViewModel only), dialog dismissed
             │
             └─ "Download (529 MB)" → setUserOptedInAi(true) + ModelDownloadService.start()
                     │
                     ▼
                 ModelDownloadService (foreground service)
                     │ writes progress to PreferencesManager every onProgress callback
                     │ fires ongoing system notification
                     │
                     ▼
                 TransactionsScreen LazyColumn item 0
                     ├─ DownloadProgressBanner (AnimatedVisibility, visible when isDownloading)
                     │   └─ collects localModelDownloadProgress, speed, ETA from ViewModel
                     │
                     └─ On 1.0f progress:
                             setLocalModelDownloaded(true)
                             setAiBackendTier("local_model") stays
                             │
                             ▼
                         AiAvailabilityRepository.isAiAvailable emits true
                             │
                             ▼
                         TransactionsScreen FAB AnimatedVisibility appears (existing logic)
                         DownloadProgressBanner hides (existing logic)
```

### Recommended Project Structure (new files only)

```
MoneyManager/app/src/main/java/
└── com/moneymanager/
    └── app/
        └── ui/
            └── transactions/
                ├── TransactionsScreen.kt      (MODIFY — add consent dialog + banner)
                ├── TransactionsViewModel.kt   (MODIFY — add download state + dialog trigger)
                └── components/
                    ├── AiDownloadConsentDialog.kt   (NEW)
                    └── DownloadProgressBanner.kt    (NEW)
```

Alternative placement: `ui/components/` (shared components folder). Either works; scoping under `transactions/components/` is slightly more precise since both components are only used in TransactionsScreen.

### Pattern 1: AiDownloadConsentDialog (AlertDialog)

```kotlin
// Source: UI-SPEC section 1; pattern from AiModelsScreen.kt HuggingFaceLoginDialog
@Composable
fun AiDownloadConsentDialog(
    onDownload: () -> Unit,
    onMaybeLater: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onMaybeLater,   // scrim/back = "Maybe Later"
        icon = {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = "AI feature",
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Enable On-Device AI?") },
        text = {
            Column {
                Text(
                    "All AI processing happens entirely on your device. " +
                    "Your financial data is never sent to any server.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Requires a one-time 529 MB download. Wi-Fi recommended.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(onClick = onDownload) { Text("Download (529 MB)") }
        },
        dismissButton = {
            TextButton(onClick = onMaybeLater) { Text("Maybe Later") }
        },
    )
}
```

### Pattern 2: DownloadProgressBanner (Card in LazyColumn)

```kotlin
// Source: UI-SPEC section 2; pattern from AiModelsScreen.kt BackendStatusCard
@Composable
fun DownloadProgressBanner(
    isVisible: Boolean,
    progress: Float,      // 0f–1f; 0f = indeterminate
    captionText: String?, // null when totalBytes unknown
    percentText: String,  // "47%"
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Downloading AI model",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        percentText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                } else {
                    LinearProgressIndicator(   // indeterminate
                        modifier = Modifier.fillMaxWidth().height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
                if (captionText != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        captionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
```

### Pattern 3: Caption text builder (reuse from AiModelsScreen)

```kotlin
// Source: AiModelsScreen.kt buildDownloadProgressText() — copy or extract to shared util
fun buildDownloadProgressText(
    receivedBytes: Long,
    totalBytes: Long,
    bytesPerSecond: Long,
    remainingMs: Long,
): String {
    val receivedMb = "%.1f".format(receivedBytes / 1_000_000.0)
    val totalMb = "%.0f".format(totalBytes / 1_000_000.0)
    val pct = if (totalBytes > 0) (receivedBytes * 100 / totalBytes).toInt() else 0
    // speed and ETA formatting...
    return "$receivedMb / $totalMb MB ($pct%)"  // + optional speed/ETA
}
```

### Pattern 4: Session-suppress flag in TransactionsViewModel

```kotlin
// NOT a DataStore key — lives in ViewModel memory only
private var isDownloadPromptSuppressedForSession = false  // plain var, no StateFlow needed

fun suppressDownloadPromptForSession() {
    isDownloadPromptSuppressedForSession = true
}
```

Exposed as a `StateFlow<Boolean>` only if TransactionsScreen needs to reactively re-check the dialog trigger.

### Anti-Patterns to Avoid

- **Re-showing dialog after session suppress:** The `user_opted_in_ai` key is `false` throughout the session if user taps "Maybe Later". The session-suppress flag must be checked BEFORE showing the dialog. Store it as `var` in ViewModel — do NOT persist to DataStore on "Maybe Later" (only on "Download" tap).
- **Showing dialog when `isLocalModelDownloaded == true`:** DeviceCapabilityManager writes `LOCAL_READY` (not `LOCAL_DOWNLOADABLE`) when the model is already downloaded. The dialog condition must check `aiAvailabilityStatus == "LOCAL_DOWNLOADABLE"` (or equivalently `tier == LOCAL_MODEL && !isLocalModelDownloaded`).
- **Calling `ModelDownloadService.start()` before `setUserOptedInAi(true)`:** Set the preference first, then start the service. If the service starts but the preference write fails, the next launch will re-show the consent dialog (recoverable). The reverse (opted-in flag set but no download started) is also acceptable — the user re-sees the dialog on next launch.
- **Using WorkManager here:** The existing `ModelDownloadService` is a proper foreground service already wired in the manifest. Do not introduce a WorkManager `DownloadWorker` — it would duplicate the download infrastructure.
- **New notification channel ID:** UI-SPEC says `"ai_model_download"` but `ModelDownloadService` uses `"model_download"`. The planner must reconcile this — updating the channel ID in `ModelDownloadService` is the right fix (additive; old channel persists until user clears notifications for the app).
- **Hardcoded colors:** No `Color()` values. All color tokens from `MaterialTheme.colorScheme`.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Download progress | Custom HTTP client progress tracking | Existing `ModelDownloadService.onProgress` → `PreferencesManager` keys | Already writes received, total, speed keys every callback |
| Reactive FAB reveal | Manual state observation after download | `AiAvailabilityRepository.isAiAvailable` Flow | Already combines `tier + isLocalModelDownloaded`; emits `true` when model is ready |
| Persistent storage of opted-in state | Custom SharedPreferences | `PreferencesManager.setUserOptedInAi(true)` | Key already declared and wired |
| Session suppress persistence | DataStore key for session state | ViewModel in-memory `Boolean` | Session state must not survive process death (HYBRID-05 "re-prompts on next launch") |
| Progress caption formatting | Custom string builder | Copy/extract `buildDownloadProgressText()` from `AiModelsScreen.kt` | Exact pattern already tested and used |
| System notification | Custom notification implementation | Existing `ModelDownloadService.updateNotification()` | Already fires on every `onProgress` callback |

---

## Common Pitfalls

### Pitfall 1: Dialog fires on every recomposition

**What goes wrong:** `AiDownloadConsentDialog` shown state derived from `aiBackendTier Flow` re-evaluates on every recomposition, causing the dialog to reappear after the session-suppress flag is set.

**Why it happens:** If the show condition is `tier == LOCAL_DOWNLOADABLE && !userOptedInAi && !sessionSuppressed`, and `sessionSuppressed` is plain ViewModel `var` (not a StateFlow), the composable won't re-collect it.

**How to avoid:** Expose session suppress as a `StateFlow<Boolean>` in the ViewModel so the composable can reactively read it. Alternatively, combine all three conditions into a single `StateFlow<Boolean> showConsentDialog` in the ViewModel.

**Warning signs:** Dialog reappears immediately after "Maybe Later" is tapped.

### Pitfall 2: AiModule singleton is stale after download

**What goes wrong:** `AiModule.providePreferredGenAiClient` reads `isLocalModelDownloaded` at Hilt graph construction time (via `runBlocking`). After `ModelDownloadService` sets `isLocalModelDownloaded = true`, the singleton `@Named("preferredClient") GenAiClient?` is still `null`.

**Why it happens:** Hilt `@Singleton` providers execute once at DI graph construction. The `providePreferredGenAiClient` provider is not reactive.

**How to avoid — already handled:** `AiModule.provideGenAiClient(router: AiClientRouter)` returns `router` (the `AiClientRouter`), NOT the static `providePreferredGenAiClient`. The `AiClientRouter` calls `deviceCapabilityManager.resolveCurrentTier()` and `modelManager.isModelDownloaded()` on every invocation — it is reactive. Confirm at planning time that all use sites call `AiClientRouter` (the non-named `GenAiClient` binding), not `@Named("preferredClient")`.

**Warning signs:** AI buttons appear but "AI Fill" shows "AI unavailable" error after download.

### Pitfall 3: Notification channel ID mismatch

**What goes wrong:** `ModelDownloadService` uses channel ID `"model_download"`. UI-SPEC prescribes `"ai_model_download"`. If Phase 40 creates a new channel without updating the service, the existing notification uses the old channel.

**Why it happens:** UI-SPEC was written against the REQUIREMENTS.md spec, not the actual ModelDownloadService code.

**How to avoid:** Update `ModelDownloadService.CHANNEL_ID` constant from `"model_download"` to `"ai_model_download"` AND update the human-readable channel name to `"AI Model Download"` (IMPORTANCE_LOW, no sound — already correct). This is a one-line constant change.

**Warning signs:** Notification appears in wrong channel in device notification settings.

### Pitfall 4: Banner blocks user interaction when downloading in background

**What goes wrong:** If the banner is implemented as a persistent overlay (Box with Alignment.Top) rather than a LazyColumn item, it intercepts touch events for the transactions list.

**Why it happens:** Composing a sticky overlay rather than using the established LazyColumn first-item pattern.

**How to avoid:** Implement `DownloadProgressBanner` as the `item {}` call at index 0 in the `TransactionsScreen` LazyColumn content — it scrolls away naturally. UI-SPEC section 2 explicitly prescribes "first item in the LazyColumn (sticky=false, scrolls away)".

**Warning signs:** Users cannot tap transactions while banner is visible.

### Pitfall 5: DeviceCapabilityManager called again on dialog trigger

**What goes wrong:** Re-running `checkAndCacheAvailability()` on dialog show re-checks AICore availability, which blocks the UI thread or causes double detection.

**Why it happens:** Over-eagerness to refresh state before showing dialog.

**How to avoid:** Do NOT call `DeviceCapabilityManager` in Phase 40. The dialog trigger reads cached `aiBackendTier` from PreferencesManager — it never re-detects. Detection already happens in `MoneyManagerApp.onCreate()`.

### Pitfall 6: `user_opted_in_ai` written incorrectly — "Maybe Later" persists opt-out

**What goes wrong:** Setting `userOptedInAi = false` explicitly on "Maybe Later" — this is redundant (default is false) but harmless. However, some implementations set `userOptedInAi = true` on "Maybe Later" to suppress future prompts — this permanently suppresses consent.

**Why it happens:** Confusion about the semantics: `user_opted_in_ai` = "user said YES to download". "Maybe Later" means nothing is written; the session flag handles the session-level suppress.

**How to avoid:** On "Maybe Later": set in-memory session flag only. On "Download": call `setUserOptedInAi(true)` + `ModelDownloadService.start()`. Never write `false` explicitly on "Maybe Later".

---

## Code Examples

### TransactionsViewModel — dialog trigger state

```kotlin
// Source: verified from AiDraftViewModel.kt + PreferencesManager + AiAvailabilityRepository patterns
// New properties to add to TransactionsViewModel:

private val _isDownloadPromptSuppressedForSession = MutableStateFlow(false)

val showDownloadConsentDialog: StateFlow<Boolean> = combine(
    aiAvailabilityRepository.aiBackendTier,         // AiBackend enum
    preferencesManager.userOptedInAi,               // Flow<Boolean> — already exists
    preferencesManager.isLocalModelDownloaded,      // Flow<Boolean> — already exists
    _isDownloadPromptSuppressedForSession,           // in-memory session flag
) { tier, optedIn, downloaded, suppressed ->
    tier == AiBackend.LOCAL_MODEL && !downloaded && !optedIn && !suppressed
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

val isDownloading: StateFlow<Boolean> =
    preferencesManager.localModelDownloadProgress.map { it > 0f && it < 1f }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

fun onDownloadConsented() {
    viewModelScope.launch {
        preferencesManager.setUserOptedInAi(true)
        ModelDownloadService.start(context, modelName)  // use selectedLocalModel name
    }
}

fun onDownloadPromptSuppressed() {
    _isDownloadPromptSuppressedForSession.value = true
}
```

**Note:** `TransactionsViewModel` currently does not inject `AiAvailabilityRepository` or `PreferencesManager.userOptedInAi`. These injections must be added.

### TransactionsScreen — dialog and banner integration

```kotlin
// Source: TransactionsScreen.kt existing pattern (lines 463-503 for FAB, lines 514+ for content)
val showDownloadConsent by viewModel.showDownloadConsentDialog.collectAsState()
val isDownloading by viewModel.isDownloading.collectAsState()
val downloadProgress by viewModel.downloadProgress.collectAsState()

if (showDownloadConsent) {
    AiDownloadConsentDialog(
        onDownload = { viewModel.onDownloadConsented() },
        onMaybeLater = { viewModel.onDownloadPromptSuppressed() },
    )
}

// Inside LazyColumn content, as first item before date-grouped transactions:
item(key = "download_banner") {
    DownloadProgressBanner(
        isVisible = isDownloading,
        progress = downloadProgress,
        captionText = /* built from received/total/speed/ETA */,
        percentText = "${(downloadProgress * 100).toInt()}%",
    )
}
```

### Notification channel update (ModelDownloadService)

```kotlin
// Source: ModelDownloadService.kt line 39 — change constant only
private const val CHANNEL_ID = "ai_model_download"   // was "model_download"
// Line 208 human name:
NotificationChannel(CHANNEL_ID, "AI Model Download", NotificationManager.IMPORTANCE_LOW)
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| WorkManager DownloadWorker | Foreground Service (`ModelDownloadService`) | Phase 37 implementation | WorkManager is on classpath but not used for download; foreground service gives tighter progress control |
| Static `GenAiClient?` singleton | `AiClientRouter` live-routing | Phase 39 | Router checks `isModelDownloaded()` on every call; singleton `@Named("preferredClient")` exists but is not the bound `GenAiClient` |
| `DeviceCapabilityManager` 3-state (READY/NEVER/PENDING) | 5-state string + `AiBackend` enum | Phase 37/39 | `LOCAL_DOWNLOADABLE` is a real state written by `resolveLocalModelTier()` when model is not yet downloaded |

**Deprecated/outdated:**
- `@Named("preferredClient") GenAiClient?`: Exists in `AiModule` but `provideGenAiClient(router)` is the actual binding. The named provider is vestigial; Phase 40 does not need to touch it.

---

## Verified Codebase Findings (answers to specific questions)

### Q1: What does `ModelDownloadManagerImpl.download()` look like?

[VERIFIED: `ModelDownloadManagerImpl.kt`] Returns `Flow<DownloadProgress>` using `callbackFlow`. It is **NOT WorkManager-based** — it delegates to `LiteRtModelManager.downloadModel()` (OkHttp/URL-based, implemented in `ModelDownloader.kt`). The **foreground service** is `ModelDownloadService` which calls `ModelDownloader.downloadFile()` directly and writes 4 PreferencesManager keys (`localModelDownloadProgress`, `localModelDownloadReceived`, `localModelDownloadTotal`, `localModelDownloadSpeed`) on every progress callback. `ModelDownloadManagerImpl.download()` is a secondary abstraction used by the Domain interface; the actual download path for the UI is `ModelDownloadService.start(context, modelName)` → `downloadModel()` → `ModelDownloader.downloadFile()`.

### Q2: What status values does `DeviceCapabilityManager` expose?

[VERIFIED: `DeviceCapabilityManager.kt`] Writes to two PreferencesManager keys:
- `aiBackendTier`: `"aicore"` | `"local_model"` | `"none"` (maps to `AiBackend.id`)
- `aiAvailabilityStatus`: `"READY"` | `"LOCAL_READY"` | `"LOCAL_DOWNLOADABLE"` | `"NEVER"` | `"PENDING"` (default)

`LOCAL_DOWNLOADABLE` is written by `resolveLocalModelTier()` when `modelManager.isModelDownloaded()` returns false but a compatible model exists. This is exactly the trigger condition for Phase 40 dialog.

### Q3: Does `TransactionsScreen` already have `isAiAssistAvailable` and AnimatedVisibility FAB group?

[VERIFIED: `TransactionsScreen.kt` lines 463-503] Yes. `isAiAssistAvailable: Boolean` parameter at line 112, `AnimatedVisibility(visible = aiDraftExpanded)` at line 464, outer `if (isAiAssistAvailable)` at line 463. FAB group for SMS/Receipt/Voice mini-FABs is complete. Phase 40 requires no FAB changes.

### Q4: Does `AiModelsScreen.kt` already exist?

[VERIFIED: directory listing + file read] Yes. Contains `BackendStatusCard`, `ModelCard`, `buildDownloadProgressText()`, `HuggingFaceLoginDialog` — all used as pattern sources in UI-SPEC. The `buildDownloadProgressText()` function (lines 435-460) is the canonical caption format.

### Q5: Does `MoneyManagerApp` have the startup hook?

[VERIFIED: `MoneyManagerApp.kt`] Yes. `deviceCapabilityManager.checkAndCacheAvailability()` is launched on `Dispatchers.IO` in `onCreate()`. No changes needed for Phase 40.

### Q6: What permissions are declared in AndroidManifest?

[VERIFIED: `AndroidManifest.xml`]
- `INTERNET` — declared
- `ACCESS_NETWORK_STATE` — declared
- `FOREGROUND_SERVICE` — declared
- `FOREGROUND_SERVICE_DATA_SYNC` — declared
- `POST_NOTIFICATIONS` — declared (for Android 13+)
- `ModelDownloadService` declared with `foregroundServiceType="dataSync"` — correct for file downloads

No new manifest changes needed for Phase 40.

### Q7: What does `AiModule` provide after Phase 39?

[VERIFIED: `AiModule.kt`] Two providers:
1. `provideGenAiClient(router: AiClientRouter): GenAiClient` — the main binding, always returns the router
2. `@Named("preferredClient") providePreferredGenAiClient(...)`: GenAiClient?` — static singleton, reads tier once at startup

`AiClientRouter` (the live-routing implementation) is the actual bound `GenAiClient`. It calls `deviceCapabilityManager.resolveCurrentTier()` and `modelManager.isModelDownloaded()` on every inference call — no staleness issue for Phase 40.

### Q8: Is there a `DownloadViewModel` equivalent already?

[VERIFIED: `AiModelsViewModel.kt`] `AiModelsViewModel` has `_downloadingModelName`, `_modelProgressMap`, and collects progress from PreferencesManager. It calls `ModelDownloadService.start(context, model.name)`. The consent dialog and banner are new to Phase 40, but the download-orchestration pattern is fully established.

### Q9: Notification icon name

[VERIFIED: `res/drawable/` listing] No `ic_notification` drawable exists. `ModelDownloadService` currently uses `android.R.drawable.stat_sys_download` (system resource). The UI-SPEC says "executor to confirm icon name." The planner should keep `android.R.drawable.stat_sys_download` for Phase 40 — adding a custom notification icon is out of scope unless a drawable is created.

### Q10: `PreferencesManager.userOptedInAi` existence

[VERIFIED: `PreferencesManager.kt` line 133-135 + line 291-295] Key `user_opted_in_ai` exists as `booleanPreferencesKey`. Flow `userOptedInAi: Flow<Boolean>` is exposed. Setter `setUserOptedInAi(value: Boolean)` exists. All required.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `buildDownloadProgressText()` in `AiModelsScreen.kt` should be extracted to a shared location or copied rather than called cross-package | Code Examples | Low — copying is always safe; extraction is a nice-to-have refactor |
| A2 | `TransactionsViewModel` needs `AiAvailabilityRepository` and `PreferencesManager` injected (currently uses only `PreferencesManager` for `currency`) | Architecture Patterns | Medium — if ViewModel constructor change causes Hilt compile error, the download state could live in `AiDraftViewModel` instead (already injected in TransactionsScreen NavHost composable block) |
| A3 | The dialog should be shown from `TransactionsScreen` (the main screen after launch) rather than from a dedicated `MainActivity` `LaunchedEffect` | Architecture Patterns | Low — MainActivity approach also valid, but TransactionsScreen + ViewModel is consistent with the codebase pattern |

---

## Open Questions

1. **Where does `showDownloadConsentDialog` state live?**
   - What we know: `TransactionsViewModel` is an `AndroidViewModel` with `@ApplicationContext` available. `AiDraftViewModel` is already scoped to the TransactionsScreen composable block in NavHost.
   - What's unclear: Whether to add download state to existing `TransactionsViewModel` (requires new injections: `AiAvailabilityRepository`, `PreferencesManager.userOptedInAi`) or create a thin new `DownloadConsentViewModel`.
   - Recommendation: Add to `TransactionsViewModel` — it already injects `PreferencesManager` and the download banner belongs on the same screen. The planner should verify no circular Hilt dependency is introduced.

2. **Notification channel ID: update `ModelDownloadService` or leave as-is?**
   - What we know: `ModelDownloadService.CHANNEL_ID = "model_download"`. UI-SPEC says `"ai_model_download"`.
   - What's unclear: Whether the discrepancy matters to users (they see channel name, not ID).
   - Recommendation: Update the constant and human name — one-line change, no migration issues (Android creates a new channel; old channel persists silently).

3. **Should `setUserOptedInAi(true)` happen before or in parallel with `ModelDownloadService.start()`?**
   - Recommendation: `setUserOptedInAi(true)` first (suspending), then `ModelDownloadService.start()`. If the app is killed between the two, next launch will re-show consent (safe recovery).

---

## Environment Availability

Step 2.6: SKIPPED — Phase 40 is a purely UI + data-layer coordination change. All runtime dependencies (Android Foreground Service, DataStore, existing services) are already running in the existing app. No new external tools, services, or CLI utilities required.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + Mockito Kotlin (from `build.gradle.kts`) |
| Config file | No `src/test/` config — uses `testOptions { unitTests.isReturnDefaultValues = true }` |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "*.download*"` |
| Full suite command | `./gradlew :app:testDebugUnitTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| HYBRID-05 | Dialog shown when tier=LOCAL_DOWNLOADABLE and not opted-in and not session-suppressed | unit | `./gradlew :app:testDebugUnitTest --tests "*.DownloadConsentViewModelTest"` | ❌ Wave 0 |
| HYBRID-05 | "Maybe Later" sets session flag, does NOT write userOptedInAi | unit | Same test class | ❌ Wave 0 |
| HYBRID-05 | Dialog NOT shown when isLocalModelDownloaded=true (tier=LOCAL_READY) | unit | Same test class | ❌ Wave 0 |
| HYBRID-05 | Dialog NOT shown when session-suppressed | unit | Same test class | ❌ Wave 0 |
| HYBRID-06 | DownloadProgressBanner visible state driven by localModelDownloadProgress 0<p<1 | unit | `./gradlew :app:testDebugUnitTest --tests "*.DownloadBannerStateTest"` | ❌ Wave 0 |
| HYBRID-06 | AiAvailabilityRepository emits true when isLocalModelDownloaded flips to true | unit | Existing or new `AiAvailabilityRepositoryTest` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "*.Download*"`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/.../DownloadConsentViewModelTest.kt` — covers HYBRID-05 dialog trigger logic
- [ ] `app/src/test/.../AiAvailabilityRepositoryTest.kt` — covers HYBRID-06 reactive FAB reveal

*(Existing test infrastructure: `build.gradle.kts` already has `testImplementation("junit:junit:4.13.2")` and `mockito-kotlin:5.4.0`. Test directory exists from Phase 39 work.)*

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | — |
| V3 Session Management | no | — |
| V4 Access Control | no | — |
| V5 Input Validation | no | Dialog has no user text input |
| V6 Cryptography | no | Model file is downloaded over HTTPS (handled by ModelDownloader) |

**No new security surface area introduced.** The consent dialog collects no user data. The download path (HTTPS to HuggingFace) is already implemented in `ModelDownloadService` + `ModelDownloader`.

### Known Threat Patterns for this phase

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| User bypasses consent by direct service call | Repudiation | `ModelDownloadService.start()` is only called from consent dialog `onDownload` lambda; no public API bypass |
| Re-download without consent on clear-data | Elevation of Privilege | `user_opted_in_ai` is cleared on app data wipe; DeviceCapabilityManager re-runs on next launch; dialog re-shows — correct behavior |

---

## Sources

### Primary (HIGH confidence)
- `MoneyManager/app/src/main/java/com/moneymanager/data/ai/ModelDownloadService.kt` — foreground service implementation, notification channel, progress keys
- `MoneyManager/app/src/main/java/com/moneymanager/data/preferences/PreferencesManager.kt` — all keys verified: `user_opted_in_ai`, `local_model_download_progress`, `local_model_download_received`, `local_model_download_total`, `local_model_download_speed`, `isLocalModelDownloaded`, `ai_backend_tier`
- `MoneyManager/app/src/main/java/com/moneymanager/data/repository/AiAvailabilityRepository.kt` — reactive `isAiAvailable` Flow combining tier + downloaded
- `MoneyManager/app/src/main/java/com/moneymanager/di/AiModule.kt` — singleton analysis, AiClientRouter as primary binding
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/aimodels/AiModelsScreen.kt` — `buildDownloadProgressText()`, `BackendStatusCard`, `LinearProgressIndicator` at 4dp pattern
- `MoneyManager/app/src/main/AndroidManifest.xml` — all required permissions and service declaration verified
- `MoneyManager/app/build.gradle.kts` — no new Gradle dependencies needed (confirmed all required libs present)
- `MoneyManager/app/src/main/java/com/moneymanager/app/ui/transactions/TransactionsScreen.kt` — `isAiAssistAvailable` parameter, AnimatedVisibility FAB group at lines 463-503
- `.planning/phases/40-user-facing-download-flow/40-UI-SPEC.md` — approved interaction contract

### Secondary (MEDIUM confidence)
- `.planning/REQUIREMENTS.md` — HYBRID-05, HYBRID-06 requirements text
- `.planning/STATE.md` — architecture decisions locked in v3.1
- `.planning/ROADMAP.md` — Phase 40 success criteria

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries verified in `build.gradle.kts`; no new deps
- Architecture: HIGH — all infrastructure verified by reading actual implementation files
- Pitfalls: HIGH — identified from reading actual code (AiModule singleton, channel ID mismatch, notification icon)
- UI patterns: HIGH — exact code from `AiModelsScreen.kt` and `TransactionsScreen.kt` cited

**Research date:** 2026-05-18
**Valid until:** 2026-06-18 (stable codebase; no fast-moving external dependencies)
