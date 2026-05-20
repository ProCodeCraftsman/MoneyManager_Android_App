package com.moneymanager.data.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AllowlistFile(
    val models: List<AllowedModel> = emptyList(),
)

@Serializable
data class AllowedModel(
    val name: String,
    val modelId: String,
    val modelFile: String,
    val commitHash: String,
    val description: String = "",
    val sizeInBytes: Long = 0L,
    val defaultConfig: AllowedModelConfig = AllowedModelConfig(),
    val taskTypes: List<String> = emptyList(),
    val minDeviceMemoryInGb: Int? = null,
    val llmSupportImage: Boolean? = null,
    val llmSupportAudio: Boolean? = null,
    val capabilities: List<String>? = null,
    val capabilityToTaskTypes: Map<String, List<String>>? = null,
    val bestForTaskTypes: List<String>? = null,
    val updatableModelFiles: List<UpdatableFile>? = null,
    val updateInfo: String? = null,
    val disabled: Boolean? = null,
    val url: String? = null,
    val runtimeType: String? = null,
) {
    fun toModelEntry(): ModelEntry = ModelEntry(
        name = name,
        modelId = modelId,
        modelFile = modelFile,
        commitHash = commitHash,
        sizeBytes = sizeInBytes,
        minRamGb = minDeviceMemoryInGb ?: 6,
        description = description,
        defaultConfig = ModelConfig(
            topK = defaultConfig.topK ?: 64,
            topP = defaultConfig.topP ?: 0.95f,
            temperature = defaultConfig.temperature ?: 1.0f,
            maxTokens = defaultConfig.maxTokens ?: 1024,
            maxContextLength = defaultConfig.maxContextLength,
            accelerators = defaultConfig.accelerators ?: "gpu,cpu",
            visionAccelerator = defaultConfig.visionAccelerator ?: "gpu",
        ),
        taskTypes = taskTypes,
        capabilities = capabilities ?: emptyList(),
        bestForTaskTypes = bestForTaskTypes ?: emptyList(),
        llmSupportImage = llmSupportImage ?: false,
        llmSupportAudio = llmSupportAudio ?: false,
        updatableModelFiles = updatableModelFiles?.map {
            UpdatableModelFile(it.fileName, it.commitHash)
        } ?: emptyList(),
        updateInfo = updateInfo ?: "",
        customUrl = url,
    )
}

@Serializable
data class AllowedModelConfig(
    val topK: Int? = null,
    val topP: Float? = null,
    val temperature: Float? = null,
    val accelerators: String? = null,
    val visionAccelerator: String? = null,
    val maxContextLength: Int? = null,
    val maxTokens: Int? = null,
)

@Serializable
data class UpdatableFile(
    val fileName: String,
    val commitHash: String,
)

internal val allowlistJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

fun parseAllowlistJson(json: String): List<AllowedModel> {
    val trimmed = json.trim()
    return when {
        trimmed.startsWith("[") ->
            allowlistJson.decodeFromString<List<AllowedModel>>(trimmed)
        trimmed.startsWith("{") ->
            try {
                allowlistJson.decodeFromString<AllowlistFile>(trimmed).models
            } catch (_: Exception) {
                listOf(allowlistJson.decodeFromString<AllowedModel>(trimmed))
            }
        else -> emptyList()
    }
}
