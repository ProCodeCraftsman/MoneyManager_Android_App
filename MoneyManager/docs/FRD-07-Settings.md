# Settings Screen - Functional Requirements Document

**Screen:** Settings  
**File:** `app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt`  
**ViewModel:** `SettingsViewModel`  
**Last Updated:** April 2026

---

## 1. Overview

The Settings screen provides app configuration, security settings, data management, and cloud sync functionality.

---

## 2. Features

### 2.1 Appearance
| ID | Feature | Description |
|----|---------|-------------|
| SET-01 | Dark Mode | Toggle light/dark theme |
| SET-02 | Currency | Set default currency (INR, USD, EUR, etc.) |

### 2.2 Security
| ID | Feature | Description |
|----|---------|-------------|
| SET-10 | PIN Lock | Enable 4-digit PIN |
| SET-11 | PIN Setup | Create/change PIN |
| SET-12 | Biometric Auth | Fingerprint/face unlock |
| SET-13 | Auto Lock | Lock after inactivity (1, 5, 15, 30 min) |
| SET-14 | Lock on Background | Lock when app goes to background |

### 2.3 Cloud Sync
| ID | Feature | Description |
|----|---------|-------------|
| SET-20 | Google Sign In | Firebase authentication |
| SET-21 | Sync Status | Show sync state (Idle, Syncing, Success, Error) |
| SET-22 | Last Sync Time | Display last sync timestamp |
| SET-23 | Manual Sync | Trigger manual sync |
| SET-24 | Sign Out | Sign out of Firebase |

### 2.4 Data Management
| ID | Feature | Description |
|----|---------|-------------|
| SET-30 | Export CSV | Export data to CSV |
| SET-31 | Import CSV | Import data from CSV |
| SET-32 | Export JSON | Export all data to JSON |
| SET-33 | Import JSON | Import all data from JSON |
| SET-34 | Clear Data | Reset all app data |
| SET-35 | Storage Usage | Show storage used |

### 2.5 Navigation Items
| ID | Feature | Description |
|----|---------|-------------|
| SET-40 | Accounts | Navigate to accounts |
| SET-41 | Categories | Navigate to categories |
| SET-42 | Tags | Navigate to tags |
| SET-43 | Peers | Navigate to peers |
| SET-44 | Budgets | Navigate to budgets |
| SET-45 | Goals | Navigate to goals |
| SET-46 | Recurring | Navigate to recurring |
| SET-47 | Templates | Navigate to templates |

---

## 3. Data Dependencies

### 3.1 Preferences
| Key | Type | Default | Purpose |
|-----|------|---------|---------|
| darkMode | Boolean | false | Theme preference |
| currency | String | "INR" | Display currency |
| pinEnabled | Boolean | false | PIN lock status |
| pinHash | String? | null | Hashed PIN |
| pinSalt | String? | null | PIN salt |
| biometricEnabled | Boolean | false | Biometric auth |
| autoLockMinutes | Int | 5 | Lock timeout |
| lastSyncTime | Long? | null | Last sync timestamp |

### 3.2 Repositories
| Repository | Methods Used |
|------------|--------------|
| PreferencesManager | get/set all preferences |
| AuthManager | signInWithGoogle, signOut, authState |
| FirebaseSyncManager | sync(), getSyncState() |
| ExportRepository | exportToCsv, importFromCsv, exportToJson, importFromJson |
| SecurityManager | hashPin() |

### 3.3 Data Classes
```kotlin
enum class ExportType {
    ACCOUNTS, TRANSACTIONS, CATEGORIES, BUDGETS, GOALS, RECURRING, TEMPLATES, TAGS, ALL
}

sealed class ExportResult {
    data class Success(val path: String, val count: Int) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(val count: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

enum class SyncStatus {
    Idle, Syncing, Success, Error
}

sealed class AuthState {
    data object SignedOut : AuthState()
    data class SignedIn(val user: FirebaseUser) : AuthState()
}
```

---

## 4. State Management

### 4.1 UiState Fields
```kotlin
data class SettingsUiState(
    val darkMode: Boolean,
    val currency: String,
    val pinEnabled: Boolean,
    val pinSetupRequired: Boolean,
    val biometricEnabled: Boolean,
    val autoLockMinutes: Int,
    val isSignedIn: Boolean,
    val userEmail: String?,
    val userName: String?,
    val syncStatus: SyncStatus,
    val lastSyncTime: Long?,
    val isSyncing: Boolean,
    val storageUsedKb: Long,
    val importResult: ImportResult?,
    val exportResult: ExportResult?,
)
```

