# Stack Research — AI-Assisted Transaction Drafting (v3.0)

**Project:** MoneyManager — v3.0 AI-Assisted Transaction Drafting
**Researched:** 2026-05-15
**Overall Confidence:** HIGH (all artifacts verified via Google Maven metadata, official Google sample build.gradle, and AAR manifest inspection)

---

## Existing Stack (Unchanged — Do Not Re-research)

The v3.0 milestone inherits the full v2.x stack. These are confirmed in `app/build.gradle.kts`:

| Component | Version | Relevant to v3.0 |
|-----------|---------|-----------------|
| Kotlin (via `kotlin.plugin.compose`) | 2.3.20 | Must match serialization plugin version |
| Compose BOM | 2024.12.01 | Camera preview composable, permission dialogs |
| Hilt | 2.59.2 | `AiModule` for conditional `GenAiClient?` provision |
| Room | 2.8.4 | Reads existing Category/Account/Tag/Peer data for prompt context |
| DataStore Preferences | 1.1.1 | Caches `isAiAssistAvailable: Boolean` |
| Firebase BOM | 34.12.0 | Already present — no new Firebase additions needed |
| WorkManager | 2.11.2 | Available for background model download monitoring if needed |
| minSdk | 26 | GenAI AAR declares minSdkVersion 26 — compatible |
| compileSdk | 36 | Compatible with all new dependencies |

---

## New Dependencies Required for v3.0

### 1. ML Kit GenAI Prompt API (On-Device Gemini Nano)

**Confirmed via:** Google Maven metadata XML, official `googlesamples/mlkit` sample `build.gradle`, AAR AndroidManifest inspection.

| Group:Artifact:Version | Purpose | APK Impact | Notes |
|------------------------|---------|------------|-------|
| `com.google.mlkit:genai-prompt:1.0.0-beta2` | Core Prompt API — `GenerativeModel`, `GenerateContentRequest`, `TextPart`, `ImagePart`, streaming callbacks | ~856KB AAR, but model is system-managed | Latest as of 2026-04-01. No stable `1.0.0` yet — beta2 is the release channel |
| `com.google.mlkit:genai-common:1.0.0-beta3` | Transitive — `DownloadCallback`, `FeatureStatus`, `StreamingCallback` | ~61KB AAR | Pulled transitively by `genai-prompt`; declare explicitly to control version and access `FeatureStatus` API for availability checks |

**Gradle declaration:**
```kotlin
// ML Kit GenAI — Gemini Nano on-device inference
implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")
implementation("com.google.mlkit:genai-common:1.0.0-beta3")
```

**Runtime SDK requirement:** The AAR itself declares `android:minSdkVersion="26"` — compatible with this project. However, AICore (the system service providing Gemini Nano) only exists on supported devices running Android 12+ (API 31) with a Snapdragon 8 Gen 3 or equivalent NPU. The availability check API (`generativeModel.checkFeatureStatus()` returning a `ListenableFuture<@FeatureStatus Int>`) handles unsupported devices at runtime — the `DeviceCapabilityManager` component must call this and cache the Boolean result in DataStore. No hard minSdk bump is needed.

**Key API classes** (from `com.google.mlkit.genai.prompt`):
- `GenerativeModel` — entry point, created via a factory/options builder
- `GenerateContentRequest` / `generateContentRequest { }` DSL builder
- `TextPart`, `ImagePart` — content parts
- `PromptPrefix` — system prompt / context injection
- `GenerateContentResponse`, `CountTokensResponse`
- `FinishReason` enum on `Candidate`

**Key API classes** (from `com.google.mlkit.genai.common`):
- `FeatureStatus` — `UNAVAILABLE`, `DOWNLOADABLE`, `DOWNLOADING`, `AVAILABLE`
- `DownloadCallback` — progress tracking for model download
- `StreamingCallback` — for streaming token output

**Hilt integration pattern:**
```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AiModule {
    @Provides
    @Singleton
    fun provideGenAiClient(
        @ApplicationContext context: Context,
        deviceCapabilityManager: DeviceCapabilityManager
    ): GenAiClient? {
        // Returns null when AICore unavailable; callers use nullable injection
        return if (deviceCapabilityManager.isAiAssistAvailable.value) {
            NanoAiClient(context)
        } else null
    }
}
```

