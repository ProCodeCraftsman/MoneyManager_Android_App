# Architecture Patterns: AI-Assisted Transaction Drafting Integration

**Domain:** Android Clean Architecture — Gemini Nano / AICore integration into existing MVVM app
**Researched:** 2026-05-15
**Confidence:** HIGH (derived from direct codebase inspection of all referenced files)

---

## 1. Package Structure: What Lives Where

The existing project has two root package trees that must remain consistent:

- `com.moneymanager.data.*` — Room entities, DAOs, repository impls, preferences, sync
- `com.moneymanager.domain.*` — repository interfaces only (no entities, no impls)
- `com.moneymanager.app.*` — UI screens, ViewModels, components, DI modules, app entrypoint

New packages follow the same split. Three new subtrees are added, one in each layer.

### New Package Breakdown

#### `com.moneymanager.domain.ai` — Domain layer AI contracts

| File | Kind | Responsibility |
|------|------|----------------|
| `GenAiClient.kt` | `interface` | Single method `suspend fun generateDraft(prompt: String): Result<String>`. Pure domain contract — no Android imports, no AICore imports. |
| `TransactionDraft.kt` | `data class` | Carries resolved entity IDs (`categoryId: Long?`, `accountId: Long?`, `peerContactId: Long?`) plus display strings (`categoryName`, `accountName`, `peerName`) and raw fields (`amount: Double?`, `type: String?`, `date: Long?`, `note: String?`). Does NOT extend `TransactionEntity` — draft-only model. |
| `TransactionType.kt` | `enum class` | Centralized registry. See section 7. |
| `PromptContext.kt` | `data class` | Snapshot of live master data for prompt building. See section 3. |
| `PromptContextBuilder.kt` | `class` | Builds `PromptContext` from repositories. See section 3. |
| `GenerateDraftFromTextUseCase.kt` | `class` | Orchestrates prompt building and `GenAiClient` call. See section 3. |

#### `com.moneymanager.data.ai` — Data layer AI implementation

| File | Kind | Responsibility |
|------|------|----------------|
| `NanoAiClient.kt` | `class` | Implements `GenAiClient`. Wraps `com.google.android.gms.aicore` API calls. Returns `Result.failure` if session creation fails at runtime. `@Inject constructor`. |
| `DeviceCapabilityManager.kt` | `class` | Checks AICore feature availability at startup. Writes result to DataStore. Exposes `isAiAssistAvailableSync(): Boolean` (blocking, for Hilt graph construction) and delegates `isAiAssistAvailable: Flow<Boolean>` to `PreferencesManager`. `@Inject constructor(context: Context, preferencesManager: PreferencesManager)`. |
| `PromptBuilder.kt` | `object` | Pure function `fun build(rawText: String, context: PromptContext): String`. Stateless utility; no injection needed. |
| `DraftParser.kt` | `object` | Pure function `fun parse(json: String): TransactionDraft`. Parses the AI JSON response into the domain model. No Android dependencies. |

#### `com.moneymanager.app.ui.aidraft` — UI layer AI screens

| File | Kind | Responsibility |
|------|------|----------------|
| `SmsPickerScreen.kt` | `@Composable` | Reads SMS inbox (READ_SMS permission flow), displays list, triggers AI fill. |
| `ReceiptScanScreen.kt` | `@Composable` | Camera/gallery capture, ML Kit OCR text extraction, triggers AI fill. |
| `VoiceMemoScreen.kt` | `@Composable` | Records via `SpeechRecognizer` (EXTRA_PREFER_OFFLINE), displays transcript, triggers AI fill. |
| `AiDraftViewModel.kt` | `@HiltViewModel` | Shared ViewModel for all three screens. Holds `AiDraftUiState`, calls `GenerateDraftFromTextUseCase`, exposes `isAiAvailable` via `StateFlow<Boolean>`. |
| `AiDraftUiState.kt` | `data class` | Loading/Success/Error state for the draft generation step. See section 4. |

#### `com.moneymanager.app.di` — New DI module (additive only)

