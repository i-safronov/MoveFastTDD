package com.mobile.finsolve.app.movefasttdd.domain.model

sealed class ValidationResult {
    
    data object Valid: ValidationResult()
    
    data object Invalid: ValidationResult()
    
}