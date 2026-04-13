---
phase: 06-build-publish
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/moneymanager/app/data/repository/ExportRepository.kt
  - app/src/main/java/com/moneymanager/app/data/local/dao/
  - app/src/main/java/com/moneymanager/app/ui/screens/SettingsScreen.kt
  - app/src/main/res/drawable/
  - app/src/main/res/mipmap-hdpi/
  - app/src/main/res/mipmap-mdpi/
  - app/src/main/res/mipmap-xhdpi/
  - app/src/main/res/mipmap-xxhdpi/
  - app/src/main/res/mipmap-xxxhdpi/
  - app/src/main/res/values/
  - app/build.gradle.kts
autonomous: true
requirements: []
user_setup:
  - service: google-play-console
    why: "Publish Android app"
    dashboard_config:
      - task: "Create Play Store listing"
        location: "Google Play Console > App releases"
      - task: "Upload privacy policy"
        location: "App content > Privacy policy"

must_haves:
  truths:
    - User can export all data as JSON
    - User can export all data as CSV
    - User can import data from JSON
    - User can import data from CSV
    - App has custom icon in Play Store
    - Privacy policy is accessible in Store listing
  artifacts:
    - path: app/src/main/java/com/moneymanager/app/data/repository/ExportRepository.kt
      provides: JSON/CSV export functionality
      min_lines: 100
    - path: app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
      provides: Adaptive icon configuration
    - path: app/src/main/res/drawable/ic_launcher_foreground.xml
      provides: Foreground icon drawable
    - path: app/src/main/res/drawable/ic_launcher_background.xml
      provides: Background icon drawable
    - path:PrivacyPolicy.html
      provides: Privacy policy for Play Store
      contains: "privacy policy"
  key_links:
    - from: SettingsScreen.kt
      to: ExportRepository
      via: exportViewModel.exportData()
      pattern: "exportData.*JSON|exportData.*CSV"
---

<objective>
Phase 6: Build & Publish

Purpose: Enable data export/import for backup, prepare Play Store listing, publish to Google Play

Output: Ready-to-publish APK with export features, Play Store assets, privacy policy
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/PROJECT.md
@.planning/ROADMAP.md
@.planning/STATE.md
@.planning/phases/06-build-publish/06-CONTEXT.md
</context>

<tasks>

<task type="auto">
  <name>Task 1: Implement JSON/CSV Import/Export</name>
  <files>app/src/main/java/com/moneymanager/app/data/repository/ExportRepository.kt</files>
  <read_first>
    - app/src/main/java/com/moneymanager/app/data/local/dao/TransactionDao.kt
    - app/src/main/java/com/moneymanager/app/data/local/dao/AccountDao.kt
    - app/src/main/java/com/moneymanager/app/data/local/dao/CategoryDao.kt
    - app/src/main/java/com/moneymanager/app/data/local/dao/BudgetDao.kt
    - app/src/main/java/com/moneymanager/app/data/local/dao/GoalDao.kt
    - app/src/main/java/com/moneymanager/app/data/local/dao/TagDao.kt
  </read_first>
  <action>
    Create ExportRepository with:

    1. Export all data to JSON:
       - Get all transactions, accounts, categories, budgets, goals, tags from Room
       - Format as JSON object with root keys: transactions, accounts, categories, budgets, goals, tags
       - Include metadata: exportedAt timestamp, appVersion, dataVersion

    2. Export to CSV (one file per entity type):
       - transactions.csv: date, amount, description, category, account, isRecurring
       - accounts.csv: name, type, balance, currency
       - categories.csv: name, type, color, icon
       - budgets.csv: category, limit, period
       - goals.csv: name, targetAmount, currentAmount, deadline

    3. Import from JSON:
       - Validate structure matches export format
       - Upsert each entity type (update if exists by ID, insert if new)
       - Return import result with counts

    4. Import from CSV:
       - Parse each CSV file
       - Map to entity type and insert/update

    5. Add to Settings screen:
       - Export button triggers exportOptionsDialog(JSON, CSV, Export All)
       - Import button opens file picker, shows import preview, confirms

    Reference pattern: Use existing PreferencesManager for file I/O on Android
  </action>
  <acceptance_criteria>
    - ExportRepository.kt exists with exportToJson(), exportToCsv(), importJson(), importCsv() methods
    - DAOs have getAllTransactions(), getAllAccounts() etc for bulk export
    - Settings screen has export/import UI elements
  </acceptance_criteria>
  <verify>
    <automated>grep -l "exportToJson\|exportToCsv" app/src/main/java/com/moneymanager/app/data/repository/ExportRepository.kt</automated>
  </verify>
  <done>
    - JSON export produces valid JSON with all 6 data types
    - CSV export produces multiple .csv files
    - Import successfully restores exported data
  </done>