| File | Kind | Responsibility |
|------|------|----------------|
| `AiModule.kt` | `@Module @InstallIn(SingletonComponent)` | Provides `GenAiClient?` conditionally; provides `DeviceCapabilityManager`. See section 2. |

---

## 2. Hilt AiModule — Nullable Provider Pattern

### Why nullable, not Optional

Hilt supports `@Provides` methods that return nullable types. The injection site must also declare the type as nullable (`@Inject constructor(private val aiClient: GenAiClient?)`). This is the correct idiom for an optionally-available platform feature. Guava `Optional<T>` and Dagger `@BindsOptionalOf` are valid but add indirection; nullable is simpler and idiomatic in Kotlin.

A `@Singleton`-scoped `@Provides` method that returns `null` caches the `null` result for the lifetime of `SingletonComponent` — same behavior as a non-null singleton. No special handling is needed at the call site beyond the null check.

### Hilt limitation: @Nullable annotation required

A `@Provides` method returning a nullable Kotlin type must also carry a `@Nullable` annotation (JSR-305 `javax.annotation.Nullable`). Hilt's annotation processor reads Java bytecode and the Kotlin `?` suffix does not reliably emit `@Nullable` in all Kotlin compiler / KSP version combinations. Without the annotation Hilt may throw a compile-time error about unprovided dependencies. The explicit annotation is the safe, version-independent choice.

```kotlin
// com.moneymanager.app.di.AiModule

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    @javax.annotation.Nullable
    fun provideGenAiClient(
        @ApplicationContext context: Context,
        deviceCapabilityManager: DeviceCapabilityManager,
    ): GenAiClient? {
        // Reads the DataStore boolean cached by DeviceCapabilityManager at startup.
        // This is the only blocking DataStore read in the entire app — justified here
        // because Hilt graph construction happens before the first Activity frame.
        return if (deviceCapabilityManager.isAiAssistAvailableSync()) {
            NanoAiClient(context)
        } else {
            null
        }
    }

    @Provides
    @Singleton
    fun provideDeviceCapabilityManager(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager,
    ): DeviceCapabilityManager = DeviceCapabilityManager(context, preferencesManager)
}
```

### DeviceCapabilityManager startup sequence

`DeviceCapabilityManager` must write the AICore availability result to DataStore **before** `AiModule.provideGenAiClient` is called. Hilt lazily initializes `@Singleton` bindings on first access — first access occurs when the first Activity calls `hiltViewModel()`. `Application.onCreate()` runs before any Activity, so the following pattern is safe:

```kotlin
// com.moneymanager.app.MoneyManagerApp — MODIFIED (additive only)
@HiltAndroidApp
class MoneyManagerApp : Application() {
    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var deviceCapabilityManager: DeviceCapabilityManager  // NEW

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(appLockManager)
        // Writes isAiAssistAvailable to DataStore asynchronously.
        // Completes before any Activity can call hiltViewModel() and trigger
        // AiModule.provideGenAiClient(), which reads the value synchronously.
        applicationScope.launch {
            deviceCapabilityManager.checkAndCacheAvailability()
        }
    }
}
```

`DeviceCapabilityManager.isAiAssistAvailableSync()` is a blocking `runBlocking { preferencesManager.isAiAssistAvailable.first() }`. It is called only once, during Hilt graph initialization, before any UI renders. This is an acceptable trade-off given the one-time check is < 1ms (DataStore read from disk cache).

---

## 3. PromptContext Design

### Decision: dedicated PromptContextBuilder class

Three candidate designs:

**Option A — Inject 4 repositories directly into GenerateDraftFromTextUseCase**
Straightforward but makes the use case a service locator: 4 repository mocks required in tests, and the use case is doing data assembly work that is not its responsibility.

**Option B — PromptContext built externally (e.g., in ViewModel), passed as parameter**
Use case becomes easily testable but the caller (ViewModel) must know how to build the context, leaking data assembly logic into the UI layer.

**Option C — PromptContextBuilder class injected into the use case (RECOMMENDED)**
The use case depends on one `PromptContextBuilder`. In tests, mock the builder with a single stub. The builder internally uses the 3 existing repository interfaces. This is the cleanest separation: the use case orchestrates, the builder assembles.

