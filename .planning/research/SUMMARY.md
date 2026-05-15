# Project Research Summary

**Project:** MoneyManager - v3.0 AI-Assisted Transaction Drafting
**Domain:** On-device AI integration into existing Android MVVM/Hilt/Compose/Room app
**Researched:** 2026-05-15
**Confidence:** HIGH (stack and architecture from direct codebase + artifact inspection) / MEDIUM (AICore API specifics, Play Store policy)

---

## Executive Summary

MoneyManager v3.0 adds Gemini Nano (via Android AICore) as an opt-in transaction drafting assistant: users create transaction drafts from SMS text, receipt photos, or voice memos. The AI suggests a pre-filled form the user reviews and confirms. The app remains 100% functional without AI - all AI buttons hide gracefully when AICore is unavailable. Every inference runs on-device via Gemini Nano, no data ever leaves the device.

The recommended implementation follows a strict 5-phase build order derived from the Clean Architecture dependency graph. Domain layer contracts come first (zero Android dependencies, fully unit-testable), then data implementations, then DI wiring, then UI screens, and finally minimal integration changes to the existing AddEditTransactionDialog. The three input sources - SMS, OCR, and voice - share a single AiDraftViewModel and a single use case.

The two highest-risk decisions are (1) the READ_SMS Play Store approval gate - the SMS flow must be designed clipboard-first with READ_SMS as a gated enhancement - and (2) the AICore 4-state availability model, which must not be collapsed to a Boolean. Both risks are fully mitigatable with deliberate architectural choices described below.

---

## Key Findings

### Recommended Stack

See STACK.md for full artifact verification. v3.0 adds exactly 4 implementation lines to app/build.gradle.kts plus one serialization plugin to the root build file. No minSdk bump, no new Gradle modules, no settings.gradle.kts changes.

**Exact Gradle additions - Root build.gradle.kts (plugins block):**

    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false

**Exact Gradle additions - app/build.gradle.kts:**

    // plugins block
    id("org.jetbrains.kotlin.plugin.serialization")

    // dependencies block
    implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")
    implementation("com.google.mlkit:genai-common:1.0.0-beta3")
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

**Core technology rationale:**
- genai-prompt:1.0.0-beta2: ML Kit GenAI Prompt API; system-managed model means zero APK bloat.
- genai-common:1.0.0-beta3: exposes FeatureStatus enum (UNAVAILABLE, DOWNLOADABLE, DOWNLOADING, AVAILABLE) for the 4-state availability check. Declare explicitly despite being pulled transitively.
- play-services-mlkit-text-recognition:19.0.1: unbundled OCR; model delivered via Play Services, zero APK size increase. Use this over bundled variant which adds 1.38MB to APK.
- kotlinx-serialization-json:1.8.1: parses Gemini Nano JSON output into TransactionDraft. Plugin version must exactly match Kotlin 2.3.20.
- SpeechRecognizer and ContentResolver/Telephony.Sms.Inbox: Android SDK APIs, no Gradle lines needed.

**What NOT to add:** com.google.android.aicore:aicore-client-api (does not exist on Google Maven), play-services-mlkit-genai-inference (does not exist), MediaPipe LLM Inference (bundles model GBs into APK), Google AI SDK com.google.ai.client.generativeai (cloud inference, violates offline contract), CameraX (use ActivityResultContracts.TakePicture instead).

**Manifest additions required (3 runtime permissions):**

    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.READ_SMS" />

Note: READ_SMS must only be added after Play Store declaration approval is confirmed. AICore permissions auto-merge from the genai-prompt AAR; no manual addition needed.

---

### Expected Features

See FEATURES.md for complete table stakes / differentiator / anti-feature breakdowns per flow.

**Must have - SMS flow (table stakes):**
- Clipboard/paste path first (no READ_SMS required). READ_SMS is a Play Store gated enhancement - design the flow around pasted text.
- Financial sender filter (editable, defaults to known Indian bank sender IDs like HDFCBK, SBIINB, PAYTM)
- Sender / timestamp / 60-char message preview per row; full SMS preview pane before "Use This Message"
- Multi-part SMS reconstruction; date-descending order, max 50 results, graceful denial fallback to manual entry

