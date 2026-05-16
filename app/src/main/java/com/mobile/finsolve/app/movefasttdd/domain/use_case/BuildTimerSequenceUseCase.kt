package com.mobile.finsolve.app.movefasttdd.domain.use_case

import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig

class BuildTimerSequenceUseCase {

    operator fun invoke(config: WorkoutConfig): List<TimerPhase> {
        val mutableList = mutableListOf<TimerPhase>()
        (0 until config.reps).forEach { i ->
            mutableList.add(TimerPhase.Work(config.repDuration))
            if (i != config.reps - 1) {
                mutableList.add(TimerPhase.Rest(config.restDuration))
            }
        }

        mutableList.add(TimerPhase.Finished)

        return mutableList
    }

}