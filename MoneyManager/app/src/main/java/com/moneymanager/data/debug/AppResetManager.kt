package com.moneymanager.data.debug

import android.content.Context
import com.moneymanager.data.MoneyManagerDatabase
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.data.seed.CategorySeeder
import com.moneymanager.domain.repository.CategoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEBUG-ONLY UTILITY: This file is intended for mobile testing phases only.
 * It provides a "Hard Reset" that clears all transaction data, accounts, and settings,
 * effectively resetting the app to a fresh-install state.
 * 
 * CRITICAL: It DOES NOT delete downloaded AI models to avoid large re-downloads.
 * 
 * TO REMOVE THIS FEATURE:
 * 1. Delete this file (AppResetManager.kt).
 * 2. Remove [PreferencesManager.resetExceptAi].
 * 3. Remove the "DEBUG: Hard Reset" button in [SettingsScreen.kt].
 * 4. Remove [SettingsViewModel.hardResetApp].
 */
@Singleton
class AppResetManager @Inject constructor(
    private val database: MoneyManagerDatabase,
    private val preferencesManager: PreferencesManager,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context
) {
    suspend fun hardReset() {
        // 1. Clear all Room database tables
        database.clearAllTables()

        // 2. Clear preferences while preserving AI model metadata
        preferencesManager.resetExceptAi()

        // 3. Delete transaction receipt images
        val receiptsDir = File(context.filesDir, "receipts")
        if (receiptsDir.exists()) {
            receiptsDir.deleteRecursively()
        }

        // 4. Re-seed default categories (optional, but makes "fresh install" usable)
        CategorySeeder.seed(categoryRepository)
    }
}