**No special manifest permissions required.** The AAR's merged manifest already declares:
```xml
<uses-permission android:name="com.google.android.apps.aicore.service.BIND_SERVICE" />
<queries>
    <package android:name="com.google.android.aicore" />
</queries>
```
These are merged from the AAR automatically by the Android Gradle Plugin. No manual addition needed.

---

### 2. Unbundled ML Kit Text Recognition (OCR for Receipts)

**Confirmed via:** Google Maven metadata XML (`latest: 19.0.1`), official Google sample `build.gradle` (shows bundled vs unbundled pair), AAR size comparison.

| Group:Artifact:Version | Purpose | APK Impact | Notes |
|------------------------|---------|------------|-------|
| `com.google.android.gms:play-services-mlkit-text-recognition:19.0.1` | OCR — recognizes Latin text in receipt images via camera or gallery | +78KB to APK | Model (~7MB) downloaded via Google Play Services at app install — zero APK bloat. Requires Google Play Services on device. |

**Gradle declaration:**
```kotlin
// Unbundled ML Kit OCR — model delivered via Google Play Services (no APK size increase)
implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")
```

**Do NOT use the bundled alternative:**
```kotlin
// AVOID — bundles 1.38MB model inside the APK
// implementation("com.google.mlkit:text-recognition:16.0.1")
```

The bundled version (`com.google.mlkit:text-recognition:16.0.1`) ships the OCR model inside your APK, adding ~1.38MB to the download size. The unbundled GMS version delivers the same model via Google Play Services on first use. Since this app targets Google Play, the unbundled version is the correct choice.

**API compatibility:** `minSdkVersion 21` in the AAR manifest — fully compatible with this project's `minSdk 26`.

**Required manifest permission for camera:**
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```
The `android:required="false"` on the feature declaration ensures the app remains installable on devices without a rear camera (tablets, Chromebooks). Camera permission must be requested at runtime (API 23+).

**Integration pattern:** The OCR flow calls `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)`, processes a `InputImage.fromBitmap()` or `InputImage.fromMediaImage()`, and returns a `Task<Text>` with `Text.TextBlock` / `Text.Line` / `Text.Element` results. The recognized string is passed to `GenerateDraftFromTextUseCase`.

---

### 3. kotlinx.serialization — JSON Parsing for AI Output

**Confirmed via:** Maven Central metadata (latest stable: `1.8.1` as of 2026-04-09), Kotlin serialization plugin version must match Kotlin version (`2.3.20`).

**Assessment: The project does NOT already have kotlinx.serialization.** Inspecting `app/build.gradle.kts`, there is no `kotlinx-serialization-json` dependency and no `org.jetbrains.kotlin.plugin.serialization` plugin. It must be added.

**Two changes required:**

**Change 1 — Root `build.gradle.kts` plugins block** (add the serialization plugin):
```kotlin
// Add to root build.gradle.kts plugins block
id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
```
The plugin version MUST match the Kotlin version already in the project (`2.3.20` from `kotlin.plugin.compose`). Mismatching versions cause compilation errors.

**Change 2 — App `app/build.gradle.kts`** (apply plugin + add runtime):
```kotlin
// Apply in app/build.gradle.kts plugins block
id("org.jetbrains.kotlin.plugin.serialization")

// Add to dependencies block
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
```

| Group:Artifact:Version | Purpose | APK Impact | Notes |
|------------------------|---------|------------|-------|
| `org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1` | Parse AI-generated JSON output into `TransactionDraft` domain model | +~300KB to APK (runtime library only, minification reduces to ~80KB with R8) | Version-matched to Kotlin 2.3.20 |
| `org.jetbrains.kotlin.plugin.serialization` (Gradle plugin) | Code generation for `@Serializable` data classes | Zero APK impact | Must equal Kotlin version: `2.3.20` |

**Why needed:** The `GenerateDraftFromTextUseCase` receives a text response from Gemini Nano. For reliable field extraction (amount, category, date, memo), the prompt instructs the model to return JSON. `@Serializable` + `Json.decodeFromString<TransactionDraft>()` is the correct parsing pattern. Manual string parsing of AI output is fragile and should be avoided.

**Alternative considered:** Gson / Moshi. Both are viable, but kotlinx.serialization is the idiomatic Kotlin-first choice, is compile-time safe with `@Serializable`, works with Kotlin value classes, and is already the de facto standard in new Kotlin/Android projects. No reason to add a Java-based JSON library when the Kotlin-native solution exists.

---

### 4. Android SpeechRecognizer (Voice Memo) — No New Dependency

**The Android `SpeechRecognizer` API is part of the Android SDK.** No Gradle dependency required.

```kotlin
// Built-in — no implementation() line needed
import android.speech.SpeechRecognizer
import android.speech.RecognizerIntent
import android.speech.RecognitionListener
```

**Offline operation:** Pass `RecognizerIntent.EXTRA_PREFER_OFFLINE = true` in the Intent extras. On Android 10+ (API 29), this reliably routes to the on-device recognizer if available. Devices that downloaded the offline speech model via Google Assistant or Google app will work fully offline.

**Required manifest permission:**
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
```
Must be requested at runtime (API 23+). The permission dialog should be shown only when the user taps the voice input button.

