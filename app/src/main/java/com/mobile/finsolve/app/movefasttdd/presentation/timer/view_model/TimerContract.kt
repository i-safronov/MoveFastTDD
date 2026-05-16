package com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model

import com.mobile.finsolve.app.movefasttdd.data.datastore.TimerSnapshot
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.Apex

object TimerContract {

    data class State(
        val isLoading: Boolean = true,
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
        data class ConfigLoaded(val config: WorkoutConfig?) : Executor
        data object Tick : Executor
        data object Stop : Executor
        data object Resume : Executor
        data object Cancel : Executor
    }

    enum class SoundType { START, END }

    sealed interface Event : Apex.Event {
        data object NavigateBack : Event
        data object WorkoutFinished : Event
        data class PlaySound(val type: SoundType) : Event
    }

    sealed interface Effect : Apex.Effect {
        data object LoadState : Effect
        data object LoadConfig : Effect
        data class SaveState(val snapshot: TimerSnapshot) : Effect
        data object ClearState : Effect
        data class StartTimer(val generation: Int) : Effect
    }
}