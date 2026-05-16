package com.mobile.finsolve.app.movefasttdd.di

import com.mobile.finsolve.app.movefasttdd.data.datastore.WorkoutDraftDataStore
import com.mobile.finsolve.app.movefasttdd.data.datastore.WorkoutDraftDataStoreImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataStoreModule {

    @Binds
    @Singleton
    abstract fun bindWorkoutDraftDataStore(
        impl: WorkoutDraftDataStoreImpl,
    ): WorkoutDraftDataStore
}
