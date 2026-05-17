package com.moneymanager.data.repository

import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

data class AiState(
    val tier: AiBackend = AiBackend.NONE,
    val isLocalModelDownloaded: Boolean = false,
    val localModelDownloadProgress: Float = 0f,
    val isAvailable: Boolean = false,
)

@Singleton
class AiAvailabilityRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    val isAiAvailable: Flow<Boolean> = combine(
        preferencesManager.aiBackendTier,
        preferencesManager.isLocalModelDownloaded,
    ) { tierStr, downloaded ->
        val tier = AiBackend.fromId(tierStr)
        tier == AiBackend.AICORE || (tier == AiBackend.LOCAL_MODEL && downloaded)
    }

    val aiState: Flow<AiState> = combine(
        preferencesManager.aiBackendTier,
        preferencesManager.isLocalModelDownloaded,
        preferencesManager.localModelDownloadProgress,
    ) { tierStr, downloaded, progress ->
        val backendTier = AiBackend.fromId(tierStr)
        AiState(
            tier = backendTier,
            isLocalModelDownloaded = downloaded,
            localModelDownloadProgress = progress,
            isAvailable = backendTier == AiBackend.AICORE ||
                (backendTier == AiBackend.LOCAL_MODEL && downloaded),
        )
    }

    val isLocalModelDownloaded: Flow<Boolean> = preferencesManager.isLocalModelDownloaded
    val localModelDownloadProgress: Flow<Float> = preferencesManager.localModelDownloadProgress
    val aiBackendTier: Flow<AiBackend> = preferencesManager.aiBackendTier.map {
        AiBackend.fromId(it)
    }
}
