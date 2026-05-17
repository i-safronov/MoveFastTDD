package com.mobile.finsolve.app.movefasttdd.domain.repository

import com.mobile.finsolve.app.movefasttdd.domain.model.TimerState
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeWorkoutTimerRepository : WorkoutTimerRepository {

    private val _state = MutableStateFlow<TimerState?>(null)
    override val state: StateFlow<TimerState?> = _state.asStateFlow()

    var tryRestoreResult = false
    var startCallCount = 0
    var pauseCallCount = 0
    var resumeCallCount = 0
    var cancelCallCount = 0

    // Emitted automatically when tryRestore() returns true
    var restoreEmitsState: TimerState? = null

    // Emitted automatically when start() is called
    var startEmitsState: TimerState? = null

    fun emit(state: TimerState?) {
        _state.value = state
    }

    override suspend fun tryRestore(): Boolean {
        if (tryRestoreResult) {
            restoreEmitsState?.let { _state.value = it }
            return true
        }
        return false
    }

    override suspend fun start(config: WorkoutConfig) {
        startCallCount++
        startEmitsState?.let { _state.value = it }
    }

    override suspend fun pause() { pauseCallCount++ }
    override suspend fun resume() { resumeCallCount++ }
    override suspend fun cancel() { cancelCallCount++ }
}
