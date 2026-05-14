package com.moneymanager.app.ui.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.moneymanager.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class AppLockState {
    data object Loading : AppLockState()
    data object Locked : AppLockState()
    data object Unlocked : AppLockState()
}

@Singleton
class AppLockManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Application.ActivityLifecycleCallbacks {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _lockState = MutableStateFlow<AppLockState>(AppLockState.Loading)
    val lockState: StateFlow<AppLockState> = _lockState.asStateFlow()

    private var isAppInForeground: Boolean = false
    private var activityCount: Int = 0
    private var authenticatedInSession: Boolean = false
    private var checkJob: Job? = null

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (!isAppInForeground) {
            isAppInForeground = true
            checkLockStatus()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            isAppInForeground = false
        }
    }

    private fun checkLockStatus() {
        checkJob?.cancel()
        checkJob = scope.launch {
            val pinEnabled = preferencesManager.pinEnabled.first()
            val biometricEnabled = preferencesManager.biometricEnabled.first()
            if (!pinEnabled && !biometricEnabled) {
                _lockState.value = AppLockState.Unlocked
                return@launch
            }

            if (pinEnabled) {
                val pinHash = preferencesManager.pinHash.first()
                if (pinHash.isNullOrEmpty()) {
                    _lockState.value = AppLockState.Unlocked
                    return@launch
                }
            }

            if (!authenticatedInSession) {
                _lockState.value = AppLockState.Locked
                return@launch
            }

            val autoLockMinutes = preferencesManager.autoLockMinutes.first()
            if (autoLockMinutes <= 0) {
                authenticatedInSession = false
                _lockState.value = AppLockState.Locked
                return@launch
            }

            val lastUnlock = preferencesManager.lastUnlockTime.first()
            if (lastUnlock != null) {
                val elapsedMinutes = (System.currentTimeMillis() - lastUnlock) / 60000
                if (elapsedMinutes >= autoLockMinutes) {
                    authenticatedInSession = false
                    _lockState.value = AppLockState.Locked
                    return@launch
                }
            }

            _lockState.value = AppLockState.Unlocked
        }
    }

    fun unlock() {
        authenticatedInSession = true
        scope.launch {
            preferencesManager.setLastUnlockTime(System.currentTimeMillis())
        }
        _lockState.value = AppLockState.Unlocked
    }

    fun lockNow() {
        authenticatedInSession = false
        _lockState.value = AppLockState.Locked
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
