package com.moneymanager.app.ui.aimodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.data.ai.DeviceCapabilityManager
import com.moneymanager.data.ai.HuggingFaceAuthManager
import com.moneymanager.data.ai.ModelEntry
import com.moneymanager.data.ai.LiteRtModelManager
import com.moneymanager.data.ai.ModelDownloadService
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AiModelsEvent {
    data class Snackbar(val message: String) : AiModelsEvent
    data object NeedsHfLogin : AiModelsEvent
}

private data class CoreAiState(
    val backendTier: AiBackend,
    val aiStatus: String,
    val aiDownloadProgress: Float,
    val isLocalModelDownloaded: Boolean,
    val localModelDownloadProgress: Float,
    val allModels: List<ModelEntry>,
    val downloadStates: Map<String, Boolean>,
)

@HiltViewModel
class AiModelsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val modelManager: LiteRtModelManager,
    private val deviceCapabilityManager: DeviceCapabilityManager,
    private val hfAuthManager: HuggingFaceAuthManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _downloadingModelName = MutableStateFlow<String?>(null)
    val downloadingModelName: StateFlow<String?> = _downloadingModelName.asStateFlow()

    private val _modelProgressMap = MutableStateFlow<Map<String, ModelDownloadProgress>>(emptyMap())
    val modelProgressMap: StateFlow<Map<String, ModelDownloadProgress>> = _modelProgressMap.asStateFlow()

    private val _events = Channel<AiModelsEvent>(Channel.BUFFERED)
    val events: Flow<AiModelsEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                preferencesManager.localModelDownloadReceived,
                preferencesManager.localModelDownloadTotal,
                preferencesManager.localModelDownloadSpeed,
            ) { rec, tot, spd ->
                val name = _downloadingModelName.value
                if (name != null && tot > 0L) {
                    val remainingMs = if (spd > 0L) ((tot - rec) * 1000L) / spd else 0L
                    ModelDownloadProgress(
                        receivedBytes = rec,
                        totalBytes = tot,
                        bytesPerSecond = spd,
                        remainingMs = remainingMs,
                    )
                } else null
            }.collect { dlProgress ->
                val name = _downloadingModelName.value
                if (name != null && dlProgress != null) {
                    _modelProgressMap.value = _modelProgressMap.value + (name to dlProgress)
                }
            }
        }

        viewModelScope.launch {
            preferencesManager.localModelDownloadProgress.collect { progress ->
                if (progress >= 1f) {
                    _downloadingModelName.value = null
                    _modelProgressMap.value = emptyMap()
                }
            }
        }
    }

    fun checkAiStatus() {
        viewModelScope.launch {
            deviceCapabilityManager.resolveBackendTier()
        }
    }

    val uiState: StateFlow<AiModelsUiState> = combine(
        combine(
            preferencesManager.aiAvailabilityStatus,
            preferencesManager.aiDownloadProgress,
            preferencesManager.aiBackendTier,
            preferencesManager.isLocalModelDownloaded,
            preferencesManager.localModelDownloadProgress,
        ) { aiStatus, aiDownloadProgress, aiBackendTierStr, isLocalModelDownloaded, localModelDownloadProgress ->
            val backendTier = AiBackend.fromId(aiBackendTierStr)
            val allModels = modelManager.getAllowlist()
            val downloadStates = allModels.associate { model ->
                model.name to (modelManager.getModelFile(model).exists() &&
                    modelManager.getModelFile(model).length() > 100_000L)
            }
            CoreAiState(backendTier, aiStatus, aiDownloadProgress, isLocalModelDownloaded,
                localModelDownloadProgress, allModels, downloadStates)
        },
        combine(
            preferencesManager.selectedLocalModel,
            preferencesManager.wifiOnlyDownload,
            preferencesManager.hfAccessToken,
            _downloadingModelName,
            _modelProgressMap,
        ) { a, b, c, d, e -> arrayOf(a, b, c, d, e) },
    ) { core, arr ->
        @Suppress("UNCHECKED_CAST")
        val selectedModelStr = arr[0] as String
        @Suppress("UNCHECKED_CAST")
        val wifiOnly = arr[1] as Boolean
        @Suppress("UNCHECKED_CAST")
        val token = arr[2] as String
        @Suppress("UNCHECKED_CAST")
        val downloadingName = arr[3] as String?
        @Suppress("UNCHECKED_CAST")
        val progressMap = arr[4] as Map<String, ModelDownloadProgress>
        AiModelsUiState(
            backendTier = core.backendTier,
            aiStatus = core.aiStatus,
            aiDownloadProgress = core.aiDownloadProgress,
            allModels = core.allModels,
            selectedLocalModel = modelManager.getModelByName(selectedModelStr),
            isLocalModelDownloaded = core.isLocalModelDownloaded,
            localModelDownloadProgress = core.localModelDownloadProgress,
            downloadingModelName = downloadingName,
            wifiOnlyDownload = wifiOnly,
            hfAccessToken = token,
            isHfTokenValid = token.isNotEmpty(),
            modelDownloadStates = core.downloadStates,
            modelProgress = progressMap,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiModelsUiState()
    )

    fun downloadModel(model: ModelEntry) {
        if (_downloadingModelName.value != null) return
        viewModelScope.launch {
            val token = preferencesManager.getHfAccessTokenSync()
            if (token.isEmpty() && hfAuthManager.needsAuth(model.downloadUrl)) {
                _events.send(AiModelsEvent.NeedsHfLogin)
                return@launch
            }
            _downloadingModelName.value = model.name
            _events.send(AiModelsEvent.Snackbar("Downloading ${model.modelFile}..."))
            ModelDownloadService.start(context, model.name)
        }
    }

    fun downloadModelWithToken(model: ModelEntry, token: String) {
        if (_downloadingModelName.value != null) return
        viewModelScope.launch {
            preferencesManager.setHfAccessToken(token)
            _downloadingModelName.value = model.name
            _events.send(AiModelsEvent.Snackbar("Downloading ${model.modelFile}..."))
            ModelDownloadService.start(context, model.name)
        }
    }

    fun deleteModel(model: ModelEntry) {
        viewModelScope.launch {
            modelManager.deleteModel(model)
            if (_downloadingModelName.value == model.name) {
                _downloadingModelName.value = null
            }
            _modelProgressMap.value = _modelProgressMap.value - model.name
            _events.send(AiModelsEvent.Snackbar("Deleted ${model.modelFile}"))
        }
    }

    fun setSelectedModel(model: ModelEntry) {
        viewModelScope.launch {
            preferencesManager.setSelectedLocalModel(model.name)
            _events.send(AiModelsEvent.Snackbar("Selected ${model.modelFile}"))
        }
    }

    fun setWifiOnlyDownload(value: Boolean) {
        viewModelScope.launch {
            preferencesManager.setWifiOnlyDownload(value)
        }
    }

    fun setHuggingFaceToken(token: String) {
        viewModelScope.launch {
            preferencesManager.setHfAccessToken(token)
            _events.send(AiModelsEvent.Snackbar("HuggingFace token saved"))
        }
    }

    fun clearHuggingFaceToken() {
        viewModelScope.launch {
            preferencesManager.clearHfAccessToken()
            _events.send(AiModelsEvent.Snackbar("HuggingFace token cleared"))
        }
    }

    fun openHuggingFaceLogin() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/settings/tokens"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openHuggingFaceModelAgreement() {
        val model = uiState.value.selectedLocalModel ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${model.modelId}"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun importModel() {
        viewModelScope.launch {
            _events.send(AiModelsEvent.Snackbar("Import from HuggingFace coming soon"))
        }
    }
}
