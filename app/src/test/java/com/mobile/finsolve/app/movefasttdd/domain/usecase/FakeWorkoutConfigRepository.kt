package com.mobile.finsolve.app.movefasttdd.domain.usecase

import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository

class FakeWorkoutConfigRepository : WorkoutConfigRepository {

    var savedConfig: WorkoutConfig? = null
    var configToReturn: WorkoutConfig? = null
    var saveCallCount: Int = 0
    var loadCallCount: Int = 0

    override suspend fun save(config: WorkoutConfig) {
        saveCallCount++
        savedConfig = config
    }

    override suspend fun load(): WorkoutConfig? {
        loadCallCount++
        return configToReturn
    }
}
