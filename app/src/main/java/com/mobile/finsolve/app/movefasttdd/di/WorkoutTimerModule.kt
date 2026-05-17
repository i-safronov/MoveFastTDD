package com.mobile.finsolve.app.movefasttdd.di

import com.mobile.finsolve.app.movefasttdd.data.repository.WorkoutTimerRepositoryImpl
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutTimerRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WorkoutTimerModule {

    @Binds
    @Singleton
    abstract fun bindWorkoutTimerRepository(
        impl: WorkoutTimerRepositoryImpl,
    ): WorkoutTimerRepository
}
