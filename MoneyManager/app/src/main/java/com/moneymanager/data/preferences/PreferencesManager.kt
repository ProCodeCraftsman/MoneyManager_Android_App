package com.moneymanager.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.moneymanager.app.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

}