package com.mobile.finsolve.app.movefasttdd.domain.usecase

import com.mobile.finsolve.app.movefasttdd.domain.model.DataResult
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository

class FakeWorkoutConfigRepository : WorkoutConfigRepository {

    var savedConfig: WorkoutConfig? = null
    var configToReturn: WorkoutConfig? = null
    var saveCallCount: Int = 0
    var loadCallCount: Int = 0
    var shouldFailOnSave: Boolean = false
    var shouldFailOnLoad: Boolean = false

    override suspend fun save(config: WorkoutConfig): DataResult<Unit> {
        if (shouldFailOnSave) return DataResult.Error
        saveCallCount++
        savedConfig = config
        return DataResult.Success(Unit)
    }

    override suspend fun load(): DataResult<WorkoutConfig?> {
        if (shouldFailOnLoad) return DataResult.Error
        loadCallCount++
        return DataResult.Success(configToReturn)
    }
}
