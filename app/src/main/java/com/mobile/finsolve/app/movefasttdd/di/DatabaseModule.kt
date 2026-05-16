package com.mobile.finsolve.app.movefasttdd.di

import android.content.Context
import androidx.room.Room
import com.mobile.finsolve.app.movefasttdd.data.local.WorkoutConfigDao
import com.mobile.finsolve.app.movefasttdd.data.local.WorkoutConfigDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): WorkoutConfigDatabase =
        Room.databaseBuilder(
            context,
            WorkoutConfigDatabase::class.java,
            "workout_config.db",
        ).build()

    @Provides
    fun provideWorkoutConfigDao(db: WorkoutConfigDatabase): WorkoutConfigDao =
        db.workoutConfigDao()
}