### PromptContext data class (domain layer — no Room entity imports)

```kotlin
// com.moneymanager.domain.ai.PromptContext
data class PromptContext(
    val transactionTypes: List<TransactionType>,
    val categories: List<CategoryEntry>,
    val accounts: List<AccountEntry>,
    val peers: List<PeerEntry>,
    val tags: List<TagEntry>,
)

// Lightweight domain-only projections — no com.moneymanager.data.entity imports
data class CategoryEntry(val id: Long, val name: String, val type: String)
data class AccountEntry(val id: Long, val name: String)
data class PeerEntry(val id: Long, val displayName: String)
data class TagEntry(val id: Long, val name: String)
```

The projection types exist because `CategoryEntity`, `AccountEntity`, etc. live in `com.moneymanager.data.entity`. Importing them into the domain layer violates Clean Architecture (domain must not depend on data). These four small value classes are cheap to write and eliminate the dependency violation.

### PromptContextBuilder

```kotlin
// com.moneymanager.domain.ai.PromptContextBuilder
class PromptContextBuilder @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository,
    private val peerContactRepository: PeerContactRepository,
) {
    suspend fun build(): PromptContext {
        // All three repositories already exist. getAllTags() lives on CategoryRepository.
        val categories = categoryRepository.getAllCategories().first().map {
            CategoryEntry(it.id, it.name, it.type)
        }
        val accounts = accountRepository.getAllAccounts().first().map {
            AccountEntry(it.id, it.name)
        }
        val peers = peerContactRepository.getAllPeers().first().map {
            PeerEntry(it.id, it.effectiveDisplayName)
        }
        val tags = categoryRepository.getAllTags().first().map {
            TagEntry(it.id, it.name)
        }
        return PromptContext(
            transactionTypes = TransactionType.values().toList(),
            categories = categories,
            accounts = accounts,
            peers = peers,
            tags = tags,
        )
    }
}
```

Note: `TagRepository` does not need to be created. Tags are already exposed via `CategoryRepository.getAllTags()` (confirmed in `CategoryRepository.kt` interface).

### GenerateDraftFromTextUseCase

```kotlin
// com.moneymanager.domain.ai.GenerateDraftFromTextUseCase
class GenerateDraftFromTextUseCase @Inject constructor(
    @javax.annotation.Nullable private val aiClient: GenAiClient?,
    private val promptContextBuilder: PromptContextBuilder,
) {
    suspend operator fun invoke(rawText: String): Result<TransactionDraft> {
        if (aiClient == null) return Result.failure(AiUnavailableException())
        val context = promptContextBuilder.build()
        val prompt = PromptBuilder.build(rawText, context)   // PromptBuilder is an object
        return aiClient.generateDraft(prompt)
            .mapCatching { json -> DraftParser.parse(json) } // DraftParser is an object
    }
}
```

**Extensibility:** When a new entry is added to `TransactionType`, `PromptContext.transactionTypes` includes it automatically on the next invocation. `PromptBuilder.build()` iterates `context.transactionTypes` to enumerate types in the prompt. No changes are needed in `GenerateDraftFromTextUseCase` or `PromptContextBuilder` when new transaction types are introduced.

---

## 4. Draft-to-Form Flow: AddEditTransactionDialog Integration

### The constraint

`AddEditTransactionDialog` uses individual `rememberSaveable` variables as its state — `type`, `amount`, `selectedAccountId`, `selectedCategoryId`, `selectedPeerId`, `selectedDate`, `description`, etc. The `TransactionFormState` sealed class is defined but is consumed by `TransactionFormConverter`, not by the dialog itself as a state container. There is no single object to merge a draft into.

### Recommended approach: optional initialDraft parameter

Do NOT restructure the dialog's internal state management. Add one optional parameter with a `null` default:

```kotlin
@Composable
fun AddEditTransactionDialog(
    transaction: TransactionEntity?,
    // ... all existing parameters unchanged ...
    initialDraft: TransactionDraft? = null,    // NEW — null preserves all existing call sites
    onDismiss: () -> Unit,
    onConfirm: (TransactionEntity, List<TransactionEntity>?) -> Unit,
)
```

