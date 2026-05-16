package com.mobile.finsolve.app.movefasttdd.data.repository

import com.mobile.finsolve.app.movefasttdd.data.local.FakeWorkoutConfigDao
import com.mobile.finsolve.app.movefasttdd.data.local.WorkoutConfigEntity
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class WorkoutConfigRepositoryImplTest {

    private lateinit var dao: FakeWorkoutConfigDao
    private lateinit var repository: WorkoutConfigRepositoryImpl

    @Before
    fun setup() {
        dao = FakeWorkoutConfigDao()
        repository = WorkoutConfigRepositoryImpl(dao)
    }

    // region load

    @Test
    fun `load returns null when dao returns null`() = runTest {
        assertNull(repository.load())
    }

    @Test
    fun `load maps entity reps to domain model`() = runTest {
        dao.save(WorkoutConfigEntity(reps = 7, repDuration = 30, restDuration = 10))
        assertEquals(7, repository.load()?.reps)
    }

    @Test
    fun `load maps entity repDuration to domain model`() = runTest {
        dao.save(WorkoutConfigEntity(reps = 3, repDuration = 45, restDuration = 10))
        assertEquals(45, repository.load()?.repDuration)
    }

    @Test
    fun `load maps entity restDuration to domain model`() = runTest {
        dao.save(WorkoutConfigEntity(reps = 3, repDuration = 30, restDuration = 20))
        assertEquals(20, repository.load()?.restDuration)
    }

    @Test
    fun `load returns correct WorkoutConfig`() = runTest {
        dao.save(WorkoutConfigEntity(reps = 5, repDuration = 40, restDuration = 15))
        assertEquals(WorkoutConfig(reps = 5, repDuration = 40, restDuration = 15), repository.load())
    }

    // endregion

    // region save

    @Test
    fun `save calls dao save exactly once`() = runTest {
        repository.save(WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10))
        assertEquals(1, dao.saveCallCount)
    }

    @Test
    fun `save maps domain reps to entity`() = runTest {
        repository.save(WorkoutConfig(reps = 6, repDuration = 30, restDuration = 10))
        assertEquals(6, dao.load()?.reps)
    }

    @Test
    fun `save maps domain repDuration to entity`() = runTest {
        repository.save(WorkoutConfig(reps = 3, repDuration = 50, restDuration = 10))
        assertEquals(50, dao.load()?.repDuration)
    }

    @Test
    fun `save maps domain restDuration to entity`() = runTest {
        repository.save(WorkoutConfig(reps = 3, repDuration = 30, restDuration = 25))
        assertEquals(25, dao.load()?.restDuration)
    }

    @Test
    fun `save then load returns same config`() = runTest {
        val config = WorkoutConfig(reps = 4, repDuration = 35, restDuration = 12)
        repository.save(config)
        assertEquals(config, repository.load())
    }

    @Test
    fun `save overwrites previous config`() = runTest {
        repository.save(WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10))
        repository.save(WorkoutConfig(reps = 8, repDuration = 60, restDuration = 20))
        assertEquals(WorkoutConfig(reps = 8, repDuration = 60, restDuration = 20), repository.load())
    }

    // endregion
}
