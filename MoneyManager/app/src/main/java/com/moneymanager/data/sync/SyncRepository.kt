package com.moneymanager.data.sync

import kotlinx.coroutines.flow.Flow

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Syncing : SyncStatus()
    data object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
    data class Offline(val pendingChanges: Int) : SyncStatus()
}

data class SyncState(
    val status: SyncStatus = SyncStatus.Idle,
    val lastSyncTime: Long? = null,
    val pendingChangesCount: Int = 0,
    val isOnline: Boolean = true
)

interface SyncRepository {
    fun getSyncState(): Flow<SyncState>
    suspend fun sync(): Result<Unit>
    suspend fun push(): Result<Unit>
    suspend fun pull(): Result<Unit>
    suspend fun getLastSyncTime(): Long?
    suspend fun queueChange(entityType: String, entityId: Long, changeType: ChangeType)
    suspend fun clearPendingChanges()
}

enum class ChangeType {
    CREATE, UPDATE, DELETE
}