Inside the dialog, a `LaunchedEffect(initialDraft)` applied **after** all `rememberSaveable` declarations populates the local vars:

```kotlin
LaunchedEffect(initialDraft) {
    initialDraft ?: return@LaunchedEffect
    initialDraft.type?.let { onTypeSelected(it) }          // reuses existing type-switch handler
    initialDraft.amount?.let { amount = it.toString() }
    initialDraft.accountId?.let { selectedAccountId = it }
    initialDraft.categoryId?.let { selectedCategoryId = it }
    initialDraft.peerContactId?.let { selectedPeerId = it }
    initialDraft.date?.let { selectedDate = it }
    initialDraft.note?.takeIf { it.isNotBlank() }?.let {
        description = it
        showNoteInput = true
    }
    // isCalculatorVisible = false — draft has amount, no need for keypad
    initialDraft.amount?.let { isCalculatorVisible = false }
}
```

Using `onTypeSelected(it)` from the existing `fun onTypeSelected(newType: String)` local function is important: it resets dependent state (`selectedCategoryId`, `selectedPeerId`, `selectedToAccountId`, etc.) before the draft values are applied. Without this the category dropdown could show a category from the wrong type.

**Why this approach is safe:**

1. All existing call sites pass zero extra arguments — the `null` default means nothing changes in their behavior.
2. The `LaunchedEffect(initialDraft)` only fires when `initialDraft` is non-null and only re-fires when the draft object reference changes, which is the correct behavior if "Apply different draft" is added later.
3. All user edits after draft application are preserved normally; the draft just provides starting values.

### AiDraftUiState

```kotlin
// com.moneymanager.app.ui.aidraft.AiDraftUiState
data class AiDraftUiState(
    val isAiAvailable: Boolean = false,
    val isGenerating: Boolean = false,
    val draft: TransactionDraft? = null,
    val error: String? = null,
)
```

### Data flow: source screen to AddEditTransactionDialog

```
SmsPickerScreen
  user selects SMS text
  └─ AiDraftViewModel.generateDraft(smsText)
       └─ GenerateDraftFromTextUseCase.invoke(smsText)
            ├─ PromptContextBuilder.build()       — suspends, reads 3 repos
            ├─ PromptBuilder.build(text, context) — pure function, no suspension
            └─ GenAiClient.generateDraft(prompt)  — suspends, calls AICore
                 └─ DraftParser.parse(json) → TransactionDraft
  AiDraftUiState.draft = TransactionDraft
  └─ SmsPickerScreen navigates to addtransaction route
       └─ TransactionDraft serialized as JSON nav argument
            └─ AddTransactionViewModel parses JSON → state.initialDraft
                 └─ AddTransactionScreen passes initialDraft to dialog
                      └─ AddEditTransactionDialog(initialDraft = draft)
                           └─ LaunchedEffect applies draft fields to rememberSaveable vars
                                └─ user reviews, edits if needed, saves via existing onConfirm
```

### Passing the draft via navigation

Serialize `TransactionDraft` to JSON (Kotlin `kotlinx.serialization` or Gson — the project uses Gson for Firebase). Extend the existing route:

- Existing: `addtransaction/{type}`
- Extended: `addtransaction/{type}?draft={draftJson}`

`AddTransactionViewModel` reads the optional `draft` SavedStateHandle argument, deserializes it, and sets `state.initialDraft`. `AddTransactionUiState` gets one new nullable field:

```kotlin
data class AddTransactionUiState(
    // ... all existing fields unchanged ...
    val initialDraft: TransactionDraft? = null,   // NEW
)
```

All existing `AddTransactionUiState(...)` construction sites are unaffected — the field has a `null` default.

---

## 5. DeviceCapabilityManager — isAiAssistAvailable Exposure to UI

### DataStore key addition to PreferencesManager

`PreferencesManager` follows the established pattern: one key per boolean, one `Flow<Boolean>` property, one setter. Add exactly one new key:

