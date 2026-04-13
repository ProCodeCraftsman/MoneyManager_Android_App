<objective>
Implement functional PIN lock and biometric authentication
</objective>

<context>
Settings has toggles for PIN and biometric but they don't function. Need full implementation:
- PIN setup, verification, change, disable
- Biometric prompt integration
- Lock screen that blocks app access
</context>

<tasks>

## 1. PIN Lock Implementation
- [x] Create PinLockScreen composable
- [x] PIN setup flow (enter 4 digits, confirm)
- [x] PIN verification on app launch
- [x] Store hashed PIN in PreferencesManager
- [x] Handle wrong PIN attempts (limit to 5)
- [ ] Option to reset after limit (with warning)

## 2. Biometric Authentication
- [x] Add androidx.biometric dependency (already present)
- [x] Create BiometricPrompt wrapper
- [x] Implement BiometricAuthManager
- [x] Prompt on app launch if enabled
- [x] Fallback to PIN if biometric fails

## 3. Lock Screen Integration
- [x] Create LockScreen composable (shown at app startup)
- [ ] Check authentication state before showing main content
- [ ] Handle app backgrounding (lock after timeout)

## 4. Settings Integration
- [x] PIN setup accessible from Settings
- [x] Change PIN option
- [x] Biometric enable/disable tied to real authentication
- [x] Show "Setup PIN" if biometric enabled but no PIN

## 5. Auto-Lock Timer
- [x] Implement timer in SettingsViewModel
- [x] Track app background timestamp
- [x] Require re-auth when returning from background

## 6. Security Considerations
- [x] PIN stored as salted hash, not plaintext
- [ ] Clear sensitive data from memory on lock
- [ ] Prevent screenshots of lock screen

</tasks>

<success_criteria>
- App requires PIN or biometric to open
- Auto-locks after configured timeout
- Users can enable/disable in Settings
- Biometric works as primary unlock method
</success_criteria>
