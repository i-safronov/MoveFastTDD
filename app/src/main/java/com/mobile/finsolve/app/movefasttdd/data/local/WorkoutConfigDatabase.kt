package com.mobile.finsolve.app.movefasttdd.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [WorkoutConfigEntity::class], version = 1, exportSchema = false)
abstract class WorkoutConfigDatabase : RoomDatabase() {
    abstract fun workoutConfigDao(): WorkoutConfigDao
}
