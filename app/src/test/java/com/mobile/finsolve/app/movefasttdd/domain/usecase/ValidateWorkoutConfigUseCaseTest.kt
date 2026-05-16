package com.mobile.finsolve.app.movefasttdd.domain.usecase

import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.model.ValidationResult
import com.mobile.finsolve.app.movefasttdd.domain.use_case.ValidateWorkoutConfigUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val result = useCase(WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10))
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `rest duration zero is valid — no rest between reps`() {
        val result = useCase(WorkoutConfig(reps = 3, repDuration = 30, restDuration = 0))
        assertEquals(ValidationResult.Valid, result)
    }

    @Test
    fun `single rep with minimum duration is valid`() {
        val result = useCase(WorkoutConfig(reps = 1, repDuration = 1, restDuration = 0))
        assertEquals(ValidationResult.Valid, result)
    }

    // endregion

    // region Invalid reps

    @Test
    fun `reps zero returns Invalid with repsError`() {
        val result = useCase(WorkoutConfig(reps = 0, repDuration = 30, restDuration = 10))
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).repsError)
        assertFalse(result.repDurationError)
        assertFalse(result.restDurationError)
    }

    @Test
    fun `reps negative returns Invalid with repsError`() {
        val result = useCase(WorkoutConfig(reps = -1, repDuration = 30, restDuration = 10))
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).repsError)
    }

    // endregion

    // region Invalid repDuration

    @Test
    fun `rep duration zero returns Invalid with repDurationError`() {
        val result = useCase(WorkoutConfig(reps = 3, repDuration = 0, restDuration = 10))
        assertTrue(result is ValidationResult.Invalid)
        assertFalse((result as ValidationResult.Invalid).repsError)
        assertTrue(result.repDurationError)
        assertFalse(result.restDurationError)
    }

    @Test
    fun `rep duration negative returns Invalid with repDurationError`() {
        val result = useCase(WorkoutConfig(reps = 3, repDuration = -10, restDuration = 10))
        assertTrue(result is ValidationResult.Invalid)
        assertTrue((result as ValidationResult.Invalid).repDurationError)
    }

    // endregion

    // region Invalid restDuration

    @Test
    fun `rest duration negative returns Invalid with restDurationError`() {
        val result = useCase(WorkoutConfig(reps = 3, repDuration = 30, restDuration = -1))
        assertTrue(result is ValidationResult.Invalid)
        assertFalse((result as ValidationResult.Invalid).repsError)
        assertFalse(result.repDurationError)
        assertTrue(result.restDurationError)
    }

    // endregion

    // region Multiple invalid fields

    @Test
    fun `all fields invalid returns Invalid with all errors set`() {
        val result = useCase(WorkoutConfig(reps = 0, repDuration = 0, restDuration = -1))
        assertTrue(result is ValidationResult.Invalid)
        with(result as ValidationResult.Invalid) {
            assertTrue(repsError)
            assertTrue(repDurationError)
            assertTrue(restDurationError)
        }
    }

    @Test
    fun `reps and rep duration invalid returns Invalid with both errors set`() {
        val result = useCase(WorkoutConfig(reps = -1, repDuration = -1, restDuration = 10))
        assertTrue(result is ValidationResult.Invalid)
        with(result as ValidationResult.Invalid) {
            assertTrue(repsError)
            assertTrue(repDurationError)
            assertFalse(restDurationError)
        }
    }

    // endregion
}