---

### 5. SMS Reading — No New Dependency

Reading SMS messages uses the Android `ContentResolver` API against `Telephony.Sms.Inbox`. No Gradle dependency required.

```kotlin
// Built-in — no implementation() line needed
import android.provider.Telephony
```

**Required manifest permissions:**
```xml
<uses-permission android:name="android.permission.READ_SMS" />
```

**Important warning:** `READ_SMS` is a "Special" dangerous permission. Google Play policy restricts SMS access to apps whose core function requires it (e.g., SMS managers, financial apps). For a personal finance app, SMS reading for transaction import is a defensible use case, but the **Default SMS App** status is NOT required — READ_SMS as a regular permission with a clear user-facing purpose statement is sufficient. The permission request dialog must clearly explain why SMS access is needed. Prepare a privacy policy disclosure.

---

## Complete Manifest Permissions Summary for v3.0

Add these to `AndroidManifest.xml`. The AICore permissions are auto-merged from the AAR and are listed here for documentation only.

```xml
<!-- Camera — for receipt scanning -->
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />

<!-- Audio — for voice memo input -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- SMS — for financial SMS parsing (runtime, user-initiated only) -->
<uses-permission android:name="android.permission.READ_SMS" />

<!-- AICore — auto-merged from genai-prompt AAR manifest, listed for awareness -->
<!-- <uses-permission android:name="com.google.android.apps.aicore.service.BIND_SERVICE" /> -->
<!-- <queries><package android:name="com.google.android.aicore" /></queries> -->
```

All three runtime permissions (CAMERA, RECORD_AUDIO, READ_SMS) must be requested at runtime using `ActivityResultContracts.RequestPermission()`. Request each permission only at the moment the user initiates the relevant action — do not request all three at app launch.

---

## What NOT to Add

| Library | Why Not |
|---------|---------|
| `com.google.mlkit:text-recognition:16.0.1` (bundled OCR) | Adds ~1.38MB model to APK. Use unbundled GMS version instead |
| `com.google.android.aicore:aicore-client-api` | This group/artifact does NOT exist on Google Maven. The AICore client API is accessed exclusively through `com.google.mlkit:genai-*` artifacts |
| `com.google.android.gms:play-services-mlkit-genai-inference` | Does NOT exist on Google Maven. This artifact ID appears in some outdated blog posts; the correct artifacts are `com.google.mlkit:genai-prompt` and siblings |
| `com.google.android.gms:play-services-mlkit-language-id` | Not needed for transaction drafting |
| MediaPipe LLM Inference (`com.google.mediapipe:tasks-genai`) | Requires bundling the model file inside the APK (~1-4GB) — incompatible with zero-bloat constraint. Use ML Kit GenAI (system-managed model) instead |
| Google AI SDK (`com.google.ai.client.generativeai`) | This is the Gemini API SDK for cloud inference (requires API key, internet, sends data to Google servers). Incompatible with the offline/privacy-preserving requirement |
| Firebase ML | Firebase ML Vision is deprecated. All ML features now route through ML Kit directly |
| `androidx.camera:camera-*` | CameraX adds ~500KB. For this milestone, the receipt capture can use `ActivityResultContracts.TakePicture()` (system camera intent) which requires zero new dependencies. CameraX would only be justified if a live viewfinder composable is required |
| Moshi / Gson | Redundant with kotlinx.serialization which is the idiomatic Kotlin choice |
| `kotlinx-datetime` | Not needed for v3.0 AI features. Date extraction from AI output can use `java.time.LocalDate` (available since API 26, matching minSdk) |
| `com.google.mlkit:genai-summarization`, `genai-proofreading`, `genai-rewriting` | Not needed for transaction drafting use case — only `genai-prompt` is required |

