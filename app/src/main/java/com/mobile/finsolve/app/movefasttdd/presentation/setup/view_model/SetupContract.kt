package com.mobile.finsolve.app.movefasttdd.presentation.setup.view_model

import com.mobile.finsolve.app.movefasttdd.data.datastore.WorkoutDraft
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.Apex

object SetupContract {

    data class State(
        val reps: Int = DEFAULT_REPS,
        val repDuration: Int = DEFAULT_REP_DURATION,
        val restDuration: Int = DEFAULT_REST_DURATION,
        val repsError: Boolean = false,
        val repDurationError: Boolean = false,
        val restDurationError: Boolean = false,
        val saveError: Boolean = false,
    ) : Apex.State {
        val hasError: Boolean get() = repsError || repDurationError || restDurationError
    }

    sealed interface Executor : Apex.Executor {
        data class UpdateReps(val value: Int) : Executor
        data class UpdateRepDuration(val value: Int) : Executor
        data class UpdateRestDuration(val value: Int) : Executor
        data object Start : Executor
        data object LoadDraft : Executor
        data class DraftLoaded(val draft: WorkoutDraft?, val savedConfig: WorkoutConfig?) : Executor
        data class ConfigSaved(val config: WorkoutConfig) : Executor
        data object SaveFailed : Executor
    }

    sealed interface Event : Apex.Event {
        data class NavigateToTimer(val config: WorkoutConfig) : Event
    }

    sealed interface Effect : Apex.Effect {
        data object LoadDraft : Effect
        data class SaveDraft(val draft: WorkoutDraft) : Effect
        data class SaveConfig(val config: WorkoutConfig) : Effect
    }

    internal const val DEFAULT_REPS = 3
    internal const val DEFAULT_REP_DURATION = 30
    internal const val DEFAULT_REST_DURATION = 10
}