---

## 5. User Interactions

### 5.1 Actions
| Action | Method | Description |
|--------|--------|-------------|
| Toggle Dark Mode | setDarkMode() | Switch theme |
| Change Currency | setCurrency() | Update default currency |
| Enable/Disable PIN | setPinEnabled() | Toggle PIN lock |
| Setup PIN | onPinSetupComplete() | Save new PIN |
| Enable Biometric | setBiometricEnabled() | Toggle biometric |
| Set Auto Lock | setAutoLockMinutes() | Configure timeout |
| Sign In | signInWithGoogle() | Authenticate |
| Sign Out | signOut() | Sign out |
| Trigger Sync | triggerSync() | Manual sync |
| Export CSV | exportToCsv() | Export to CSV |
| Import CSV | importFromCsv() | Import from CSV |
| Export JSON | exportToJson() | Export all data |
| Import JSON | importFromJson() | Import all data |
| Clear Results | clearResults() | Dismiss result dialogs |

### 5.2 Navigation Callbacks
| Callback | Navigates To |
|----------|--------------|
| onNavigateToAccounts | Accounts Screen |
| onNavigateToCategories | Categories Screen |
| onNavigateToTags | Tags Screen |
| onNavigateToPeers | Peers Screen |
| onNavigateToBudgets | Budgets Screen |
| onNavigateToGoals | Goals Screen |
| onNavigateToRecurring | Recurring Screen |
| onNavigateToTemplates | Templates Screen |

---

## 6. Business Logic

### 6.1 PIN Setup
```kotlin
fun onPinSetupComplete(pin: String)
- Hash PIN with salt using SecurityManager
- Store hash and salt in preferences
- Enable PIN lock
- Dismiss setup dialog
```

### 6.2 Biometric Flow
- Requires PIN to be set first
- If no PIN: prompt to set PIN
- Enable biometric flag in preferences

### 6.3 Export Flow
```kotlin
exportToCsv(type: ExportType, uri: Uri)
- Convert entity to CSV format
- Write to selected URI
- Return success with record count
```

### 6.4 Import Flow
```kotlin
importFromCsv(type: ExportType, uri: Uri)
- Read CSV from URI
- Parse and validate
- Insert into database
- Return success with record count
```

---

## 7. Security Implementation

### 7.1 PIN Hashing
- Algorithm: PBKDF2 with SHA-256
- Salt: Randomly generated, stored separately
- Verification: Hash input, compare with stored

### 7.2 Biometric
- Uses Android BiometricPrompt API
- Fallback to PIN if biometric fails

### 7.3 Auto Lock
- Timer starts when app backgrounds
- Locks after configured minutes
- Resets on successful unlock

---

## 8. Connected Screens

| Screen | Connection | Relation |
|--------|------------|----------|
| All Screens | Theme/Currency | Global settings affect all |
| Lock Screen | Auth | Uses PIN/biometric settings |
| Dashboard | Sync status | Shows sync state |
| Export Repository | Data | Import/export all entities |

---

## 9. Edge Cases

| Scenario | Handling |
|----------|----------|
| No network | Show error, allow retry |
| Export fails | Show error message |
| Import invalid data | Show validation errors |
| Clear data | Confirm dialog, then wipe |
| Biometric not available | Hide option, show PIN only |
| PIN forgotten | Clear data to reset |

---

## 10. Related Files

| File | Purpose |
|------|---------|
| `SettingsScreen.kt` | UI composable |
| `SettingsViewModel.kt` | Business logic |
| `PreferencesManager.kt` | Preferences storage |
| `SecurityManager.kt` | PIN hashing |
| `BiometricAuthManager.kt` | Biometric auth |
| `AuthManager.kt` | Firebase auth |
| `FirebaseSyncManager.kt` | Cloud sync |
| `ExportRepository.kt` | Import/export |
| `LockScreen.kt` | Lock UI |
| `PinLockScreen.kt` | PIN setup UI |

---

## 11. Impact Analysis Reference

When modifying Settings features, check impact on:
- All screens (theme, currency)
- App startup (PIN/biometric)
- Data integrity (import/export)
- Cloud sync (Firebase)
- Lock screen behavior