**Must have - OCR flow (table stakes):**
- Camera as primary action (ActivityResultContracts.TakePicture), gallery as secondary
- Image thumbnail + scrollable read-only OCR text pane before AI Fill
- Pre-processing normalization before AI prompt (7 rules: rupee variants, 0/O in numerics, thousands separators, boilerplate stripping)
- OCR failure state with retry; CAMERA permission with gallery fallback on denial
- Auto-attach captured image as receiptPath via existing FileHelper.saveReceipt()

**Must have - Voice flow (table stakes):**
- Tap-start / tap-stop (not hold-to-record); animated mic feedback during recording
- Editable transcription field post-recording; 60-second max with countdown
- RECORD_AUDIO permission with denial fallback; offline availability check before showing voice option
- Re-record button; AI Fill disabled until transcription non-empty; Use as Note fallback when AI unavailable

**Must have - Shared / Draft Review (table stakes):**
- Pre-populate existing AddEditTransactionDialog via initialDraft: TransactionDraft? = null - no new screen
- AI-suggested fields visually distinguished (tinted background + sparkle badge); highlighting clears on user edit
- Source banner at top of form (e.g. "Draft from SMS - HDFCBK - 2 minutes ago")
- All fields remain fully editable; empty fields for low-confidence AI extractions (never guess)
- Three-source expand FAB (SMS / Receipt / Voice) alongside existing + FAB - do not replace the existing FAB
- AI drafting FAB shown only when isAiAssistAvailable = true

**Should have - v3.1 differentiators:**
- SMS: remember last-used sender filter per session; Save sender as financial DataStore preference
- OCR: crop/rotate controls before OCR (UCropActivity)
- Voice: locale-aware language selection; multiple transcription alternatives as selectable chips
- Shared: per-field AI confidence indicators; Undo AI Fill button
- Shared: opt-in save as note toggle (default OFF for SMS - OTP/account number risk; default ON for Voice)

**Anti-features (do not build in v3.0):**
- Background SMS scanning (Play Store surveillance concern)
- Cloud AI for any input type (violates offline privacy contract)
- Auto-save without user review (data integrity risk in finance app)
- PDF receipt support in v3.0 (complexity vs frequency mismatch)
- Bottom nav AI Draft item or replacing the existing + FAB
- AI accuracy feedback / fine-tuning loop (Gemini Nano has no fine-tuning API)

**Raw source text storage decision:** Opt-in toggle. Default OFF for SMS (OTPs, account numbers). Default ON for Voice. For OCR: image already stored as receiptPath - do not store OCR text separately. No new DB column; map source text to existing note field when opted in.

---

### Architecture Approach

Three new package subtrees following the existing domain/data/app split. No new Gradle modules. All existing DAOs, repository interfaces, implementations, TransactionFormState, TransactionFormConfig, TransactionFormConverter, and all screens outside addtransaction and MoneyManagerNavHost are completely untouched. All changes to existing files are strictly additive.

**Major components:**

1. domain/ai/GenAiClient (interface): suspend fun generateDraft(prompt: String): Result<String>. No Android imports. Pure domain contract.

2. domain/ai/TransactionDraft (data class): resolved entity IDs (categoryId, accountId, peerContactId) + raw fields. Does NOT extend TransactionEntity.

3. domain/ai/TransactionType (enum class): centralized registry with id, displayName, promptHint, requiresCategory, requiresPeer. Replaces TransactionEntity.VALID_TYPES hardcoded list. Prevents prompt/registry drift (PITFALL-21). Includes fromId() and allIds() companion methods.

4. domain/ai/PromptContextBuilder: injected into GenerateDraftFromTextUseCase. Reads top-20 categories by usage frequency, all accounts, all peers, all tags from 3 existing repository interfaces. Uses domain-only projection types (CategoryEntry, AccountEntry, PeerEntry, TagEntry). Reuses categoryUsageCounts already computed in AddTransactionViewModel.

