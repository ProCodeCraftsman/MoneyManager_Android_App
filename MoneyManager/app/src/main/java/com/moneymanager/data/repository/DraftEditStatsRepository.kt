package com.moneymanager.data.repository

import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.capture.DraftEditStats
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Counts how often drafts are saved as they were captured (AI-014). Kept in preferences rather than the database:
 * it is a diagnostic tally, so it needs no migration and is not part of backups. The mutex stops two saves from
 * overwriting each other's count.
 */
@Singleton
class DraftEditStatsRepository @Inject constructor(private val preferences: PreferencesManager) {

    private val lock = Mutex()

    suspend fun load(): DraftEditStats =
        preferences.getDraftEditStatsJson()
            ?.let { runCatching { JSON.decodeFromString(DraftEditStats.serializer(), it) }.getOrNull() }
            ?: DraftEditStats()

    suspend fun record(sourceType: String, changedFields: List<String>) = lock.withLock {
        val updated = load().record(sourceType, changedFields)
        preferences.setDraftEditStatsJson(JSON.encodeToString(DraftEditStats.serializer(), updated))
    }

    private companion object {
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
