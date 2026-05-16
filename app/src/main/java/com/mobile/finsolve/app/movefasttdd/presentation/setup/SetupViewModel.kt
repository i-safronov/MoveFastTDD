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

data object SetupContract {

    data class State(
        val reps: Int = 3,
        val repDuration: Int = 30,
        val restDuration: Int = 10,
        val isError: Boolean = false,
    ) : Apex.State

    sealed interface SetupExecutor : Apex.Executor {
        data class UpdateReps(val value: Int) : SetupExecutor
        data class UpdateRepDuration(val value: Int) : SetupExecutor
        data class UpdateRestDuration(val value: Int) : SetupExecutor
        data object Start : SetupExecutor
        data object LoadConfig : SetupExecutor
        data class ConfigLoaded(val config: WorkoutConfig?) : SetupExecutor
        data class ConfigSaved(val config: WorkoutConfig) : SetupExecutor
    }

    sealed interface SetupEvent : Apex.Event {
        data class NavigateToTimer(val config: WorkoutConfig) : SetupEvent
    }

    sealed interface SetupEffect : Apex.Effect {
        data object LoadConfig : SetupEffect
        data class SaveConfig(val config: WorkoutConfig) : SetupEffect
    }

}

class SetupViewModel(
    private val repository: WorkoutConfigRepository,
    private val validate: ValidateWorkoutConfigUseCase,
    dispatchers: DispatchersList = DispatchersList.Base(),
) : APEXViewModel<SetupContract.State, SetupContract.SetupExecutor, SetupContract.SetupEffect, SetupContract.SetupEvent>(
    initState = SetupContract.State(),
    dispatchers = dispatchers,
) {

    init {
        dispatch(SetupContract.SetupExecutor.LoadConfig)
    }

    override suspend fun ExecutorScope<SetupContract.SetupEffect, SetupContract.SetupEvent>.execute(
        ex: SetupContract.SetupExecutor
    ): SetupContract.State = when (ex) {
        SetupContract.SetupExecutor.LoadConfig -> {
            sendEffect(SetupContract.SetupEffect.LoadConfig)
            state
        }

        is SetupContract.SetupExecutor.ConfigLoaded -> {
            val config = ex.config ?: return@execute state
            state.copy(
                reps = config.reps,
                repDuration = config.repDuration,
                restDuration = config.restDuration,
            )
        }

        SetupContract.SetupExecutor.Start -> {
            val config = WorkoutConfig(
                reps = state.reps,
                repDuration = state.repDuration,
                restDuration = state.restDuration
            )
            when (
                validate(
                    config
                )
            ) {
                ValidationResult.Invalid -> state.copy(isError = true)

                ValidationResult.Valid -> {
                    sendEffect(SetupContract.SetupEffect.SaveConfig(config = config))
                    state
                }
            }
        }

        is SetupContract.SetupExecutor.UpdateReps ->
            state.copy(reps = ex.value, isError = false)

        is SetupContract.SetupExecutor.UpdateRepDuration ->
            state.copy(repDuration = ex.value, isError = false)

        is SetupContract.SetupExecutor.UpdateRestDuration ->
            state.copy(restDuration = ex.value, isError = false)

        is SetupContract.SetupExecutor.ConfigSaved -> {
            sendEvent(SetupContract.SetupEvent.NavigateToTimer(config = ex.config))
            state
        }
    }

    override suspend fun EffectorScope<SetupContract.SetupExecutor>.affect(
        ef: SetupContract.SetupEffect
    ) = when (ef) {
        SetupContract.SetupEffect.LoadConfig -> {
            val config = repository.load()
            dispatch(SetupContract.SetupExecutor.ConfigLoaded(config))
        }

        is SetupContract.SetupEffect.SaveConfig -> {
            repository.save(ef.config)
            dispatch(SetupContract.SetupExecutor.ConfigSaved(ef.config))
        }
    }

}