5. domain/ai/GenerateDraftFromTextUseCase: orchestrates build context -> build prompt -> call GenAiClient -> parse JSON. Returns Result.failure(AiUnavailableException()) when aiClient is null.

6. data/ai/NanoAiClient: implements GenAiClient. Wraps ML Kit GenAI API calls.

7. data/ai/DeviceCapabilityManager: checks AICore availability fully async on Dispatchers.IO at startup. Writes result to PreferencesManager as string enum ("READY", "NEVER", "PENDING") - NOT a Boolean (see PITFALL-01). Exposes isAiAssistAvailableSync(): Boolean called once only during Hilt graph initialization.

8. data/ai/PromptBuilder (object) + data/ai/DraftParser (object): pure functions, no injection. PromptBuilder caps categories at top-20 by usage and sanitizes user strings. DraftParser strips markdown fences, extracts JSON object between first { and last }, uses Json { ignoreUnknownKeys = true } lenient parsing.

9. app/di/AiModule: @Provides @Singleton @javax.annotation.Nullable fun provideGenAiClient(): GenAiClient?. First nullable binding in this Hilt graph. @javax.annotation.Nullable annotation is mandatory - KSP does not reliably emit @Nullable from Kotlin ? alone.

10. app/ui/aidraft/AiDraftViewModel (@HiltViewModel): shared across all 3 source screens. Holds AiDraftUiState, calls use case, exposes isAiAvailable: StateFlow<Boolean>.

11. Three source screens in app/ui/aidraft/: SmsPickerScreen, ReceiptScanScreen, VoiceMemoScreen. All share AiDraftViewModel.

**Draft handoff path:**

    AiDraftUiState.draft (TransactionDraft)
      -> serialized as JSON nav argument
        -> AddTransactionViewModel reads SavedStateHandle
          -> AddTransactionUiState.initialDraft
            -> AddTransactionScreen passes initialDraft to dialog
              -> AddEditTransactionDialog(initialDraft = draft)
                -> LaunchedEffect(initialDraft) populates rememberSaveable vars via onTypeSelected()
                  -> user reviews and saves via existing onConfirm path

**DataStore extension pattern (CRITICAL):** Extend PreferencesManager directly with one new string key (ai_availability_status). Never create a second preferencesDataStore(name = "settings") delegate - this corrupts PIN security keys (PITFALL-04).

**5-phase build order (from ARCHITECTURE.md section 8):**
- Phase 1 (files 1-6): domain/ai contracts - TransactionType, TransactionDraft, GenAiClient, PromptContext, PromptContextBuilder, GenerateDraftFromTextUseCase
- Phase 2 (files 7-11): data/ai implementation - PreferencesManager extension, PromptBuilder, DraftParser, NanoAiClient, DeviceCapabilityManager
- Phase 3 (files 12-13): DI wiring - AiModule, MoneyManagerApp startup hook
- Phase 4 (files 14-18): UI screens - AiDraftUiState, AiDraftViewModel, SmsPickerScreen, ReceiptScanScreen, VoiceMemoScreen
- Phase 5 (files 19-23): dialog integration - AddEditTransactionDialog, AddTransactionUiState, AddTransactionViewModel, AddTransactionScreen, MoneyManagerNavHost

---

### Critical Pitfalls

Full details in PITFALLS.md. Top 5 watch-out items:

1. **AICore 4-state availability - store string enum, not Boolean (PITFALL-01, CRITICAL).**
   FeatureStatus has four states: AVAILABLE, UNAVAILABLE, DOWNLOADABLE, DOWNLOADING. Caching as Boolean causes Pixel 9 users to never see AI features even after the model downloads. Store "READY" / "NEVER" / "PENDING". Write "NEVER" only on NOT_SUPPORTED or NOT_DOWNLOADABLE. Re-check on next launch for "PENDING". Never use PackageManager.getPackageInfo("com.google.android.aicore") as a proxy (PITFALL-02) - use the ML Kit availability API.

