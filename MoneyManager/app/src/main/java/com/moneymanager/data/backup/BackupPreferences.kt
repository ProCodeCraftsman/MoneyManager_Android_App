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
        private val BACKUP_WEEKLY = booleanPreferencesKey("backup_weekly")
        private val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
    }

    val autoBackupEnabled: Flow<Boolean> =
        context.backupDataStore.data.map { it[AUTO_BACKUP_ENABLED] ?: false }

    val backupWeekly: Flow<Boolean> =
        context.backupDataStore.data.map { it[BACKUP_WEEKLY] ?: true }

    val lastBackupTime: Flow<Long?> =
        context.backupDataStore.data.map { it[LAST_BACKUP_TIME] }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.backupDataStore.edit { it[AUTO_BACKUP_ENABLED] = enabled }
    }

    suspend fun setBackupWeekly(weekly: Boolean) {
        context.backupDataStore.edit { it[BACKUP_WEEKLY] = weekly }
    }

    suspend fun setLastBackupTime(time: Long) {
        context.backupDataStore.edit { it[LAST_BACKUP_TIME] = time }
    }
}
