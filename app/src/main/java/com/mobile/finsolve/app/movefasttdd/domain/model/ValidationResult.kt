package com.mobile.finsolve.app.movefasttdd.domain.model

sealed class ValidationResult {

    data object Valid : ValidationResult()

    data class Invalid(
        val repsError: Boolean = false,
        val repDurationError: Boolean = false,
        val restDurationError: Boolean = false,
    ) : ValidationResult()
}