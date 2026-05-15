# Features Research — AI-Assisted Transaction Drafting

**Domain:** AI-assisted data entry in Android personal finance app
**Milestone:** v3.0 — SMS, OCR, and Voice drafting flows
**Researched:** 2026-05-15
**Confidence:** HIGH (codebase direct inspection) / MEDIUM (Android UX conventions from training data)

---

## How to Read This Document

Each flow is analyzed separately, then a shared section covers the "draft review" step and the entry point question. Features are tagged:

- **TABLE STAKES** — must ship in v3.0; absence makes the flow feel broken
- **DIFFERENTIATOR** — meaningful but optional; candidates for a v3.1 polish phase
- **ANTI-FEATURE** — do not build; reason given

Complexity ratings assume Kotlin/Compose/Hilt patterns already in place:
- **Low** — fewer than ~150 lines of new code, no new permissions beyond what the flow already requires
- **Medium** — new permission, new ViewModel, or meaningful async state machine
- **High** — third-party SDK, new system API, or significant cross-cutting concern

---

## Flow 1: SMS-Based Drafting

### Context

The user picks one SMS message from their inbox, its text becomes the input to the AI extraction. The app reads SMS via `READ_SMS` permission (dangerous permission, requires runtime grant). The existing codebase has no SMS reading code.

### Table Stakes

| Feature | Why Required | Complexity | "Without AI" Fallback |
|---------|--------------|------------|----------------------|
| Runtime `READ_SMS` permission request with clear rationale string ("To let you pick a bank SMS to auto-fill a transaction") | Users who decline are the majority — the app must handle denial gracefully and still allow manual entry | Low | Permission denied → show plain text field where user pastes SMS body manually |
| SMS picker shows messages filtered to financial senders by default | Showing all 300+ SMS messages is unusable. Financial senders (bank OTPs, payment apps) have predictable sender ID patterns (e.g. "HDFCBK", "SBIINB", "PAYTM"). Default to a filtered view | Medium | Without filter: show all senders with a search field |
| Each SMS row shows: sender ID, message timestamp, and first 60 characters of the body | Users identify their transaction SMS by sender + preview, not by date alone. Without the preview, a list of "HDFCBK" entries is indistinguishable | Low | — |
| Show the full SMS body in a scrollable preview pane before the user taps "Use This Message" | Users must be able to verify they selected the right message before AI extraction runs. No surprise inputs | Low | — |
| "Use This Message" → "AI Fill" → spinner → pre-populated draft form | The core happy-path flow. Without this, the feature does not exist | Medium | "Use This Message" → opens form with only the raw text visible in the Note field; user fills manually |
| Multi-part SMS treated as a single message | Network OTPs and bank alerts are sometimes split across two SMS parts (140-byte GSM segments). The picker must reconstruct concatenated messages before displaying the preview and before passing to AI | Medium | Without reconstruction: user sees truncated message; AI sees incomplete data; inform user with "(continued)" label if reconstruction unavailable |
| Sender filter is editable by the user (can clear filter to see all SMS) | Some users have non-standard sender IDs (regional banks, corporate accounts). A locked filter would silently hide their messages | Low | — |
| SMS list ordered by date descending (most recent first) | Users are recording today's transaction, not last month's. The target SMS is almost always one of the most recent | Low | — |
| Maximum 50 SMS shown in the filtered list (paginate or cap) | Loading the full SMS inbox (potentially thousands) into memory for a form-filling flow is wasteful. A 50-message cap covers all practical cases | Low | — |

### Differentiators

| Feature | Value Proposition | Complexity |
|---------|-------------------|------------|
| Remember the last-used sender filter per session | After the user selects from HDFCBK once, they likely want HDFCBK again. Pre-select that sender next time the picker opens in the same session | Low |
| Show a badge on senders with unread/recent (last 7 days) messages | Helps users find the right sender cluster without reading every row | Low |
| "Save this sender as financial" toggle so it appears at top in future sessions | Persistent per-device preference in DataStore; sender list sorted by user-flagged senders first | Medium |

