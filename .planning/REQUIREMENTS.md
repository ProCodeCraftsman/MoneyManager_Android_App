# Requirements: MoneyManager v3.0

**Defined:** 2026-05-15
**Core Value:** AI-assisted transaction drafting — Gemini Nano suggests, user decides. App is 100% functional without AI.

---

## v3.0 Requirements

### AI Foundation (AIFND)

- [ ] **AIFND-01**: App checks AICore availability on first launch and caches a 3-state result ("READY" / "NEVER" / "PENDING") in DataStore by extending PreferencesManager — no second DataStore instance created
- [ ] **AIFND-02**: AI availability is exposed as `Flow<Boolean>` from a repository; all "AI Fill" button visibility decisions are driven by this flow (not hard-coded checks)
- [ ] **AIFND-03**: `GenAiClient` interface lives in `domain/ai/` with a single method `generateDraft(prompt: String): Result<String>` — no Android imports
- [ ] **AIFND-04**: `NanoAiClient` in `data/ai/` implements `GenAiClient` using ML Kit GenAI Prompt API (`com.google.mlkit:genai-prompt`)
- [ ] **AIFND-05**: `TransactionDraft` domain model carries resolved entity IDs (`categoryId: Long?`, `accountId: Long?`, `peerContactId: Long?`) alongside display names and raw fields — does not extend `TransactionEntity`
- [ ] **AIFND-06**: `TransactionType` enum in `domain/ai/` is the single source of truth for transaction type strings in all new AI code; each entry carries `id`, `displayName`, `promptHint`, `requiresCategory`, `requiresPeer` — existing `TransactionEntity.VALID_TYPES` strings unchanged
- [ ] **AIFND-07**: `PromptContextBuilder` reads top-20 categories by usage frequency (reusing `categoryUsageCounts` already computed in `AddTransactionViewModel`), all accounts, all peers, all tags from existing repositories; exposes a `PromptContext` data class using domain-only projection types (`CategoryEntry`, `AccountEntry`, `PeerEntry`, `TagEntry`)
- [ ] **AIFND-08**: `GenerateDraftFromTextUseCase` accepts `rawText: String` + `PromptContext` and returns `Result<TransactionDraft>`; returns `Result.failure(AiUnavailableException())` when `GenAiClient` is null — no silent no-ops
- [ ] **AIFND-09**: `DraftParser` (pure object in `data/ai/`) defensively parses AI JSON output: strips markdown fences, extracts content between first `{` and last `}`, uses `kotlinx-serialization` with `ignoreUnknownKeys = true`; returns parse error to caller on failure
- [ ] **AIFND-10**: `PromptBuilder` (pure object in `data/ai/`) sanitizes all user master data strings before prompt injection, caps category list at top-20 by usage, and builds a deterministic single-shot JSON-extraction prompt
- [ ] **AIFND-11**: `AiModule` Hilt object provides `GenAiClient?` as nullable `@Singleton` with `@javax.annotation.Nullable` annotation; returns `null` when AICore availability is "NEVER" or "PENDING"
- [ ] **AIFND-12**: `MoneyManagerApp` launches DeviceCapabilityManager availability check asynchronously on `Dispatchers.IO` at startup — no blocking IPC on main thread

---

### SMS Flow (SMS)

