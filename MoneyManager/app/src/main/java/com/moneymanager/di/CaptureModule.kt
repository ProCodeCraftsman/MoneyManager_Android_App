package com.moneymanager.di

import com.moneymanager.data.repository.CaptureRepository
import com.moneymanager.domain.capture.CaptureReview
import com.moneymanager.domain.capture.CaptureStore
import com.moneymanager.domain.learn.SavedHistory
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Flavor-independent capture bindings. The inbox itself is bound per flavor (see CaptureSourceModule). */
@Module
@InstallIn(SingletonComponent::class)
abstract class CaptureModule {

    @Binds
    abstract fun bindCaptureStore(impl: CaptureRepository): CaptureStore

    @Binds
    abstract fun bindCaptureReview(impl: CaptureRepository): CaptureReview

    @Binds
    abstract fun bindSavedHistory(impl: CaptureRepository): SavedHistory
}
