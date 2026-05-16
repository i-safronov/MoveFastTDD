package com.mobile.finsolve.app.movefasttdd.presentation.setup.view_model

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.FakeWorkoutDraftDataStore
import com.mobile.finsolve.app.movefasttdd.data.datastore.WorkoutDraft
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.use_case.ValidateWorkoutConfigUseCase
import com.mobile.finsolve.app.movefasttdd.domain.usecase.FakeWorkoutConfigRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SetupViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: FakeWorkoutConfigRepository
    private lateinit var draftDataStore: FakeWorkoutDraftDataStore
    private lateinit var viewModel: SetupViewModel

    private fun buildViewModel() = SetupViewModel(
        repository = repository,
        validate = ValidateWorkoutConfigUseCase(),
        draftDataStore = draftDataStore,
        dispatchers = object : DispatchersList {
            override fun io(): CoroutineDispatcher = testDispatcher
            override fun ui(): CoroutineDispatcher = testDispatcher
        },
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeWorkoutConfigRepository()
        draftDataStore = FakeWorkoutDraftDataStore()
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
        Assert.assertTrue(viewModel.state.reps > 0)
    }

    @Test
    fun `initial state has default rep duration value`() = runTest {
        advanceUntilIdle()
        Assert.assertTrue(viewModel.state.repDuration > 0)
    }

    @Test
    fun `initial state has no errors`() = runTest {
        advanceUntilIdle()
        Assert.assertFalse(viewModel.state.repsError)
        Assert.assertFalse(viewModel.state.repDurationError)
        Assert.assertFalse(viewModel.state.restDurationError)
    }

    // endregion

    // region Draft restoration

    @Test
    fun `restores reps from draft on launch`() = runTest {
        draftDataStore.storedDraft = WorkoutDraft(reps = 9, repDuration = 30, restDuration = 10)
        viewModel = buildViewModel()
        advanceUntilIdle()
        Assert.assertEquals(9, viewModel.state.reps)
    }

    @Test
    fun `restores rep duration from draft on launch`() = runTest {
        draftDataStore.storedDraft = WorkoutDraft(reps = 3, repDuration = 55, restDuration = 10)
        viewModel = buildViewModel()
        advanceUntilIdle()
        Assert.assertEquals(55, viewModel.state.repDuration)
    }

    @Test
    fun `restores rest duration from draft on launch`() = runTest {
        draftDataStore.storedDraft = WorkoutDraft(reps = 3, repDuration = 30, restDuration = 25)
        viewModel = buildViewModel()
        advanceUntilIdle()
        Assert.assertEquals(25, viewModel.state.restDuration)
    }

    @Test
    fun `draft takes priority over saved config`() = runTest {
        draftDataStore.storedDraft = WorkoutDraft(reps = 5, repDuration = 30, restDuration = 10)
        repository.configToReturn = WorkoutConfig(reps = 99, repDuration = 99, restDuration = 99)
        viewModel = buildViewModel()
        advanceUntilIdle()
        Assert.assertEquals(5, viewModel.state.reps)
        Assert.assertEquals(30, viewModel.state.repDuration)
    }

    @Test
    fun `loads saved config when no draft exists`() = runTest {
        draftDataStore.storedDraft = null
        repository.configToReturn = WorkoutConfig(reps = 7, repDuration = 45, restDuration = 20)
        viewModel = buildViewModel()
        advanceUntilIdle()
        Assert.assertEquals(7, viewModel.state.reps)
        Assert.assertEquals(45, viewModel.state.repDuration)
        Assert.assertEquals(20, viewModel.state.restDuration)
    }

    @Test
    fun `uses defaults when no draft and no saved config`() = runTest {
        draftDataStore.storedDraft = null
        repository.configToReturn = null
        viewModel = buildViewModel()
        advanceUntilIdle()
        Assert.assertEquals(SetupContract.DEFAULT_REPS, viewModel.state.reps)
        Assert.assertEquals(SetupContract.DEFAULT_REP_DURATION, viewModel.state.repDuration)
        Assert.assertEquals(SetupContract.DEFAULT_REST_DURATION, viewModel.state.restDuration)
    }

    // endregion

    // region Draft saving on input

    @Test
    fun `UpdateReps saves draft with new reps`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(7))
        advanceUntilIdle()
        Assert.assertEquals(7, draftDataStore.storedDraft?.reps)
    }

    @Test
    fun `UpdateRepDuration saves draft with new rep duration`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(40))
        advanceUntilIdle()
        Assert.assertEquals(40, draftDataStore.storedDraft?.repDuration)
    }

    @Test
    fun `UpdateRestDuration saves draft with new rest duration`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateRestDuration(15))
        advanceUntilIdle()
        Assert.assertEquals(15, draftDataStore.storedDraft?.restDuration)
    }

    @Test
    fun `draft is saved on every input change`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(5))
        viewModel.dispatch(SetupContract.Executor.UpdateReps(6))
        viewModel.dispatch(SetupContract.Executor.UpdateReps(7))
        advanceUntilIdle()
        Assert.assertEquals(3, draftDataStore.saveCallCount)
    }

    // endregion

    // region Update fields

    @Test
    fun `UpdateReps updates reps in state`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(5))
        advanceUntilIdle()
        Assert.assertEquals(5, viewModel.state.reps)
    }

    @Test
    fun `UpdateRepDuration updates rep duration in state`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(60))
        advanceUntilIdle()
        Assert.assertEquals(60, viewModel.state.repDuration)
    }

    @Test
    fun `UpdateRestDuration updates rest duration in state`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateRestDuration(15))
        advanceUntilIdle()
        Assert.assertEquals(15, viewModel.state.restDuration)
    }

    @Test
    fun `updating reps does not affect rep duration`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(45))
        viewModel.dispatch(SetupContract.Executor.UpdateReps(5))
        advanceUntilIdle()
        Assert.assertEquals(45, viewModel.state.repDuration)
    }

    @Test
    fun `updating rep duration does not affect reps`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(8))
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(60))
        advanceUntilIdle()
        Assert.assertEquals(8, viewModel.state.reps)
    }

    @Test
    fun `updating reps clears reps error`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(0))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()
        viewModel.dispatch(SetupContract.Executor.UpdateReps(3))
        advanceUntilIdle()
        Assert.assertFalse(viewModel.state.repsError)
    }

    @Test
    fun `updating reps does not clear rep duration error`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(0))
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(0))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()
        viewModel.dispatch(SetupContract.Executor.UpdateReps(3))
        advanceUntilIdle()
        Assert.assertFalse(viewModel.state.repsError)
        Assert.assertTrue(viewModel.state.repDurationError)
    }

    @Test
    fun `updating rep duration does not clear reps error`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(0))
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(0))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(30))
        advanceUntilIdle()
        Assert.assertTrue(viewModel.state.repsError)
        Assert.assertFalse(viewModel.state.repDurationError)
    }

    // endregion

    // region Start — valid config

    @Test
    fun `Start with valid config sends NavigateToTimer event`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(3))
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(30))
        viewModel.dispatch(SetupContract.Executor.UpdateRestDuration(10))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        val event = viewModel.events.tryReceive()
        Assert.assertTrue(event.isSuccess)
        Assert.assertTrue(event.getOrNull() is SetupContract.Event.NavigateToTimer)
    }

    @Test
    fun `Start with valid config saves config to repository`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(3))
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(30))
        viewModel.dispatch(SetupContract.Executor.UpdateRestDuration(10))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        Assert.assertEquals(3, repository.savedConfig?.reps)
        Assert.assertEquals(30, repository.savedConfig?.repDuration)
        Assert.assertEquals(10, repository.savedConfig?.restDuration)
    }

    @Test
    fun `Start with valid config clears draft`() = runTest {
        draftDataStore.storedDraft = WorkoutDraft(reps = 3, repDuration = 30, restDuration = 10)
        viewModel.dispatch(SetupContract.Executor.UpdateReps(3))
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(30))
        viewModel.dispatch(SetupContract.Executor.UpdateRestDuration(10))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        Assert.assertEquals(1, draftDataStore.clearCallCount)
        Assert.assertNull(draftDataStore.storedDraft)
    }

    @Test
    fun `Start with valid config does not set any errors`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(3))
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(30))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        Assert.assertFalse(viewModel.state.repsError)
        Assert.assertFalse(viewModel.state.repDurationError)
        Assert.assertFalse(viewModel.state.restDurationError)
    }

    // endregion

    // region Start — invalid config

    @Test
    fun `Start with zero reps sets only reps error`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(0))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        Assert.assertTrue(viewModel.state.repsError)
        Assert.assertFalse(viewModel.state.repDurationError)
        Assert.assertFalse(viewModel.state.restDurationError)
    }

    @Test
    fun `Start with zero rep duration sets only rep duration error`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(0))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        Assert.assertFalse(viewModel.state.repsError)
        Assert.assertTrue(viewModel.state.repDurationError)
        Assert.assertFalse(viewModel.state.restDurationError)
    }

    @Test
    fun `Start with invalid config does not send NavigateToTimer event`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(0))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        Assert.assertFalse(viewModel.events.tryReceive().isSuccess)
    }

    @Test
    fun `Start with invalid config does not save to repository`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(0))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        Assert.assertEquals(0, repository.saveCallCount)
    }

    @Test
    fun `Start with invalid config does not clear draft`() = runTest {
        viewModel.dispatch(SetupContract.Executor.UpdateReps(0))
        viewModel.dispatch(SetupContract.Executor.Start)
        advanceUntilIdle()

        Assert.assertEquals(0, draftDataStore.clearCallCount)
    }

    // endregion
}
