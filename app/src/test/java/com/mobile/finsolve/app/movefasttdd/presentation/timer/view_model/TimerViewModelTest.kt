package com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.FakeTimerStateDataStore
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerSnapshot
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.use_case.BuildTimerSequenceUseCase
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

// WorkoutConfig(reps=2, repDuration=3, restDuration=2)
// → [Work(3), Rest(2), Work(3), Finished]

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dataStore: FakeTimerStateDataStore
    private lateinit var viewModel: TimerViewModel

    private val config = WorkoutConfig(reps = 2, repDuration = 3, restDuration = 2)

    private fun buildViewModel() = TimerViewModel(
        buildTimerSequence = BuildTimerSequenceUseCase(),
        timerStateDataStore = dataStore,
        dispatchers = object : DispatchersList {
            override fun io(): CoroutineDispatcher = testDispatcher
            override fun ui(): CoroutineDispatcher = testDispatcher
        },
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStore = FakeTimerStateDataStore()
        viewModel = buildViewModel()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // region Init

    @Test
    fun `Init builds phases from config`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        Assert.assertEquals(4, viewModel.state.phases.size)
    }

    @Test
    fun `Init sets first phase as Work`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Work)
    }

    @Test
    fun `Init sets remainingSeconds to first phase duration`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        Assert.assertEquals(3, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Init sets currentPhaseIndex to 0`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        Assert.assertEquals(0, viewModel.state.currentPhaseIndex)
    }

    @Test
    fun `Init sets isRunning to true`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        Assert.assertTrue(viewModel.state.isRunning)
    }

    @Test
    fun `Init saves snapshot to DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        Assert.assertNotNull(dataStore.snapshot)
        Assert.assertEquals(2, dataStore.snapshot?.reps)
        Assert.assertEquals(0, dataStore.snapshot?.phaseIndex)
        Assert.assertEquals(3, dataStore.snapshot?.remainingSeconds)
    }

    // endregion

    // region Tick — countdown

    @Test
    fun `Tick decrements remainingSeconds by 1`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `multiple Ticks decrement remainingSeconds correctly`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        repeat(2) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertEquals(1, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Tick saves updated snapshot to DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(2, dataStore.snapshot?.remainingSeconds)
        Assert.assertEquals(0, dataStore.snapshot?.phaseIndex)
    }

    // endregion

    // region Tick — phase transitions
    // [Work(3), Rest(2), Work(3), Finished]

    @Test
    fun `Tick at last second of Work advances to Rest`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        repeat(3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Rest)
    }

    @Test
    fun `Rest phase starts with full rest duration`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        repeat(3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `phase transition saves new phase to DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        repeat(3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertEquals(1, dataStore.snapshot?.phaseIndex)
        Assert.assertEquals(2, dataStore.snapshot?.remainingSeconds)
    }

    @Test
    fun `completing all phases reaches Finished`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        repeat(3 + 2 + 3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertTrue(viewModel.state.currentPhase is TimerPhase.Finished)
    }

    @Test
    fun `WorkoutFinished event sent when Finished phase reached`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        repeat(3 + 2 + 3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertTrue(viewModel.events.tryReceive().getOrNull() is TimerContract.Event.WorkoutFinished)
    }

    @Test
    fun `DataStore is cleared when workout finishes`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        repeat(3 + 2 + 3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertEquals(1, dataStore.clearCallCount)
        Assert.assertNull(dataStore.snapshot)
    }

    @Test
    fun `isRunning is false when Finished`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        repeat(3 + 2 + 3) { viewModel.dispatch(TimerContract.Executor.Tick) }
        Assert.assertFalse(viewModel.state.isRunning)
    }

    // endregion

    // region Stop (пауза)

    @Test
    fun `Stop sets isRunning to false`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        Assert.assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `Stop does not send NavigateBack event`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        Assert.assertFalse(viewModel.events.tryReceive().isSuccess)
    }

    @Test
    fun `Stop does not clear DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        Assert.assertEquals(0, dataStore.clearCallCount)
        Assert.assertNotNull(dataStore.snapshot)
    }

    @Test
    fun `Tick after Stop does not change remainingSeconds`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        val secondsBefore = viewModel.state.remainingSeconds
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(secondsBefore, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Tick after Stop does not change phase`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        val phaseBefore = viewModel.state.currentPhaseIndex
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(phaseBefore, viewModel.state.currentPhaseIndex)
    }

    // endregion

    // region Resume

    @Test
    fun `Resume sets isRunning to true`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        viewModel.dispatch(TimerContract.Executor.Resume)
        Assert.assertTrue(viewModel.state.isRunning)
    }

    @Test
    fun `Tick works after Resume`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        viewModel.dispatch(TimerContract.Executor.Resume)
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Resume does not change remainingSeconds`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Tick) // остаётся 2
        viewModel.dispatch(TimerContract.Executor.Stop)
        val secondsBefore = viewModel.state.remainingSeconds
        viewModel.dispatch(TimerContract.Executor.Resume)
        Assert.assertEquals(secondsBefore, viewModel.state.remainingSeconds)
    }

    @Test
    fun `Resume does not change phase`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        val phaseBefore = viewModel.state.currentPhaseIndex
        viewModel.dispatch(TimerContract.Executor.Resume)
        Assert.assertEquals(phaseBefore, viewModel.state.currentPhaseIndex)
    }

    // endregion

    // region Cancel

    @Test
    fun `Cancel sets isRunning to false`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `Cancel sends NavigateBack event`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertTrue(viewModel.events.tryReceive().getOrNull() is TimerContract.Event.NavigateBack)
    }

    @Test
    fun `Cancel clears DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertEquals(1, dataStore.clearCallCount)
        Assert.assertNull(dataStore.snapshot)
    }

    @Test
    fun `Cancel after Stop also clears DataStore`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Init(config))
        viewModel.dispatch(TimerContract.Executor.Stop)
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertEquals(1, dataStore.clearCallCount)
        Assert.assertNull(dataStore.snapshot)
    }

    // endregion

    // region Process death restoration

    private fun snapshotAt(phaseIndex: Int, remainingSeconds: Int) = TimerSnapshot(
        reps = 2, repDuration = 3, restDuration = 2,
        phaseIndex = phaseIndex,
        remainingSeconds = remainingSeconds,
    )

    @Test
    fun `restores currentPhaseIndex from DataStore without Init`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 1, remainingSeconds = 2)
        viewModel = buildViewModel()
        Assert.assertEquals(1, viewModel.state.currentPhaseIndex)
    }

    @Test
    fun `restores remainingSeconds from DataStore without Init`() = runTest {
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
    fun `restored timer is running immediately`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 0, remainingSeconds = 3)
        viewModel = buildViewModel()
        Assert.assertTrue(viewModel.state.isRunning)
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
        Assert.assertTrue(viewModel.events.tryReceive().getOrNull() is TimerContract.Event.WorkoutFinished)
    }

    @Test
    fun `Cancel after restoration clears DataStore`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 1, remainingSeconds = 2)
        viewModel = buildViewModel()
        viewModel.dispatch(TimerContract.Executor.Cancel)
        Assert.assertEquals(1, dataStore.clearCallCount)
        Assert.assertNull(dataStore.snapshot)
    }

    @Test
    fun `Tick after restoration saves updated snapshot to DataStore`() = runTest {
        dataStore.snapshot = snapshotAt(phaseIndex = 0, remainingSeconds = 3)
        viewModel = buildViewModel()
        val savesBefore = dataStore.saveCallCount
        viewModel.dispatch(TimerContract.Executor.Tick)
        Assert.assertEquals(savesBefore + 1, dataStore.saveCallCount)
        Assert.assertEquals(2, dataStore.snapshot?.remainingSeconds)
    }

    // endregion
}
