package com.mobile.finsolve.app.movefasttdd.data.repository

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerSnapshot
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerStateDataStore
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerState
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutTimerRepository
import com.mobile.finsolve.app.movefasttdd.domain.use_case.BuildTimerSequenceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutTimerRepositoryImpl @Inject constructor(
    private val buildTimerSequence: BuildTimerSequenceUseCase,
    private val timerStateDataStore: TimerStateDataStore,
    private val dispatchers: DispatchersList,
) : WorkoutTimerRepository {

    private val scope = CoroutineScope(SupervisorJob() + dispatchers.io())

    private val _state = MutableStateFlow<TimerState?>(null)
    override val state: StateFlow<TimerState?> = _state.asStateFlow()

    private var tickJob: Job? = null

    override suspend fun tryRestore(): Boolean {
        val snapshot = withContext(dispatchers.io()) { timerStateDataStore.load() } ?: return false
        val config = WorkoutConfig(snapshot.reps, snapshot.repDuration, snapshot.restDuration)
        val phases = buildTimerSequence(config)
        val timerState = TimerState(
            config = config,
            phases = phases,
            currentPhaseIndex = snapshot.phaseIndex,
            remainingSeconds = snapshot.remainingSeconds,
            isRunning = snapshot.isRunning,
        )
        _state.value = timerState
        if (timerState.isRunning) startTicking()
        return true
    }

    override suspend fun start(config: WorkoutConfig) {
        tickJob?.cancel()
        val phases = buildTimerSequence(config)
        val firstDuration = phases.first().durationOrZero
        val timerState = TimerState(
            config = config,
            phases = phases,
            currentPhaseIndex = 0,
            remainingSeconds = firstDuration,
            isRunning = true,
        )
        _state.value = timerState
        withContext(dispatchers.io()) { timerStateDataStore.save(timerState.toSnapshot()) }
        startTicking()
    }

    override suspend fun pause() {
        tickJob?.cancel()
        tickJob = null
        updateState { copy(isRunning = false) }
    }

    override suspend fun resume() {
        updateState { copy(isRunning = true) }
        startTicking()
    }

    override suspend fun cancel() {
        tickJob?.cancel()
        tickJob = null
        _state.value = null
        withContext(dispatchers.io()) { timerStateDataStore.clear() }
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive && _state.value?.isRunning == true) {
                delay(1_000L)
                tick()
            }
        }
    }

    private suspend fun tick() {
        val current = _state.value?.takeIf { it.isRunning } ?: return
        if (current.remainingSeconds > 1) {
            updateState { copy(remainingSeconds = remainingSeconds - 1) }
        } else {
            advancePhase(current)
        }
    }

    private suspend fun advancePhase(current: TimerState) {
        val nextIndex = current.currentPhaseIndex + 1
        val nextPhase = current.phases.getOrElse(nextIndex) { TimerPhase.Finished }
        when (nextPhase) {
            is TimerPhase.Finished -> {
                _state.value = current.copy(
                    currentPhaseIndex = nextIndex,
                    remainingSeconds = 0,
                    isRunning = false,
                )
                withContext(dispatchers.io()) { timerStateDataStore.clear() }
            }
            else -> {
                updateState { copy(currentPhaseIndex = nextIndex, remainingSeconds = nextPhase.durationOrZero) }
            }
        }
    }

    private suspend fun updateState(transform: TimerState.() -> TimerState) {
        val next = _state.value?.transform() ?: return
        _state.value = next
        withContext(dispatchers.io()) { timerStateDataStore.save(next.toSnapshot()) }
    }
}

private val TimerPhase.durationOrZero: Int
    get() = when (this) {
        is TimerPhase.Work -> duration
        is TimerPhase.Rest -> duration
        TimerPhase.Finished -> 0
    }

private fun TimerState.toSnapshot() = TimerSnapshot(
    reps = config.reps,
    repDuration = config.repDuration,
    restDuration = config.restDuration,
    phaseIndex = currentPhaseIndex,
    remainingSeconds = remainingSeconds,
    isRunning = isRunning,
)
