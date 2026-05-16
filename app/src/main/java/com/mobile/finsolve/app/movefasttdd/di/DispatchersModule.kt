package com.mobile.finsolve.app.movefasttdd.di

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {

    @Provides
    @Singleton
    fun provideDispatchersList(): DispatchersList = DispatchersList.Base()
}
