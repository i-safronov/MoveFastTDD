package com.mobile.finsolve.app.movefasttdd.domain.repository

import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig

interface WorkoutConfigRepository {
    suspend fun save(config: WorkoutConfig)
    suspend fun load(): WorkoutConfig?

}