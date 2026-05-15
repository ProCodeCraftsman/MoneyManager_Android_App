package com.moneymanager.app

import android.app.Application
import com.moneymanager.app.ui.util.AppLockManager
import com.moneymanager.data.ai.DeviceCapabilityManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MoneyManagerApp : Application() {
    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var deviceCapabilityManager: DeviceCapabilityManager

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(appLockManager)
        CoroutineScope(Dispatchers.IO).launch {
            deviceCapabilityManager.checkAndCacheAvailability()
        }
    }
}
