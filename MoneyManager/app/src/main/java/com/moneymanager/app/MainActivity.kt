package com.moneymanager.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.moneymanager.data.worker.RecurringGenerationWorker
import com.moneymanager.app.ui.MoneyManagerNavHost
import com.moneymanager.app.ui.theme.MoneyManagerTheme
import com.moneymanager.app.ui.theme.rememberThemePreferences
import com.moneymanager.data.ai.EdgeAiClient
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.app.ui.util.AppLockManager
import com.moneymanager.data.security.BiometricAuthManager
import com.moneymanager.data.security.SecurityManager
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var biometricAuthManager: BiometricAuthManager

    @Inject
    lateinit var appLockManager: AppLockManager

    @Inject
    lateinit var edgeAiClient: EdgeAiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Trigger recurring transaction generation on app start
        val workRequest = OneTimeWorkRequestBuilder<RecurringGenerationWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "RecurringGeneration",
            ExistingWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            val themeState = rememberThemePreferences()
            MoneyManagerTheme(
                appTheme = themeState.theme,
                isDarkMode = themeState.isDarkMode
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneyManagerNavHost(
                        preferencesManager = preferencesManager,
                        securityManager = securityManager,
                        biometricAuthManager = biometricAuthManager,
                        appLockManager = appLockManager
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        lifecycleScope.launch {
            edgeAiClient.close()
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL) {
            lifecycleScope.launch {
                edgeAiClient.close()
            }
        }
    }
}
