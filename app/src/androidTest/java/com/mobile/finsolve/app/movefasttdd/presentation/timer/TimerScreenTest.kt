package com.mobile.finsolve.app.movefasttdd.presentation.timer

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model.TimerContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    // region Helpers

    private val defaultPhases = listOf(
        TimerPhase.Work(30),
        TimerPhase.Rest(10),
        TimerPhase.Work(30),
        TimerPhase.Finished,
    )

    private fun workState(
        remainingSeconds: Int = 30,
        currentPhaseIndex: Int = 0,
        isRunning: Boolean = true,
    ) = TimerContract.State(
        isLoading = false,
        config = WorkoutConfig(reps = 2, repDuration = 30, restDuration = 10),
        phases = defaultPhases,
        currentPhaseIndex = currentPhaseIndex,
        remainingSeconds = remainingSeconds,
        isRunning = isRunning,
    )

    private fun restState(remainingSeconds: Int = 10) = TimerContract.State(
        isLoading = false,
        config = WorkoutConfig(reps = 2, repDuration = 30, restDuration = 10),
        phases = defaultPhases,
        currentPhaseIndex = 1,
        remainingSeconds = remainingSeconds,
        isRunning = true,
    )

    private fun finishedState() = TimerContract.State(
        isLoading = false,
        config = WorkoutConfig(reps = 2, repDuration = 30, restDuration = 10),
        phases = defaultPhases,
        currentPhaseIndex = 3,
        remainingSeconds = 0,
        isRunning = false,
    )

    private fun setContent(
        state: TimerContract.State,
        onStop: () -> Unit = {},
        onResume: () -> Unit = {},
        onCancel: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            BackHandler { onCancel() }
            TimerContent(
                state = state,
                onStop = onStop,
                onResume = onResume,
                onCancel = onCancel,
            )
        }
    }

    // endregion

    // region Display — Work phase

    @Test
    fun workPhase_showsWorkLabel() {
        setContent(state = workState())
        composeTestRule.onNodeWithTag(TimerScreenTags.PHASE_LABEL).assertIsDisplayed()
        composeTestRule.onNodeWithText("WORK").assertIsDisplayed()
    }

    @Test
    fun workPhase_showsCountdown() {
        setContent(state = workState(remainingSeconds = 30))
        composeTestRule.onNodeWithTag(TimerScreenTags.COUNTDOWN).assertIsDisplayed()
        composeTestRule.onNodeWithText("30").assertIsDisplayed()
    }

    @Test
    fun workPhase_showsRepCounter() {
        setContent(state = workState())
        composeTestRule.onNodeWithTag(TimerScreenTags.REP_COUNTER).assertIsDisplayed()
        composeTestRule.onNodeWithText("Rep 1 of 2").assertIsDisplayed()
    }

    @Test
    fun workPhase_showsPauseButton_whenRunning() {
        setContent(state = workState(isRunning = true))
        composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun workPhase_showsCancelButton() {
        setContent(state = workState())
        composeTestRule.onNodeWithTag(TimerScreenTags.CANCEL_BUTTON).assertIsDisplayed()
    }

    @Test
    fun workPhase_doesNotShowFinishedMessage() {
        setContent(state = workState())
        composeTestRule.onNodeWithTag(TimerScreenTags.FINISHED_MESSAGE).assertDoesNotExist()
    }

    @Test
    fun workPhase_doesNotShowDoneButton() {
        setContent(state = workState())
        composeTestRule.onNodeWithTag(TimerScreenTags.DONE_BUTTON).assertDoesNotExist()
    }

    @Test
    fun workPhase_doesNotShowResumeButton() {
        setContent(state = workState(isRunning = true))
        composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).assertDoesNotExist()
    }

    // endregion

    // region Display — Rest phase

    @Test
    fun restPhase_showsRestLabel() {
        setContent(state = restState())
        composeTestRule.onNodeWithText("REST").assertIsDisplayed()
    }
 
    @Test
    fun restPhase_showsCorrectRepCounter() {
        // index=1 (Rest) — пользователь выполнил 1 Work → Rep 1 of 2
        setContent(state = restState())
        composeTestRule.onNodeWithText("Rep 1 of 2").assertIsDisplayed()
    }

    @Test
    fun restPhase_showsRestCountdown() {
        setContent(state = restState(remainingSeconds = 7))
        composeTestRule.onNodeWithText("7").assertIsDisplayed()
    }

    // endregion

    // region Display — Paused state

    @Test
    fun paused_showsResumeButton() {
        setContent(state = workState(isRunning = false))
        composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).assertIsDisplayed()
    }

    @Test
    fun paused_doesNotShowPauseButton() {
        setContent(state = workState(isRunning = false))
        composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).assertDoesNotExist()
    }

    @Test
    fun paused_stillShowsCancelButton() {
        setContent(state = workState(isRunning = false))
        composeTestRule.onNodeWithTag(TimerScreenTags.CANCEL_BUTTON).assertIsDisplayed()
    }

    @Test
    fun paused_stillShowsCountdown() {
        setContent(state = workState(remainingSeconds = 15, isRunning = false))
        composeTestRule.onNodeWithText("15").assertIsDisplayed()
    }

    // endregion

    // region Display — Finished state

    @Test
    fun finished_showsFinishedMessage() {
        setContent(state = finishedState())
        composeTestRule.onNodeWithTag(TimerScreenTags.FINISHED_MESSAGE).assertIsDisplayed()
        composeTestRule.onNodeWithText("Workout Complete!").assertIsDisplayed()
    }

    @Test
    fun finished_showsGreatJob() {
        setContent(state = finishedState())
        composeTestRule.onNodeWithText("Great job!").assertIsDisplayed()
    }

    @Test
    fun finished_showsDoneButton() {
        setContent(state = finishedState())
        composeTestRule.onNodeWithTag(TimerScreenTags.DONE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun finished_doesNotShowPhaseLabel() {
        setContent(state = finishedState())
        composeTestRule.onNodeWithTag(TimerScreenTags.PHASE_LABEL).assertDoesNotExist()
    }

    @Test
    fun finished_doesNotShowCountdown() {
        setContent(state = finishedState())
        composeTestRule.onNodeWithTag(TimerScreenTags.COUNTDOWN).assertDoesNotExist()
    }

    @Test
    fun finished_doesNotShowRepCounter() {
        setContent(state = finishedState())
        composeTestRule.onNodeWithTag(TimerScreenTags.REP_COUNTER).assertDoesNotExist()
    }

    @Test
    fun finished_doesNotShowPauseButton() {
        setContent(state = finishedState())
        composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).assertDoesNotExist()
    }

    @Test
    fun finished_doesNotShowCancelButton() {
        setContent(state = finishedState())
        composeTestRule.onNodeWithTag(TimerScreenTags.CANCEL_BUTTON).assertDoesNotExist()
    }

    // endregion

    // region Countdown formatting

    @Test
    fun countdown_showsSecondsOnly_whenUnder60() {
        setContent(state = workState(remainingSeconds = 45))
        composeTestRule.onNodeWithText("45").assertIsDisplayed()
    }

    @Test
    fun countdown_showsMinutesAndSeconds_whenOver60() {
        setContent(state = workState(remainingSeconds = 90))
        composeTestRule.onNodeWithText("1:30").assertIsDisplayed()
    }

    @Test
    fun countdown_padsSeconds_whenUnder10() {
        setContent(state = workState(remainingSeconds = 65))
        composeTestRule.onNodeWithText("1:05").assertIsDisplayed()
    }

    @Test
    fun countdown_shows1_whenOneSecondLeft() {
        setContent(state = workState(remainingSeconds = 1))
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
    }

    // endregion

    // region Rep counter

    @Test
    fun repCounter_showsRep1_whenOnFirstWork() {
        setContent(state = workState(currentPhaseIndex = 0))
        composeTestRule.onNodeWithText("Rep 1 of 2").assertIsDisplayed()
    }

    @Test
    fun repCounter_showsRep2_whenOnSecondWork() {
        // index=2 → второй Work
        setContent(state = workState(currentPhaseIndex = 2))
        composeTestRule.onNodeWithText("Rep 2 of 2").assertIsDisplayed()
    }

    @Test
    fun repCounter_showsRep1_duringRestAfterFirstWork() {
        // index=1 → Rest (после первого Work) — текущий реп ещё 1
        setContent(state = restState())
        composeTestRule.onNodeWithText("Rep 1 of 2").assertIsDisplayed()
    }

    // endregion

    // region Interactions

    @Test
    fun clickingPause_invokesOnStop() {
        var called = false
        setContent(state = workState(isRunning = true), onStop = { called = true })
        composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).performClick()
        assertTrue(called)
    }

    @Test
    fun clickingResume_invokesOnResume() {
        var called = false
        setContent(state = workState(isRunning = false), onResume = { called = true })
        composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).performClick()
        assertTrue(called)
    }

    @Test
    fun clickingCancel_invokesOnCancel() {
        var called = false
        setContent(state = workState(), onCancel = { called = true })
        composeTestRule.onNodeWithTag(TimerScreenTags.CANCEL_BUTTON).performClick()
        assertTrue(called)
    }

    @Test
    fun clickingDone_invokesOnCancel() {
        var called = false
        setContent(state = finishedState(), onCancel = { called = true })
        composeTestRule.onNodeWithTag(TimerScreenTags.DONE_BUTTON).performClick()
        assertTrue(called)
    }

    @Test
    fun clickingCancel_doesNotInvokeOnStop() {
        var stopCalled = false
        setContent(state = workState(), onStop = { stopCalled = true })
        composeTestRule.onNodeWithTag(TimerScreenTags.CANCEL_BUTTON).performClick()
        assertTrue(!stopCalled)
    }

    @Test
    fun clickingPause_doesNotInvokeOnCancel() {
        var cancelCalled = false
        setContent(state = workState(isRunning = true), onCancel = { cancelCalled = true })
        composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).performClick()
        assertTrue(!cancelCalled)
    }

    // endregion

    // region Back press

    @Test
    fun backPress_invokesOnCancel_whenRunning() {
        var cancelCalled = false
        setContent(state = workState(isRunning = true), onCancel = { cancelCalled = true })
        Espresso.pressBack()
        assertTrue(cancelCalled)
    }

    @Test
    fun backPress_invokesOnCancel_whenPaused() {
        var cancelCalled = false
        setContent(state = workState(isRunning = false), onCancel = { cancelCalled = true })
        Espresso.pressBack()
        assertTrue(cancelCalled)
    }

    @Test
    fun backPress_invokesOnCancel_whenFinished() {
        var cancelCalled = false
        setContent(state = finishedState(), onCancel = { cancelCalled = true })
        Espresso.pressBack()
        assertTrue(cancelCalled)
    }

    @Test
    fun backPress_doesNotInvokeOnStop() {
        var stopCalled = false
        setContent(state = workState(isRunning = true), onStop = { stopCalled = true })
        Espresso.pressBack()
        assertTrue(!stopCalled)
    }

    @Test
    fun backPress_callCount_isExactlyOne() {
        var cancelCount = 0
        setContent(state = workState(), onCancel = { cancelCount++ })
        Espresso.pressBack()
        assertEquals(1, cancelCount)
    }

    // endregion

    // region Rapid Stop/Resume taps

    @Test
    fun rapidPause_callsOnStop_eachTime() {
        var stopCount = 0
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = true),
                onStop = { stopCount++ },
                onResume = {},
                onCancel = {},
            )
        }
        repeat(10) {
            composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).performClick()
        }
        assertEquals(10, stopCount)
    }

    @Test
    fun rapidResume_callsOnResume_eachTime() {
        var resumeCount = 0
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = false),
                onStop = {},
                onResume = { resumeCount++ },
                onCancel = {},
            )
        }
        repeat(10) {
            composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).performClick()
        }
        assertEquals(10, resumeCount)
    }

    @Test
    fun rapidAlternation_resumeButtonVisible_afterOddTaps() {
        // Нечётное число тапов: начали с isRunning=true → в конце false → Resume видна
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                onStop = { isRunning = false },
                onResume = { isRunning = true },
                onCancel = {},
            )
        }
        repeat(9) {
            if (isRunning) composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).performClick()
            else composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).performClick()
            composeTestRule.waitForIdle()
        }
        // 9 тапов (нечётное) → isRunning=false
        assertFalse(isRunning)
        composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).assertIsDisplayed()
    }

    @Test
    fun rapidAlternation_pauseButtonVisible_afterEvenTaps() {
        // Чётное число тапов: начали с isRunning=true → в конце true → Pause видна
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                onStop = { isRunning = false },
                onResume = { isRunning = true },
                onCancel = {},
            )
        }
        repeat(10) {
            if (isRunning) composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).performClick()
            else composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).performClick()
            composeTestRule.waitForIdle()
        }
        // 10 тапов (чётное) → isRunning=true
        assertTrue(isRunning)
        composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun rapidAlternation_correctButtonShownAfterEachTap() {
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                onStop = { isRunning = false },
                onResume = { isRunning = true },
                onCancel = {},
            )
        }
        repeat(6) {
            if (isRunning) {
                composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).assertIsDisplayed().performClick()
                composeTestRule.waitForIdle()
                composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).assertIsDisplayed()
            } else {
                composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).assertIsDisplayed().performClick()
                composeTestRule.waitForIdle()
                composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).assertIsDisplayed()
            }
        }
    }

    @Test
    fun rapidAlternation_neverShowsBothButtonsAtOnce() {
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                onStop = { isRunning = false },
                onResume = { isRunning = true },
                onCancel = {},
            )
        }
        repeat(8) {
            if (isRunning) composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).performClick()
            else composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).performClick()
            composeTestRule.waitForIdle()

            if (isRunning) {
                composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).assertDoesNotExist()
                composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).assertIsDisplayed()
            } else {
                composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).assertDoesNotExist()
                composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).assertIsDisplayed()
            }
        }
    }

    @Test
    fun cancelButton_alwaysVisible_duringRapidTaps() {
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                onStop = { isRunning = false },
                onResume = { isRunning = true },
                onCancel = {},
            )
        }
        repeat(10) {
            composeTestRule.onNodeWithTag(TimerScreenTags.CANCEL_BUTTON).assertIsDisplayed()
            if (isRunning) composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).performClick()
            else composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).performClick()
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun rapidPause_thenCancel_callsOnCancel() {
        var cancelCalled = false
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                onStop = { isRunning = false },
                onResume = { isRunning = true },
                onCancel = { cancelCalled = true },
            )
        }
        repeat(3) {
            composeTestRule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON).performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON).performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag(TimerScreenTags.CANCEL_BUTTON).performClick()
        assertTrue(cancelCalled)
    }

    // endregion
}
