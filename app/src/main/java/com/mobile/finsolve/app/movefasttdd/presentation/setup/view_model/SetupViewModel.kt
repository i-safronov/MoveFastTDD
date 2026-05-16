package com.mobile.finsolve.app.movefasttdd.presentation.setup.view_model

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.WorkoutDraft
import com.mobile.finsolve.app.movefasttdd.data.datastore.WorkoutDraftDataStore
import com.mobile.finsolve.app.movefasttdd.domain.model.ValidationResult
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository
import com.mobile.finsolve.app.movefasttdd.domain.use_case.ValidateWorkoutConfigUseCase
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.APEXViewModel
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.Apex
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.EffectorScope
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.ExecutorScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

object SetupContract {

    data class State(
        val reps: Int = DEFAULT_REPS,
        val repDuration: Int = DEFAULT_REP_DURATION,
        val restDuration: Int = DEFAULT_REST_DURATION,
        val repsError: Boolean = false,
        val repDurationError: Boolean = false,
        val restDurationError: Boolean = false,
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

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repository: WorkoutConfigRepository,
    private val validate: ValidateWorkoutConfigUseCase,
    private val draftDataStore: WorkoutDraftDataStore,
    dispatchers: DispatchersList,
) : APEXViewModel<SetupContract.State, SetupContract.Executor, SetupContract.Effect, SetupContract.Event>(
    initState = SetupContract.State(),
    dispatchers = dispatchers,
) {

    init {
        dispatch(SetupContract.Executor.LoadDraft)
    }

    override suspend fun ExecutorScope<SetupContract.Effect, SetupContract.Event>.execute(
        ex: SetupContract.Executor,
    ): SetupContract.State = when (ex) {

        SetupContract.Executor.LoadDraft -> {
            sendEffect(SetupContract.Effect.LoadDraft)
            state
        }

        is SetupContract.Executor.DraftLoaded -> {
            // Приоритет: черновик > последний сохранённый конфиг > дефолт
            val source = ex.draft?.toConfig() ?: ex.savedConfig
            source?.let {
                state.copy(
                    reps = it.reps,
                    repDuration = it.repDuration,
                    restDuration = it.restDuration,
                )
            } ?: state
        }

        SetupContract.Executor.Start -> when (val result = validate(currentConfig())) {
            is ValidationResult.Invalid -> state.copy(
                repsError = result.repsError,
                repDurationError = result.repDurationError,
                restDurationError = result.restDurationError,
            )
            ValidationResult.Valid -> {
                sendEffect(SetupContract.Effect.SaveConfig(currentConfig()))
                state
            }
        }

        is SetupContract.Executor.UpdateReps -> {
            val next = state.copy(reps = ex.value, repsError = false)
            sendEffect(SetupContract.Effect.SaveDraft(next.toDraft()))
            next
        }

        is SetupContract.Executor.UpdateRepDuration -> {
            val next = state.copy(repDuration = ex.value, repDurationError = false)
            sendEffect(SetupContract.Effect.SaveDraft(next.toDraft()))
            next
        }

        is SetupContract.Executor.UpdateRestDuration -> {
            val next = state.copy(restDuration = ex.value, restDurationError = false)
            sendEffect(SetupContract.Effect.SaveDraft(next.toDraft()))
            next
        }

        is SetupContract.Executor.ConfigSaved -> {
            sendEvent(SetupContract.Event.NavigateToTimer(ex.config))
            state
        }
    }

    override suspend fun EffectorScope<SetupContract.Executor>.affect(
        ef: SetupContract.Effect,
    ) = when (ef) {
        SetupContract.Effect.LoadDraft -> {
            val draft = draftDataStore.load()
            val savedConfig = if (draft == null) repository.load() else null
            dispatch(SetupContract.Executor.DraftLoaded(draft, savedConfig))
        }

        is SetupContract.Effect.SaveDraft ->
            draftDataStore.save(ef.draft)

        is SetupContract.Effect.SaveConfig -> {
            repository.save(ef.config)
            draftDataStore.clear()
            dispatch(SetupContract.Executor.ConfigSaved(ef.config))
        }
    }

    private fun currentConfig() = WorkoutConfig(
        reps = state.reps,
        repDuration = state.repDuration,
        restDuration = state.restDuration,
    )
}

private fun WorkoutDraft.toConfig() = WorkoutConfig(reps, repDuration, restDuration)

private fun SetupContract.State.toDraft() = WorkoutDraft(reps, repDuration, restDuration)
