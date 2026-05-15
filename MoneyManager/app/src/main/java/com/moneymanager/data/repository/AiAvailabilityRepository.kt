package com.moneymanager.data.repository

import com.moneymanager.data.preferences.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiAvailabilityRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    val isAiAvailable: Flow<Boolean> = preferencesManager.aiAvailabilityStatus.map { status ->
        status == "READY"
    }
}