### Anti-Features

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Auto-scanning inbox in the background for financial SMS | Requires background process, additional `RECEIVE_SMS` broadcast, and will be flagged as surveillance-adjacent in Play Store review — Play Store policy has tightened heavily on SMS permission scope | User-initiated picker only; no background scanning |
| Sending SMS content to any remote server (cloud AI) | The entire v3.0 AI contract is Gemini Nano on-device. Any cloud fallback for SMS content violates user privacy expectations and potentially banking data regulations | Gemini Nano only; if AI unavailable, manual entry |
| Storing the raw SMS body in the transaction record by default | Full SMS bodies may contain OTP codes, account numbers, and balances — storing them permanently is a security risk and exposes sensitive data if the device is accessed | See "Saving raw source text" in the Shared section |

### Dependencies on Existing Code

- `AddEditTransactionDialog` receives the draft via its existing parameter signature — no dialog changes needed to receive pre-populated values
- The permission request must happen at picker entry, not at app launch — handle denial with a fallback path

---

## Flow 2: Receipt / Invoice OCR Drafting

### Context

The user captures a photo or picks from gallery → ML Kit's on-device OCR (`TextRecognition` with `TextRecognizerOptions.Builder()`) converts the image to a text block → text is sent to Gemini Nano for extraction. ML Kit Text Recognition v2 is an unbundled module that downloads on first use (no APK size increase).

### Camera vs Gallery — Primary

**Camera is primary.** Rationale: receipts degrade rapidly (thermal paper fades, physical damage), users record them at point of purchase. Gallery pick is secondary (for invoice PDFs converted to image, or screenshots of e-receipts). The UI should show a camera button as the main CTA and a "From Gallery" secondary action.

### Table Stakes

| Feature | Why Required | Complexity | "Without AI" Fallback |
|---------|--------------|------------|----------------------|
| Camera capture as the primary action (uses `ActivityResultContracts.TakePicture`) | Point-of-purchase recording is the dominant case. Camera-first matches user intent | Medium | Camera → user sees OCR text in read-only pane → user manually fills form |
| Gallery pick as secondary action (`ActivityResultContracts.GetContent` with `image/*`) | E-receipts, screenshots, scanned invoices from gallery are a real secondary use case | Low | Same as camera fallback |
| Show the captured/selected image as a thumbnail alongside the OCR text | Users need to verify the OCR extracted the right region. Without the image, they cannot spot if the wrong receipt was captured | Low | Image always shown; OCR text always shown |
| OCR text shown in a scrollable read-only pane before "AI Fill" | Users must see what the AI will receive. Opaque input → untrusted output | Low | Without AI: this pane is the input; user reads it and fills form manually |
| Pre-process OCR text before sending to AI: normalize common OCR noise | Common noise patterns: `0` vs `O` confusion in currency amounts (e.g. `T0TAL` → `TOTAL`), `1` vs `l` confusion, rupee symbol variants (`₹`, `Rs`, `Rs.`, `Rs `, `INR`). Run a lightweight regex normalization pass to clean the most common cases before the AI prompt. This improves extraction accuracy without requiring a better model | Low | Normalization still runs even without AI — cleaned text in the read-only pane is easier for the user to read |
| OCR failure state (no text detected, blurry image) shows a retry prompt | Blank or near-blank OCR output should not proceed to AI. Show "No text detected — try a clearer photo" and offer retake | Low | — |
| `CAMERA` permission request with clear rationale | Runtime permission required; denial path must allow gallery fallback | Low | On denial: hide camera button; show gallery-only picker |
| OCR progress indicator while ML Kit runs (typically 200–600ms) | Without feedback, users tap again thinking nothing happened | Low | — |
| Attach the captured image to the transaction as a receipt automatically | The image was captured specifically for this transaction — it should become the `receiptPath` on the saved transaction without requiring the user to also tap "Attach" separately. `TransactionEntity.receiptPath` already supports base64-encoded image data | Low | Still attach automatically; receipt attachment is independent of AI |