- [ ] **SMS-01**: User can paste or type SMS text directly into a text field on SmsPickerScreen to generate an AI draft (clipboard path — no `READ_SMS` permission required)
- [ ] **SMS-02**: User can access SMS inbox picker on SmsPickerScreen to select a financial message when `READ_SMS` permission is granted; READ_SMS path is feature-flagged and only shipped after Play Store Permission Declaration approval is confirmed
- [ ] **SMS-03**: SMS inbox picker shows an editable financial sender filter (default includes common Indian bank sender IDs: HDFCBK, SBIINB, PAYTM, ICICIT, AXISBK, KOTAKB) with results in date-descending order, max 50 messages
- [ ] **SMS-04**: Each SMS row in the inbox picker shows sender, timestamp, and 60-character message preview; tapping a row shows a full preview pane before "Use This Message"
- [ ] **SMS-05**: Multi-part SMS messages are reconstructed into a single body before display
- [ ] **SMS-06**: When `READ_SMS` permission is denied, the app falls back gracefully to the clipboard/paste path — manual entry is always available
- [ ] **SMS-07**: User taps "AI Fill" to send selected/pasted SMS text to `GenerateDraftFromTextUseCase`; on success the resulting `TransactionDraft` pre-populates `AddEditTransactionDialog`
- [ ] **SMS-08**: "AI Fill" button is visible only when `isAiAssistAvailable = true`; button is absent (not just disabled) when AI is unavailable
- [ ] **SMS-09**: User sees a Snackbar "Could not generate draft. Please enter details manually." on any runtime AI failure; form remains fully editable with blank fields
- [ ] **SMS-10**: User sees an opt-in "Save as note" toggle on SmsPickerScreen (default OFF) to persist the source SMS text to the transaction note field

---

### Receipt OCR Flow (OCR)

- [ ] **OCR-01**: User can capture a receipt/invoice via camera (primary action via `ActivityResultContracts.TakePicture`) or select an image from the gallery (secondary) on ReceiptScanScreen
- [ ] **OCR-02**: Unbundled ML Kit Text Recognition (`play-services-mlkit-text-recognition:19.0.1`) performs on-device OCR on `Dispatchers.IO` via `InputImage.fromFilePath(context, uri)` — the `ActivityResultContracts.TakePicture` URI path means no `ImageProxy` is involved; if CameraX/ImageAnalysis were used instead, `ImageProxy.close()` would be required before any coroutine launch (PITFALL-08)
- [ ] **OCR-03**: A pre-processing normalization pass runs on OCR output before AI prompt: rupee symbol variants → `₹`, digit–letter confusion (0↔O) in numeric contexts, thousands separators, boilerplate merchant/header stripping
- [ ] **OCR-04**: User sees image thumbnail alongside a scrollable read-only OCR text pane on ReceiptScanScreen before tapping "AI Fill"
- [ ] **OCR-05**: Captured image is auto-attached as `receiptPath` via the existing `FileHelper.saveReceipt()` — no parallel storage path created
- [ ] **OCR-06**: When OCR fails (model not yet downloaded, `MlKitException.UNAVAILABLE`), user sees a retry button with plain error message; second failure offers manual entry path
- [ ] **OCR-07**: When CAMERA permission is denied, gallery picker is offered as fallback; when gallery is also unavailable, screen returns to manual entry
- [ ] **OCR-08**: User taps "AI Fill" to send normalized OCR text to `GenerateDraftFromTextUseCase`; "AI Fill" button visible only when `isAiAssistAvailable = true`
- [ ] **OCR-09**: Without AI, normalized OCR text is displayed in a read-only field the user can refer to while manually entering transaction details

---

### Voice Memo Flow (VOICE)

- [ ] **VOICE-01**: User can record a voice memo on VoiceMemoScreen using tap-to-start / tap-to-stop with animated microphone feedback; maximum recording duration is 60 seconds with a visible countdown
- [ ] **VOICE-02**: `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE = true` performs on-device speech-to-text; result is shown as editable transcription text
- [ ] **VOICE-03**: Transcribed text is displayed in an editable field; user can edit the transcription before tapping "AI Fill"
- [ ] **VOICE-04**: Re-record button is shown after transcription; tapping it clears the current transcription and starts a new recording session
- [ ] **VOICE-05**: Voice entry point (FAB voice option and VoiceMemoScreen) is hidden entirely when `SpeechRecognizer.isRecognitionAvailable()` returns false — not just disabled
- [ ] **VOICE-06**: When RECORD_AUDIO permission is denied, voice entry point is hidden and user is returned to the main transaction entry path
- [ ] **VOICE-07**: User taps "AI Fill" to send transcription to `GenerateDraftFromTextUseCase`; "AI Fill" button is disabled (not hidden) until transcription is non-empty; button is absent when AI unavailable
- [ ] **VOICE-08**: Without AI, transcription text is pre-filled into the transaction note field in `AddEditTransactionDialog` so the voice flow has standalone utility
- [ ] **VOICE-09**: `SpeechRecognizer.destroy()` is called in `DisposableEffect.onDispose` to prevent memory leaks
- [ ] **VOICE-10**: User sees an opt-in "Save transcription as note" toggle (default ON) on VoiceMemoScreen; when ON, transcription is saved to the transaction note field

