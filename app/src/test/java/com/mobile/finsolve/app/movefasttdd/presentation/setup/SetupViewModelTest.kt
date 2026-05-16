package com.mobile.finsolve.app.movefasttdd.presentation.setup

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.use_case.ValidateWorkoutConfigUseCase
import com.mobile.finsolve.app.movefasttdd.domain.usecase.FakeWorkoutConfigRepository
import com.mobile.finsolve.app.movefasttdd.presentation.setup.SetupContract.Executor.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: FakeWorkoutConfigRepository
    private lateinit var viewModel: SetupViewModel

    private fun buildViewModel() = SetupViewModel(
        repository = repository,
        validate = ValidateWorkoutConfigUseCase(),
        dispatchers = object : DispatchersList {
            override fun io(): CoroutineDispatcher = testDispatcher
            override fun ui(): CoroutineDispatcher = testDispatcher
        }
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWorkoutConfigRepository()
        viewModel = buildViewModel()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // region Initial state

    @Test
    fun `initial state has default reps value`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.state.reps > 0)
    }

    @Test
    fun `initial state has default rep duration value`() = runTest {
        advanceUntilIdle()
        assertTrue(viewModel.state.repDuration > 0)
    }

    @Test
    fun `initial state has no error`() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.state.isError)
    }

    // endregion

    // region Load config

    @Test
    fun `on start loads saved config from repository`() = runTest {
        repository.configToReturn = WorkoutConfig(reps = 7, repDuration = 45, restDuration = 20)
        viewModel = buildViewModel()
        advanceUntilIdle()
        assertEquals(7, viewModel.state.reps)
        assertEquals(45, viewModel.state.repDuration)
        assertEquals(20, viewModel.state.restDuration)
    }

    @Test
    fun `when no saved config state keeps defaults`() = runTest {
        repository.configToReturn = null
        viewModel = buildViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.state.reps > 0)
        assertTrue(viewModel.state.repDuration > 0)
    }

    @Test
    fun `loaded config reps are shown in state`() = runTest {
        repository.configToReturn = WorkoutConfig(reps = 10, repDuration = 30, restDuration = 15)
        viewModel = buildViewModel()
        advanceUntilIdle()
        assertEquals(10, viewModel.state.reps)
    }

    @Test
    fun `loaded config rest duration is shown in state`() = runTest {
        repository.configToReturn = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 25)
        viewModel = buildViewModel()
        advanceUntilIdle()
        assertEquals(25, viewModel.state.restDuration)
    }

    // endregion

    // region Update fields

    @Test
    fun `UpdateReps updates reps in state`() = runTest {
        viewModel.dispatch(UpdateReps(5))
        advanceUntilIdle()
        assertEquals(5, viewModel.state.reps)
    }

    @Test
    fun `UpdateRepDuration updates rep duration in state`() = runTest {
        viewModel.dispatch(UpdateRepDuration(60))
        advanceUntilIdle()
        assertEquals(60, viewModel.state.repDuration)
    }

    @Test
    fun `UpdateRestDuration updates rest duration in state`() = runTest {
        viewModel.dispatch(UpdateRestDuration(15))
        advanceUntilIdle()
        assertEquals(15, viewModel.state.restDuration)
    }

    @Test
    fun `updating reps does not affect rep duration`() = runTest {
        viewModel.dispatch(UpdateRepDuration(45))
        viewModel.dispatch(UpdateReps(5))
        advanceUntilIdle()
        assertEquals(45, viewModel.state.repDuration)
    }

    @Test
    fun `updating rep duration does not affect reps`() = runTest {
        viewModel.dispatch(UpdateReps(8))
        viewModel.dispatch(UpdateRepDuration(60))
        advanceUntilIdle()
        assertEquals(8, viewModel.state.reps)
    }

    @Test
    fun `updating field after error clears error`() = runTest {
        viewModel.dispatch(UpdateReps(0))
        viewModel.dispatch(Start)
        advanceUntilIdle()
        assertTrue(viewModel.state.isError)

        viewModel.dispatch(UpdateReps(3))
        advanceUntilIdle()
        assertFalse(viewModel.state.isError)
    }

    // endregion

    // region Start — valid config

    @Test
    fun `Start with valid config sends NavigateToTimer event`() = runTest {
        viewModel.dispatch(UpdateReps(3))
        viewModel.dispatch(UpdateRepDuration(30))
        viewModel.dispatch(UpdateRestDuration(10))
        viewModel.dispatch(Start)
        advanceUntilIdle()

        val event = viewModel.events.tryReceive()
        assertTrue(event.isSuccess)
        assertTrue(event.getOrNull() is SetupContract.Event.NavigateToTimer)
    }

    @Test
    fun `Start with valid config saves config to repository`() = runTest {
        viewModel.dispatch(UpdateReps(3))
        viewModel.dispatch(UpdateRepDuration(30))
        viewModel.dispatch(UpdateRestDuration(10))
        viewModel.dispatch(Start)
        advanceUntilIdle()

        assertNotNull(repository.savedConfig)
        assertEquals(3, repository.savedConfig?.reps)
        assertEquals(30, repository.savedConfig?.repDuration)
        assertEquals(10, repository.savedConfig?.restDuration)
    }

    @Test
    fun `Start with valid config does not set error`() = runTest {
        viewModel.dispatch(UpdateReps(3))
        viewModel.dispatch(UpdateRepDuration(30))
        viewModel.dispatch(UpdateRestDuration(10))
        viewModel.dispatch(Start)
        advanceUntilIdle()

        assertFalse(viewModel.state.isError)
    }

    @Test
    fun `NavigateToTimer event contains correct config`() = runTest {
        viewModel.dispatch(UpdateReps(5))
        viewModel.dispatch(UpdateRepDuration(45))
        viewModel.dispatch(UpdateRestDuration(20))
        viewModel.dispatch(Start)
        advanceUntilIdle()

        val event = viewModel.events.tryReceive().getOrNull() as? SetupContract.Event.NavigateToTimer
        assertNotNull(event)
        assertEquals(WorkoutConfig(reps = 5, repDuration = 45, restDuration = 20), event?.config)
    }

    @Test
    fun `repository save is called exactly once on Start`() = runTest {
        viewModel.dispatch(UpdateReps(3))
        viewModel.dispatch(UpdateRepDuration(30))
        viewModel.dispatch(UpdateRestDuration(10))
        viewModel.dispatch(Start)
        advanceUntilIdle()

        assertEquals(1, repository.saveCallCount)
    }

    // endregion

    // region Start — invalid config

    @Test
    fun `Start with zero reps sets error`() = runTest {
        viewModel.dispatch(UpdateReps(0))
        viewModel.dispatch(Start)
        advanceUntilIdle()
        assertTrue(viewModel.state.isError)
    }

    @Test
    fun `Start with zero rep duration sets error`() = runTest {
        viewModel.dispatch(UpdateRepDuration(0))
        viewModel.dispatch(Start)
        advanceUntilIdle()
        assertTrue(viewModel.state.isError)
    }

    @Test
    fun `Start with negative rest duration sets error`() = runTest {
        viewModel.dispatch(UpdateRestDuration(-1))
        viewModel.dispatch(Start)
        advanceUntilIdle()
        assertTrue(viewModel.state.isError)
    }

    @Test
    fun `Start with invalid config does not send NavigateToTimer event`() = runTest {
        viewModel.dispatch(UpdateReps(0))
        viewModel.dispatch(Start)
        advanceUntilIdle()

        val event = viewModel.events.tryReceive()
        assertFalse(event.isSuccess)
    }

    @Test
    fun `Start with invalid config does not save to repository`() = runTest {
        viewModel.dispatch(UpdateReps(0))
        viewModel.dispatch(Start)
        advanceUntilIdle()

        assertEquals(0, repository.saveCallCount)
    }

    @Test
    fun `Start with valid config after previous error clears error and navigates`() = runTest {
        viewModel.dispatch(UpdateReps(0))
        viewModel.dispatch(Start)
        advanceUntilIdle()
        assertTrue(viewModel.state.isError)

        viewModel.dispatch(UpdateReps(3))
        viewModel.dispatch(Start)
        advanceUntilIdle()

        assertFalse(viewModel.state.isError)
        val event = viewModel.events.tryReceive()
        assertTrue(event.isSuccess)
    }

    // endregion
}
