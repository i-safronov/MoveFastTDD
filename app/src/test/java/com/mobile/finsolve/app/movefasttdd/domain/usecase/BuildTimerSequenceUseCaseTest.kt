package com.mobile.finsolve.app.movefasttdd.domain.usecase

import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.domain.use_case.BuildTimerSequenceUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BuildTimerSequenceUseCaseTest {

    private lateinit var useCase: BuildTimerSequenceUseCase

    @Before
    fun setup() {
        useCase = BuildTimerSequenceUseCase()
    }

    // region Sequence structure

    @Test
    fun `single rep produces Work then Finished`() {
        val config = WorkoutConfig(reps = 1, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        assertEquals(listOf(TimerPhase.Work(30), TimerPhase.Finished), result)
    }

    @Test
    fun `two reps produces Work Rest Work Finished`() {
        val config = WorkoutConfig(reps = 2, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        assertEquals(
            listOf(
                TimerPhase.Work(30),
                TimerPhase.Rest(10),
                TimerPhase.Work(30),
                TimerPhase.Finished
            ),
            result
        )
    }

    @Test
    fun `three reps produces correct interleaved sequence`() {
        val config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        assertEquals(
            listOf(
                TimerPhase.Work(30),
                TimerPhase.Rest(10),
                TimerPhase.Work(30),
                TimerPhase.Rest(10),
                TimerPhase.Work(30),
                TimerPhase.Finished
            ),
            result
        )
    }

    @Test
    fun `no rest after last rep`() {
        val config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        val lastMeaningfulPhase = result[result.size - 2]
        assertTrue(lastMeaningfulPhase is TimerPhase.Work)
    }

    @Test
    fun `always ends with Finished`() {
        val config = WorkoutConfig(reps = 5, repDuration = 20, restDuration = 5)
        val result = useCase(config)
        assertEquals(TimerPhase.Finished, result.last())
    }

    // endregion

    // region Sequence size

    @Test
    fun `sequence size for N reps is 2N`() {
        // N Work phases + (N-1) Rest phases + 1 Finished = 2N
        listOf(1, 2, 3, 5, 10).forEach { reps ->
            val config = WorkoutConfig(reps = reps, repDuration = 30, restDuration = 10)
            val result = useCase(config)
            assertEquals("Expected ${2 * reps} phases for $reps reps", 2 * reps, result.size)
        }
    }

    @Test
    fun `work phases count equals reps count`() {
        val config = WorkoutConfig(reps = 4, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        val workCount = result.filterIsInstance<TimerPhase.Work>().size
        assertEquals(4, workCount)
    }

    @Test
    fun `rest phases count is reps minus one`() {
        val config = WorkoutConfig(reps = 4, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        val restCount = result.filterIsInstance<TimerPhase.Rest>().size
        assertEquals(3, restCount)
    }

    // endregion

    // region Durations

    @Test
    fun `all Work phases have correct duration`() {
        val config = WorkoutConfig(reps = 3, repDuration = 45, restDuration = 15)
        val result = useCase(config)
        result.filterIsInstance<TimerPhase.Work>().forEach { phase ->
            assertEquals(45, phase.duration)
        }
    }

    @Test
    fun `all Rest phases have correct duration`() {
        val config = WorkoutConfig(reps = 3, repDuration = 45, restDuration = 15)
        val result = useCase(config)
        result.filterIsInstance<TimerPhase.Rest>().forEach { phase ->
            assertEquals(15, phase.duration)
        }
    }

    @Test
    fun `zero rest duration produces Rest phases with duration zero`() {
        val config = WorkoutConfig(reps = 2, repDuration = 30, restDuration = 0)
        val result = useCase(config)
        result.filterIsInstance<TimerPhase.Rest>().forEach { phase ->
            assertEquals(0, phase.duration)
        }
    }

    // endregion

    // region Order

    @Test
    fun `sequence always starts with Work phase`() {
        val config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        assertTrue(result.first() is TimerPhase.Work)
    }

    @Test
    fun `Work and Rest phases alternate correctly`() {
        val config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10)
        val result = useCase(config)
        val withoutFinished = result.dropLast(1)
        withoutFinished.forEachIndexed { index, phase ->
            if (index % 2 == 0) assertTrue("Index $index should be Work", phase is TimerPhase.Work)
            else assertTrue("Index $index should be Rest", phase is TimerPhase.Rest)
        }
    }

    // endregion
}
