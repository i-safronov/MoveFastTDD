package com.mobile.finsolve.app.movefasttdd.data.repository

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.FakeTimerStateDataStore
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerSnapshot
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.use_case.BuildTimerSequenceUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// config: [Work(3), Rest(2), Work(3), Finished]  — total 8 ticks = 8_000 ms
@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutTimerRepositoryImplTest {

    private lateinit var dataStore: FakeTimerStateDataStore

    private val config = WorkoutConfig(reps = 2, repDuration = 3, restDuration = 2)

    @Before
    fun setup() {
        dataStore = FakeTimerStateDataStore()
    }

    private fun TestScope.buildRepository(): WorkoutTimerRepositoryImpl {
        val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher(testScheduler)
        return WorkoutTimerRepositoryImpl(
            buildTimerSequence = BuildTimerSequenceUseCase(),
            timerStateDataStore = dataStore,
            dispatchers = object : DispatchersList {
                override fun io() = dispatcher
                override fun ui() = dispatcher
            },
        )
    }

    private fun snapshot(
        phaseIndex: Int,
        remainingSeconds: Int,
        isRunning: Boolean = true,
    ) = TimerSnapshot(
        reps = 2, repDuration = 3, restDuration = 2,
        phaseIndex = phaseIndex,
        remainingSeconds = remainingSeconds,
        isRunning = isRunning,
    )

    // region state is null before any action

    @Test
    fun `state is null before tryRestore or start`() = runTest {
        val repo = buildRepository()
        assertNull(repo.state.value)
    }

    // endregion

    // region tryRestore

    @Test
    fun `tryRestore returns false when no snapshot`() = runTest {
        val repo = buildRepository()
        assertFalse(repo.tryRestore())
    }

    @Test
    fun `tryRestore state remains null when no snapshot`() = runTest {
        val repo = buildRepository()
        repo.tryRestore()
        assertNull(repo.state.value)
    }

    @Test
    fun `tryRestore returns true when snapshot exists`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 0, remainingSeconds = 3)
        val repo = buildRepository()
        assertTrue(repo.tryRestore())
    }

    @Test
    fun `tryRestore restores currentPhaseIndex`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 1, remainingSeconds = 2)
        val repo = buildRepository()
        repo.tryRestore()
        assertEquals(1, repo.state.value?.currentPhaseIndex)
    }

    @Test
    fun `tryRestore restores remainingSeconds`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 1, remainingSeconds = 2)
        val repo = buildRepository()
        repo.tryRestore()
        assertEquals(2, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `tryRestore restores isRunning true`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 0, remainingSeconds = 3, isRunning = true)
        val repo = buildRepository()
        repo.tryRestore()
        assertTrue(repo.state.value?.isRunning == true)
    }

    @Test
    fun `tryRestore restores isRunning false`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 0, remainingSeconds = 3, isRunning = false)
        val repo = buildRepository()
        repo.tryRestore()
        assertFalse(repo.state.value?.isRunning == true)
    }

    @Test
    fun `tryRestore restores config from snapshot`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 0, remainingSeconds = 3)
        val repo = buildRepository()
        repo.tryRestore()
        assertEquals(config, repo.state.value?.config)
    }

    @Test
    fun `tryRestore rebuilds correct phase count`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 0, remainingSeconds = 3)
        val repo = buildRepository()
        repo.tryRestore()
        assertEquals(4, repo.state.value?.phases?.size)
    }

    @Test
    fun `tryRestore starts ticking when isRunning=true`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 0, remainingSeconds = 3, isRunning = true)
        val repo = buildRepository()
        repo.tryRestore()
        advanceTimeBy(1_001L)
        assertEquals(2, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `tryRestore does not start ticking when isRunning=false`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 0, remainingSeconds = 3, isRunning = false)
        val repo = buildRepository()
        repo.tryRestore()
        advanceTimeBy(3_000L)
        assertEquals(3, repo.state.value?.remainingSeconds)
    }

    // endregion

    // region start

    @Test
    fun `start sets currentPhaseIndex to 0`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        assertEquals(0, repo.state.value?.currentPhaseIndex)
    }

    @Test
    fun `start sets remainingSeconds to first phase duration`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        assertEquals(3, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `start sets isRunning to true`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        assertTrue(repo.state.value?.isRunning == true)
    }

    @Test
    fun `start sets first phase as Work`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        assertTrue(repo.state.value?.currentPhase is TimerPhase.Work)
    }

    @Test
    fun `start builds correct number of phases`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        assertEquals(4, repo.state.value?.phases?.size)
    }

    @Test
    fun `start saves snapshot to DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        assertNotNull(dataStore.snapshot)
        assertEquals(0, dataStore.snapshot?.phaseIndex)
        assertEquals(3, dataStore.snapshot?.remainingSeconds)
        assertEquals(true, dataStore.snapshot?.isRunning)
    }

    @Test
    fun `start saves correct config to DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        assertEquals(2, dataStore.snapshot?.reps)
        assertEquals(3, dataStore.snapshot?.repDuration)
        assertEquals(2, dataStore.snapshot?.restDuration)
    }

    @Test
    fun `start begins ticking immediately`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(1_001L)
        assertEquals(2, repo.state.value?.remainingSeconds)
    }

    // endregion

    // region Tick — countdown — [Work(3), Rest(2), Work(3), Finished]

    @Test
    fun `one tick decrements remainingSeconds by 1`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(1_001L)
        assertEquals(2, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `two ticks decrement remainingSeconds correctly`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(2_001L)
        assertEquals(1, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `tick saves updated remainingSeconds to DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(1_001L)
        assertEquals(2, dataStore.snapshot?.remainingSeconds)
        assertEquals(0, dataStore.snapshot?.phaseIndex)
    }

    // endregion

    // region Tick — phase transitions

    @Test
    fun `Work phase ends and advances to Rest`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(3_001L) // 3 тика → Work(3) исчерпан
        assertTrue(repo.state.value?.currentPhase is TimerPhase.Rest)
    }

    @Test
    fun `Rest starts with full rest duration`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(3_001L)
        assertEquals(2, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `phase transition updates phaseIndex in DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(3_001L)
        assertEquals(1, dataStore.snapshot?.phaseIndex)
        assertEquals(2, dataStore.snapshot?.remainingSeconds)
    }

    @Test
    fun `Rest phase ends and advances to second Work`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(5_001L) // Work(3) + Rest(2)
        assertTrue(repo.state.value?.currentPhase is TimerPhase.Work)
        assertEquals(2, repo.state.value?.currentPhaseIndex)
    }

    @Test
    fun `completing all phases reaches Finished`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(8_001L) // 3 + 2 + 3
        assertTrue(repo.state.value?.isFinished == true)
    }

    @Test
    fun `isRunning is false when Finished`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(8_001L)
        assertFalse(repo.state.value?.isRunning == true)
    }

    @Test
    fun `DataStore is cleared when workout finishes`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(8_001L)
        assertEquals(1, dataStore.clearCallCount)
        assertNull(dataStore.snapshot)
    }

    @Test
    fun `ticking stops after workout finishes`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(8_001L)
        val clearCountAfterFinish = dataStore.clearCallCount
        advanceTimeBy(5_000L) // дополнительное время — не должно вызывать эффектов
        assertEquals(clearCountAfterFinish, dataStore.clearCallCount)
    }

    // endregion

    // region pause

    @Test
    fun `pause sets isRunning to false`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        assertFalse(repo.state.value?.isRunning == true)
    }

    @Test
    fun `pause saves isRunning=false to DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        assertEquals(false, dataStore.snapshot?.isRunning)
    }

    @Test
    fun `pause preserves phaseIndex in DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        assertEquals(0, dataStore.snapshot?.phaseIndex)
    }

    @Test
    fun `pause preserves remainingSeconds after tick`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(1_001L) // 1 тик → remaining = 2
        repo.pause()
        assertEquals(2, dataStore.snapshot?.remainingSeconds)
    }

    @Test
    fun `ticking stops after pause`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        val remainingAfterPause = repo.state.value?.remainingSeconds
        advanceTimeBy(5_000L)
        assertEquals(remainingAfterPause, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `pause does not clear DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        assertEquals(0, dataStore.clearCallCount)
        assertNotNull(dataStore.snapshot)
    }

    // endregion

    // region resume

    @Test
    fun `resume sets isRunning to true`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        repo.resume()
        assertTrue(repo.state.value?.isRunning == true)
    }

    @Test
    fun `resume saves isRunning=true to DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        repo.resume()
        assertEquals(true, dataStore.snapshot?.isRunning)
    }

    @Test
    fun `resume does not change remainingSeconds`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(1_000L) // remaining = 2
        repo.pause()
        val remainingAfterPause = repo.state.value?.remainingSeconds
        repo.resume()
        assertEquals(remainingAfterPause, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `resume restarts ticking`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        repo.resume()
        advanceTimeBy(1_001L)
        assertEquals(2, repo.state.value?.remainingSeconds)
    }

    @Test
    fun `resume after pause continues from correct position`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(2_001L) // 2 тика: 3→2→1, remaining=1, Work
        repo.pause()
        repo.resume()
        advanceTimeBy(1_001L) // 1 тик: 1→0 → переход в Rest
        assertTrue(repo.state.value?.currentPhase is TimerPhase.Rest)
    }

    // endregion

    // region cancel

    @Test
    fun `cancel sets state to null`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.cancel()
        assertNull(repo.state.value)
    }

    @Test
    fun `cancel clears DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.cancel()
        assertEquals(1, dataStore.clearCallCount)
        assertNull(dataStore.snapshot)
    }

    @Test
    fun `cancel stops ticking`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.cancel()
        advanceTimeBy(5_000L)
        assertNull(repo.state.value)
    }

    @Test
    fun `cancel after pause clears DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        repo.cancel()
        assertEquals(1, dataStore.clearCallCount)
        assertNull(dataStore.snapshot)
    }

    @Test
    fun `cancel after tryRestore clears DataStore`() = runTest {
        dataStore.snapshot = snapshot(phaseIndex = 1, remainingSeconds = 2)
        val repo = buildRepository()
        repo.tryRestore()
        repo.cancel()
        assertEquals(1, dataStore.clearCallCount)
        assertNull(dataStore.snapshot)
    }

    // endregion

    // region action sequences

    @Test
    fun `tryRestore returns false after cancel`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.cancel()
        assertFalse(repo.tryRestore())
    }

    @Test
    fun `tryRestore returns true after pause`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        // Новый экземпляр — симулируем перезапуск приложения
        val repo2 = buildRepository()
        assertTrue(repo2.tryRestore())
    }

    @Test
    fun `start after cancel resets to initial state`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        advanceTimeBy(2_000L) // remaining = 1
        repo.cancel()
        repo.start(config)
        assertEquals(0, repo.state.value?.currentPhaseIndex)
        assertEquals(3, repo.state.value?.remainingSeconds)
        assertTrue(repo.state.value?.isRunning == true)
    }

    @Test
    fun `pause then resume then pause results in isRunning=false`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repo.pause()
        repo.resume()
        repo.pause()
        assertFalse(repo.state.value?.isRunning == true)
    }

    @Test
    fun `multiple pauses do not clear DataStore`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repeat(5) { repo.pause() }
        assertEquals(0, dataStore.clearCallCount)
    }

    @Test
    fun `multiple resumes do not duplicate tick speed`() = runTest {
        val repo = buildRepository()
        repo.start(config)
        repeat(5) { repo.resume() }
        advanceTimeBy(1_001L)
        // Должен быть ровно 1 тик — не 5
        assertEquals(2, repo.state.value?.remainingSeconds)
    }

    // endregion
}