### Differentiators

| Feature | Value Proposition | Complexity |
|---------|-------------------|------------|
| Crop/rotate controls before OCR runs | Receipts are often photographed at angles; a tilt correction improves OCR accuracy significantly. Android's `UCropActivity` (open source, no APK size penalty) handles this in ~3 lines of integration | Medium |
| Highlight OCR confidence regions on the image (low-confidence text shown with amber underline) | ML Kit returns a confidence score per word. Showing the user which words OCR was uncertain about lets them correct those fields manually before saving | High |
| "Try again with better photo" when AI confidence in extracted amount is low | If the Gemini Nano extraction returns an amount of 0.0 or null, prompt the user to retake rather than silently saving a zero-amount draft | Low |

### Anti-Features

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| PDF receipt support in v3.0 | PDF parsing requires `PdfRenderer` (API 21+, only works for single-page PDFs without restrictions) or a third-party library. The complexity is disproportionate to the use case frequency in a personal finance app | Support only images (JPEG/PNG) in v3.0; add PDF support in a later phase if demand is confirmed |
| Cloud OCR (Google Cloud Vision, AWS Textract) as fallback | Sends receipt images — which contain merchant names, prices, and potentially partial card numbers — to a cloud service. Privacy violation for sensitive financial documents | ML Kit on-device only; if ML Kit module download fails, show manual entry path |
| Dual-image display (original + annotated) on a single screen | Annotated image with bounding boxes around extracted fields looks impressive in demos but consumes the majority of screen space on a phone, leaving no room for the form fields. The cognitive load of reconciling the image with the form is high | Show image thumbnail (tappable to full-screen) + OCR text pane + "AI Fill" button as a vertical stack |
| Video capture / multi-frame receipt scanning | Adds motion blur handling, frame selection complexity. No meaningful accuracy improvement over a single well-lit still photo for the receipt use case | Single still image only |

### OCR Pre-Processing — Normalization Rules

This is table stakes behavior. The normalization pass runs before the AI prompt, not after.

```
1. Normalize rupee variants: Rs., Rs , INR, ₹  →  ₹
2. Strip currency symbol before amount: ₹1,234.56  →  1234.56  (keep as string for AI)
3. Replace O with 0 in numeric contexts: T0TAL  →  TOTAL  (only O surrounded by digits)
4. Normalize comma-as-thousands-separator: 1,234.56  →  1234.56
5. Collapse multiple whitespace runs to single space
6. Strip header/footer boilerplate: lines matching "GST NO:", "FSSAI:", "CIN:" etc (merchant metadata)
7. Trim lines under 3 characters (isolated OCR noise fragments)
```

Complexity: Low. Pure string processing, no library required, runs synchronously in under 5ms even for large receipts.

### Dependencies on Existing Code

- `FileHelper.saveReceipt(context, uri)` already converts URI to base64 for `receiptPath` — the OCR flow must pass the same URI through this function to populate the receipt attachment, not create a parallel storage path
- `AddEditTransactionDialog` already renders the receipt thumbnail via `FormReceiptPreviewDialog` — no dialog changes needed for receipt display

---

## Flow 3: Voice Memo Drafting

### Context

Android's `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE = true` runs fully on-device using the device's language pack. No network required. The user speaks the transaction (e.g. "Paid 450 rupees for groceries at D-Mart") and the transcription feeds the AI.

### Table Stakes

