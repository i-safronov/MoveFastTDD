package com.mobile.finsolve.app.movefasttdd.domain.repository

import com.mobile.finsolve.app.movefasttdd.domain.model.TimerState
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import kotlinx.coroutines.flow.StateFlow

interface WorkoutTimerRepository {
    val state: StateFlow<TimerState?>

    suspend fun tryRestore(): Boolean
    suspend fun start(config: WorkoutConfig)
    suspend fun pause()
    suspend fun resume()
    suspend fun cancel()
}
