package com.moneymanager.app

import android.app.Application
import androidx.work.Configuration
import androidx.hilt.work.HiltWorkerFactory
import com.moneymanager.app.ui.util.AppLockManager
import com.moneymanager.data.ai.DeviceCapabilityManager
import com.moneymanager.data.backup.BackupScheduler
import com.moneymanager.data.seed.CategorySeeder
import com.moneymanager.data.worker.EmiPostingScheduler
import com.moneymanager.data.worker.RecurringScheduler
import com.moneymanager.domain.repository.CategoryRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MoneyManagerApp : Application(), Configuration.Provider {

    @Inject lateinit var appLockManager: AppLockManager
    @Inject lateinit var deviceCapabilityManager: DeviceCapabilityManager
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var backupScheduler: BackupScheduler
    @Inject lateinit var recurringScheduler: RecurringScheduler
    @Inject lateinit var emiPostingScheduler: EmiPostingScheduler
    @Inject lateinit var categoryRepository: CategoryRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(appLockManager)
        CoroutineScope(Dispatchers.IO).launch {
            // Previously only seeded when the user opened the Categories screen, so any
            // other screen (e.g. Add Transaction) could be reached first with zero categories.
            if (categoryRepository.getAllCategoriesWithArchived().first().isEmpty()) {
                CategorySeeder.seed(categoryRepository)
            }
            deviceCapabilityManager.checkAndCacheAvailability()
            backupScheduler.initialize()
            recurringScheduler.schedule()
            emiPostingScheduler.schedule()
        }
    }
}