---

### Draft Review & Dialog Integration (DRAFT)

- [x] **DRAFT-01**: A three-source expandable FAB (SMS / Receipt / Voice) appears alongside the existing + FAB; the AI draft FAB group is visible only when `isAiAssistAvailable = true`; the existing + FAB behavior is completely unchanged
- [x] **DRAFT-02**: `AddEditTransactionDialog` accepts an optional `initialDraft: TransactionDraft? = null` parameter; all existing call sites pass `null` — no existing behavior changes
- [x] **DRAFT-03**: A `LaunchedEffect(initialDraft)` inside `AddEditTransactionDialog` applies draft field values to form state via the existing `onTypeSelected()` path before any user interaction
- [ ] **DRAFT-04**: AI-suggested fields are visually distinguished (tinted container background + small sparkle badge icon); the tint and badge clear automatically when the user edits that specific field
- [x] **DRAFT-05**: A source banner is shown at the top of `AddEditTransactionDialog` when opened from an AI flow (e.g. "Draft from SMS · HDFCBK · 2 minutes ago"); absent for manually opened dialogs
- [x] **DRAFT-06**: AI draft fields with low confidence are left empty (never guessed); user must fill them manually
- [x] **DRAFT-07**: Draft does not re-populate form state on a second dialog open after dismiss; `clearDraft()` is called on `onDismiss`
- [x] **DRAFT-08**: Three new routes in `MoneyManagerNavHost` for `SmsPickerScreen`, `ReceiptScanScreen`, `VoiceMemoScreen`; routes pass `TransactionDraft` as a JSON-serialized optional navigation argument to `AddTransactionScreen`
- [x] **DRAFT-09**: AI-drafted transactions go through the same save and validation path as manually entered transactions — no separate AI save code path

---

### Code Standards & Project Structure (STD)

- [ ] **STD-01**: All new AI-related files follow the package hierarchy: `domain/ai/` (interfaces, models, use cases, enums), `data/ai/` (implementations, builders, parsers, managers), `app/ui/aidraft/` (screens, ViewModels, UiState)
- [ ] **STD-02**: New files use existing project naming conventions: `*Screen.kt`, `*ViewModel.kt`, `*UiState.kt`, `*UseCase.kt`, `*Client.kt`, `*Manager.kt`, `*Builder.kt`, `*Parser.kt`
- [ ] **STD-03**: `AiDraftViewModel` is a single shared `@HiltViewModel` used by all three source screens — not three separate ViewModels
- [x] **STD-04**: All modifications to existing files (`AddEditTransactionDialog`, `AddTransactionViewModel`, `AddTransactionScreen`, `MoneyManagerNavHost`, `PreferencesManager`) are strictly additive (null-default parameters, new DataStore keys, new routes); no existing behavior is altered

---

## Future Requirements (v3.1)

### SMS Enhancements
- **SMS-F01**: Remember last-used sender filter per DataStore preference
- **SMS-F02**: READ_SMS permission inbox picker (after Play Store approval confirmed — start declaration process during v3.0)

### OCR Enhancements
- **OCR-F01**: Crop / rotate controls before OCR (`UCropActivity` or custom)
- **OCR-F02**: Per-field AI confidence indicators shown in draft review

### Voice Enhancements
- **VOICE-F01**: Locale-aware language selection for SpeechRecognizer
- **VOICE-F02**: Multiple transcription alternatives displayed as selectable chips

### Draft Review Enhancements
- **DRAFT-F01**: Per-field AI confidence indicators (color + percentage)
- **DRAFT-F02**: Undo AI Fill button that reverts all AI-suggested fields to blank