---

## Dependency Version Compatibility Matrix

| Dependency | Version | Compatible With | Risk |
|------------|---------|-----------------|------|
| `com.google.mlkit:genai-prompt` | 1.0.0-beta2 | minSdk 26, Kotlin 2.x | MEDIUM — beta API, no stability guarantee until 1.0.0 stable |
| `com.google.mlkit:genai-common` | 1.0.0-beta3 | minSdk 26 | MEDIUM — same beta channel |
| `com.google.android.gms:play-services-mlkit-text-recognition` | 19.0.1 | minSdk 21, GMS required | LOW — stable, widely deployed |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.8.1 | Kotlin 2.3.20 | LOW — stable release |
| `org.jetbrains.kotlin.plugin.serialization` (plugin) | 2.3.20 | Kotlin 2.3.20 (must match) | LOW if versions match |

---

## Build Configuration Changes Summary

### Root `build.gradle.kts` — add one plugin:
```kotlin
id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
```

### App `app/build.gradle.kts` — apply plugin + add 4 implementation lines:
```kotlin
// plugins block
id("org.jetbrains.kotlin.plugin.serialization")

// dependencies block
// AI — Gemini Nano on-device via AICore
implementation("com.google.mlkit:genai-prompt:1.0.0-beta2")
implementation("com.google.mlkit:genai-common:1.0.0-beta3")

// OCR — unbundled ML Kit text recognition (model via Play Services, zero APK bloat)
implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.1")

// JSON — parsing AI output into TransactionDraft
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
```

No changes to `settings.gradle.kts` — `google()` and `mavenCentral()` repositories are already declared.

---

## Confidence Assessment

| Area | Confidence | Basis |
|------|------------|-------|
| GenAI artifact IDs and versions | HIGH | Verified via Google Maven group-index.xml, maven-metadata.xml, and AAR inspection |
| genai-prompt minSdk compatibility | HIGH | AAR AndroidManifest declares `android:minSdkVersion="26"` — directly inspected |
| AICore runtime availability | HIGH | `com.google.android.aicore` package query in AAR manifest; `FeatureStatus` API confirmed in official sample source |
| Unbundled OCR artifact and version | HIGH | Google Maven metadata confirms `play-services-mlkit-text-recognition:19.0.1` as latest stable |
| APK size comparison (bundled vs unbundled) | HIGH | AAR file sizes measured directly: bundled 1.38MB vs unbundled 78KB |
| kotlinx.serialization version | HIGH | Maven Central metadata confirms `1.8.1` as latest stable (updated 2026-04-09) |
| Kotlin plugin version match requirement | HIGH | Kotlin serialization plugin must match Kotlin version — fundamental requirement from JetBrains |
| `play-services-mlkit-genai-inference` non-existence | HIGH | 404 on Google Maven, confirmed absent from group-index.xml |
| `aicore-client-api` non-existence | HIGH | Absent from Google Maven — not a real artifact |
| SpeechRecognizer / SMS — no dependency needed | HIGH | Both are standard Android SDK APIs |

---

## Sources

- Google Maven group index: `https://dl.google.com/dl/android/maven2/com/google/mlkit/group-index.xml`
- Google Maven group index (GMS): `https://dl.google.com/dl/android/maven2/com/google/android/gms/group-index.xml`
- Official ML Kit GenAI sample (2025): `https://github.com/googlesamples/mlkit/tree/master/android/genai`
- Official ML Kit GenAI sample `build.gradle` (minSdk 31, genai-prompt:1.0.0-beta1): `https://raw.githubusercontent.com/googlesamples/mlkit/master/android/genai/app/build.gradle`
- `genai-prompt:1.0.0-beta2` AAR AndroidManifest (direct inspection): declares `minSdkVersion 26`, `BIND_SERVICE` permission, AICore package query
- `genai-common:1.0.0-beta3` AAR AndroidManifest (direct inspection): declares `minSdkVersion 26`, AICore package query
- `play-services-mlkit-text-recognition:19.0.1` AAR AndroidManifest (direct inspection): declares `minSdkVersion 21`
- Maven Central metadata for `kotlinx-serialization-json`: latest 1.8.1, lastUpdated 20260409
- Maven Central metadata for `kotlin-serialization` plugin: latest 2.4.0-RC (stable = 2.3.21, but project uses 2.3.20 — use 2.3.20 to match)
