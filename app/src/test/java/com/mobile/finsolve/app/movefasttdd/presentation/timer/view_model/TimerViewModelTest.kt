package com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.FakeTimerStateDataStore
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerSnapshot
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.use_case.BuildTimerSequenceUseCase
import com.mobile.finsolve.app.movefasttdd.domain.usecase.FakeWorkoutConfigRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataStore: FakeTimerStateDataStore
    private lateinit var repository: FakeWorkoutConfigRepository
    private lateinit var viewModel: TimerViewModel

    private val config = WorkoutConfig(reps = 2, repDuration = 3, restDuration = 2)

    private fun buildViewModel() = TimerViewModel(
        buildTimerSequence = BuildTimerSequenceUseCase(),
        timerStateDataStore = dataStore,
        workoutConfigRepository = repository,
        dispatchers = object : DispatchersList {
            override fun io(): CoroutineDispatcher = testDispatcher
            override fun ui(): CoroutineDispatcher = testDispatcher
        },
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = FakeTimerStateDataStore()
        repository = FakeWorkoutConfigRepository()
        repository.configToReturn = config
        viewModel = buildViewModel()
    }

    private fun firstNonSoundEvent(): TimerContract.Event? {
        repeat(20) {
            val event = viewModel.events.tryReceive().getOrNull() ?: return null
            if (event !is TimerContract.Event.PlaySound) return event
        }
        return null
    }

    private fun drainSoundEvents() {
        while (viewModel.events.tryReceive().getOrNull() is TimerContract.Event.PlaySound) { /* drain */ }
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // region Init from Repository (fresh start)

    @Test
    fun `fresh start builds phases from repository config`() = runTest {
        Assert.assertEquals(4, viewModel.state.phases.size)
    }

    @Test
    fun `fresh start sets first phase as Work`() = runTest {
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Work)
    }

    @Test
    fun `fresh start sets remainingSeconds to first phase duration`() = runTest {
        Assert.assertEquals(3, viewModel.state.remainingSeconds)
    }

    @Test
    fun `fresh start sets currentPhaseIndex to 0`() = runTest {
        Assert.assertEquals(0, viewModel.state.currentPhaseIndex)
    }

    @Test
    fun `fresh start sets isRunning to true`() = runTest {
        Assert.assertTrue(viewModel.state.isRunning)
    }

    @Test
    fun `fresh start saves snapshot to DataStore`() = runTest {
        Assert.assertNotNull(dataStore.snapshot)
        Assert.assertEquals(2, dataStore.snapshot?.reps)
        Assert.assertEquals(0, dataStore.snapshot?.phaseIndex)
        Assert.assertEquals(3, dataStore.snapshot?.remainingSeconds)
        Assert.assertEquals(true, dataStore.snapshot?.isRunning)
    }

    @Test
    fun `navigates back when repository load fails`() = runTest {
        dataStore.snapshot = null
        repository.shouldFailOnLoad = true
        viewModel = buildViewModel()
        val event = viewModel.events.tryReceive()
        Assert.assertTrue(event.getOrNull() is TimerContract.Event.NavigateBack)
    }

    @Test
    fun `navigates back when no config in repository`() = runTest {
        // @Before уже сохранил snapshot в DataStore — сбрасываем, чтобы ViewModel шёл в repository
        dataStore.snapshot = null
        repository.configToReturn = null
        viewModel = buildViewModel()
        val event = viewModel.events.tryReceive()
        Assert.assertTrue(event.getOrNull() is TimerContract.Event.NavigateBack)
    }

    // endregion

    // region Tick — countdown

    @Test
    fun `Tick decrements remainingSeconds by 1`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `multiple Ticks decrement remainingSeconds correctly`() = runTest {
        repeat(2) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertEquals(1, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Tick saves updated snapshot to DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(2, dataStore.snapshot?.remainingSeconds)
        Assert.assertEquals(0, dataStore.snapshot?.phaseIndex)
    }

    // endregion

    // region Tick — phase transitions
    // [Work(3), Rest(2), Work(3), Finished]

    @Test
    fun `Tick at last second of Work advances to Rest`() = runTest {
        repeat(3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Rest)
    }

    @Test
    fun `Rest phase starts with full rest duration`() = runTest {
        repeat(3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `phase transition saves new phase to DataStore`() = runTest {
        repeat(3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertEquals(1, dataStore.snapshot?.phaseIndex)
        Assert.assertEquals(2, dataStore.snapshot?.remainingSeconds)
    }

    @Test
    fun `completing all phases reaches Finished`() = runTest {
        repeat(3 + 2 + 3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Finished)
    }

    @Test
    fun `WorkoutFinished event sent when Finished phase reached`() = runTest {
        repeat(3 + 2 + 3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertTrue(firstNonSoundEvent() is TimerContract.Event.WorkoutFinished)
    }

    @Test
    fun `DataStore is cleared when workout finishes`() = runTest {
        repeat(3 + 2 + 3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertEquals(1, dataStore.clearCallCount)
        Assert.assertNull(dataStore.snapshot)
    }

    @Test
    fun `isRunning is false when Finished`() = runTest {
        repeat(3 + 2 + 3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertFalse(viewModel.state.isRunning)
    }

    // endregion

    // region Stop (пауза)

    @Test
    fun `Stop sets isRunning to false`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        Assert.assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `Stop does not send NavigateBack event`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        drainSoundEvents()
        Assert.assertFalse(viewModel.events.tryReceive().isSuccess)
    }

    @Test
    fun `Stop does not clear DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        Assert.assertEquals(0, dataStore.clearCallCount)
        Assert.assertNotNull(dataStore.snapshot)
    }

    @Test
    fun `Tick after Stop does not change remainingSeconds`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        val secondsBefore = viewModel.state.remainingSeconds
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(secondsBefore, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Tick after Stop does not change phase`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        val phaseBefore = viewModel.state.currentPhaseIndex
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(phaseBefore, viewModel.state.currentPhaseIndex)
    }

    // endregion

    // region Resume

    @Test
    fun `Resume sets isRunning to true`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        viewModel.dispatch(TimerContract.Executor.Resume)
        Assert.assertTrue(viewModel.state.isRunning)
    }

    @Test
    fun `Tick works after Resume`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        viewModel.dispatch(TimerContract.Executor.Resume)
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Resume does not change remainingSeconds`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Tick)
        viewModel.dispatch(TimerContract.Executor.Stop)
        val secondsBefore = viewModel.state.remainingSeconds
        viewModel.dispatch(TimerContract.Executor.Resume)
        Assert.assertEquals(secondsBefore, viewModel.state.remainingSeconds)
    }

    // endregion

    // region Cancel

    @Test
    fun `Cancel sets isRunning to false`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `Cancel sends NavigateBack event`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertTrue(firstNonSoundEvent() is TimerContract.Event.NavigateBack)
    }

    @Test
    fun `Cancel clears DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertEquals(1, dataStore.clearCallCount)
        Assert.assertNull(dataStore.snapshot)
    }

    @Test
    fun `Cancel after Stop also clears DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertEquals(1, dataStore.clearCallCount)
        Assert.assertNull(dataStore.snapshot)
    }

    // endregion

    // region Process death restoration

    private fun snapshotAt(
        phaseIndex: Int,
        remainingSeconds: Int,
        isRunning: Boolean = true,
    ) = TimerSnapshot(
        reps = 2, repDuration = 3, restDuration = 2,
        phaseIndex = phaseIndex,
        remainingSeconds = remainingSeconds,
        isRunning = isRunning,
    )

    @Test
    fun `restores currentPhaseIndex from DataStore`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 1, remainingSeconds = 2)
        viewModel = buildViewModel()
        Assert.assertEquals(1, viewModel.state.currentPhaseIndex)
    }

    @Test
    fun `restores remainingSeconds from DataStore`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 1, remainingSeconds = 2)
        viewModel = buildViewModel()
        Assert.assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `restored timer has correct current phase`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 1, remainingSeconds = 2)
        viewModel = buildViewModel()
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Rest)
    }

    @Test
    fun `restored timer is running when snapshot isRunning=true`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 0, remainingSeconds = 3, isRunning = true)
        viewModel = buildViewModel()
        Assert.assertTrue(viewModel.state.isRunning)
    }

    @Test
    fun `restored timer does not load from repository when snapshot exists`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 0, remainingSeconds = 3)
        // @Before already called load() once — reset before building to count only new calls
        repository.loadCallCount = 0
        viewModel = buildViewModel()
        Assert.assertEquals(0, repository.loadCallCount)
    }

    @Test
    fun `Tick works correctly after restoration`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 1, remainingSeconds = 2)
        viewModel = buildViewModel()
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(1, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Tick after restoration advances phase when remainingSeconds reaches zero`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 1, remainingSeconds = 1)
        viewModel = buildViewModel()
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(2, viewModel.state.currentPhaseIndex)
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Work)
        Assert.assertEquals(3, viewModel.state.remainingSeconds)
    }

    @Test
    fun `restored timer completes workout correctly`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 2, remainingSeconds = 1)
        viewModel = buildViewModel()
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Finished)
        Assert.assertTrue(firstNonSoundEvent() is TimerContract.Event.WorkoutFinished)
    }

    @Test
    fun `Cancel after restoration clears DataStore`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 1, remainingSeconds = 2)
        viewModel = buildViewModel()
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertEquals(1, dataStore.clearCallCount)
        Assert.assertNull(dataStore.snapshot)
    }

    // endregion

    // region Pause/Resume state persistence

    @Test
    fun `Stop saves isRunning=false to DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        Assert.assertEquals(false, dataStore.snapshot?.isRunning)
    }

    @Test
    fun `Stop preserves phaseIndex and remainingSeconds in DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Tick) // remaining = 2
        viewModel.dispatch(TimerContract.Executor.Stop)
        Assert.assertEquals(0, dataStore.snapshot?.phaseIndex)
        Assert.assertEquals(2, dataStore.snapshot?.remainingSeconds)
        Assert.assertEquals(false, dataStore.snapshot?.isRunning)
    }

    @Test
    fun `restored timer is paused when snapshot has isRunning=false`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 0, remainingSeconds = 3, isRunning = false)
        viewModel = buildViewModel()
        Assert.assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `Tick does not work when restored in paused state`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 0, remainingSeconds = 3, isRunning = false)
        viewModel = buildViewModel()
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(3, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Resume after restored pause starts timer correctly`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 0, remainingSeconds = 3, isRunning = false)
        viewModel = buildViewModel()
        viewModel.dispatch(TimerContract.Executor.Resume)
        Assert.assertTrue(viewModel.state.isRunning)
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(2, viewModel.state.remainingSeconds)
    }

    // endregion

    // region Rapid Stop/Resume taps

    @Test
    fun `rapid Stops result in isRunning=false`() = runTest {
        repeat(10) { viewModel.dispatch(TimerContract.Executor.Stop) }
        Assert.assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `rapid Resumes result in isRunning=true`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        repeat(10) { viewModel.dispatch(TimerContract.Executor.Resume) }
        Assert.assertTrue(viewModel.state.isRunning)
    }

    @Test
    fun `rapid alternation ends in state matching last action — Resume`() = runTest {
        // 10 итераций: 0=Stop,1=Resume,...,9=Resume → последний Resume
        repeat(10) { i ->
            if (i % 2 == 0) viewModel.dispatch(TimerContract.Executor.Stop)
            else viewModel.dispatch(TimerContract.Executor.Resume)
        }
        Assert.assertTrue(viewModel.state.isRunning)
    }

    @Test
    fun `rapid alternation ends in state matching last action — Stop`() = runTest {
        // 9 итераций: 0=Stop,...,8=Stop → последний Stop
        repeat(9) { i ->
            if (i % 2 == 0) viewModel.dispatch(TimerContract.Executor.Stop)
            else viewModel.dispatch(TimerContract.Executor.Resume)
        }
        Assert.assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `Tick decrements by exactly 1 after rapid Stop-Resume alternation`() = runTest {
        val initialRemaining = viewModel.state.remainingSeconds
        repeat(10) { i ->
            if (i % 2 == 0) viewModel.dispatch(TimerContract.Executor.Stop)
            else viewModel.dispatch(TimerContract.Executor.Resume)
        }

        Assert.assertEquals(initialRemaining, viewModel.state.remainingSeconds)

        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(initialRemaining - 1, viewModel.state.remainingSeconds)
    }

    @Test
    fun `rapid Stops do not send multiple NavigateBack events`() = runTest {
        repeat(10) { viewModel.dispatch(TimerContract.Executor.Stop) }
        drainSoundEvents()
        Assert.assertFalse(viewModel.events.tryReceive().isSuccess)
    }

    @Test
    fun `rapid Cancels send NavigateBack only once`() = runTest {
        repeat(5) { viewModel.dispatch(TimerContract.Executor.Cancel) }
        Assert.assertTrue(firstNonSoundEvent() is TimerContract.Event.NavigateBack)
    }

    @Test
    fun `rapid Stop does not change remainingSeconds`() = runTest {
        val initial = viewModel.state.remainingSeconds
        repeat(10) { viewModel.dispatch(TimerContract.Executor.Stop) }
        Assert.assertEquals(initial, viewModel.state.remainingSeconds)
    }

    @Test
    fun `rapid Resume does not change remainingSeconds`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        val secondsAfterStop = viewModel.state.remainingSeconds
        repeat(10) { viewModel.dispatch(TimerContract.Executor.Resume) }
        Assert.assertEquals(secondsAfterStop, viewModel.state.remainingSeconds)
    }

    @Test
    fun `DataStore saves isRunning correctly after rapid alternation`() = runTest {
        repeat(6) { i ->
            if (i % 2 == 0) viewModel.dispatch(TimerContract.Executor.Stop)
            else viewModel.dispatch(TimerContract.Executor.Resume)
        }
        Assert.assertEquals(true, dataStore.snapshot?.isRunning)
    }

    @Test
    fun `phase does not change during rapid Stop-Resume`() = runTest {
        val initialPhaseIndex = viewModel.state.currentPhaseIndex
        repeat(20) { i ->
            if (i % 2 == 0) viewModel.dispatch(TimerContract.Executor.Stop)
            else viewModel.dispatch(TimerContract.Executor.Resume)
        }
        Assert.assertEquals(initialPhaseIndex, viewModel.state.currentPhaseIndex)
    }

    // endregion
}
