package com.moneymanager.di

import com.moneymanager.data.ai.AiClientRouter
import com.moneymanager.data.ai.DownloadRepository
import com.moneymanager.data.ai.DownloadRepositoryImpl
import com.moneymanager.domain.ai.GenAiClient
import com.moneymanager.domain.ai.GenerateDraftFromImageUseCase
import com.moneymanager.domain.ai.GenerateDraftFromTextUseCase
import com.moneymanager.domain.repository.AiConversationRepository
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
    fun provideDownloadRepository(impl: DownloadRepositoryImpl): DownloadRepository = impl

    @Provides
    @Singleton
    fun provideGenAiClient(router: AiClientRouter): GenAiClient {
        return router
    }

    @Provides
    @Singleton
    fun provideGenerateDraftFromTextUseCase(
        client: GenAiClient,
        conversationRepository: AiConversationRepository,
    ): GenerateDraftFromTextUseCase {
        return GenerateDraftFromTextUseCase(client, conversationRepository)
    }

    @Provides
    @Singleton
    fun provideGenerateDraftFromImageUseCase(
        client: GenAiClient,
        conversationRepository: AiConversationRepository,
    ): GenerateDraftFromImageUseCase {
        return GenerateDraftFromImageUseCase(client, conversationRepository)
    }
}