```kotlin
// com.moneymanager.data.preferences.PreferencesManager — MODIFIED (additive only)
companion object {
    // ... all existing keys unchanged ...
    private val AI_ASSIST_AVAILABLE = booleanPreferencesKey("ai_assist_available")
}

val isAiAssistAvailable: Flow<Boolean> = context.dataStore.data.map { preferences ->
    preferences[AI_ASSIST_AVAILABLE] ?: false
}

suspend fun setAiAssistAvailable(available: Boolean) {
    context.dataStore.edit { it[AI_ASSIST_AVAILABLE] = available }
}
```

`DeviceCapabilityManager` calls `preferencesManager.setAiAssistAvailable(result)` after checking AICore.

### ViewModel consumption (matches existing currency/darkMode pattern)

```kotlin
// com.moneymanager.app.ui.aidraft.AiDraftViewModel
val isAiAvailable: StateFlow<Boolean> = preferencesManager.isAiAssistAvailable
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
```

In the three AI screens:

```kotlin
val uiState by viewModel.uiState.collectAsState()
AnimatedVisibility(visible = uiState.isAiAvailable) {
    Button(onClick = { viewModel.generateDraft(inputText) }) { Text("AI Fill") }
}
```

This is identical to how `darkMode` and `currency` are consumed throughout the app. No new pattern is introduced.

---

## 6. Package Naming Conflicts — Risk Assessment

### Existing package tree
- `com.moneymanager.app.*` — app module
- `com.moneymanager.data.*` — data layer
- `com.moneymanager.domain.*` — domain layer

### New packages
- `com.moneymanager.domain.ai` — SAFE: no existing `ai` subdirectory under domain
- `com.moneymanager.data.ai` — SAFE: no existing `ai` subdirectory under data
- `com.moneymanager.app.ui.aidraft` — SAFE: no existing `aidraft` package; distinct from `addtransaction`

### Naming distinction between aidraft and addtransaction
`com.moneymanager.app.ui.addtransaction` (existing) houses the form that saves transactions. `com.moneymanager.app.ui.aidraft` houses the three source-selection screens that produce a `TransactionDraft`. The `aidraft` screens are upstream of `addtransaction` in the user flow. The naming makes this directionality explicit.

### No multi-module considerations
The project uses a single `:app` Gradle module. All source lives under `MoneyManager/app/src/main/java`. New packages are source directories only — no new Gradle module, no new build configuration needed.

---

## 7. Transaction Type Registry

### Current state
`TransactionEntity.VALID_TYPES` is a hardcoded `List<String>` in the entity companion object. `TransactionFormConfig.allTypes` is a hardcoded `listOf(FormTypeConfig(...))` in the UI layer. If a prompt builder hardcodes the same list, there are three separate sources of truth that can drift.

### Recommended: enum class TransactionType in domain.ai

```kotlin
// com.moneymanager.domain.ai.TransactionType
enum class TransactionType(
    val id: String,
    val displayName: String,
    val promptHint: String,
    val requiresCategory: Boolean,
    val requiresPeer: Boolean,
) {
    EXPENSE("expense", "Expense", "a debit or purchase", true, false),
    INCOME("income", "Income", "a credit or payment received", true, false),
    SAVINGS("savings", "Savings", "a savings deposit or investment", true, false),
    TRANSFER("transfer", "Transfer", "a transfer between two accounts", false, false),
    LEND("lend", "Lending", "money given to another person", false, true),
    BORROW("borrow", "Borrowing", "money received from another person as debt", false, true),
    RECEIVE("receive", "Receive Repayment", "repayment of money previously lent", false, true),
    REPAY("repay", "Repay Debt", "repayment of money previously borrowed", false, true),
    ;

    companion object {
        fun fromId(id: String): TransactionType? = values().firstOrNull { it.id == id }
        fun allIds(): List<String> = values().map { it.id }
    }
}
```

### Why enum and not sealed class or companion constants

| Option | Verdict | Reason |
|--------|---------|--------|
| `enum class` | RECOMMENDED | Iterable via `values()`, exhaustive `when` branches, serializable, supports per-type metadata fields. Adding a new type is one enum entry. |
| `sealed class` | Overkill | Each type needs a subclass. No built-in iteration. Better when subtypes carry different data structures, which transaction types do not. |
| `companion object const val` | Worst option | Already exists as `TransactionEntity.VALID_TYPES`. Cannot carry metadata. Adding a type requires updating multiple lists across layers. Highest drift risk. |

