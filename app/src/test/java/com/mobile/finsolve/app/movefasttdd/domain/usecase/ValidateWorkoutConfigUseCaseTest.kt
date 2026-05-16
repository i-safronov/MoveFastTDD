package com.mobile.finsolve.app.movefasttdd.domain.usecase

import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.model.ValidationResult
import com.mobile.finsolve.app.movefasttdd.domain.use_case.ValidateWorkoutConfigUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateWorkoutConfigUseCaseTest {

    private lateinit var useCase: ValidateWorkoutConfigUseCase

    @Before
    fun setup() {
        useCase = ValidateWorkoutConfigUseCase()
    }

    // region Valid config

    @Test
    fun `valid config returns Valid`() {
        val config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `rest duration zero is valid — no rest between reps`() {
        val config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 0)
        val result = useCase(config)
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `single rep with minimum duration is valid`() {
        val config = WorkoutConfig(reps = 1, repDuration = 1, restDuration = 0)
        val result = useCase(config)
        assertEquals(ValidationResult.Valid, result)
    }

    // endregion

    // region Invalid reps

    @Test
    fun `reps zero returns Invalid`() {
        val config = WorkoutConfig(reps = 0, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `reps negative returns Invalid`() {
        val config = WorkoutConfig(reps = -1, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        assertTrue(result is ValidationResult.Invalid)
    }

    // endregion

    // region Invalid repDuration

    @Test
    fun `rep duration zero returns Invalid`() {
        val config = WorkoutConfig(reps = 3, repDuration = 0, restDuration = 10)
        val result = useCase(config)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `rep duration negative returns Invalid`() {
        val config = WorkoutConfig(reps = 3, repDuration = -10, restDuration = 10)
        val result = useCase(config)
        assertTrue(result is ValidationResult.Invalid)
    }

    // endregion

    // region Invalid restDuration

    @Test
    fun `rest duration negative returns Invalid`() {
        val config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = -1)
        val result = useCase(config)
        assertTrue(result is ValidationResult.Invalid)
    }

    // endregion

    // region Multiple invalid fields

    @Test
    fun `all fields invalid returns Invalid`() {
        val config = WorkoutConfig(reps = 0, repDuration = 0, restDuration = -1)
        val result = useCase(config)
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `reps and rep duration invalid returns Invalid`() {
        val config = WorkoutConfig(reps = -1, repDuration = -1, restDuration = 10)
        val result = useCase(config)
        assertTrue(result is ValidationResult.Invalid)
    }

    // endregion
}
