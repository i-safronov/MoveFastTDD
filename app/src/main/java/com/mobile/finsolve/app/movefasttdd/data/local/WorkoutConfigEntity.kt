package com.mobile.finsolve.app.movefasttdd.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_config")
data class WorkoutConfigEntity(
    @PrimaryKey val id: Int = 0,
    val reps: Int,
    val repDuration: Int,
    val restDuration: Int,
)
