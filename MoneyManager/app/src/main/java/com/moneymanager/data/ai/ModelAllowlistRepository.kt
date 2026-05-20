package com.moneymanager.data.ai

import android.content.Context
import android.util.Log
import com.moneymanager.data.preferences.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class AllowlistValidationResult {
    data class Valid(
        val models: List<AllowedModel>,
        val newCount: Int,
        val overrideCount: Int,
    ) : AllowlistValidationResult()

    data class Error(val message: String) : AllowlistValidationResult()
}

@Singleton
class ModelAllowlistRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
) {
    companion object {
        private const val TAG = "AllowlistRepo"
        private const val ASSET_FILE = "model_allowlist.json"
    }

    fun loadBundled(): List<AllowedModel> {
        return try {
            val json = context.assets.open(ASSET_FILE).bufferedReader().readText()
            parseAllowlistJson(json).filter { it.disabled != true }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load bundled allowlist", e)
            emptyList()
        }
    }

    private fun mergeByName(
        base: List<AllowedModel>,
        override: List<AllowedModel>,
    ): List<AllowedModel> {
        val result = base.toMutableList()
        for (model in override) {
            val idx = result.indexOfFirst { it.name == model.name }
            if (idx >= 0) result[idx] = model else result.add(model)
        }
        return result
    }

    suspend fun getEffectiveAllowlist(): List<AllowedModel> {
        val bundled = loadBundled()
        val userJson = preferencesManager.getUserAllowlistJson()
        if (userJson.isBlank()) return bundled
        return try {
            val user = parseAllowlistJson(userJson).filter { it.disabled != true }
            if (user.isEmpty()) bundled else mergeByName(bundled, user)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user allowlist, falling back to bundled", e)
            bundled
        }
    }

    suspend fun saveUserJson(json: String) {
        preferencesManager.setUserAllowlistJson(json)
    }

    suspend fun resetToOriginal() {
        preferencesManager.setUserAllowlistJson("")
    }

    fun validateUserJson(jsonStr: String): AllowlistValidationResult {
        if (jsonStr.isBlank()) return AllowlistValidationResult.Error("JSON is empty")
        return try {
            val models = parseAllowlistJson(jsonStr)
            if (models.isEmpty()) return AllowlistValidationResult.Error("No models found in JSON")
            val invalid = models.filter {
                it.name.isBlank() || it.modelId.isBlank() ||
                    it.modelFile.isBlank() || it.commitHash.isBlank()
            }
            if (invalid.isNotEmpty()) {
                return AllowlistValidationResult.Error(
                    "${invalid.size} model(s) missing required fields: name, modelId, modelFile, commitHash"
                )
            }
            val bundledNames = loadBundled().map { it.name }.toSet()
            val newCount = models.count { it.name !in bundledNames }
            val overrideCount = models.count { it.name in bundledNames }
            AllowlistValidationResult.Valid(models, newCount, overrideCount)
        } catch (e: Exception) {
            AllowlistValidationResult.Error("Parse error: ${e.message}")
        }
    }
}