</task>

<task type="auto">
  <name>Task 2: Create App Icon and Play Store Assets</name>
  <files>
    - app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml
    - app/src/main/res/drawable/ic_launcher_foreground.xml
    - app/src/main/res/drawable/ic_launcher_background.xml
  </files>
  <read_first>
    - app/build.gradle.kts
  </read_first>
  <action>
    Create Android Adaptive Icon:

    1. Foreground: Use "$" symbol or wallet icon on transparent background (#2A6049 green accent)
    2. Background: Solid or gradient using app's accent colors (#F5F1EA cream, #0F0E0C dark)
    3. Create all mipmap densities: mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi
    4. Reference in AndroidManifest.xml

    Create Privacy Policy document in HTML format:
    - Stored in app/src/main/assets/privacy_policy.html
    - Linked from Settings > Privacy Policy
    - Uploaded to Play Console

    Create Play Store screenshots:
    - Dashboard screen
    - Add transaction flow
    - Reports/charts
    - Use device frame or clean screenshots
  </action>
  <acceptance_criteria>
    - Adaptive icon XML exists in mipmap-anydpi-v26
    - Foreground and background drawables exist
    - Privacy policy HTML file exists
  </acceptance_criteria>
  <verify>
    <automated>test -f app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml && test -f app/src/main/res/drawable/ic_launcher_foreground.xml</automated>
  </verify>
  <done>
    - App displays custom icon on device
    - Privacy policy is accessible from Settings
    - Screenshots ready for Play Console upload
  </done>
</task>

<task type="auto">
  <name>Task 3: Configure Build & Publish</name>
  <files>app/build.gradle.kts</files>
  <read_first>
    - app/build.gradle.kts
  </read_first>
  <action>
    Configure build for release:

    1. Update build.gradle.kts:
       - versionCode = 1 (increment for releases)
       - versionName = "1.0.0"
       - applicationId = "com.moneymanager.app"
       - Enable ProGuard/R8 for release

    2. Create signing config:
       - Use existing debug keystore for development
       - Document release keystore requirement in comments

    3. Update AndroidManifest.xml:
       - android:label = "MoneyManager"
       - android:icon = "@mipmap/ic_launcher"
       - android:theme = "@style/Theme.MoneyManager"

    4. Build verification:
       - Run: ./gradlew assembleRelease
       - Verify APK builds successfully
  </action>
  <acceptance_criteria>
    - build.gradle.kts has release buildType configured
    - Version code and name are set
    - APK builds without errors
  </acceptance_criteria>
  <verify>
    <automated>grep -E "versionCode|versionName|release" app/build.gradle.kts | head -5</automated>
  </verify>
  <done>
    - Release APK builds successfully
    - Version configured for Play Store
  </done>
</task>

</tasks>

<verification>
- Export functionality tested with real data
- Import restores data correctly
- Icon displays on Android 8.0+ devices
- Privacy policy accessible
- Release APK generated
</verification>

<success_criteria>
- User can export all data (transactions, accounts, categories, budgets, goals, tags) to JSON
- User can export all data to CSV
- User can import from JSON backup
- User can import from CSV files
- App has custom Android adaptive icon
- Privacy policy present in app and Play Store listing
- Release APK ready for Google Play submission
</success_criteria>

<output>
After completion, create `.planning/phases/06-build-publish/06-01-SUMMARY.md`
</output>