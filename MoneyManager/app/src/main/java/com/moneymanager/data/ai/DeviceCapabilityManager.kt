package com.moneymanager.data.ai

import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.moneymanager.data.preferences.PreferencesManager

class DeviceCapabilityManager(
    private val preferencesManager: PreferencesManager
) {
    suspend fun checkAndCacheAvailability() {
        try {
            val client = Generation.getClient()
            val status = client.checkStatus()
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
