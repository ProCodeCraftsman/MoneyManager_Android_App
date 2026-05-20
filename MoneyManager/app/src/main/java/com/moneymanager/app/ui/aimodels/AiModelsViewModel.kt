package com.moneymanager.app.ui.aimodels

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.moneymanager.domain.ai.AiBackend
import com.moneymanager.data.ai.DeviceCapabilityManager
import com.moneymanager.data.ai.DownloadRepository
import com.moneymanager.data.ai.DownloadWorker
import com.moneymanager.data.ai.HuggingFaceAuthManager
import com.moneymanager.data.ai.LiteRtModelManager
import com.moneymanager.data.ai.ModelAllowlistRepository
import com.moneymanager.data.ai.ModelEntry
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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

@HiltViewModel
class AiModelsViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val modelManager: LiteRtModelManager,
    private val deviceCapabilityManager: DeviceCapabilityManager,
    private val hfAuthManager: HuggingFaceAuthManager,
    private val downloadRepository: DownloadRepository,
    private val allowlistRepository: ModelAllowlistRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _events = Channel<AiModelsEvent>(Channel.BUFFERED)
    val events: Flow<AiModelsEvent> = _events.receiveAsFlow()

    /** Model waiting for HF token before download can proceed. */
    private val _pendingModel = MutableStateFlow<ModelEntry?>(null)

    /** Loaded model list — refreshed on allowlist change. */
    private val _models = MutableStateFlow<List<ModelEntry>>(emptyList())

    init {
        loadModels()
        // Re-load whenever user allowlist JSON changes
        viewModelScope.launch {
            preferencesManager.userAllowlistJsonFlow.collect { loadModels() }
        }
        // Watch for auth-failure WorkInfo completions
        viewModelScope.launch {
            downloadRepository.observeAllDownloads().collect { infos ->
                infos.filter { it.state == WorkInfo.State.FAILED }.forEach { info ->
                    val err = info.outputData.getString(DownloadWorker.OUTPUT_ERROR)
                    if (err == DownloadWorker.ERROR_AUTH) {
                        val modelName = info.tags
                            .firstOrNull { it.startsWith(DownloadWorker.TAG_PREFIX) }
                            ?.removePrefix(DownloadWorker.TAG_PREFIX) ?: return@forEach
                        val model = _models.value.firstOrNull { it.name == modelName }
                        if (model != null) {
                            _pendingModel.value = model
                            _events.send(AiModelsEvent.NeedsHfLogin)
                        }
                    }
                }
            }
        }
    }

    private fun loadModels() {
        viewModelScope.launch {
            _models.value = allowlistRepository.getEffectiveAllowlist().map { it.toModelEntry() }
        }
    }

    fun checkAiStatus() {
        viewModelScope.launch { deviceCapabilityManager.resolveBackendTier() }
    }

    val uiState: StateFlow<AiModelsUiState> = combine<Any?, AiModelsUiState>(
        preferencesManager.aiAvailabilityStatus,
        preferencesManager.aiDownloadProgress,
        preferencesManager.aiBackendTier,
        preferencesManager.isLocalModelDownloaded,
        preferencesManager.localModelDownloadProgress,
        preferencesManager.selectedLocalModel,
        preferencesManager.wifiOnlyDownload,
        preferencesManager.hfAccessToken,
        _models,
        downloadRepository.observeAllDownloads(),
    ) { args ->
        val aiStatus = args[0] as String
        val aiDlProg = args[1] as Float
        val tierStr = args[2] as String
        val isLmDownloaded = args[3] as Boolean
        val lmDlProg = args[4] as Float
        val selectedStr = args[5] as String
        val wifiOnly = args[6] as Boolean
        val token = args[7] as String
        @Suppress("UNCHECKED_CAST")
        val allModels = args[8] as List<ModelEntry>
        @Suppress("UNCHECKED_CAST")
        val workInfos = args[9] as List<WorkInfo>

        val downloadingNames = mutableSetOf<String>()
        val progressMap = mutableMapOf<String, ModelDownloadProgress>()

        workInfos.forEach { info ->
            val tag = info.tags.firstOrNull { it.startsWith(DownloadWorker.TAG_PREFIX) } ?: return@forEach
            val name = tag.removePrefix(DownloadWorker.TAG_PREFIX)
            if (info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED) {
                downloadingNames.add(name)
            }
            if (info.state == WorkInfo.State.RUNNING) {
                val rec = info.progress.getLong(DownloadWorker.PROGRESS_RECEIVED, 0L)
                val tot = info.progress.getLong(DownloadWorker.PROGRESS_TOTAL, 0L)
                val spd = info.progress.getLong(DownloadWorker.PROGRESS_SPEED, 0L)
                val rem = info.progress.getLong(DownloadWorker.PROGRESS_REMAINING_MS, 0L)
                if (tot > 0L) progressMap[name] = ModelDownloadProgress(rec, tot, spd, rem)
            }
        }

        val downloadStates = allModels.associate { it.name to modelManager.isModelDownloaded(it) }
        val updatable = allModels
            .filter { !downloadStates[it.name]!! && modelManager.isModelUpdatable(it) }
            .map { it.name }.toSet()

        val activeProg = progressMap.values.firstOrNull()?.progress ?: lmDlProg

        AiModelsUiState(
            backendTier = AiBackend.fromId(tierStr),
            aiStatus = aiStatus,
            aiDownloadProgress = aiDlProg,
            allModels = allModels,
            selectedLocalModel = allModels.firstOrNull { it.name == selectedStr },
            isLocalModelDownloaded = isLmDownloaded,
            localModelDownloadProgress = if (downloadingNames.isNotEmpty()) activeProg else lmDlProg,
            downloadingModelNames = downloadingNames,
            wifiOnlyDownload = wifiOnly,
            hfAccessToken = token,
            isHfTokenValid = token.isNotEmpty(),
            modelDownloadStates = downloadStates,
            modelProgress = progressMap,
            updatableModelNames = updatable,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AiModelsUiState(),
    )

    fun downloadModel(model: ModelEntry) {
        viewModelScope.launch {
            val token = preferencesManager.getHfAccessTokenSync()
            if (token.isEmpty() && withContext(Dispatchers.IO) { hfAuthManager.needsAuth(model.downloadUrl) }) {
                _pendingModel.value = model
                _events.send(AiModelsEvent.NeedsHfLogin)
                return@launch
            }
            downloadRepository.downloadModel(model)
            _events.send(AiModelsEvent.Snackbar("Downloading ${model.name}…"))
        }
    }

    fun downloadModelWithToken(token: String) {
        val model = _pendingModel.value ?: return
        viewModelScope.launch {
            val valid = withContext(Dispatchers.IO) {
                hfAuthManager.validateToken(model.downloadUrl, token)
            }
            if (!valid) {
                _events.send(AiModelsEvent.Snackbar("Token rejected (401/403) — check the token has access to this model"))
                return@launch
            }
            preferencesManager.setHfAccessToken(token)
            downloadRepository.downloadModel(model)
            _pendingModel.value = null
            _events.send(AiModelsEvent.Snackbar("Downloading ${model.name}…"))
        }
    }

    fun cancelDownload(model: ModelEntry) {
        downloadRepository.cancelDownload(model.name)
        AiModelsEvent.Snackbar("Download cancelled").send()
    }

    private fun AiModelsEvent.send() = viewModelScope.launch { _events.send(this@send) }

    fun deleteModel(model: ModelEntry) {
        viewModelScope.launch {
            downloadRepository.cancelDownload(model.name)
            modelManager.deleteModel(model)
            _events.send(AiModelsEvent.Snackbar("Deleted ${model.name}"))
        }
    }

    fun setSelectedModel(model: ModelEntry) {
        viewModelScope.launch {
            preferencesManager.setSelectedLocalModel(model.name)
            _events.send(AiModelsEvent.Snackbar("Active model: ${model.name}"))
        }
    }

    fun setWifiOnlyDownload(value: Boolean) {
        viewModelScope.launch { preferencesManager.setWifiOnlyDownload(value) }
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
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openHuggingFaceModelPage(model: ModelEntry) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/${model.modelId}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // Keep legacy name used by existing AiModelsScreen dialog
    fun openHuggingFaceModelAgreement() {
        val model = uiState.value.selectedLocalModel ?: return
        openHuggingFaceModelPage(model)
    }

    fun importModel() {
        viewModelScope.launch { _events.send(AiModelsEvent.Snackbar("Use Manage Allowlist to add custom models")) }
    }
}
