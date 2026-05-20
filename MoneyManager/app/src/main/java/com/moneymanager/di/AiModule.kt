package com.moneymanager.di

import com.moneymanager.data.ai.AiClientRouter
import com.moneymanager.data.ai.DownloadRepository
import com.moneymanager.data.ai.DownloadRepositoryImpl
import com.moneymanager.data.ai.EdgeAiClient
import com.moneymanager.data.ai.NanoAiClient
import com.moneymanager.data.preferences.PreferencesManager
import com.moneymanager.domain.ai.GenAiClient
import com.moneymanager.domain.ai.GenerateDraftFromImageUseCase
import com.moneymanager.domain.ai.GenerateDraftFromTextUseCase
import com.moneymanager.domain.repository.AiConversationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository = impl

    @Provides
    @Singleton
    fun provideGenAiClient(router: AiClientRouter): GenAiClient {
        return router
    }

    @Provides
    @Singleton
    @Named("preferredClient")
    @androidx.annotation.Nullable
    fun providePreferredGenAiClient(
        preferencesManager: PreferencesManager,
        nanoAiClient: NanoAiClient,
        edgeAiClient: EdgeAiClient,
    ): GenAiClient? {
        val tier = runBlocking { preferencesManager.aiBackendTier.first() }
        return when (tier) {
            "aicore" -> nanoAiClient
            "local_model" -> {
                val downloaded = runBlocking { preferencesManager.isLocalModelDownloaded.first() }
                if (downloaded) edgeAiClient else null
            }
            else -> null
        }
    }

    @Provides
    @Singleton
    fun provideGenerateDraftFromTextUseCase(
        @Named("preferredClient") @androidx.annotation.Nullable client: GenAiClient?,
        conversationRepository: AiConversationRepository,
    ): GenerateDraftFromTextUseCase {
        return GenerateDraftFromTextUseCase(client, conversationRepository)
    }

    @Provides
    @Singleton
    fun provideGenerateDraftFromImageUseCase(
        @Named("preferredClient") @androidx.annotation.Nullable client: GenAiClient?,
        conversationRepository: AiConversationRepository,
    ): GenerateDraftFromImageUseCase {
        return GenerateDraftFromImageUseCase(client, conversationRepository)
    }
}
