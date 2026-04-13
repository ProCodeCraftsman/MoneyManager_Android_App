package com.moneymanager.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FirebaseSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val preferencesManager: com.moneymanager.data.preferences.PreferencesManager
) : SyncRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private val _syncState = MutableStateFlow(SyncState())
    override fun getSyncState(): Flow<SyncState> = _syncState.asStateFlow()

    private val pendingChanges = mutableListOf<PendingChange>()
    private var retryCount = 0
    private val maxRetries = 3

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _syncState.update { it.copy(isOnline = true) }
            if (_syncState.value.pendingChangesCount > 0) {
                scope.launch { sync() }
            }
        }

        override fun onLost(network: Network) {
            _syncState.update { it.copy(isOnline = false) }
        }
    }

    init {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        _syncState.update { it.copy(isOnline = isNetworkAvailable()) }
        loadLastSyncTime()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadLastSyncTime() {
        scope.launch {
            preferencesManager.lastSyncTime.collect { time ->
                _syncState.update { it.copy(lastSyncTime = time) }
            }
        }
    }

    override suspend fun sync(): Result<Unit> {
        if (!isNetworkAvailable()) {
            _syncState.update { 
                it.copy(status = SyncStatus.Offline(it.pendingChangesCount)) 
            }
            return Result.failure(Exception("No network connection"))
        }

        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        _syncState.update { it.copy(status = SyncStatus.Syncing) }

        return try {
            pull()
            push()
            
            val syncTime = System.currentTimeMillis()
            preferencesManager.setLastSyncTime(syncTime)
            _syncState.update { 
                it.copy(
                    status = SyncStatus.Success,
                    lastSyncTime = syncTime,
                    pendingChangesCount = 0
                )
            }
            pendingChanges.clear()
            retryCount = 0
            Result.success(Unit)
        } catch (e: Exception) {
            handleSyncError(e)
        }
    }

    override suspend fun push(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        
        for (change in pendingChanges.toList()) {
            try {
                val collectionPath = "${COLLECTIONS_BASE}/${userId}/${change.entityType}"
                when (change.changeType) {
                    ChangeType.CREATE, ChangeType.UPDATE -> {
                        // For create/update, data should be pushed separately
                        // This is called after local changes are committed
                    }
                    ChangeType.DELETE -> {
                        firestore.document("$collectionPath/${change.entityId}")
                            .delete()
                            .await()
                    }
                }
            } catch (e: Exception) {
                return Result.failure(e)
            }
        }
        return Result.success(Unit)
    }

    override suspend fun pull(): Result<Unit> {
        val userId = auth.currentUser?.uid ?: return Result.failure(Exception("Not signed in"))
        
        try {
            val collections = listOf("accounts", "transactions", "categories", "budgets", "goals", "tags")
            for (collection in collections) {
                val snapshot = firestore.collection("${COLLECTIONS_BASE}/${userId}/${collection}")
                    .get()
                    .await()
                // Process remote data - compare with local and update if needed
                // This is a placeholder for the actual merge logic
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun getLastSyncTime(): Long? {
        return _syncState.value.lastSyncTime
    }

    override suspend fun queueChange(entityType: String, entityId: Long, changeType: ChangeType) {
        pendingChanges.add(PendingChange(entityType, entityId, changeType, System.currentTimeMillis()))
        _syncState.update { it.copy(pendingChangesCount = pendingChanges.size) }
        
        if (_syncState.value.isOnline && _syncState.value.status !is SyncStatus.Syncing) {
            sync()
        }
    }

    override suspend fun clearPendingChanges() {
        pendingChanges.clear()
        _syncState.update { it.copy(pendingChangesCount = 0) }
    }

    private suspend fun handleSyncError(e: Exception): Result<Unit> {
        retryCount++
        return if (retryCount <= maxRetries) {
            delay(calculateBackoff(retryCount))
            sync()
        } else {
            retryCount = 0
            _syncState.update { it.copy(status = SyncStatus.Error(e.message ?: "Sync failed")) }
            Result.failure(e)
        }
    }

    private fun calculateBackoff(retry: Int): Long {
        return (1000L * (1 shl retry)).coerceAtMost(30000L)
    }

    data class PendingChange(
        val entityType: String,
        val entityId: Long,
        val changeType: ChangeType,
        val timestamp: Long
    )

    companion object {
        private const val COLLECTIONS_BASE = "users"
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T? {
        return try {
            suspendCancellableCoroutine { continuation ->
                addOnSuccessListener { result ->
                    continuation.resume(result)
                }
                addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}