2. **DataStore key collision corrupts PIN security (PITFALL-04, CRITICAL).**
   PreferencesManager.kt already owns the "settings" DataStore at line 11. A second Context.dataStore delegate with the same file name causes undefined behavior and can silently corrupt pin_hash, pin_salt, pin_enabled. Prevention: extend PreferencesManager with AI keys only.

3. **READ_SMS requires Play Store declaration approval before manifest inclusion (PITFALL-12, HIGH).**
   READ_SMS is a restricted permission. App submission will be rejected without prior Play Console Permission Declaration Form approval. Order: (a) build clipboard/paste path first; (b) apply for Play Console approval separately; (c) add READ_SMS to manifest only after approval confirmed.

4. **Token overflow on power users - cap categories at top-20 by usage (PITFALL-05, CRITICAL).**
   Gemini Nano context window is ~2048-4096 tokens. A user with 200 categories + 50 accounts + 100 peers hits ~5,050 tokens. Prevention: sort categories by categoryUsageCounts (already available in AddTransactionViewModel) and take top 20. No extra DB query needed.

5. **ImageProxy.close() must be called synchronously before any async work (PITFALL-08, HIGH).**
   Every CameraX ImageProxy must be closed by imageProxy.close() (not imageProxy.image?.close()) before launching any coroutine. Pattern: convert to Bitmap synchronously then close immediately then launch coroutine with the copied Bitmap only.

Additional high-severity watch-out items:
- Hilt @javax.annotation.Nullable annotation required on nullable @Provides (PITFALL-13)
- SpeechRecognizer.destroy() must be called in DisposableEffect.onDispose (PITFALL-10)
- EXTRA_PREFER_OFFLINE is a hint, not a guarantee - handle ERROR_NOT_SUPPORTED explicitly (PITFALL-11)
- Null GenAiClient must return a typed AiUnavailable result, not a silent no-op (PITFALL-03)
- AI draft must not re-populate form on second dialog open after dismiss - call clearDraft() on onDismiss (PITFALL-15)

---

## Implications for Roadmap

Research supports a 5-phase build order. Each phase is fully compilable and independently testable before the next begins.

### Phase 1: Domain AI Foundation
**Rationale:** Zero Android or Compose dependencies - purely JVM. Establishes all contracts and domain model before any implementation. Most testable phase; no device required.
**Delivers:** TransactionType enum, TransactionDraft model, GenAiClient interface, PromptContext + projection classes, PromptContextBuilder, GenerateDraftFromTextUseCase.
**Avoids:** PITFALL-21 (type string drift), PITFALL-03 (use case is the null guard for GenAiClient).
**Research flag:** Low - domain contracts are fully defined by codebase inspection. Standard pattern.

### Phase 2: Data AI Implementation
**Rationale:** Implements domain contracts. PreferencesManager change must precede DeviceCapabilityManager, which must precede AiModule. PromptBuilder and DraftParser are pure objects with no ordering dependency within the phase.
**Delivers:** PreferencesManager AI key extension, PromptBuilder (top-20 category cap + prompt injection sanitization), DraftParser (defensive markdown stripping + lenient JSON parsing), NanoAiClient, DeviceCapabilityManager (4-state async string enum).
**Uses:** genai-prompt, genai-common, kotlinx-serialization-json Gradle additions.
**Avoids:** PITFALL-01 and -02 (4-state model, ML Kit API not package check), PITFALL-04 (extend PreferencesManager only), PITFALL-05 (top-20 token budget), PITFALL-06 (sanitize user strings), PITFALL-07 (defensive JSON parsing).
**Research flag:** Medium - FeatureStatus constant names and PromptClient API signatures must be verified against actual genai-common:1.0.0-beta3 AAR at integration time. Architectural pattern is HIGH confidence; exact API surface is MEDIUM.

### Phase 3: DI Wiring
**Rationale:** AiModule depends on Phase 2 components. Isolated phase means a failing KSP nullable binding error is caught immediately without UI build noise.
**Delivers:** AiModule (@javax.annotation.Nullable nullable @Provides), MoneyManagerApp startup hook for async AICore availability check.
**Avoids:** PITFALL-13 (KSP nullable annotation), PITFALL-14 (no blocking IPC on main thread - use applicationScope.launch on Dispatchers.IO).
**Research flag:** Low - Hilt nullable binding pattern is well-documented. Clean build gate catches errors immediately.

