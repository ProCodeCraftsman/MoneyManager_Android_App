package com.moneymanager.data.ai

import android.content.Context
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.PromptClient
import com.moneymanager.data.preferences.PreferencesManager

class DeviceCapabilityManager(
    private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    suspend fun checkAndCacheAvailability() {
        try {
            val client = com.google.mlkit.genai.common.PromptClient.create(context)
            val status = client.checkFeatureStatus()
            when (status) {
                FeatureStatus.AVAILABLE -> preferencesManager.setAiAvailabilityStatus("READY")
                FeatureStatus.UNAVAILABLE -> preferencesManager.setAiAvailabilityStatus("NEVER")
                FeatureStatus.DOWNLOADING -> preferencesManager.setAiAvailabilityStatus("PENDING")
                else -> preferencesManager.setAiAvailabilityStatus("PENDING")
            }
        } catch (e: Exception) {
            preferencesManager.setAiAvailabilityStatus("NEVER")
        }
    }
}
