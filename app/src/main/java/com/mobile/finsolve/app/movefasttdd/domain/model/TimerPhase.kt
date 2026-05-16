package com.mobile.finsolve.app.movefasttdd.domain.model

sealed class TimerPhase {

    data class Work(
        val duration: Int
    ): TimerPhase()

    data class Rest(
        val duration: Int
    ): TimerPhase()

    data object Finished: TimerPhase()

}