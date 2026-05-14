---
quick_id: 260504-ewv
type: quick
description: Fix PIN and Biometric Lock Issues
wave: 1
depends_on: []
files_modified:
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/auth/LockScreen.kt
  - MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt
autonomous: true
requirements: [LOCK-FIX-01, LOCK-FIX-02, LOCK-FIX-03, LOCK-FIX-04]

must_haves:
  truths:
    - "After enabling PIN lock and re-locking the app, entering the correct existing PIN unlocks the app (not treated as a brand-new PIN setup)"
    - "Entering an incorrect PIN at the lock screen shows the 'Wrong PIN. N attempts remaining.' error and decrements the remaining attempts counter"
    - "The PinSetupDialog in Settings shows a numeric keyboard (NumberPassword) when the OutlinedTextField is focused, not the default text keyboard"
    - "If a user toggles biometric ON after the lock screen is already visible, the biometric prompt becomes available without requiring the user to leave and re-enter the lock screen"
    - "First-time PIN creation (when no PIN is set) continues to work via the existing showSetup flow in MoneyManagerNavHost (no regression)"
  artifacts:
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/auth/LockScreen.kt"
      provides: "Lock screen routes existing-PIN flow to PinLockScreen with isSetup=false"
      contains: "isSetup = false"
    - path: "MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt"
      provides: "PinSetupDialog OutlinedTextField uses numeric keyboard"
      contains: "KeyboardType.NumberPassword"
  key_links:
    - from: "LockScreen.kt unlock branch (else clause)"
      to: "PinLockScreen onPinEntered verification path"
      via: "isSetup = false routes 4-digit input through onPinEntered (not onPinSetupComplete)"
      pattern: "isSetup\\s*=\\s*false"
    - from: "LockScreen.kt LaunchedEffect"
      to: "biometricEnabled state changes"
      via: "LaunchedEffect keyed on biometricEnabled re-runs trigger logic when toggle flips"
      pattern: "LaunchedEffect\\(biometricEnabled"
    - from: "SettingsScreen.kt PinSetupDialog OutlinedTextField"
      to: "Android numeric soft keyboard"
      via: "KeyboardOptions(keyboardType = KeyboardType.NumberPassword)"
      pattern: "keyboardType\\s*=\\s*KeyboardType\\.NumberPassword"
---

<objective>
Fix four bugs in the PIN/biometric lock flow that prevent unlock from working and degrade the setup UX:
1. Unlock flow incorrectly treats existing-PIN entry as setup (isSetup=true), so the verify path is never reached.
2. Biometric prompt does not re-trigger when biometricEnabled changes while the lock screen is mounted.
3. The Settings PinSetupDialog OutlinedTextField does not request a numeric keyboard.
4. Verify (no code change expected) that BiometricAuthManager.checkAvailability() is non-blocking inside its LaunchedEffect call site.

Issue #2 from the analysis (onSetupRequired wiring) is already correctly handled in MoneyManagerNavHost.kt (lines 127–225) via the showSetup branch — verified during planning, no code change needed.

Purpose: Restore working PIN unlock and improve PIN entry UX. Without these fixes, the lock screen is effectively non-functional once a PIN is set.

Output: LockScreen.kt + SettingsScreen.kt patched; debug build green; manual smoke test confirms unlock + settings dialog UX.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/auth/LockScreen.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/auth/PinLockScreen.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt
@MoneyManager/app/src/main/java/com/moneymanager/app/ui/MoneyManagerNavHost.kt
@MoneyManager/app/src/main/java/com/moneymanager/data/security/BiometricAuthManager.kt

<interfaces>
<!-- Key contracts the executor needs without re-exploring the codebase. -->

PinLockScreen contract (from PinLockScreen.kt):
```kotlin
@Composable
fun PinLockScreen(
    isSetup: Boolean = false,           // true = creation flow, false = verify flow
    isConfirming: Boolean = false,
    storedPinHash: String? = null,
    storedPinSalt: String? = null,
    remainingAttempts: Int = 5,
    onPinEntered: (String) -> Unit,         // called in verify flow + on confirming step
    onPinSetupComplete: (String) -> Unit,   // called only when isSetup=true && !isConfirming
    onBiometricRequested: (() -> Unit)? = null,
    showBiometricButton: Boolean = false,
    biometricStatus: String = ""
)
```
Routing logic inside PinLockScreen.PinKeypad onNumberClick (PinLockScreen.kt:96-103):
- isSetup && !isConfirming -> onPinSetupComplete(pin)
- isSetup &&  isConfirming -> onPinEntered(pin)
- else                     -> onPinEntered(pin)

