package com.moneymanager.di

import com.moneymanager.capture.UnavailableSmsInbox
import com.moneymanager.domain.capture.SmsInbox
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object CaptureSourceModule {

    @Provides
    fun provideSmsInbox(): SmsInbox = UnavailableSmsInbox
}
