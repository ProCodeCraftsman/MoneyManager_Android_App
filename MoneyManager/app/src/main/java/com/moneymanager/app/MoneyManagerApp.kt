package com.moneymanager.app

import android.app.Application
import com.moneymanager.app.ui.util.AppLockManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MoneyManagerApp : Application() {
    @Inject
    lateinit var appLockManager: AppLockManager

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(appLockManager)
    }
}
