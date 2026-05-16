package com.mobile.finsolve.app.movefasttdd.presentation.setup.view_model

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.data.datastore.WorkoutDraft
import com.mobile.finsolve.app.movefasttdd.data.datastore.WorkoutDraftDataStore
import com.mobile.finsolve.app.movefasttdd.domain.model.DataResult
import com.mobile.finsolve.app.movefasttdd.domain.model.ValidationResult
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.model.fold
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository
import com.mobile.finsolve.app.movefasttdd.domain.use_case.ValidateWorkoutConfigUseCase
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.APEXViewModel
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.Apex
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.EffectorScope
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.ExecutorScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

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
            val source = ex.draft?.toConfig() ?: ex.savedConfig
            source?.let {
                state.copy(reps = it.reps, repDuration = it.repDuration, restDuration = it.restDuration)
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
            val next = state.copy(reps = ex.value, repsError = false, saveError = false)
            sendEffect(SetupContract.Effect.SaveDraft(next.toDraft()))
            next
        }

        is SetupContract.Executor.UpdateRepDuration -> {
            val next = state.copy(repDuration = ex.value, repDurationError = false, saveError = false)
            sendEffect(SetupContract.Effect.SaveDraft(next.toDraft()))
            next
        }

        is SetupContract.Executor.UpdateRestDuration -> {
            val next = state.copy(restDuration = ex.value, restDurationError = false, saveError = false)
            sendEffect(SetupContract.Effect.SaveDraft(next.toDraft()))
            next
        }

        is SetupContract.Executor.ConfigSaved -> {
            sendEvent(SetupContract.Event.NavigateToTimer(ex.config))
            state.copy(saveError = false)
        }

        SetupContract.Executor.SaveFailed ->
            state.copy(saveError = true)
    }

    override suspend fun EffectorScope<SetupContract.Executor>.affect(
        ef: SetupContract.Effect,
    ) = when (ef) {
        SetupContract.Effect.LoadDraft -> {
            val draft = draftDataStore.load()
            val savedConfig = if (draft == null) {
                repository.load().let { result ->
                    if (result is DataResult.Success) result.value else null
                }
            } else null
            dispatch(SetupContract.Executor.DraftLoaded(draft, savedConfig))
        }

        is SetupContract.Effect.SaveDraft ->
            draftDataStore.save(ef.draft)

        is SetupContract.Effect.SaveConfig ->
            repository.save(ef.config).fold(
                onSuccess = {
                    draftDataStore.clear()
                    dispatch(SetupContract.Executor.ConfigSaved(ef.config))
                },
                onError = {
                    dispatch(SetupContract.Executor.SaveFailed)
                },
            )
    }

    private fun currentConfig() = WorkoutConfig(
        reps = state.reps,
        repDuration = state.repDuration,
        restDuration = state.restDuration,
    )
}

private fun WorkoutDraft.toConfig() = WorkoutConfig(reps, repDuration, restDuration)

private fun SetupContract.State.toDraft() = WorkoutDraft(reps, repDuration, restDuration)
