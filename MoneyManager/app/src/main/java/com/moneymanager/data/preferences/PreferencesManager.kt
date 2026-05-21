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

    suspend fun setUserHasSetTheme() {
        context.dataStore.edit { preferences ->
            preferences[HAS_USER_SET_THEME] = true
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

    /**
     * DEBUG ONLY: Clears all preferences except those related to AI models.
     * This avoids re-downloading large AI models during testing.
     */
    suspend fun resetExceptAi() {
        context.dataStore.edit { preferences ->
            val aiStatus = preferences[AI_AVAILABILITY_STATUS]
            val aiProgress = preferences[AI_DOWNLOAD_PROGRESS]
            val backendTier = preferences[AI_BACKEND_TIER]
            val modelDownloaded = preferences[LOCAL_MODEL_DOWNLOADED]
            val modelProgress = preferences[LOCAL_MODEL_DOWNLOAD_PROGRESS]
            val modelReceived = preferences[LOCAL_MODEL_DOWNLOAD_RECEIVED]
            val modelTotal = preferences[LOCAL_MODEL_DOWNLOAD_TOTAL]
            val modelSpeed = preferences[LOCAL_MODEL_DOWNLOAD_SPEED]
            val selectedModel = preferences[SELECTED_LOCAL_MODEL]
            val wifiOnly = preferences[WIFI_ONLY_DOWNLOAD]
            val hfToken = preferences[HF_ACCESS_TOKEN]
            val hfExpires = preferences[HF_TOKEN_EXPIRES_AT]
            val optIn = preferences[USER_OPTED_IN_AI]
            val allowlist = preferences[USER_ALLOWLIST_JSON]

            preferences.clear()

            // Restore AI-related settings
            if (aiStatus != null) preferences[AI_AVAILABILITY_STATUS] = aiStatus
            if (aiProgress != null) preferences[AI_DOWNLOAD_PROGRESS] = aiProgress
            if (backendTier != null) preferences[AI_BACKEND_TIER] = backendTier
            if (modelDownloaded != null) preferences[LOCAL_MODEL_DOWNLOADED] = modelDownloaded
            if (modelProgress != null) preferences[LOCAL_MODEL_DOWNLOAD_PROGRESS] = modelProgress
            if (modelReceived != null) preferences[LOCAL_MODEL_DOWNLOAD_RECEIVED] = modelReceived
            if (modelTotal != null) preferences[LOCAL_MODEL_DOWNLOAD_TOTAL] = modelTotal
            if (modelSpeed != null) preferences[LOCAL_MODEL_DOWNLOAD_SPEED] = modelSpeed
            if (selectedModel != null) preferences[SELECTED_LOCAL_MODEL] = selectedModel
            if (wifiOnly != null) preferences[WIFI_ONLY_DOWNLOAD] = wifiOnly
            if (hfToken != null) preferences[HF_ACCESS_TOKEN] = hfToken
            if (hfExpires != null) preferences[HF_TOKEN_EXPIRES_AT] = hfExpires
            if (optIn != null) preferences[USER_OPTED_IN_AI] = optIn
            if (allowlist != null) preferences[USER_ALLOWLIST_JSON] = allowlist
        }
    }
}
