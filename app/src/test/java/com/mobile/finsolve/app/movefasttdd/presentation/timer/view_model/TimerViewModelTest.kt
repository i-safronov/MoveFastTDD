package com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerState
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.repository.FakeWorkoutTimerRepository
import com.mobile.finsolve.app.movefasttdd.domain.usecase.FakeWorkoutConfigRepository
import com.mobile.finsolve.app.movefasttdd.service.WorkoutServiceStarter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var timerRepo: FakeWorkoutTimerRepository
    private lateinit var configRepo: FakeWorkoutConfigRepository
    private lateinit var serviceStarter: FakeWorkoutServiceStarter
    private lateinit var viewModel: TimerViewModel

    // config: [Work(3), Rest(2), Work(3), Finished]
    private val config = WorkoutConfig(reps = 2, repDuration = 3, restDuration = 2)

    private val phases = listOf(
        TimerPhase.Work(3),
        TimerPhase.Rest(2),
        TimerPhase.Work(3),
        TimerPhase.Finished,
    )

    private fun timerState(
        phaseIndex: Int = 0,
        remainingSeconds: Int = 3,
        isRunning: Boolean = true,
    ) = TimerState(
        config = config,
        phases = phases,
        currentPhaseIndex = phaseIndex,
        remainingSeconds = remainingSeconds,
        isRunning = isRunning,
    )

    private fun buildViewModel() = TimerViewModel(
        workoutConfigRepository = configRepo,
        workoutTimerRepository = timerRepo,
        serviceStarter = serviceStarter,
        dispatchers = object : DispatchersList {
            override fun io(): CoroutineDispatcher = testDispatcher
            override fun ui(): CoroutineDispatcher = testDispatcher
        },
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        timerRepo = FakeWorkoutTimerRepository()
        configRepo = FakeWorkoutConfigRepository()
        serviceStarter = FakeWorkoutServiceStarter()
        configRepo.configToReturn = config
        timerRepo.startEmitsState = timerState()
        viewModel = buildViewModel()
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun drainSoundEvents() {
        while (viewModel.events.tryReceive().getOrNull() is TimerContract.Event.PlaySound) { /* drain */ }
    }

    private fun firstNonSoundEvent(): TimerContract.Event? {
        repeat(20) {
            val event = viewModel.events.tryReceive().getOrNull() ?: return null
            if (event !is TimerContract.Event.PlaySound) return event
        }
        return null
    }

    // region fresh start

    @Test
    fun `fresh start sets isLoading to false`() = runTest {
        assertFalse(viewModel.state.isLoading)
    }

    @Test
    fun `fresh start sets Work as first phase`() = runTest {
        assertTrue(viewModel.state.currentPhase is TimerPhase.Work)
    }

    @Test
    fun `fresh start sets remainingSeconds to repDuration`() = runTest {
        assertEquals(3, viewModel.state.remainingSeconds)
    }

    @Test
    fun `fresh start sets isRunning to true`() = runTest {
        assertTrue(viewModel.state.isRunning)
    }

    @Test
    fun `fresh start sets correct phase count`() = runTest {
        assertEquals(4, viewModel.state.phases.size)
    }

    @Test
    fun `fresh start calls repository start`() = runTest {
        assertEquals(1, timerRepo.startCallCount)
    }

    @Test
    fun `fresh start calls serviceStarter`() = runTest {
        assertEquals(1, serviceStarter.startCallCount)
    }

    @Test
    fun `fresh start plays START sound`() = runTest {
        val event = viewModel.events.tryReceive().getOrNull()
        assertEquals(TimerContract.Event.PlaySound(TimerContract.SoundType.START), event)
    }

    @Test
    fun `navigates back when repository returns no config`() = runTest {
        configRepo.configToReturn = null
        timerRepo.startEmitsState = null
        viewModel = buildViewModel()
        assertTrue(firstNonSoundEvent() is TimerContract.Event.NavigateBack)
    }

    @Test
    fun `navigates back when config load fails`() = runTest {
        configRepo.shouldFailOnLoad = true
        timerRepo.startEmitsState = null
        viewModel = buildViewModel()
        assertTrue(firstNonSoundEvent() is TimerContract.Event.NavigateBack)
    }

    @Test
    fun `does not navigate back on fresh start`() = runTest {
        assertFalse(firstNonSoundEvent() is TimerContract.Event.NavigateBack)
    }

    // endregion

    // region restore

    private fun setupRestore(
        phaseIndex: Int = 1,
        remainingSeconds: Int = 2,
        isRunning: Boolean = true,
    ) {
        timerRepo = FakeWorkoutTimerRepository()
        serviceStarter = FakeWorkoutServiceStarter()
        timerRepo.tryRestoreResult = true
        timerRepo.restoreEmitsState = timerState(phaseIndex, remainingSeconds, isRunning)
        viewModel = buildViewModel()
    }

    @Test
    fun `restore does not call repository start`() = runTest {
        setupRestore()
        assertEquals(0, timerRepo.startCallCount)
    }

    @Test
    fun `restore calls serviceStarter`() = runTest {
        setupRestore()
        assertEquals(1, serviceStarter.startCallCount)
    }

    @Test
    fun `restore sets correct phaseIndex`() = runTest {
        setupRestore(phaseIndex = 1, remainingSeconds = 2)
        assertEquals(1, viewModel.state.currentPhaseIndex)
    }

    @Test
    fun `restore sets correct remainingSeconds`() = runTest {
        setupRestore(phaseIndex = 1, remainingSeconds = 2)
        assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `restore plays START sound when isRunning=true`() = runTest {
        setupRestore(phaseIndex = 0, remainingSeconds = 3, isRunning = true)
        val event = viewModel.events.tryReceive().getOrNull()
        assertEquals(TimerContract.Event.PlaySound(TimerContract.SoundType.START), event)
    }

    @Test
    fun `restore does not play sound when isRunning=false`() = runTest {
        setupRestore(phaseIndex = 0, remainingSeconds = 3, isRunning = false)
        assertFalse(viewModel.events.tryReceive().isSuccess)
    }

    // endregion

    // region state mapping from repository

    @Test
    fun `state update reflects new remainingSeconds`() = runTest {
        timerRepo.emit(timerState(remainingSeconds = 2))
        assertEquals(2, viewModel.state.remainingSeconds)
    }

    @Test
    fun `state update reflects new phaseIndex`() = runTest {
        timerRepo.emit(timerState(phaseIndex = 1, remainingSeconds = 2))
        assertEquals(1, viewModel.state.currentPhaseIndex)
    }

    @Test
    fun `state update reflects isRunning=false`() = runTest {
        timerRepo.emit(timerState(isRunning = false))
        assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `state update reflects Rest phase`() = runTest {
        timerRepo.emit(timerState(phaseIndex = 1, remainingSeconds = 2))
        assertTrue(viewModel.state.currentPhase is TimerPhase.Rest)
    }

    // endregion

    // region phase transition sounds

    @Test
    fun `Work to Rest plays END sound`() = runTest {
        drainSoundEvents()
        timerRepo.emit(timerState(phaseIndex = 1, remainingSeconds = 2)) // Rest phase
        val event = viewModel.events.tryReceive().getOrNull()
        assertEquals(TimerContract.Event.PlaySound(TimerContract.SoundType.END), event)
    }

    @Test
    fun `Rest to Work plays START sound`() = runTest {
        timerRepo.emit(timerState(phaseIndex = 1, remainingSeconds = 2)) // Rest
        drainSoundEvents()
        timerRepo.emit(timerState(phaseIndex = 2, remainingSeconds = 3)) // Work
        val event = viewModel.events.tryReceive().getOrNull()
        assertEquals(TimerContract.Event.PlaySound(TimerContract.SoundType.START), event)
    }

    @Test
    fun `Finished phase plays END sound`() = runTest {
        drainSoundEvents()
        timerRepo.emit(timerState(phaseIndex = 3, remainingSeconds = 0, isRunning = false))
        val event = viewModel.events.tryReceive().getOrNull()
        assertEquals(TimerContract.Event.PlaySound(TimerContract.SoundType.END), event)
    }

    @Test
    fun `Finished phase sends WorkoutFinished event`() = runTest {
        drainSoundEvents()
        timerRepo.emit(timerState(phaseIndex = 3, remainingSeconds = 0, isRunning = false))
        assertTrue(firstNonSoundEvent() is TimerContract.Event.WorkoutFinished)
    }

    @Test
    fun `WorkoutFinished sent only once for repeated Finished emissions`() = runTest {
        val finishedState = timerState(phaseIndex = 3, remainingSeconds = 0, isRunning = false)
        timerRepo.emit(finishedState)
        timerRepo.emit(finishedState)
        var workoutFinishedCount = 0
        repeat(10) {
            val event = viewModel.events.tryReceive().getOrNull() ?: return@repeat
            if (event is TimerContract.Event.WorkoutFinished) workoutFinishedCount++
        }
        assertEquals(1, workoutFinishedCount)
    }

    // endregion

    // region Stop / Resume / Cancel

    @Test
    fun `Stop calls repository pause`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        assertEquals(1, timerRepo.pauseCallCount)
    }

    @Test
    fun `Stop does not send NavigateBack event`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        drainSoundEvents()
        assertFalse(viewModel.events.tryReceive().isSuccess)
    }

    @Test
    fun `Resume calls repository resume`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Resume)
        assertEquals(1, timerRepo.resumeCallCount)
    }

    @Test
    fun `Cancel calls repository cancel`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Cancel)
        assertEquals(1, timerRepo.cancelCallCount)
    }

    @Test
    fun `Cancel sends NavigateBack event`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Cancel)
        assertTrue(firstNonSoundEvent() is TimerContract.Event.NavigateBack)
    }

    @Test
    fun `Cancel sets isRunning to false immediately`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Cancel)
        assertFalse(viewModel.state.isRunning)
    }

    @Test
    fun `Stop then Resume calls pause then resume`() = runTest {
        viewModel.dispatch(TimerContract.Executor.Stop)
        viewModel.dispatch(TimerContract.Executor.Resume)
        assertEquals(1, timerRepo.pauseCallCount)
        assertEquals(1, timerRepo.resumeCallCount)
    }

    // endregion

    // region computed state properties

    @Test
    fun `currentRep is 1 on first Work phase`() = runTest {
        assertEquals(1, viewModel.state.currentRep)
    }

    @Test
    fun `totalReps matches config`() = runTest {
        assertEquals(2, viewModel.state.totalReps)
    }

    @Test
    fun `progress is 0 at start of phase`() = runTest {
        assertEquals(0f, viewModel.state.progress, 0.01f)
    }

    @Test
    fun `phaseDuration matches repDuration on Work phase`() = runTest {
        assertEquals(3, viewModel.state.phaseDuration)
    }

    @Test
    fun `isFinished is false during active workout`() = runTest {
        assertFalse(viewModel.state.isFinished)
    }

    @Test
    fun `isFinished is true when Finished phase emitted`() = runTest {
        timerRepo.emit(timerState(phaseIndex = 3, remainingSeconds = 0, isRunning = false))
        assertTrue(viewModel.state.isFinished)
    }

    // endregion
}

private class FakeWorkoutServiceStarter : WorkoutServiceStarter {
    var startCallCount = 0
    override fun start() { startCallCount++ }
}
