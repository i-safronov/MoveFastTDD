package com.mobile.finsolve.app.movefasttdd.domain.repository

import com.mobile.finsolve.app.movefasttdd.domain.model.DataResult
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig

interface WorkoutConfigRepository {
    suspend fun save(config: WorkoutConfig): DataResult<Unit>
    suspend fun load(): DataResult<WorkoutConfig?>
}