### Migration path

- `TransactionEntity.VALID_TYPES` can be replaced with `TransactionType.allIds()` — string values are identical across both lists, so no data migration is needed.
- `TransactionFormConfig.allTypes` keeps its own `FormTypeConfig` list because it carries Compose `ImageVector` properties that belong in the UI layer, not the domain. The `id` strings align with `TransactionType.id`.
- `TransactionFormConfig.getType(id)` can add a validation step: assert `TransactionType.fromId(id) != null` before looking up config.

---

## 8. File-by-File Build Order

Build order respects the Clean Architecture dependency graph: domain before data before DI wiring before UI.

### Phase 1: Domain AI foundation (zero Android or Compose dependencies — fully JVM unit-testable)

| Order | File | Status | Notes |
|-------|------|--------|-------|
| 1 | `domain/ai/TransactionType.kt` | NEW | No dependencies at all. Registry for all subsequent files. |
| 2 | `domain/ai/TransactionDraft.kt` | NEW | Primitive fields only. No dependencies. |
| 3 | `domain/ai/GenAiClient.kt` | NEW | Interface only. No dependencies. |
| 4 | `domain/ai/PromptContext.kt` + projection classes | NEW | Depends on `TransactionType`. |
| 5 | `domain/ai/PromptContextBuilder.kt` | NEW | Depends on 3 existing repository interfaces. |
| 6 | `domain/ai/GenerateDraftFromTextUseCase.kt` | NEW | Depends on all of the above. |

### Phase 2: Data AI implementation

| Order | File | Status | Notes |
|-------|------|--------|-------|
| 7 | `data/preferences/PreferencesManager.kt` | MODIFIED | Add `AI_ASSIST_AVAILABLE` key + `isAiAssistAvailable: Flow<Boolean>` + `setAiAssistAvailable()`. |
| 8 | `data/ai/PromptBuilder.kt` | NEW | Pure Kotlin object, no Android imports. Converts `PromptContext` + rawText to prompt string. |
| 9 | `data/ai/DraftParser.kt` | NEW | Pure Kotlin object. Parses AI JSON response to `TransactionDraft`. |
| 10 | `data/ai/NanoAiClient.kt` | NEW | Implements `GenAiClient`. Requires AICore Gradle dependency (`com.google.android.gms:play-services-mlkit-text-recognition` or equivalent AICore artifact). |
| 11 | `data/ai/DeviceCapabilityManager.kt` | NEW | Checks AICore availability, writes to `PreferencesManager`. Exposes `isAiAssistAvailableSync(): Boolean`. |

### Phase 3: DI wiring

| Order | File | Status | Notes |
|-------|------|--------|-------|
| 12 | `app/di/AiModule.kt` | NEW | Provides `GenAiClient?` with `@Nullable` and `DeviceCapabilityManager`. |
| 13 | `app/MoneyManagerApp.kt` | MODIFIED | Add `DeviceCapabilityManager` field injection + startup coroutine call. |

### Phase 4: UI screens

| Order | File | Status | Notes |
|-------|------|--------|-------|
| 14 | `app/ui/aidraft/AiDraftUiState.kt` | NEW | Pure data class. |
| 15 | `app/ui/aidraft/AiDraftViewModel.kt` | NEW | Depends on `GenerateDraftFromTextUseCase` + `PreferencesManager`. |
| 16 | `app/ui/aidraft/SmsPickerScreen.kt` | NEW | Depends on `AiDraftViewModel`. Needs READ_SMS permission. |
| 17 | `app/ui/aidraft/ReceiptScanScreen.kt` | NEW | Depends on `AiDraftViewModel`. Needs ML Kit Text Recognition dependency. |
| 18 | `app/ui/aidraft/VoiceMemoScreen.kt` | NEW | Depends on `AiDraftViewModel`. Uses `android.speech.SpeechRecognizer`. |

### Phase 5: Dialog integration (minimal changes to existing code)

