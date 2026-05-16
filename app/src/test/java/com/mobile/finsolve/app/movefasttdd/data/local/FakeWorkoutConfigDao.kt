package com.mobile.finsolve.app.movefasttdd.data.local

class FakeWorkoutConfigDao : WorkoutConfigDao {

    private var stored: WorkoutConfigEntity? = null
    var saveCallCount: Int = 0

    override suspend fun load(): WorkoutConfigEntity? = stored

    override suspend fun save(entity: WorkoutConfigEntity) {
        stored = entity
        saveCallCount++
    }
}