### Phase 4: UI Screens
**Rationale:** Depends on Phase 3 Hilt graph. Three screens share one ViewModel - build AiDraftUiState and AiDraftViewModel first, then each screen in sequence.
**Delivers:** AiDraftUiState, AiDraftViewModel, SmsPickerScreen (clipboard-first, READ_SMS gated), ReceiptScanScreen (camera primary with ActivityResultContracts.TakePicture, unbundled OCR), VoiceMemoScreen (SpeechRecognizer EXTRA_PREFER_OFFLINE, DisposableEffect destroy), expanded FAB with AnimatedVisibility 3-source menu alongside existing + FAB.
**Avoids:** PITFALL-08 (ImageProxy synchronous close), PITFALL-09 (executor shutdown in DisposableEffect), PITFALL-10 (SpeechRecognizer destroy on disposal), PITFALL-11 (handle ERROR_NOT_SUPPORTED), PITFALL-12 (clipboard-first SMS, no READ_SMS in manifest until Play approval), PITFALL-16 (handle MlKitException.UNAVAILABLE on first OCR), PITFALL-20 (Bitmap recycle after OCR).
**Research flag:** Medium - SpeechRecognizer offline behavior on hi-IN locale requires verification on real devices.

### Phase 5: Dialog Integration
**Rationale:** Comes last to minimize risk to existing flows. All changes use null-default parameters - cannot affect existing behavior.
**Delivers:** AddEditTransactionDialog(initialDraft: TransactionDraft? = null) with LaunchedEffect(initialDraft), AddTransactionUiState.initialDraft field, AddTransactionViewModel nav arg parsing, AddTransactionScreen pass-through, MoneyManagerNavHost with 3 new routes and optional ?draft={draftJson} argument, source banner composable, AI field highlighting with clear-on-edit behavior.
**Avoids:** PITFALL-15 (clearDraft() on onDismiss), PITFALL-19 (identical validation for AI-prefilled fields), PITFALL-22 (disable AI Fill button on tap, cancel previous Job before new one).
**Research flag:** Low - all integration points directly inspected from source. Standard LaunchedEffect + null-default parameter pattern.

### Phase Ordering Rationale

- Domain before data before DI before UI before integration - respects Clean Architecture dependency graph; each phase is independently verifiable.
- TransactionType enum in Phase 1 prevents prompt type drift (PITFALL-21) from the very first commit.
- DeviceCapabilityManager (Phase 2) must complete before AiModule (Phase 3) because provideGenAiClient() calls isAiAssistAvailableSync() at Hilt graph initialization time.
- Phase 3 isolated to DI wiring only so a failing KSP nullable binding error is caught before any UI code is written.
- READ_SMS clipboard-first approach (Phase 4) decouples feature delivery timeline from Play Store approval.

### Research Flags

Needs verification at implementation time:
- **Phase 2 (NanoAiClient):** FeatureStatus constant names and PromptClient.create() / checkAvailability() method signatures in genai-common:1.0.0-beta3 - beta API, verify against actual AAR before coding.
- **Phase 2 (PromptBuilder):** Call countTokens() on a representative prompt to confirm token budget; adjust top-20 cap if actual counts differ.
- **Phase 4 (VoiceMemoScreen):** Offline speech model availability for hi-IN on target Indian market devices - test on actual devices before launch.
- **Phase 4 (SMS):** Start Play Console Permission Declaration Form process for READ_SMS in parallel with Phase 4 development.

