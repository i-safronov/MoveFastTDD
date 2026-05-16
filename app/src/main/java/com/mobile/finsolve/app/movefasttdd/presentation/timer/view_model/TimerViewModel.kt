package com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerSnapshot
import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerStateDataStore
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.use_case.BuildTimerSequenceUseCase
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.APEXViewModel
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.Apex
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.EffectorScope
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.ExecutorScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import javax.inject.Inject

object TimerContract {

    data class State(
        val config: WorkoutConfig? = null,
        val phases: List<TimerPhase> = emptyList(),
        val currentPhaseIndex: Int = 0,
        val remainingSeconds: Int = 0,
        val isRunning: Boolean = false,
    ) : Apex.State {
        val currentPhase: TimerPhase
            get() = phases.getOrElse(currentPhaseIndex) { TimerPhase.Finished }

        val currentRep: Int
            get() = phases.take(currentPhaseIndex + 1).count { it is TimerPhase.Work }

        val totalReps: Int
            get() = config?.reps ?: phases.count { it is TimerPhase.Work }

        val phaseDuration: Int
            get() = when (val p = currentPhase) {
                is TimerPhase.Work -> p.duration
                is TimerPhase.Rest -> p.duration
                TimerPhase.Finished -> 0
            }

        val progress: Float
            get() = if (phaseDuration == 0) 1f
            else (phaseDuration - remainingSeconds).toFloat() / phaseDuration
    }

    sealed interface Executor : Apex.Executor {
        data object Load : Executor
        data class StateLoaded(val snapshot: TimerSnapshot?) : Executor
        data class Init(val config: WorkoutConfig) : Executor
        data object Tick : Executor
        data object Stop : Executor
        data object Resume : Executor
        data object Cancel : Executor
    }

    sealed interface Event : Apex.Event {
        data object NavigateBack : Event
        data object WorkoutFinished : Event
    }

    sealed interface Effect : Apex.Effect {
        data object LoadState : Effect
        data class SaveState(val snapshot: TimerSnapshot) : Effect
        data object ClearState : Effect
        data object StartTimer : Effect
    }
}

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val buildTimerSequence: BuildTimerSequenceUseCase,
    private val timerStateDataStore: TimerStateDataStore,
    dispatchers: DispatchersList,
) : APEXViewModel<TimerContract.State, TimerContract.Executor, TimerContract.Effect, TimerContract.Event>(
    initState = TimerContract.State(),
    dispatchers = dispatchers,
) {

    init {
        dispatch(TimerContract.Executor.Load)
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
                val config =
                    WorkoutConfig(snapshot.reps, snapshot.repDuration, snapshot.restDuration)
                val phases = buildTimerSequence(config)
                sendEffect(TimerContract.Effect.StartTimer)
                state.copy(
                    config = config,
                    phases = phases,
                    currentPhaseIndex = snapshot.phaseIndex,
                    remainingSeconds = snapshot.remainingSeconds,
                    isRunning = true,
                )
            } ?: state

        is TimerContract.Executor.Init -> {
            val phases = buildTimerSequence(ex.config)
            val firstDuration = phases.first().durationOrZero
            sendEffect(
                TimerContract.Effect.SaveState(
                    TimerSnapshot(
                        reps = ex.config.reps,
                        repDuration = ex.config.repDuration,
                        restDuration = ex.config.restDuration,
                        phaseIndex = 0,
                        remainingSeconds = firstDuration,
                    )
                )
            )
            sendEffect(TimerContract.Effect.StartTimer)
            state.copy(
                config = ex.config,
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

        TimerContract.Executor.Stop ->
            state.copy(isRunning = false)

        TimerContract.Executor.Resume -> {
            sendEffect(TimerContract.Effect.StartTimer)
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

            is TimerContract.Effect.SaveState -> timerStateDataStore.save(ef.snapshot)

            TimerContract.Effect.ClearState -> timerStateDataStore.clear()

            TimerContract.Effect.StartTimer -> {
                while (state.isRunning) {
                    delay(1_000L)
                    if (state.isRunning) dispatch(TimerContract.Executor.Tick)
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
): TimerSnapshot? {
    val config = config ?: return null
    return TimerSnapshot(
        reps = config.reps,
        repDuration = config.repDuration,
        restDuration = config.restDuration,
        phaseIndex = phaseIndex,
        remainingSeconds = remainingSeconds,
    )
}