Therefore: passing isSetup=true to LockScreen's else-branch (which is the "PIN already exists, verify it" branch) bypasses the onPinEntered verification path entirely.

LockScreen current bug site (LockScreen.kt:108-162):
- Line 108: `} else if (!isPinSetup) { onSetupRequired() }`  -> creation flow handed to parent (correct, NavHost owns it)
- Line 110: `} else {`                                       -> existing-PIN unlock flow
- Line 112: `isSetup = true,`                                 -> BUG: must be false in unlock branch

LaunchedEffect bug site (LockScreen.kt:67):
- `LaunchedEffect(Unit) { ... }` should be `LaunchedEffect(biometricEnabled) { ... }` so toggling biometric while lock screen is visible re-runs collection logic. Note: `preferencesManager.pinEnabled.collect { ... }` reads biometricEnabled INSIDE the collector, but the outer LaunchedEffect's keying does not retrigger when biometricEnabled changes via another path. Re-keying on biometricEnabled is safe because the inner collector re-runs and uses the latest .first() value.

Settings PinSetupDialog bug site (SettingsScreen.kt:780-786):
```kotlin
OutlinedTextField(
    value = enteredPin,
    onValueChange = { if (it.length <= 4) onEnteredPinChange(it) },
    placeholder = { Text("----") },
    singleLine = true,
    isError = error != null
    // MISSING: keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    // MISSING (recommended): visualTransformation = PasswordVisualTransformation()
)
```
Imports already present in SettingsScreen.kt (lines 10-11):
- import androidx.compose.foundation.text.KeyboardOptions
- import androidx.compose.ui.text.input.KeyboardType

NavHost setup branch (already correct, MoneyManagerNavHost.kt:127-226):
- `onSetupRequired = { showSetup = true }` triggers a custom Compose setup UI (not PinLockScreen) with KeyboardType.NumberPassword + PasswordVisualTransformation.
- This means analysis Issue #2 is ALREADY fixed in the codebase; do NOT add a new setup composable.

