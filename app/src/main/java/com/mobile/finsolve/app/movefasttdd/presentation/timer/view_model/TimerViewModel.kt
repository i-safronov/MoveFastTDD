package com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model

import android.util.Log
import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import com.mobile.finsolve.app.movefasttdd.domain.model.DataResult
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutConfigRepository
import com.mobile.finsolve.app.movefasttdd.domain.repository.WorkoutTimerRepository
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.APEXViewModel
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.EffectorScope
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.ExecutorScope
import com.mobile.finsolve.app.movefasttdd.service.WorkoutServiceStarter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val workoutConfigRepository: WorkoutConfigRepository,
    private val workoutTimerRepository: WorkoutTimerRepository,
    private val serviceStarter: WorkoutServiceStarter,
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
            sendEffect(TimerContract.Effect.Initialize)
            sendEffect(TimerContract.Effect.ObserveTimer)
            state
        }

        is TimerContract.Executor.ConfigLoaded -> {
            val config = ex.config ?: return@execute run {
                sendEvent(TimerContract.Event.NavigateBack)
                state.copy(isLoading = false)
            }
            sendEffect(TimerContract.Effect.StartTimer(config))
            state
        }

        is TimerContract.Executor.TimerStateUpdated -> {
            val timerState = ex.timerState ?: return@execute state
            val prevPhase = if (!state.isLoading) state.currentPhase else null
            val newState = timerState.toContractState()
            val newPhase = newState.currentPhase

            when {
                newPhase is TimerPhase.Finished && prevPhase !is TimerPhase.Finished -> {
                    sendEvent(TimerContract.Event.PlaySound(TimerContract.SoundType.END))
                    sendEvent(TimerContract.Event.WorkoutFinished)
                }

                prevPhase == null && timerState.isRunning ->
                    sendEvent(TimerContract.Event.PlaySound(TimerContract.SoundType.START))

                prevPhase is TimerPhase.Work && newPhase is TimerPhase.Rest ->
                    sendEvent(TimerContract.Event.PlaySound(TimerContract.SoundType.END))

                prevPhase is TimerPhase.Rest && newPhase is TimerPhase.Work ->
                    sendEvent(TimerContract.Event.PlaySound(TimerContract.SoundType.START))
            }

            newState
        }

        TimerContract.Executor.Stop -> {
            sendEffect(TimerContract.Effect.PauseTimer)
            state
        }

        TimerContract.Executor.Resume -> {
            sendEffect(TimerContract.Effect.ResumeTimer)
            state
        }

        TimerContract.Executor.Cancel -> {
            sendEffect(TimerContract.Effect.CancelTimer)
            sendEvent(TimerContract.Event.NavigateBack)
            state.copy(isRunning = false)
        }
    }

    override suspend fun EffectorScope<TimerContract.Executor>.affect(
        ef: TimerContract.Effect,
    ) {
        when (ef) {
            TimerContract.Effect.Initialize -> {
                val restored = workoutTimerRepository.tryRestore()
                if (restored) {
                    startServiceSafe()
                } else {
                    val config = when (val result = workoutConfigRepository.load()) {
                        is DataResult.Success -> result.value
                        DataResult.Error -> null
                    }
                    dispatch(TimerContract.Executor.ConfigLoaded(config))
                }
            }

            is TimerContract.Effect.StartTimer -> {
                workoutTimerRepository.start(ef.config)
                startServiceSafe()
            }

            TimerContract.Effect.ObserveTimer -> {
                workoutTimerRepository.state.collect { timerState ->
                    dispatch(TimerContract.Executor.TimerStateUpdated(timerState))
                }
            }

            TimerContract.Effect.PauseTimer -> workoutTimerRepository.pause()

            TimerContract.Effect.ResumeTimer -> workoutTimerRepository.resume()

            TimerContract.Effect.CancelTimer -> workoutTimerRepository.cancel()
        }
    }

    private fun startServiceSafe() {
        runCatching { serviceStarter.start() }
            .onFailure { Log.e("TimerViewModel", "Failed to start WorkoutForegroundService", it) }
    }
}
