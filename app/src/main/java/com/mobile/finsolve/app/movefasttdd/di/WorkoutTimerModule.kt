package com.mobile.finsolve.app.movefasttdd.di

import android.content.Context
import com.mobile.finsolve.app.movefasttdd.data.repository.WorkoutTimerRepositoryImpl
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutTimerRepository
import com.mobile.finsolve.app.movefasttdd.service.WorkoutForegroundService
import com.mobile.finsolve.app.movefasttdd.service.WorkoutServiceStarter
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    companion object {
        @Provides
        fun provideWorkoutServiceStarter(
            @ApplicationContext context: Context,
        ): WorkoutServiceStarter = WorkoutServiceStarter {
            context.startForegroundService(WorkoutForegroundService.intent(context))
        }
    }
}