| Order | File | Status | Notes |
|-------|------|--------|-------|
| 19 | `app/ui/dialogs/AddEditTransactionDialog.kt` | MODIFIED | Add `initialDraft: TransactionDraft? = null` parameter + `LaunchedEffect(initialDraft)`. All existing call sites unaffected (null default). |
| 20 | `app/ui/addtransaction/AddTransactionUiState.kt` | MODIFIED | Add `initialDraft: TransactionDraft? = null`. All existing construction sites unaffected. |
| 21 | `app/ui/addtransaction/AddTransactionViewModel.kt` | MODIFIED | Read optional `draft` SavedStateHandle argument, deserialize, set in `uiState`. |
| 22 | `app/ui/addtransaction/AddTransactionScreen.kt` | MODIFIED | Pass `uiState.initialDraft` through to `AddEditTransactionDialog`. |
| 23 | `app/ui/MoneyManagerNavHost.kt` | MODIFIED | Add 3 routes for AI screens; extend `addtransaction/{type}` route with optional `?draft={draftJson}` argument. |

---

## 9. Integration Points Summary

### Files modified (not new) — complete list

| File | Change description | Risk level |
|------|-------------------|------------|
| `data/preferences/PreferencesManager.kt` | +1 DataStore key, +1 Flow, +1 setter | LOW — strictly additive |
| `app/MoneyManagerApp.kt` | +1 `@Inject` field, +1 `applicationScope.launch` | LOW — additive; existing callbacks unaffected |
| `app/ui/dialogs/AddEditTransactionDialog.kt` | +1 optional null-default parameter, +1 `LaunchedEffect` | LOW — null default preserves all 4+ existing call sites |
| `app/ui/addtransaction/AddTransactionUiState.kt` | +1 nullable field with null default | LOW — all existing construction sites unaffected |
| `app/ui/addtransaction/AddTransactionViewModel.kt` | Parse optional nav arg; populate `state.initialDraft` | MEDIUM — nav contract change; requires matching nav graph update |
| `app/ui/addtransaction/AddTransactionScreen.kt` | Pass `uiState.initialDraft` to dialog | LOW — optional param with null default |
| `app/ui/MoneyManagerNavHost.kt` | +3 composable routes; extend existing addtransaction route | MEDIUM — nav graph change; test all existing routes still function |
| `data/entity/TransactionEntity.kt` | Optional: replace `VALID_TYPES` with `TransactionType.allIds()` | LOW — strings are identical; no data migration |

### Files NOT touched

The following are explicitly confirmed as requiring no changes:

- All DAOs and Room entities (beyond the optional VALID_TYPES change)
- All existing domain repository interfaces
- All existing repository implementations
- `TransactionFormState.kt`, `TransactionFormConfig.kt`, `TransactionFormConverter.kt`
- All existing DI modules: `DatabaseModule`, `RepositoryModule`, `PreferencesModule`, `FirebaseModule`
- All screens outside `addtransaction` and `MoneyManagerNavHost`: Settings, Accounts, Budgets, Goals, Recurring, Tags, Categories, BorrowLend, Summary, Insights, Transactions

---

## 10. Component Boundaries and Data Flow

