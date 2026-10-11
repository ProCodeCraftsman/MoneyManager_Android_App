package com.moneymanager.di

import com.moneymanager.capture.AndroidSmsInbox
import com.moneymanager.domain.capture.SmsInbox
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureSourceModule {

    @Binds
    abstract fun bindSmsInbox(impl: AndroidSmsInbox): SmsInbox
}