Standard patterns (skip research phase):
- **Phase 1:** Pure Kotlin domain contracts - no research needed.
- **Phase 3:** Hilt nullable @Provides - well-documented, verified pattern.
- **Phase 5:** LaunchedEffect + null-default parameter in existing Compose dialog - standard, low risk.

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack (artifact IDs, versions) | HIGH | Google Maven metadata + AAR manifest direct inspection; all 4 artifacts verified |
| Features (table stakes, flow design) | HIGH | Direct codebase inspection: AddEditTransactionDialog, TransactionsScreen, MoneyManagerNavHost |
| Architecture (component boundaries, build order) | HIGH | All referenced source files directly inspected; integration points confirmed |
| Pitfalls (critical items) | HIGH | DataStore/Hilt patterns from direct code inspection; CameraX/SpeechRecognizer from official docs |
| AICore API surface (exact method names) | MEDIUM | genai-common:1.0.0-beta3 is beta; FeatureStatus constant names from documentation summary not AAR source |
| Play Store READ_SMS approval | MEDIUM | Policy documented but approval outcome uncertain; clipboard path mitigates this |
| Gemini Nano context window size | MEDIUM | 2048-4096 from 2024 docs; verify with countTokens() at integration time |
| OCR normalization regexes | MEDIUM | Derived from observed Indian bank SMS patterns; expect minor tuning per real data |
| SpeechRecognizer offline model coverage | MEDIUM | Device and locale dependent; no direct test data available |

**Overall confidence:** HIGH for architecture and implementation approach. MEDIUM for AICore-specific API details to verify against the beta artifact at integration time.

### Gaps to Address

- **AICore FeatureStatus constant names:** Verify exact names against genai-common:1.0.0-beta3 AAR during Phase 2. The 4-state model is correct; only string identifiers may differ slightly.
- **Context window token budget:** Call countTokens() on a representative prompt during Phase 2 integration testing. The top-20 category cap is safe and conservative.
- **READ_SMS Play Console declaration:** Start the Permission Declaration Form process in parallel with Phase 4. Clipboard/paste path ships regardless; READ_SMS is additive only after approval.
- **applicationScope availability in MoneyManagerApp:** Verify whether an applicationScope coroutine scope already exists or must be added before the Phase 3 startup hook.
- **TransactionEntity.receiptPath storage limit:** Confirm Room TEXT column can accommodate full-resolution receipt images or whether downsampling is required before FileHelper.saveReceipt() in the Phase 4 OCR flow.

---

## Sources

### Primary (HIGH confidence - direct codebase and artifact inspection)
- app/src/main/java/com/moneymanager/app/ui/dialogs/AddEditTransactionDialog.kt - dialog parameter signature, rememberSaveable vars, LaunchedEffect usage
- app/src/main/java/com/moneymanager/app/ui/addtransaction/AddTransactionViewModel.kt - StateFlow/combine pattern, categoryUsageCounts availability at line 64
- app/src/main/java/com/moneymanager/data/preferences/PreferencesManager.kt - DataStore singleton pattern, existing keys, line 11 delegate
- app/src/main/java/com/moneymanager/app/MoneyManagerApp.kt - AppLockManager field injection pattern, startup hook precedent
- app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt - navigation structure, route patterns
- app/build.gradle.kts - existing stack; confirms kotlinx.serialization not yet present
- Google Maven group-index.xml for com.google.mlkit and com.google.android.gms - artifact IDs and versions confirmed
- genai-prompt:1.0.0-beta2 and genai-common:1.0.0-beta3 AAR AndroidManifest direct inspection - minSdk, auto-merged permissions
- Maven Central metadata for kotlinx-serialization-json:1.8.1 - version confirmed 2026-04-09

### Secondary (MEDIUM confidence - official documentation)
- Android CameraX ImageAnalysis guide - imageProxy.close() requirement confirmed
- Android SpeechRecognizer API reference - destroy() requirement, error codes
- Hilt documentation - nullable @Provides with @javax.annotation.Nullable, @TestInstallIn pattern
- DataStore documentation - multiple-instance prohibition for same file name
- ML Kit Text Recognition guide - MlKitException.UNAVAILABLE on first use after clean install
- Google Play SMS/Call Log Sensitive Permissions Policy - READ_SMS declaration requirement
- Gemini Nano overview documentation - context window sizing (2024 docs; verify at integration time)

---
*Research completed: 2026-05-15*
*Ready for roadmap: yes*