Issue #5 verification (BiometricAuthManager.checkAvailability):
- Called at LockScreen.kt:81 inside `preferencesManager.pinEnabled.collect { ... }` block, which already runs in the LaunchedEffect coroutine context (not on the main thread synchronously during composition).
- Confirm by reading BiometricAuthManager.checkAvailability() — it should be a fast BiometricManager.from(context).canAuthenticate(...) call. No fix expected; document verification result in the SUMMARY.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Fix LockScreen PIN verify routing and biometric re-trigger</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/auth/LockScreen.kt</files>
  <action>
    Apply two surgical edits to LockScreen.kt:

    Edit A — Fix PIN verify routing (analysis Issue #1, addresses LOCK-FIX-01):
    - Locate the existing-PIN unlock branch (`} else {` at line 110, the final branch after `else if (!isPinSetup) { onSetupRequired() }`).
    - Inside the `PinLockScreen(...)` call, change `isSetup = true,` to `isSetup = false,`.
    - This is the ONLY argument change required. Do NOT touch onPinSetupComplete — it can remain wired (PinLockScreen will not invoke it when isSetup=false except via the isConfirming branch which is unused on the unlock path; leave as defensive code).
    - Verify by reading the patched lines: line ~112 must now show `isSetup = false,`.

    Edit B — Re-key biometric LaunchedEffect (analysis Issue #4, addresses LOCK-FIX-02):
    - Locate `LaunchedEffect(Unit) {` at line 67.
    - Change the key to `biometricEnabled`: `LaunchedEffect(biometricEnabled) {`.
    - Rationale: when the user enables/disables biometric in Settings while the lock screen is composed (rare but possible via a deep navigation back-stack scenario or process death restoration), the effect re-runs and re-evaluates whether to trigger the biometric prompt.
    - Note: `biometricEnabled` is already declared as `var biometricEnabled by remember { mutableStateOf(false) }` on line 37; this is a stable Compose state key.
    - The inner `preferencesManager.pinEnabled.collect { ... }` already re-reads `biometricEnabled` via `.first()` on each emission, so re-keying is correct (no double-trigger risk because previous coroutine is cancelled when key changes).

    Do NOT modify any other lines in LockScreen.kt. Do NOT change function signatures. Do NOT touch onSetupRequired wiring (it's correctly handled in MoneyManagerNavHost.kt — verified during planning).

    Pre-edit safety: this task modifies LockScreen.kt only. Per project CLAUDE.md, run `gitnexus_impact({target: "LockScreen", direction: "upstream"})` before editing and report blast radius. Expected: only MoneyManagerNavHost.kt depends on LockScreen (single caller), so risk is LOW.

    Implements LOCK-FIX-01 and LOCK-FIX-02.
  </action>
  <verify>
    <automated>cd MoneyManager; ./gradlew.bat :app:compileDebugKotlin --console=plain</automated>
    Then visually grep the file:
    - `Grep` for `isSetup = false` in LockScreen.kt -> must match (was `isSetup = true`)
    - `Grep` for `LaunchedEffect(biometricEnabled)` in LockScreen.kt -> must match (was `LaunchedEffect(Unit)`)
    - `Grep` for `isSetup = true` in LockScreen.kt -> must NOT match anywhere
  </verify>
  <done>
    - LockScreen.kt:112 reads `isSetup = false,` in the unlock branch.
    - LockScreen.kt:67 reads `LaunchedEffect(biometricEnabled) {`.
    - `:app:compileDebugKotlin` succeeds with no errors.
    - `gitnexus_detect_changes` shows only LockScreen.kt modified.
  </done>
</task>

<task type="auto">
  <name>Task 2: Add numeric keyboard to Settings PinSetupDialog</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt</files>
  <action>
    Apply a single surgical edit to the `PinSetupDialog` private composable (SettingsScreen.kt:759-810):

    Locate the OutlinedTextField at line 780-786:
    ```kotlin
    OutlinedTextField(
        value = enteredPin,
        onValueChange = { if (it.length <= 4) onEnteredPinChange(it) },
        placeholder = { Text("----") },
        singleLine = true,
        isError = error != null
    )
    ```

    Add `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)` as the last named argument (after `isError`):
    ```kotlin
    OutlinedTextField(
        value = enteredPin,
        onValueChange = { if (it.length <= 4) onEnteredPinChange(it) },
        placeholder = { Text("----") },
        singleLine = true,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
    )
    ```

    Imports for `KeyboardOptions` (line 10) and `KeyboardType` (line 11) are ALREADY present — do not add new imports.

    Do NOT add `visualTransformation = PasswordVisualTransformation()` in this task — it is a UX nicety but not part of the analysis fix list, and adding it would require a new import. Out of scope.

    Do NOT modify any other composable, dialog, or function in SettingsScreen.kt.

    Pre-edit safety: per project CLAUDE.md, run `gitnexus_impact({target: "PinSetupDialog", direction: "upstream"})` first. Expected: only SettingsScreen.kt internal callers (showPinSetupDialog and biometric-triggered setup at lines 535 and 560), risk LOW.

    Implements LOCK-FIX-03.
  </action>
  <verify>
    <automated>cd MoneyManager; ./gradlew.bat :app:compileDebugKotlin --console=plain</automated>
    Then:
    - `Grep` for `KeyboardType.NumberPassword` in SettingsScreen.kt -> must match (currently no match in PinSetupDialog block)
    - `Grep` for `private fun PinSetupDialog` -> sanity check function still exists
    - Confirm imports unchanged (no new import lines added)
  </verify>
  <done>
    - SettingsScreen.kt PinSetupDialog OutlinedTextField has `keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)`.
    - `:app:compileDebugKotlin` succeeds.
    - `gitnexus_detect_changes` shows only SettingsScreen.kt modified by this task.
  </done>
</task>

<task type="auto">
  <name>Task 3: Verify biometric availability is non-blocking and run debug build</name>
  <files>MoneyManager/app/src/main/java/com/moneymanager/data/security/BiometricAuthManager.kt</files>
  <action>
    No code edit. This task is a verification + final build pass for analysis Issue #5 and overall regression check (LOCK-FIX-04).

    Step 1 — Inspect BiometricAuthManager.checkAvailability():
    - Read MoneyManager/app/src/main/java/com/moneymanager/data/security/BiometricAuthManager.kt.
    - Locate `checkAvailability()`. Confirm it is a thin wrapper around `BiometricManager.from(context).canAuthenticate(authenticators)` (or equivalent) and does NOT perform disk I/O, network calls, or block on a coroutine.
    - The call site in LockScreen.kt:81 is inside a `LaunchedEffect` block, so even a moderately slow call would not block composition. Document the finding.

    Step 2 — If checkAvailability() turns out to do non-trivial work (unexpected), DO NOT change it in this quick task. Instead, log the concern in the SUMMARY under "Follow-ups" and stop. Out of scope: refactoring to async availability.

    Step 3 — Final integration build:
    - From the MoneyManager directory run `./gradlew.bat :app:assembleDebug --console=plain`.
    - Confirm BUILD SUCCESSFUL.

    Step 4 — Pre-commit GitNexus check (per project CLAUDE.md):
    - Run `gitnexus_detect_changes({scope: "all"})`.
    - Expected affected files: ONLY LockScreen.kt and SettingsScreen.kt.
    - If any other file shows up unexpectedly, investigate before continuing.

    Step 5 — Manual smoke test instructions for the SUMMARY (do not execute, just document):
    - Enable PIN lock in Settings, set PIN = 1234.
    - Force-stop and relaunch app -> lock screen appears -> enter wrong PIN 2222 -> "Wrong PIN. 4 attempts remaining." -> enter 1234 -> unlocks (this is the regression that was broken).
    - In Settings, tap "Set up PIN" -> dialog opens -> tap text field -> numeric keyboard appears (no letters).
    - Toggle biometric ON in Settings while still on Settings -> navigate to a fresh launch -> lock screen shows fingerprint button.

    Implements LOCK-FIX-04 (verification + build gate).
  </action>
  <verify>
    <automated>cd MoneyManager; ./gradlew.bat :app:assembleDebug --console=plain</automated>
    BUILD SUCCESSFUL line must appear in stdout. No compilation errors. No new lint warnings introduced in LockScreen.kt or SettingsScreen.kt.
  </verify>
  <done>
    - `:app:assembleDebug` BUILD SUCCESSFUL.
    - BiometricAuthManager.checkAvailability() inspected; verdict (blocking / non-blocking) recorded in SUMMARY.
    - `gitnexus_detect_changes` confirms only LockScreen.kt and SettingsScreen.kt changed.
    - SUMMARY contains the four manual smoke-test steps for the user to run on device.
  </done>
</task>

</tasks>

<verification>
Phase-level checks (run after all three tasks complete):

1. Code:
   - `Grep` for `isSetup = true` in LockScreen.kt -> 0 matches.
   - `Grep` for `isSetup = false` in LockScreen.kt -> at least 1 match (in the unlock branch).
   - `Grep` for `LaunchedEffect(biometricEnabled)` in LockScreen.kt -> 1 match.
   - `Grep` for `KeyboardType.NumberPassword` in SettingsScreen.kt -> 2 matches expected (one pre-existing in NavHost is unrelated; check SettingsScreen file specifically — must be at least 1).

2. Build:
   - `./gradlew.bat :app:assembleDebug` -> BUILD SUCCESSFUL.

3. Scope:
   - `gitnexus_detect_changes({scope: "all"})` -> only LockScreen.kt and SettingsScreen.kt listed.

4. Behavior (manual, on device or emulator — record in SUMMARY, do not block on these):
   - Existing-PIN unlock works with correct PIN.
   - Wrong PIN decrements attempts and shows error.
   - PinSetupDialog in Settings shows numeric keyboard.
   - Biometric prompt triggers on lock screen when enabled.
</verification>

<success_criteria>
- Both flagged code bugs fixed in two files (LockScreen.kt: isSetup + LaunchedEffect key; SettingsScreen.kt: keyboardOptions).
- Debug build green.
- BiometricAuthManager.checkAvailability() confirmed non-blocking (or flagged as a follow-up if not).
- No regression in first-time PIN setup flow (NavHost showSetup branch untouched).
- gitnexus_detect_changes scope matches expectations.
- Single atomic commit covering both files.
</success_criteria>

<output>
After completion, create `.planning/quick/260504-ewv-fix-pin-and-biometric-lock-issues/260504-ewv-SUMMARY.md` with:
- Bugs fixed (1, 3, 4 from analysis) and bug verified-already-fixed (2) and bug verified-non-issue (5)
- Exact diffs (before/after snippets) for both files
- BiometricAuthManager.checkAvailability() inspection verdict
- Manual smoke-test checklist for the user
- Commit SHA
- Any follow-ups (e.g., if PasswordVisualTransformation is desired, file as future enhancement)
</output>
