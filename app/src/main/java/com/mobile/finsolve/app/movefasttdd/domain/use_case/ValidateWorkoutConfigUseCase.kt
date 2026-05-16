package com.mobile.finsolve.app.movefasttdd.domain.use_case

import com.mobile.finsolve.app.movefasttdd.domain.model.ValidationResult
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig

class ValidateWorkoutConfigUseCase {
    operator fun invoke(config: WorkoutConfig): ValidationResult {
        return if (config.reps <= 0 || config.repDuration <= 0 || config.restDuration < 0)
            ValidationResult.Invalid
        else ValidationResult.Valid
    }
}