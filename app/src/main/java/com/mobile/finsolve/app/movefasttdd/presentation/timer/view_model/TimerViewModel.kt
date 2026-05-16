package com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerSnapshot
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerStateDataStore
import com.mobile.finsolve.app.movefasttdd.domain.model.DataResult
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository
import com.mobile.finsolve.app.movefasttdd.domain.use_case.BuildTimerSequenceUseCase
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.APEXViewModel
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.EffectorScope
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.ExecutorScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val buildTimerSequence: BuildTimerSequenceUseCase,
    private val timerStateDataStore: TimerStateDataStore,
    private val workoutConfigRepository: WorkoutConfigRepository,
    dispatchers: DispatchersList,
) : APEXViewModel<TimerContract.State, TimerContract.Executor, TimerContract.Effect, TimerContract.Event>(
    initState = TimerContract.State(),
    dispatchers = dispatchers,
) {

    // Каждый новый запуск таймера получает уникальный номер поколения.
    // Старые корутины StartTimer видят что их generation устарел и выходят из цикла.
    private var timerGeneration = 0

    init {
        dispatch(TimerContract.Executor.Load)
    }

    private fun ExecutorScope<TimerContract.Effect, TimerContract.Event>.startTimer() {
        timerGeneration++
        sendEffect(TimerContract.Effect.StartTimer(timerGeneration))
    }

    override suspend fun ExecutorScope<TimerContract.Effect, TimerContract.Event>.execute(
        ex: TimerContract.Executor,
    ): TimerContract.State = when (ex) {

        TimerContract.Executor.Load -> {
            sendEffect(TimerContract.Effect.LoadState)
            state
        }

        is TimerContract.Executor.StateLoaded ->
            ex.snapshot?.let { snapshot ->
                val config = WorkoutConfig(snapshot.reps, snapshot.repDuration, snapshot.restDuration)
                val phases = buildTimerSequence(config)
                if (snapshot.isRunning) startTimer()
                state.copy(
                    isLoading = false,
                    config = config,
                    phases = phases,
                    currentPhaseIndex = snapshot.phaseIndex,
                    remainingSeconds = snapshot.remainingSeconds,
                    isRunning = snapshot.isRunning,
                )
            } ?: run {
                // Нет активной тренировки — грузим последний конфиг из Room
                sendEffect(TimerContract.Effect.LoadConfig)
                state
            }

        is TimerContract.Executor.ConfigLoaded -> {
            val config = ex.config ?: return@execute run {
                // В Room тоже нет конфига — нечего запускать
                sendEvent(TimerContract.Event.NavigateBack)
                state.copy(isLoading = false)
            }
            val phases = buildTimerSequence(config)
            val firstDuration = phases.first().durationOrZero
            sendEffect(TimerContract.Effect.SaveState(
                TimerSnapshot(
                    reps = config.reps,
                    repDuration = config.repDuration,
                    restDuration = config.restDuration,
                    phaseIndex = 0,
                    remainingSeconds = firstDuration,
                    isRunning = true,
                )
            ))
            startTimer()
            state.copy(
                isLoading = false,
                config = config,
                phases = phases,
                currentPhaseIndex = 0,
                remainingSeconds = firstDuration,
                isRunning = true,
            )
        }

        TimerContract.Executor.Tick ->
            if (!state.isRunning) state
            else if (state.remainingSeconds > 1) {
                val newRemaining = state.remainingSeconds - 1
                state.toSnapshot(remainingSeconds = newRemaining)
                    ?.let { sendEffect(TimerContract.Effect.SaveState(it)) }
                state.copy(remainingSeconds = newRemaining)
            } else {
                advanceToNextPhase()
            }

        TimerContract.Executor.Stop -> {
            state.toSnapshot(isRunning = false)
                ?.let { sendEffect(TimerContract.Effect.SaveState(it)) }
            state.copy(isRunning = false)
        }

        TimerContract.Executor.Resume -> {
            state.toSnapshot(isRunning = true)
                ?.let { sendEffect(TimerContract.Effect.SaveState(it)) }
            startTimer()
            state.copy(isRunning = true)
        }

        TimerContract.Executor.Cancel -> {
            sendEffect(TimerContract.Effect.ClearState)
            sendEvent(TimerContract.Event.NavigateBack)
            state.copy(isRunning = false)
        }
    }

    override suspend fun EffectorScope<TimerContract.Executor>.affect(
        ef: TimerContract.Effect,
    ) {
        when (ef) {
            TimerContract.Effect.LoadState -> {
                val snapshot = timerStateDataStore.load()
                dispatch(TimerContract.Executor.StateLoaded(snapshot))
            }

            TimerContract.Effect.LoadConfig -> {
                val config = when (val result = workoutConfigRepository.load()) {
                    is DataResult.Success -> result.value
                    DataResult.Error -> null
                }
                dispatch(TimerContract.Executor.ConfigLoaded(config))
            }

            is TimerContract.Effect.SaveState -> timerStateDataStore.save(ef.snapshot)

            TimerContract.Effect.ClearState -> timerStateDataStore.clear()

            is TimerContract.Effect.StartTimer -> {
                val gen = ef.generation
                while (state.isRunning && timerGeneration == gen) {
                    delay(1_000L)
                    if (state.isRunning && timerGeneration == gen) {
                        dispatch(TimerContract.Executor.Tick)
                    }
                }
            }
        }
    }

    private fun ExecutorScope<TimerContract.Effect, TimerContract.Event>.advanceToNextPhase(): TimerContract.State {
        val nextIndex = state.currentPhaseIndex + 1
        val nextPhase = state.phases[nextIndex]

        return if (nextPhase is TimerPhase.Finished) {
            sendEffect(TimerContract.Effect.ClearState)
            sendEvent(TimerContract.Event.WorkoutFinished)
            state.copy(currentPhaseIndex = nextIndex, remainingSeconds = 0, isRunning = false)
        } else {
            val nextDuration = nextPhase.durationOrZero
            state.toSnapshot(phaseIndex = nextIndex, remainingSeconds = nextDuration)
                ?.let { sendEffect(TimerContract.Effect.SaveState(it)) }
            state.copy(currentPhaseIndex = nextIndex, remainingSeconds = nextDuration)
        }
    }
}

private val TimerPhase.durationOrZero: Int
    get() = when (this) {
        is TimerPhase.Work -> duration
        is TimerPhase.Rest -> duration
        TimerPhase.Finished -> 0
    }

private fun TimerContract.State.toSnapshot(
    phaseIndex: Int = currentPhaseIndex,
    remainingSeconds: Int = this.remainingSeconds,
    isRunning: Boolean = this.isRunning,
): TimerSnapshot? {
    val config = config ?: return null
    return TimerSnapshot(
        reps = config.reps,
        repDuration = config.repDuration,
        restDuration = config.restDuration,
        phaseIndex = phaseIndex,
        remainingSeconds = remainingSeconds,
        isRunning = isRunning,
    )
}
