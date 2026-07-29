package com.moneymanager.data.backup

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backupDataStore: DataStore<Preferences> by preferencesDataStore(name = "backup_prefs")

@Singleton
class BackupPreferences @Inject constructor(@ApplicationContext private val context: Context) {

    companion object {
        private val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        private val LOCAL_BACKUP_ENABLED = booleanPreferencesKey("local_backup_enabled")
        private val BACKUP_WEEKLY = booleanPreferencesKey("backup_weekly")
        private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        private val LAST_LOCAL_BACKUP_TIME = longPreferencesKey("last_local_backup_time")
        private val DRIVE_USER_EMAIL = stringPreferencesKey("drive_user_email")
        private val DRIVE_USER_NAME = stringPreferencesKey("drive_user_name")
        private val DRIVE_USER_PHOTO = stringPreferencesKey("drive_user_photo")
    }

    val autoBackupEnabled: Flow<Boolean> =
        context.backupDataStore.data.map { it[AUTO_BACKUP_ENABLED] ?: false }

    val localBackupEnabled: Flow<Boolean> =
        context.backupDataStore.data.map { it[LOCAL_BACKUP_ENABLED] ?: true }

    val backupWeekly: Flow<Boolean> =
        context.backupDataStore.data.map { it[BACKUP_WEEKLY] ?: true }

    val lastBackupTime: Flow<Long?> =
        context.backupDataStore.data.map { it[LAST_BACKUP_TIME] }

    val lastLocalBackupTime: Flow<Long?> =
        context.backupDataStore.data.map { it[LAST_LOCAL_BACKUP_TIME] }

    val driveUserEmail: Flow<String?> =
        context.backupDataStore.data.map { it[DRIVE_USER_EMAIL] }

    val driveUserName: Flow<String?> =
        context.backupDataStore.data.map { it[DRIVE_USER_NAME] }

    val driveUserPhoto: Flow<String?> =
        context.backupDataStore.data.map { it[DRIVE_USER_PHOTO] }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setLocalBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[LOCAL_BACKUP_ENABLED] = enabled }
    }

    suspend fun setBackupWeekly(weekly: Boolean) {
        context.backupDataStore.edit { it[BACKUP_WEEKLY] = weekly }
    }

    suspend fun setLastBackupTime(time: Long) {
        context.backupDataStore.edit { it[LAST_BACKUP_TIME] = time }
    }

    suspend fun setLastLocalBackupTime(time: Long) {
        context.backupDataStore.edit { it[LAST_LOCAL_BACKUP_TIME] = time }
    }

    suspend fun setDriveUserInfo(email: String?, name: String?, photo: String?) {
        context.backupDataStore.edit { prefs ->
            if (email == null) {
                prefs.remove(DRIVE_USER_EMAIL)
                prefs.remove(DRIVE_USER_NAME)
                prefs.remove(DRIVE_USER_PHOTO)
            } else {
                prefs[DRIVE_USER_EMAIL] = email
                name?.let { prefs[DRIVE_USER_NAME] = it } ?: prefs.remove(DRIVE_USER_NAME)
                photo?.let { prefs[DRIVE_USER_PHOTO] = it } ?: prefs.remove(DRIVE_USER_PHOTO)
            }
        }
    }
}