| Feature | Why Required | Complexity | "Without AI" Fallback |
|---------|--------------|------------|----------------------|
| Tap-to-start / tap-to-stop recording paradigm (not hold-to-record) | Hold-to-record is appropriate for short ephemeral voice messages (walkie-talkie UX). Transaction descriptions can be 5–15 seconds; users should not have to hold their finger down | Low | Same tap-start/stop UI; transcription displayed; user edits transcription manually before saving |
| Animated waveform or pulsing microphone icon while recording | Without visual feedback, users cannot tell if the mic is active. 1-2 second startup latency means users often start speaking before recording is active — visual feedback manages this expectation | Low | — |
| Transcription displayed in full, editable text field immediately after recording stops | Users must be able to correct transcription errors (homophones, amounts, merchant names) before passing to AI. An editable field gives them control | Low | Without AI: the editable text field IS the output — user corrects and uses it as the note |
| Maximum recording duration: 60 seconds | `SpeechRecognizer` has an undocumented ~60-second timeout on most devices. Making the limit explicit (show a countdown from 10 seconds before cutoff) prevents silent truncation that would confuse the AI | Low | — |
| RECORD_AUDIO permission request with clear rationale | Runtime permission; denial must be handled with a "Voice input requires microphone permission" message and a path back to manual entry | Low | On denial: hide voice entry point; show manual entry |
| Offline availability check before showing voice entry point | `SpeechRecognizer.isRecognitionAvailable(context)` returns false if no offline language pack is installed. If false, show "Voice input requires offline speech recognition. Download it in your device's Language & Input settings." — do not silently fail | Low | — |
| "AI Fill" button appears below the transcription, disabled until transcription is non-empty | Clear affordance: user records → sees transcription → taps AI Fill → gets draft. The button state communicates where they are in the flow | Low | Without AI: "AI Fill" button is hidden; a "Use as Note" button appears instead, which copies the transcription into the Note field and opens the form |
| Re-record button clears current transcription and starts a new recording | Users frequently misspeak or record the wrong amount. One-tap retry | Low | — |

### Differentiators

| Feature | Value Proposition | Complexity |
|---------|-------------------|------------|
| Language auto-detection based on device locale setting | `SpeechRecognizer` uses the device locale by default. Explicitly passing `RecognizerIntent.EXTRA_LANGUAGE` derived from the app's currency locale (e.g., INR → `hi_IN` or `en_IN`) improves recognition accuracy for Indian English and Hindi-English code-switching | Low |
| Confidence score display below transcription ("High / Medium / Low confidence") | `RecognitionListener.onResults()` returns confidence floats per alternative. Showing low confidence nudges users to verify the transcription before trusting AI extraction | Low |
| Multiple transcription alternatives shown as chips (user selects best match) | `RecognitionListener.onResults()` returns up to 5 alternatives. Showing the top 3 as selectable chips lets the user pick the most accurate one. Especially useful for merchant names | Medium |

### Anti-Features

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Cloud speech-to-text (Google Cloud Speech API, Whisper API) | Requires network, sends audio of the user's financial conversation to a cloud service, adds latency, and violates the offline/privacy contract of this milestone | `SpeechRecognizer` with `EXTRA_PREFER_OFFLINE` only |
| Continuous listening / ambient transaction detection | Voice-activated ("always-on") recording for transaction detection is a privacy violation and a Play Store policy risk — it would likely result in rejection | User-initiated only; mic opens only when user taps |
| Voice commands for navigation (e.g., "go to reports") | Scope creep. The voice feature is specifically a transaction drafting input, not a general voice assistant | Transcription → AI extraction only; no command parsing |
| Waveform visualization with real-time amplitude rendering | Requires `AudioRecord` instead of `SpeechRecognizer`, adds 50–100ms latency for buffer reads, and is complex to synchronize with the speech recognition stream. A simple pulsing icon achieves the same user goal | Pulsing mic icon with CSS-style animation is sufficient |

### Dependencies on Existing Code

- No existing STT code in the codebase — all new in `ui/aidraft/`
- `RECORD_AUDIO` permission must be added to `AndroidManifest.xml`

---

## Shared: Draft Review Step

### Where the Draft Opens

**Pre-populate `AddEditTransactionDialog`, do not create a new screen.**

Rationale from codebase inspection:

1. `AddEditTransactionDialog` is a full-screen `Dialog` with `DialogProperties(usePlatformDefaultWidth = false)` — it already fills the screen and behaves like a screen, not a modal popup
2. It accepts `transaction: TransactionEntity?` as its first parameter. When `null`, it creates a new transaction. A `TransactionDraft` → `TransactionEntity` conversion step produces the right input
3. All 6 transaction types, all field sections (amount, category, peer, account, note, tags, split), and validation are already implemented. Recreating this in a new screen doubles maintenance burden for no user benefit
4. The existing dialog already has a scrollable form body — adding a "Source" info bar at the top (see below) is a single composable addition, not a restructure