---

## Out of Scope

| Feature | Reason |
|---------|--------|
| Background SMS scanning | Play Store surveillance policy — not permitted for this use case |
| Cloud AI inference | Violates 100% offline privacy contract |
| Auto-save without user review | Financial data integrity — user must always confirm |
| PDF receipt support | Complexity vs frequency mismatch for v3.0 |
| AI accuracy feedback / fine-tuning | Gemini Nano has no on-device fine-tuning API |
| Bottom nav AI item | AI is an assistive entry point, not a primary section |
| Replacing existing + FAB | The + FAB is the primary manual entry point — must not change |
| TransactionType enum replacing existing VALID_TYPES strings globally | Lower risk to defer full string migration to a dedicated refactoring phase |
| CameraX dependency | ActivityResultContracts.TakePicture covers the use case with no new Gradle dependency |
| Any AI modification to master data (categories, accounts, budgets, goals) | AI assists in reading and using master data — it never creates or modifies it |

---

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AIFND-03 | Phase 32 — Domain AI Foundation | Pending |
| AIFND-05 | Phase 32 — Domain AI Foundation | Pending |
| AIFND-06 | Phase 32 — Domain AI Foundation | Pending |
| AIFND-07 | Phase 32 — Domain AI Foundation | Pending |
| AIFND-08 | Phase 32 — Domain AI Foundation | Pending |
| AIFND-01 | Phase 33 — Data AI Implementation | Pending |
| AIFND-04 | Phase 33 — Data AI Implementation | Pending |
| AIFND-09 | Phase 33 — Data AI Implementation | Pending |
| AIFND-10 | Phase 33 — Data AI Implementation | Pending |
| AIFND-02 | Phase 34 — DI Wiring & AI Availability | Pending |
| AIFND-11 | Phase 34 — DI Wiring & AI Availability | Pending |
| AIFND-12 | Phase 34 — DI Wiring & AI Availability | Pending |
| SMS-01 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-02 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-03 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-04 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-05 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-06 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-07 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-08 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-09 | Phase 35 — AI Draft Source Screens | Pending |
| SMS-10 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-01 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-02 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-03 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-04 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-05 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-06 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-07 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-08 | Phase 35 — AI Draft Source Screens | Pending |
| OCR-09 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-01 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-02 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-03 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-04 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-05 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-06 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-07 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-08 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-09 | Phase 35 — AI Draft Source Screens | Pending |
| VOICE-10 | Phase 35 — AI Draft Source Screens | Pending |
| STD-01 | Phase 35 — AI Draft Source Screens | Pending |
| STD-02 | Phase 35 — AI Draft Source Screens | Pending |
| STD-03 | Phase 35 — AI Draft Source Screens | Pending |
| DRAFT-01 | Phase 36 — Dialog Integration & FAB | ✅ Complete |
| DRAFT-02 | Phase 36 — Dialog Integration & FAB | ✅ Complete |
| DRAFT-03 | Phase 36 — Dialog Integration & FAB | ✅ Complete |
| DRAFT-04 | Phase 36 — Dialog Integration & FAB | Pending |
| DRAFT-05 | Phase 36 — Dialog Integration & FAB | ✅ Complete |
| DRAFT-06 | Phase 36 — Dialog Integration & FAB | ✅ Complete |
| DRAFT-07 | Phase 36 — Dialog Integration & FAB | ✅ Complete |
| DRAFT-08 | Phase 36 — Dialog Integration & FAB | ✅ Complete |
| DRAFT-09 | Phase 36 — Dialog Integration & FAB | ✅ Complete |
| STD-04 | Phase 36 — Dialog Integration & FAB | ✅ Complete |

**Coverage:**
- v3.0 requirements: 54 total (12 AIFND + 10 SMS + 9 OCR + 10 VOICE + 9 DRAFT + 4 STD)
- Mapped to phases: 54/54
- Unmapped: 0

---
*Requirements defined: 2026-05-15*
*Last updated: 2026-05-15 — traceability table populated (roadmap phases 32–36)*
