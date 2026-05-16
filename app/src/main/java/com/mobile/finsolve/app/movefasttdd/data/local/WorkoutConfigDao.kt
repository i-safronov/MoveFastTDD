package com.mobile.finsolve.app.movefasttdd.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WorkoutConfigDao {

    @Query("SELECT * FROM workout_config WHERE id = 0")
    suspend fun load(): WorkoutConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: WorkoutConfigEntity)
}