**How to inject the draft:** Add an optional `draft: TransactionDraft?` parameter to `AddEditTransactionDialog`. When non-null, use it to pre-set the `rememberSaveable` state variables for type, amount, accountId, categoryId, note, peerContactId, date. The `isEdit = transaction != null` logic is unchanged.

Alternatively — and more architecturally clean — pass the draft values as the initial values of `TransactionFormState` before the dialog opens, using the existing `TransactionFormConverter` (already present at `ui/dialogs/TransactionFormConverter.kt`).

### Table Stakes for Draft Review

| Feature | Why Required | Complexity |
|---------|--------------|------------|
| AI-suggested fields visually distinguished from user-entered fields | Users must know which fields to trust and which to verify. Without highlighting, they may save incorrect AI suggestions without review. Use a subtle tinted background (e.g. `primaryContainer.copy(alpha = 0.15f)`) on pre-filled fields + a small sparkle/auto-fill icon badge | Low |
| AI-suggested field highlighting clears when user edits that field | Once the user touches a field, it becomes "user-entered" and should lose the AI indicator. Tracks which fields were AI-suggested vs user-modified in ViewModel state | Low |
| Source banner at the top of the draft form showing the input type | A small non-intrusive bar: "Draft from SMS · HDFCBK · 2 minutes ago" or "Draft from Receipt" or "Draft from Voice". Establishes context so users understand why fields are pre-populated | Low |
| "AI Fill" button or badge is absent from draft review (the AI already ran) | The review step is post-AI. No second AI button should appear here — it would confuse users about whether they need to tap it again | Low |
| All form fields remain fully editable | AI suggestions are starting points, not locked values. Every field — including type, amount, date, account, category — must be editable just as in a normal new-transaction flow | Low (no change needed — dialog is already fully editable) |
| If AI could not confidently extract a field, that field remains empty (not guessed) | An empty field communicates "I don't know" — a wrong value communicates false confidence. The Gemini Nano prompt must instruct the model to omit fields it cannot determine rather than guess | Low (prompt design, not UI work) |
| Validation and save behavior identical to normal transaction entry | The form validation (amount > 0, account required) and `onConfirm` callback are unchanged. Draft review is not a special save path | Low (no change needed) |

### Differentiators for Draft Review

| Feature | Value Proposition | Complexity |
|---------|-------------------|------------|
| Show confidence level per AI-suggested field ("high / medium / low" based on model output) | If Gemini Nano can return per-field confidence, showing it lets users know which fields need the most verification. Requires structured JSON output from the model | Medium |
| "Undo AI Fill" button that resets the form to blank | Lets users quickly discard a bad AI extraction and start over manually without closing and reopening the form | Low |

### Anti-Features for Draft Review

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| New full-screen "Review Draft" screen before the form | Adds a screen transition and a mental model of "I need to review, then go to the form" — two steps where one suffices. The existing dialog IS the review step | Open the dialog with pre-populated fields directly |
| "Auto-save" after AI extraction (skipping the review form entirely) | The AI will sometimes extract wrong amounts, wrong types, wrong accounts. Auto-saving without review is a data integrity risk in a personal finance app | Always require explicit user Save action |
| Locking AI-suggested fields from editing | Users need to fix extraction errors. Locked fields mean corrections require close-reopen-and-enter-manually — more friction than the draft flow saves | All fields editable, AI suggestion is informational only |
| Showing a diff between AI suggestion and any previous value | There is no "previous value" in the add-new flow. In edit-existing, AI drafting is not a defined use case in v3.0 | Not applicable; out of scope |

---

## "AI Fill" Entry Point — Placement Analysis

### Current Navigation Structure (from codebase)

