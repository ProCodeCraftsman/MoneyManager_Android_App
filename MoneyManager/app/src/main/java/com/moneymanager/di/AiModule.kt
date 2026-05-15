package com.moneymanager.di

import com.google.mlkit.genai.prompt.Generation
import com.moneymanager.data.ai.DeviceCapabilityManager
import com.moneymanager.data.ai.NanoAiClient
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.ai.GenAiClient
import com.moneymanager.domain.ai.GenerateDraftFromTextUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    @javax.annotation.Nullable
    fun provideGenAiClient(): GenAiClient? {
        return try {
            Generation.getClient()
            NanoAiClient()
        } catch (e: Exception) {
            null
        }
    }

    @Provides
    @Singleton
    fun provideDeviceCapabilityManager(
        preferencesManager: PreferencesManager
    ): DeviceCapabilityManager {
        return DeviceCapabilityManager(preferencesManager)
    }

    @Provides
    @Singleton
    fun provideGenerateDraftFromTextUseCase(
        @javax.annotation.Nullable client: GenAiClient?
    ): GenerateDraftFromTextUseCase {
        return GenerateDraftFromTextUseCase(client)
    }
}
