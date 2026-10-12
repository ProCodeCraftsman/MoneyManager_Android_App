package com.moneymanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.moneymanager.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val CURRENCY = stringPreferencesKey("currency")
        private val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        private val PIN_HASH = stringPreferencesKey("pin_hash")
        private val PIN_SALT = stringPreferencesKey("pin_salt")
        private val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        private val WRONG_ATTEMPTS = intPreferencesKey("wrong_attempts")
        private val LAST_SYNC = longPreferencesKey("last_sync")
        private val SELECTED_THEME = stringPreferencesKey("selected_theme")
        private val HAS_USER_SET_THEME = booleanPreferencesKey("has_user_set_theme")
        private val AUTO_LOCK_MINUTES = intPreferencesKey("auto_lock_minutes")
        private val LAST_UNLOCK_TIME = longPreferencesKey("last_unlock_time")
        private val AI_AVAILABILITY_STATUS = stringPreferencesKey("ai_availability_status")
        private val AI_DOWNLOAD_PROGRESS = floatPreferencesKey("ai_download_progress")
        private val AI_BACKEND_TIER = stringPreferencesKey("ai_backend_tier")
        private val LOCAL_MODEL_DOWNLOADED = booleanPreferencesKey("local_model_downloaded")
        private val LOCAL_MODEL_DOWNLOAD_PROGRESS = floatPreferencesKey("local_model_download_progress")
        private val LOCAL_MODEL_DOWNLOAD_RECEIVED = longPreferencesKey("local_model_download_received")
        private val LOCAL_MODEL_DOWNLOAD_TOTAL = longPreferencesKey("local_model_download_total")
        private val LOCAL_MODEL_DOWNLOAD_SPEED = longPreferencesKey("local_model_download_speed")
        private val SELECTED_LOCAL_MODEL = stringPreferencesKey("selected_local_model")
        private val WIFI_ONLY_DOWNLOAD = booleanPreferencesKey("wifi_only_download")
        private val HF_ACCESS_TOKEN = stringPreferencesKey("hf_access_token")
        private val HF_TOKEN_EXPIRES_AT = longPreferencesKey("hf_token_expires_at")
        private val USER_OPTED_IN_AI = booleanPreferencesKey("user_opted_in_ai")
        private val USER_ALLOWLIST_JSON = stringPreferencesKey("user_allowlist_json")
        private val IMAGE_ATTACHMENTS_ENABLED = booleanPreferencesKey("image_attachments_enabled")
        private const val DEFAULT_ACCOUNT_PREFIX = "default_account_"
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_MODE] ?: false
    }

    val currency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[CURRENCY] ?: "INR"
    }

    val pinEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PIN_ENABLED] ?: false
    }

    val pinHash: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PIN_HASH]
    }

    val pinSalt: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[PIN_SALT]
    }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[BIOMETRIC_ENABLED] ?: false
    }

    val wrongAttempts: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[WRONG_ATTEMPTS] ?: 0
    }

    val lastSyncTime: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[LAST_SYNC]
    }

    val selectedTheme: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        AppTheme.fromString(preferences[SELECTED_THEME] ?: AppTheme.CALM_GREEN.name)
    }

    val hasUserSetTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[HAS_USER_SET_THEME] ?: false
    }

    val autoLockMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[AUTO_LOCK_MINUTES] ?: 0
    }

    val lastUnlockTime: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[LAST_UNLOCK_TIME]
    }

    val aiAvailabilityStatus: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_AVAILABILITY_STATUS] ?: "PENDING"
    }

    val aiDownloadProgress: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[AI_DOWNLOAD_PROGRESS] ?: 0f
    }

    val aiBackendTier: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[AI_BACKEND_TIER] ?: "pending"
    }

    val isLocalModelDownloaded: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LOCAL_MODEL_DOWNLOADED] ?: false
    }

    val localModelDownloadProgress: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[LOCAL_MODEL_DOWNLOAD_PROGRESS] ?: 0f
    }

    val localModelDownloadReceived: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LOCAL_MODEL_DOWNLOAD_RECEIVED] ?: 0L
    }

    val localModelDownloadTotal: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LOCAL_MODEL_DOWNLOAD_TOTAL] ?: 0L
    }

    val localModelDownloadSpeed: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[LOCAL_MODEL_DOWNLOAD_SPEED] ?: 0L
    }

    val selectedLocalModel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SELECTED_LOCAL_MODEL] ?: "GEMMA3_1B"
    }

    val wifiOnlyDownload: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[WIFI_ONLY_DOWNLOAD] ?: true
    }

    val userOptedInAi: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[USER_OPTED_IN_AI] ?: false
    }

    val hfAccessToken: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HF_ACCESS_TOKEN] ?: ""
    }

    val hfTokenExpiresAt: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[HF_TOKEN_EXPIRES_AT] ?: 0L
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { preferences ->
            preferences[CURRENCY] = currency
        }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PIN_ENABLED] = enabled
        }
    }

    suspend fun setPinHash(hash: String?) {
        context.dataStore.edit { preferences ->
            if (hash != null) {
                preferences[PIN_HASH] = hash
            } else {
                preferences.remove(PIN_HASH)
            }
        }
    }

    suspend fun setPinSalt(salt: String?) {
        context.dataStore.edit { preferences ->
            if (salt != null) {
                preferences[PIN_SALT] = salt
            } else {
                preferences.remove(PIN_SALT)
            }
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setWrongAttempts(attempts: Int) {
        context.dataStore.edit { preferences ->
            preferences[WRONG_ATTEMPTS] = attempts
        }
    }

    suspend fun resetWrongAttempts() {
        context.dataStore.edit { preferences ->
            preferences[WRONG_ATTEMPTS] = 0
        }
    }

    suspend fun setLastSyncTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SYNC] = time
        }
    }

    suspend fun setSelectedTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_THEME] = theme.name
        }
    }

    suspend fun setUserHasSetTheme(hasSet: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[HAS_USER_SET_THEME] = hasSet
        }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_LOCK_MINUTES] = minutes
        }
    }

    suspend fun setLastUnlockTime(time: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_UNLOCK_TIME] = time
        }
    }

    suspend fun setAiAvailabilityStatus(value: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_AVAILABILITY_STATUS] = value
        }
    }

    suspend fun setAiDownloadProgress(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[AI_DOWNLOAD_PROGRESS] = value
        }
    }

    suspend fun setAiBackendTier(value: String) {
        context.dataStore.edit { preferences ->
            preferences[AI_BACKEND_TIER] = value
        }
    }

    suspend fun setLocalModelDownloaded(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_MODEL_DOWNLOADED] = value
        }
    }

    suspend fun setLocalModelDownloadProgress(value: Float) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_MODEL_DOWNLOAD_PROGRESS] = value
        }
    }

    suspend fun setLocalModelDownloadReceived(value: Long) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_MODEL_DOWNLOAD_RECEIVED] = value
        }
    }

    suspend fun setLocalModelDownloadTotal(value: Long) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_MODEL_DOWNLOAD_TOTAL] = value
        }
    }

    suspend fun setLocalModelDownloadSpeed(value: Long) {
        context.dataStore.edit { preferences ->
            preferences[LOCAL_MODEL_DOWNLOAD_SPEED] = value
        }
    }

    suspend fun setSelectedLocalModel(value: String) {
        context.dataStore.edit { preferences ->
            preferences[SELECTED_LOCAL_MODEL] = value
        }
    }

    suspend fun setWifiOnlyDownload(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[WIFI_ONLY_DOWNLOAD] = value
        }
    }

    suspend fun setUserOptedInAi(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[USER_OPTED_IN_AI] = value
        }
    }

    suspend fun setHfAccessToken(token: String, expiresAt: Long = 0L) {
        context.dataStore.edit { preferences ->
            preferences[HF_ACCESS_TOKEN] = token
            preferences[HF_TOKEN_EXPIRES_AT] = expiresAt
        }
    }

    suspend fun clearHfAccessToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(HF_ACCESS_TOKEN)
            preferences.remove(HF_TOKEN_EXPIRES_AT)
        }
    }

    fun getHfAccessTokenSync(): String {
        return runBlocking { context.dataStore.data.first()[HF_ACCESS_TOKEN] ?: "" }
    }

    fun getSelectedLocalModelSync(): String {
        return runBlocking { context.dataStore.data.first()[SELECTED_LOCAL_MODEL] ?: "GEMMA3_1B" }
    }

    fun getWifiOnlyDownloadSync(): Boolean {
        return runBlocking { context.dataStore.data.first()[WIFI_ONLY_DOWNLOAD] ?: true }
    }

    fun getLocalModelDownloadProgressSync(): Float {
        return runBlocking { context.dataStore.data.first()[LOCAL_MODEL_DOWNLOAD_PROGRESS] ?: 0f }
    }

    val userAllowlistJsonFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[USER_ALLOWLIST_JSON] ?: ""
    }

    val imageAttachmentsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IMAGE_ATTACHMENTS_ENABLED] ?: true
    }

    suspend fun getUserAllowlistJson(): String =
        context.dataStore.data.first()[USER_ALLOWLIST_JSON] ?: ""

    suspend fun setUserAllowlistJson(json: String) {
        context.dataStore.edit { prefs -> prefs[USER_ALLOWLIST_JSON] = json }
    }

    suspend fun setImageAttachmentsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IMAGE_ATTACHMENTS_ENABLED] = enabled
        }
    }

    /** Configured default account id per slot key (see DefaultAccountSlot); a missing slot means "Auto". */
    val defaultAccounts: Flow<Map<String, Long>> = context.dataStore.data.map { preferences ->
        preferences.asMap().entries.mapNotNull { (key, value) ->
            if (key.name.startsWith(DEFAULT_ACCOUNT_PREFIX) && value is Long) {
                key.name.removePrefix(DEFAULT_ACCOUNT_PREFIX) to value
            } else null
        }.toMap()
    }

    suspend fun getDefaultAccounts(): Map<String, Long> = defaultAccounts.first()

    /** Resets every slot that points at [accountId] back to "Auto". */
    suspend fun clearDefaultAccountsFor(accountId: Long) {
        context.dataStore.edit { preferences ->
            preferences.asMap().entries
                .filter { (key, value) -> key.name.startsWith(DEFAULT_ACCOUNT_PREFIX) && value == accountId }
                .forEach { (key, _) -> preferences.remove(key) }
        }
    }

    /** Passing null resets the slot to "Auto". */
    suspend fun setDefaultAccount(slotKey: String, accountId: Long?) {
        context.dataStore.edit { preferences ->
            val key = longPreferencesKey(DEFAULT_ACCOUNT_PREFIX + slotKey)
            if (accountId == null) preferences.remove(key) else preferences[key] = accountId
        }
    }

    /** SMS account source key (see SmsAccountLinks.keyFor) -> the account the user filed it under. */
    suspend fun getSmsAccountLinks(): Map<String, Long> =
        context.dataStore.data.first().asMap().entries.mapNotNull { (key, value) ->
            if (key.name.startsWith(SMS_LINK_PREFIX) && value is Long) key.name.removePrefix(SMS_LINK_PREFIX) to value else null
        }.toMap()

    suspend fun setSmsAccountLink(linkKey: String, accountId: Long) {
        context.dataStore.edit { it[longPreferencesKey(SMS_LINK_PREFIX + linkKey)] = accountId }
    }

    /** Newest SMS date the inbox scanner has fully processed (F07 scan cursor). Not backed up: see F07 risks. */
    suspend fun getCaptureLastScanned(): Long? = context.dataStore.data.first()[CAPTURE_LAST_SCANNED]

    suspend fun setCaptureLastScanned(time: Long) {
        context.dataStore.edit { it[CAPTURE_LAST_SCANNED] = time }
    }

    /** Optional floor chosen by the user for the inbox scan; null means the 31-day cap alone applies. */
    suspend fun getCaptureStartDate(): Long? = context.dataStore.data.first()[CAPTURE_START_DATE]

    suspend fun setCaptureStartDate(time: Long?) {
        context.dataStore.edit { prefs ->
            if (time == null) prefs.remove(CAPTURE_START_DATE) else prefs[CAPTURE_START_DATE] = time
        }
    }

    /** Noon / evening SMS reminders (F07 E9). Off by default; enabling asks for notification permission first. */
    val captureRemindersEnabled: Flow<Boolean> = context.dataStore.data.map { it[CAPTURE_REMINDERS_ENABLED] ?: false }

    suspend fun setCaptureRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[CAPTURE_REMINDERS_ENABLED] = enabled }
    }

    /** Local epoch day on which the home banner was dismissed with ✕. */
    val captureBannerDismissedDay: Flow<Long?> = context.dataStore.data.map { it[CAPTURE_BANNER_DISMISSED_DAY] }

    suspend fun setCaptureBannerDismissedDay(epochDay: Long) {
        context.dataStore.edit { it[CAPTURE_BANNER_DISMISSED_DAY] = epochDay }
    }

    /** "Don't show this" on the home banner. */
    val captureBannerHidden: Flow<Boolean> = context.dataStore.data.map { it[CAPTURE_BANNER_HIDDEN] ?: false }

    suspend fun setCaptureBannerHidden(hidden: Boolean) {
        context.dataStore.edit { it[CAPTURE_BANNER_HIDDEN] = hidden }
    }

    /** The one-time "set battery to Unrestricted" tip, shown after reminders are first enabled. */
    suspend fun isCaptureBatteryTipShown(): Boolean = context.dataStore.data.first()[CAPTURE_BATTERY_TIP_SHOWN] ?: false

    suspend fun setCaptureBatteryTipShown() {
        context.dataStore.edit { it[CAPTURE_BATTERY_TIP_SHOWN] = true }
    }

    /** Fingerprints of audit findings the user marked "It's correct" (F08 A2). Backed up in the JSON settings block. */
    val auditDismissed: Flow<Set<String>> = context.dataStore.data.map { it[AUDIT_DISMISSED] ?: emptySet() }

    suspend fun getAuditDismissed(): Set<String> = context.dataStore.data.first()[AUDIT_DISMISSED] ?: emptySet()

    suspend fun setAuditDismissed(fingerprints: Set<String>) {
        context.dataStore.edit { it[AUDIT_DISMISSED] = fingerprints }
    }

    /** The findings the inbox shows, as JSON, written by the audit worker. A cache: recomputed on every run, not backed up. */
    val auditFindingsJson: Flow<String?> = context.dataStore.data.map { it[AUDIT_FINDINGS] }

    suspend fun setAuditFindingsJson(json: String) {
        context.dataStore.edit { it[AUDIT_FINDINGS] = json }
    }

    /** Per-source draft edit counts (AI-014), serialized by DraftEditStatsRepository. Field names only, no values. */
    suspend fun getDraftEditStatsJson(): String? = context.dataStore.data.first()[DRAFT_EDIT_STATS]

    suspend fun setDraftEditStatsJson(json: String) {
        context.dataStore.edit { it[DRAFT_EDIT_STATS] = json }
    }
}

private const val SMS_LINK_PREFIX = "sms_account_link:"
private val CAPTURE_LAST_SCANNED = longPreferencesKey("capture_last_scanned")
private val CAPTURE_START_DATE = longPreferencesKey("capture_start_date")
private val CAPTURE_REMINDERS_ENABLED = booleanPreferencesKey("capture_reminders_enabled")
private val CAPTURE_BANNER_DISMISSED_DAY = longPreferencesKey("capture_banner_dismissed_day")
private val CAPTURE_BANNER_HIDDEN = booleanPreferencesKey("capture_banner_hidden")
private val CAPTURE_BATTERY_TIP_SHOWN = booleanPreferencesKey("capture_battery_tip_shown")
private val AUDIT_DISMISSED = stringSetPreferencesKey("audit_dismissed")
private val AUDIT_FINDINGS = stringPreferencesKey("audit_findings")
private val DRAFT_EDIT_STATS = stringPreferencesKey("draft_edit_stats")
