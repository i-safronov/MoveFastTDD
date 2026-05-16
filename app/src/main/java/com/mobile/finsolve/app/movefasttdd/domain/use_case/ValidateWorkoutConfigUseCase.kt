package com.mobile.finsolve.app.movefasttdd.domain.use_case

import com.mobile.finsolve.app.movefasttdd.domain.model.ValidationResult
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import javax.inject.Inject

class ValidateWorkoutConfigUseCase @Inject constructor() {
    operator fun invoke(config: WorkoutConfig): ValidationResult {
        val repsError = config.reps <= 0
        val repDurationError = config.repDuration <= 0
        val restDurationError = config.restDuration < 0
        return if (repsError || repDurationError || restDurationError)
            ValidationResult.Invalid(repsError, repDurationError, restDurationError)
        else
            ValidationResult.Valid
    }
}