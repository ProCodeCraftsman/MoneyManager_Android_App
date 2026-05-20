package com.moneymanager.app

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.moneymanager.app.ui.util.AppLockManager
import com.moneymanager.data.ai.DeviceCapabilityManager
import com.moneymanager.data.http.TransactionApiServer
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MoneyManagerApp : Application(), Configuration.Provider {

    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var deviceCapabilityManager: DeviceCapabilityManager
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var transactionApiServer: TransactionApiServer

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(appLockManager)
        CoroutineScope(Dispatchers.IO).launch {
            deviceCapabilityManager.checkAndCacheAvailability()
        }
        transactionApiServer.startServer()
    }
}