- Bottom nav: Summary · Transactions · Settings (3 items)
- FAB on TransactionsScreen: circular `+` button, `primaryContainer` color, `CircleShape`, opens `AddEditTransactionDialog` directly
- No existing expanded FAB (no `ExtendedFloatingActionButton`)
- `AddEditTransactionDialog` is a full-screen dialog triggered from TransactionsScreen
- The existing FAB maps to a single action: "add transaction"

### Recommendation

**Add a second FAB for AI-assisted drafting (expandable FAB pattern), scoped to TransactionsScreen only.**

Specifically: replace the single FAB with a `SmallFloatingActionButton` (the "+" for manual entry) + a larger primary FAB with a sparkle/auto-fill icon that expands to show the three input sources (SMS · Receipt · Voice) as a mini-FAB column.

This is the Material Design 3 "FAB menu" pattern — tap the primary FAB once to expand the 3 options, tap again or tap away to collapse.

**Compose implementation:** `Column` of `SmallFloatingActionButton` items with `AnimatedVisibility`. The main FAB uses `Icons.Default.AutoAwesome` or a similar "magic" icon to communicate AI-assistance.

Rationale:

1. The existing FAB is already the "add transaction" CTA. AI-assisted drafting is a distinct entry point (different flow, different first step) — it should not replace the manual entry button but sit alongside it
2. TransactionsScreen is the correct host because that is where transactions are created and listed
3. Bottom nav does not gain a new item — AI drafting is a feature of transaction entry, not a top-level section
4. The Transactions FAB is already visible and familiar — extending it is lower cognitive load than adding a new nav item
5. The expand-on-tap FAB pattern avoids needing a separate "Choose input method" screen

### Table Stakes for Entry Point

| Feature | Why Required | Complexity |
|---------|--------------|------------|
| AI drafting FAB is visible only when `isAiAssistAvailable = true` | When AICore is unavailable, the AI FAB should not appear — it would raise expectations the app cannot meet. The existing `+` FAB is unchanged and always visible | Low |
| Three source options: SMS, Receipt, Voice — each with an icon and label | Users need to understand which input method they are selecting at a glance | Low |
| SMS option hidden when `READ_SMS` permission is permanently denied | After permanent denial, the SMS flow is inaccessible. Hiding the option prevents a confusing dead end | Low |
| Voice option hidden when `SpeechRecognizer.isRecognitionAvailable` returns false | Offline recognition unavailable → voice option is non-functional; hide it to prevent confusion | Low |
| Entry points available from Summary screen's "Add transaction" CTA as well | The Summary screen also has an `onNavigateToAddTransaction` callback. A deep link or shared ViewModel event can expose AI drafting entry from there too | Medium |

### Anti-Features for Entry Point

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| Bottom nav item "AI Draft" or "Smart Add" | AI drafting is a method of creating a transaction, not a content section. A nav item implies a browsable destination with history/list — there is none | FAB entry point only |
| Replacing the existing `+` FAB entirely with a multi-mode FAB | The existing FAB has a well-established meaning in this app: "add a transaction manually". Removing it breaks users' existing muscle memory | Keep the existing `+` FAB; add the AI FAB alongside it |
| A dedicated "AI Drafting" screen in the Settings area | Settings is for configuration, not data entry. Putting a drafting entry point in Settings ensures almost no one discovers it | FAB on TransactionsScreen + optionally Summary CTA |

---

## Shared: Saving Raw Source Text

**Classification: DIFFERENTIATOR (not table stakes)**

The question is whether to save the raw SMS body / OCR text / voice transcription alongside the saved transaction.

Arguments for saving it:
- Transparency: user can re-open a transaction and see what SMS message triggered it
- Dispute resolution: if the AI extracted a wrong amount, the raw source serves as an audit trail
- Future: could power a "re-extract" feature if the AI model improves

Arguments against saving it by default:
- SMS bodies contain OTP codes, partial account numbers, and balances. Storing them in Room is a data security risk if the device is not encrypted or is accessed by another app
- Receipts are already stored as `receiptPath` (base64 image) — storing the OCR text separately is redundant
- Voice transcriptions are benign but storage in a general notes field is fine

