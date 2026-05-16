package com.mobile.finsolve.app.movefasttdd.presentation.setup

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.domain.model.ValidationResult
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository
import com.mobile.finsolve.app.movefasttdd.domain.use_case.ValidateWorkoutConfigUseCase
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.APEXViewModel
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.Apex
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.EffectorScope
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.ExecutorScope

object SetupContract {

    data class State(
        val reps: Int = 3,
        val repDuration: Int = 30,
        val restDuration: Int = 10,
        val isError: Boolean = false,
    ) : Apex.State

    sealed interface Executor : Apex.Executor {
        data class UpdateReps(val value: Int) : Executor
        data class UpdateRepDuration(val value: Int) : Executor
        data class UpdateRestDuration(val value: Int) : Executor
        data object Start : Executor
        data object LoadConfig : Executor
        data class ConfigLoaded(val config: WorkoutConfig?) : Executor
        data class ConfigSaved(val config: WorkoutConfig) : Executor
    }

    sealed interface Event : Apex.Event {
        data class NavigateToTimer(val config: WorkoutConfig) : Event
    }

    sealed interface Effect : Apex.Effect {
        data object LoadConfig : Effect
        data class SaveConfig(val config: WorkoutConfig) : Effect
    }
}

class SetupViewModel(
    private val repository: WorkoutConfigRepository,
    private val validate: ValidateWorkoutConfigUseCase,
    dispatchers: DispatchersList = DispatchersList.Base(),
) : APEXViewModel<SetupContract.State, SetupContract.Executor, SetupContract.Effect, SetupContract.Event>(
    initState = SetupContract.State(),
    dispatchers = dispatchers,
) {

    init {
        dispatch(SetupContract.Executor.LoadConfig)
    }

    override suspend fun ExecutorScope<SetupContract.Effect, SetupContract.Event>.execute(
        ex: SetupContract.Executor,
    ): SetupContract.State = when (ex) {

        SetupContract.Executor.LoadConfig -> {
            sendEffect(SetupContract.Effect.LoadConfig)
            state
        }

        is SetupContract.Executor.ConfigLoaded ->
            ex.config?.let {
                state.copy(
                    reps = it.reps,
                    repDuration = it.repDuration,
                    restDuration = it.restDuration
                )
            }
                ?: state

        SetupContract.Executor.Start -> when (validate(currentConfig())) {
            ValidationResult.Invalid -> state.copy(isError = true)
            ValidationResult.Valid -> {
                sendEffect(SetupContract.Effect.SaveConfig(currentConfig()))
                state
            }
        }

        is SetupContract.Executor.UpdateReps -> state.copy(reps = ex.value, isError = false)

        is SetupContract.Executor.UpdateRepDuration -> state.copy(
            repDuration = ex.value,
            isError = false
        )

        is SetupContract.Executor.UpdateRestDuration -> state.copy(
            restDuration = ex.value,
            isError = false
        )

        is SetupContract.Executor.ConfigSaved -> {
            sendEvent(SetupContract.Event.NavigateToTimer(ex.config))
            state
        }
    }

    override suspend fun EffectorScope<SetupContract.Executor>.affect(
        ef: SetupContract.Effect,
    ) = when (ef) {
        SetupContract.Effect.LoadConfig ->
            dispatch(SetupContract.Executor.ConfigLoaded(repository.load()))

        is SetupContract.Effect.SaveConfig -> {
            repository.save(ef.config)
            dispatch(SetupContract.Executor.ConfigSaved(ef.config))
        }
    }

    private fun currentConfig() = WorkoutConfig(
        reps = state.reps,
        repDuration = state.repDuration,
        restDuration = state.restDuration,
    )
}
