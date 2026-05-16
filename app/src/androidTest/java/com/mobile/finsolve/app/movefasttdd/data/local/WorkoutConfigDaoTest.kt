package com.mobile.finsolve.app.movefasttdd.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutConfigDaoTest {

    private lateinit var db: WorkoutConfigDatabase
    private lateinit var dao: WorkoutConfigDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutConfigDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.workoutConfigDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    // region load

    @Test
    fun load_returnsNull_whenDatabaseIsEmpty() = runTest {
        assertNull(dao.load())
    }

    @Test
    fun load_returnsEntity_afterSave() = runTest {
        dao.save(WorkoutConfigEntity(reps = 5, repDuration = 30, restDuration = 10))
        val result = dao.load()
        assertEquals(5, result?.reps)
        assertEquals(30, result?.repDuration)
        assertEquals(10, result?.restDuration)
    }

    @Test
    fun load_returnsCorrectReps() = runTest {
        dao.save(WorkoutConfigEntity(reps = 8, repDuration = 20, restDuration = 5))
        assertEquals(8, dao.load()?.reps)
    }

    @Test
    fun load_returnsCorrectRepDuration() = runTest {
        dao.save(WorkoutConfigEntity(reps = 3, repDuration = 45, restDuration = 5))
        assertEquals(45, dao.load()?.repDuration)
    }

    @Test
    fun load_returnsCorrectRestDuration() = runTest {
        dao.save(WorkoutConfigEntity(reps = 3, repDuration = 30, restDuration = 15))
        assertEquals(15, dao.load()?.restDuration)
    }

    // endregion

    // region save

    @Test
    fun save_twice_overwritesPreviousEntry() = runTest {
        dao.save(WorkoutConfigEntity(reps = 3, repDuration = 30, restDuration = 10))
        dao.save(WorkoutConfigEntity(reps = 7, repDuration = 60, restDuration = 20))
        val result = dao.load()
        assertEquals(7, result?.reps)
        assertEquals(60, result?.repDuration)
        assertEquals(20, result?.restDuration)
    }

    @Test
    fun save_withZeroRestDuration_persistsCorrectly() = runTest {
        dao.save(WorkoutConfigEntity(reps = 5, repDuration = 30, restDuration = 0))
        assertEquals(0, dao.load()?.restDuration)
    }

    @Test
    fun save_withMinimumValidValues_persistsCorrectly() = runTest {
        dao.save(WorkoutConfigEntity(reps = 1, repDuration = 1, restDuration = 0))
        val result = dao.load()
        assertEquals(1, result?.reps)
        assertEquals(1, result?.repDuration)
        assertEquals(0, result?.restDuration)
    }

    // endregion
}