**Recommendation:** Store the source text optionally, behind a toggle in the draft form ("Keep source text as note"). Default to OFF for SMS (security risk), default to ON for voice (transcription is useful as a note). For OCR: the image is already stored as receipt; do not also store the OCR text string by default.

**Implementation:** This does not require a new database column. Map the source text to the existing `note` field if the user opts in. No schema migration required.

---

## Shared: AI Accuracy Feedback Loop

**Classification: OUT OF SCOPE for v3.0**

A feedback mechanism (thumbs up/down per draft, correction tracking, local fine-tuning) requires:
- UI additions to the draft review form
- Local storage for feedback signals
- A path to use those signals (Gemini Nano does not support on-device fine-tuning from user feedback as of mid-2025)
- Non-trivial privacy analysis of what signals are collected

This is a valid v3.x feature but adds scope and complexity that does not improve the core v3.0 flow. The baseline is: Gemini Nano with a well-structured dynamic prompt (with user's live categories/accounts/peers injected) already produces high-accuracy drafts for the common SMS and voice cases without user feedback.

---

## Feature Summary Tables (by classification)

### Table Stakes (v3.0 must-have)

| Flow | Feature | Complexity |
|------|---------|------------|
| SMS | Runtime READ_SMS permission with rationale + denial fallback | Low |
| SMS | Financial sender filter (editable) | Medium |
| SMS | Sender / timestamp / 60-char preview per row | Low |
| SMS | Full message preview pane before "Use This Message" | Low |
| SMS | Multi-part SMS reconstruction | Medium |
| SMS | Date-descending order, max 50 results | Low |
| SMS | "Use This Message" → AI Fill → draft in existing dialog | Medium |
| SMS | Manual entry fallback (SMS body → Note field, no AI) | Low |
| OCR | Camera as primary action, gallery as secondary | Medium |
| OCR | Image thumbnail shown alongside OCR text pane | Low |
| OCR | OCR text in scrollable read-only pane pre-AI | Low |
| OCR | Pre-processing / normalization before AI prompt | Low |
| OCR | OCR failure state with retry | Low |
| OCR | CAMERA permission with rationale + gallery fallback on denial | Low |
| OCR | OCR progress indicator | Low |
| OCR | Auto-attach captured image as receipt | Low |
| OCR | Manual entry fallback (OCR pane → user fills form) | Low |
| Voice | Tap-start / tap-stop recording | Low |
| Voice | Animated mic / waveform feedback during recording | Low |
| Voice | Editable transcription field post-recording | Low |
| Voice | 60-second max duration with countdown | Low |
| Voice | RECORD_AUDIO permission + denial fallback | Low |
| Voice | Offline availability check (hide if unavailable) | Low |
| Voice | Re-record button | Low |
| Voice | "AI Fill" disabled until transcription non-empty | Low |
| Voice | "Use as Note" fallback when AI unavailable | Low |
| Shared | Pre-populate existing AddEditTransactionDialog (no new screen) | Medium |
| Shared | AI-suggested fields visually distinguished (tinted + badge) | Low |
| Shared | Highlighting clears when user edits a field | Low |
| Shared | Source banner ("Draft from SMS · HDFCBK") at top of form | Low |
| Shared | All fields remain fully editable post-draft | Low (no change) |
| Shared | Empty fields for low-confidence AI extractions | Low (prompt design) |
| Shared | AI drafting FAB only shown when AI available | Low |
| Shared | Three-source expand FAB (SMS / Receipt / Voice) | Medium |
| Shared | Individual source options hidden when access unavailable | Low |

### Differentiators (v3.1 candidates)

| Flow | Feature | Complexity |
|------|---------|------------|
| SMS | Remember last-used sender filter per session | Low |
| SMS | Recent-sender badge | Low |
| SMS | "Save this sender as financial" persistent preference | Medium |
| OCR | Crop/rotate controls before OCR | Medium |
| OCR | OCR confidence region highlighting on image | High |
| OCR | Retry prompt when AI returns zero amount | Low |
| Voice | Locale-aware language selection for recognizer | Low |
| Voice | Confidence score display under transcription | Low |
| Voice | Multiple transcription alternatives as selectable chips | Medium |
| Shared | Per-field AI confidence indicators | Medium |
| Shared | "Undo AI Fill" button | Low |
| Shared | Source text preserved as note (opt-in toggle, default OFF for SMS / ON for Voice) | Low |
| Shared | Entry point from Summary screen CTA | Medium |

### Anti-Features (do not build)

| Flow | Anti-Feature | Reason |
|------|-------------|--------|
| SMS | Background SMS scanning | Play Store policy risk; surveillance concern |
| SMS | Cloud AI for SMS content | Privacy / banking data violation |
| SMS | Storing raw SMS body by default | Security risk (OTPs, account numbers) |
| OCR | PDF support in v3.0 | Complexity vs frequency mismatch |
| OCR | Cloud OCR | Sends financial receipt images off-device |
| OCR | Dual-image annotated view | Screen real estate; high cognitive load |
| OCR | Video/multi-frame scanning | Complexity for negligible accuracy gain |
| Voice | Cloud STT | Network dependency; privacy violation |
| Voice | Continuous/ambient listening | Privacy violation; Play Store rejection risk |
| Voice | Voice navigation commands | Scope creep; not part of this flow |
| Voice | Real-time amplitude waveform | Requires AudioRecord; complex; cosmetic only |
| Shared | New "Review Draft" screen before form | Unnecessary extra step |
| Shared | Auto-save without review | Data integrity risk |
| Shared | Locked AI-suggested fields | Prevents error correction |
| Shared | Bottom nav "AI Draft" item | AI drafting is not a content section |
| Shared | Replace existing + FAB | Breaks existing muscle memory |
| Shared | AI feedback loop / correction tracking | Out of scope v3.0; Gemini Nano lacks fine-tuning API |

---

## Confidence Assessment

| Area | Confidence | Basis |
|------|------------|-------|
| Codebase structure (dialog params, FAB placement, nav) | HIGH | Direct file inspection |
| AddEditTransactionDialog pre-population feasibility | HIGH | Read full source; `rememberSaveable` state vars clearly injectable |
| TransactionFormState / TransactionFormConverter integration | HIGH | Both files read; `TransactionFormConverter.kt` already exists for this purpose |
| Android SMS permission behavior | MEDIUM | Training data; permission model well-established but Play Store policy details evolve |
| ML Kit TextRecognition on-device behavior | MEDIUM | Training data; module name and API confirmed, performance numbers are approximations |
| SpeechRecognizer EXTRA_PREFER_OFFLINE reliability | MEDIUM | Training data; known to vary by device and language pack availability |
| Gemini Nano / AICore API details | MEDIUM | Training data current to mid-2025; Android AICore API has evolved — verify against current Android AI SDK docs before implementation |
| OCR normalization regex rules | MEDIUM | Based on commonly observed Indian bank SMS formats; specific patterns may vary |

---

## Sources

- `AddEditTransactionDialog.kt` — direct inspection; HIGH confidence for all dialog-related recommendations
- `TransactionFormConfig.kt` + `TransactionFormState.kt` — direct inspection; HIGH confidence for form extension approach
- `TransactionsScreen.kt` — direct inspection; HIGH confidence for FAB and dialog trigger patterns
- `MoneyManagerNavHost.kt` — direct inspection; HIGH confidence for navigation structure
- `PROJECT.md` — direct inspection; HIGH confidence for v3.0 constraints (Gemini Nano, offline, `data/ai/`, `ui/aidraft/` packages)
- Android `SpeechRecognizer`, `TextRecognition`, `ActivityResultContracts` — MEDIUM confidence (training data, stable APIs)
- Material Design 3 FAB menu pattern — MEDIUM confidence (training data)
- Play Store SMS permission policy — MEDIUM confidence (training data; verify before submission)