```
┌──────────────────────────────────────────────────────────────────────────┐
│  UI Layer (com.moneymanager.app.ui.aidraft)                               │
│                                                                            │
│  SmsPickerScreen ──┐                                                      │
│  ReceiptScanScreen ─┤──► AiDraftViewModel                                 │
│  VoiceMemoScreen ──┘     isAiAvailable: StateFlow<Boolean>                │
│                          isGenerating / draft / error: AiDraftUiState     │
│                          generateDraft(text: String): Unit                 │
└───────────────────────────────┬──────────────────────────────────────────┘
                                │ calls invoke(rawText)
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Domain Layer (com.moneymanager.domain.ai)                                │
│                                                                            │
│  GenerateDraftFromTextUseCase                                              │
│    ├── PromptContextBuilder.build()  [suspend — reads 3 repository flows] │
│    ├── PromptBuilder.build(text, ctx)  [pure function — no suspension]    │
│    └── GenAiClient?.generateDraft(prompt)  [suspend — AICore call]        │
│              └── DraftParser.parse(json) → TransactionDraft               │
└──────────────────────────────────────────────────────────────────────────┘
                                │ implements / uses
                                ▼
┌──────────────────────────────────────────────────────────────────────────┐
│  Data Layer (com.moneymanager.data.ai)                                    │
│                                                                            │
│  NanoAiClient  ───────────────────────► AICore (GMS / Snapdragon NPU)    │
│  DeviceCapabilityManager ─────────────► PreferencesManager (DataStore)   │
│  PromptBuilder  [pure object — no deps]                                   │
│  DraftParser    [pure object — JSON parsing only]                         │
└──────────────────────────────────────────────────────────────────────────┘

Draft handoff:
AiDraftUiState.draft (TransactionDraft)
  └─► serialized as JSON nav argument
        └─► AddTransactionViewModel.SavedStateHandle → state.initialDraft
              └─► AddTransactionScreen passes initialDraft to dialog
                    └─► AddEditTransactionDialog(initialDraft = draft)
                          └─► LaunchedEffect(initialDraft) applies fields
                                └─► user reviews and saves via existing onConfirm path
```

---

## 11. Testability by Component

| Component | Test type | Approach |
|-----------|-----------|---------|
| `TransactionType` | Unit | Verify `allIds()` equals `TransactionEntity.VALID_TYPES`; verify enum is exhaustive |
| `PromptBuilder` | Unit | Pure function — no mocks. Assert prompt strings contain type names, category names |
| `DraftParser` | Unit | Parameterized JSON inputs. Assert null-safe handling of missing/malformed fields |
| `PromptContextBuilder` | Unit | Mock 3 repository interfaces returning stub flows. Assert `PromptContext` fields |
| `GenerateDraftFromTextUseCase` | Unit | Mock `GenAiClient?` (null path / success / failure). Mock `PromptContextBuilder` |
| `AiDraftViewModel` | ViewModel unit | Fake use case. Assert state transitions: initial → loading → success / error |
| `DeviceCapabilityManager` | Instrumentation | Requires device / emulator with AICore installed. Cannot be unit tested |
| `AddEditTransactionDialog` + draft | Compose UI test | Pass non-null `initialDraft`. Assert form fields pre-populated correctly |

---

## Sources

- Direct inspection: `com.moneymanager.data.preferences.PreferencesManager` — DataStore key/Flow/setter pattern
- Direct inspection: `com.moneymanager.app.ui.dialogs.AddEditTransactionDialog` — all `rememberSaveable` variables, `onTypeSelected` function, `LaunchedEffect` usage pattern
- Direct inspection: `com.moneymanager.app.ui.dialogs.TransactionFormState` — sealed class structure; confirmed dialog does not consume it as primary state
- Direct inspection: `com.moneymanager.app.ui.dialogs.TransactionFormConfig` — current hardcoded type list; confirmed `TransactionType` enum is the right replacement
- Direct inspection: `com.moneymanager.data.entity.TransactionEntity` — VALID_TYPES list; all field names used in `TransactionDraft` design
- Direct inspection: `com.moneymanager.domain.repository.CategoryRepository` — confirmed `getAllTags()` lives here; no separate TagRepository needed
- Direct inspection: `com.moneymanager.domain.repository.AccountRepository`, `PeerContactRepository` — confirmed interface contracts for `PromptContextBuilder`
- Direct inspection: `com.moneymanager.app.ui.addtransaction.AddTransactionViewModel` — StateFlow/combine pattern; confirmed AndroidViewModel with `@Inject constructor`
- Direct inspection: `com.moneymanager.app.ui.addtransaction.AddTransactionUiState` — confirmed field structure; null default extension approach
- Direct inspection: `com.moneymanager.app.MoneyManagerApp` — confirmed field injection pattern via `AppLockManager`; established startup hook precedent
- Direct inspection: `com.moneymanager.data.MoneyManagerDatabase` — Room version 8; no AI tables needed (AI is stateless per call)
- Hilt documentation (training knowledge, HIGH confidence): nullable `@Provides` with `@javax.annotation.Nullable` is the required pattern for optional dependencies; `@Singleton` null results are cached identically to non-null results
