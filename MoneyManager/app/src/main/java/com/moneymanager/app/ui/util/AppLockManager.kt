package com.moneymanager.app.ui.util

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import com.moneymanager.data.preferences.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLockManager @Inject constructor(
    private val preferencesManager: PreferencesManager
) : Application.ActivityLifecycleCallbacks {

    private var lastForegroundTime: Long = System.currentTimeMillis()
    private var isAppInForeground: Boolean = false
    private var activityCount: Int = 0

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (!isAppInForeground) {
            isAppInForeground = true
            checkLockStatus(activity)
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            isAppInForeground = false
            lastForegroundTime = System.currentTimeMillis()
        }
    }

    private fun checkLockStatus(activity: Activity) {
        // Don't lock if we are already on the lock screen
        if (activity::class.java.name.contains("LockActivity") || 
            activity::class.java.name.contains("PinLockActivity")) {
            return
        }

        CoroutineScope(Dispatchers.Main).launch {
            val pinEnabled = preferencesManager.pinEnabled.first()
            if (pinEnabled) {
                val autoLockMinutes = preferencesManager.autoLockMinutes.first()
                if (autoLockMinutes > 0) {
                    val timeInBackground = System.currentTimeMillis() - lastForegroundTime
                    if (timeInBackground >= autoLockMinutes * 60 * 1000L) {
                        navigateToLockScreen(activity)
                    }
                } else if (autoLockMinutes == 0 && lastForegroundTime != 0L) {
                    // Lock immediately if autoLockMinutes is not set or app was in background
                     navigateToLockScreen(activity)
                }
            }
        }
    }

    private fun navigateToLockScreen(activity: Activity) {
        // This is a placeholder for actual navigation logic.
        // In a real app, you'd start the LockActivity or update a state.
        // Since we are using Compose and NavHost, we might need a different approach
        // or a dedicated LockActivity that sits on top.
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
