package com.mobile.finsolve.app.movefasttdd.domain.model

data class TimerState(
    val config: WorkoutConfig,
    val phases: List<TimerPhase>,
    val currentPhaseIndex: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
) {
    val currentPhase: TimerPhase
        get() = phases.getOrElse(currentPhaseIndex) { TimerPhase.Finished }

    val isFinished: Boolean
        get() = currentPhase is TimerPhase.Finished

    val currentRep: Int
        get() = phases.take(currentPhaseIndex + 1).count { it is TimerPhase.Work }

    val totalReps: Int
        get() = config.reps

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
