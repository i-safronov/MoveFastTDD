package com.mobile.finsolve.app.movefasttdd.di

import com.mobile.finsolve.app.movefasttdd.data.repository.WorkoutConfigRepositoryImpl
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWorkoutConfigRepository(
        impl: WorkoutConfigRepositoryImpl,
    ): WorkoutConfigRepository
}
