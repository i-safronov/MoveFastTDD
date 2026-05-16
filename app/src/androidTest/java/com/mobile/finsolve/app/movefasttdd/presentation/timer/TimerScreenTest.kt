package com.mobile.finsolve.app.movefasttdd.presentation.timer

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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

    private val page = TimerPage(composeTestRule)

    private fun noopActions() = object : TimerActions {
        override fun onStop() = Unit
        override fun onResume() = Unit
        override fun onCancel() = Unit
    }

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
                actions = object : TimerActions {
                    override fun onStop() = onStop()
                    override fun onResume() = onResume()
                    override fun onCancel() = onCancel()
                },
            )
        }
    }

    // endregion

    // region Display — Work phase

    @Test
    fun workPhase_showsWorkLabel() {
        setContent(state = workState())
        page.assertPhaseLabelVisible().assertPhaseLabelShows("WORK")
    }

    @Test
    fun workPhase_showsCountdown() {
        setContent(state = workState(remainingSeconds = 30))
        page.assertCountdownVisible().assertCountdownShows("30")
    }

    @Test
    fun workPhase_showsRepCounter() {
        setContent(state = workState())
        page.assertRepCounterVisible().assertRepCounterShows("Rep 1 of 2")
    }

    @Test
    fun workPhase_showsPauseButton_whenRunning() {
        setContent(state = workState(isRunning = true))
        page.assertPauseButtonVisible()
    }

    @Test
    fun workPhase_showsCancelButton() {
        setContent(state = workState())
        page.assertCancelButtonVisible()
    }

    @Test
    fun workPhase_doesNotShowFinishedMessage() {
        setContent(state = workState())
        page.assertFinishedMessageNotExists()
    }

    @Test
    fun workPhase_doesNotShowDoneButton() {
        setContent(state = workState())
        page.assertDoneButtonNotExists()
    }

    @Test
    fun workPhase_doesNotShowResumeButton_whenRunning() {
        setContent(state = workState(isRunning = true))
        page.assertResumeButtonNotExists()
    }

    // endregion

    // region Display — Rest phase

    @Test
    fun restPhase_showsRestLabel() {
        setContent(state = restState())
        page.assertPhaseLabelShows("REST")
    }

    @Test
    fun restPhase_showsCorrectRepCounter() {
        setContent(state = restState())
        page.assertRepCounterShows("Rep 1 of 2")
    }

    @Test
    fun restPhase_showsRestCountdown() {
        setContent(state = restState(remainingSeconds = 7))
        page.assertCountdownShows("7")
    }

    // endregion

    // region Display — Paused state

    @Test
    fun paused_showsResumeButton() {
        setContent(state = workState(isRunning = false))
        page.assertResumeButtonVisible()
    }

    @Test
    fun paused_doesNotShowPauseButton() {
        setContent(state = workState(isRunning = false))
        page.assertPauseButtonNotExists()
    }

    @Test
    fun paused_stillShowsCancelButton() {
        setContent(state = workState(isRunning = false))
        page.assertCancelButtonVisible()
    }

    @Test
    fun paused_stillShowsCountdown() {
        setContent(state = workState(remainingSeconds = 15, isRunning = false))
        page.assertCountdownShows("15")
    }

    // endregion

    // region Display — Finished state

    @Test
    fun finished_showsFinishedMessage() {
        setContent(state = finishedState())
        page.assertFinishedScreenVisible().assertTextVisible("Workout Complete!")
    }

    @Test
    fun finished_showsGreatJob() {
        setContent(state = finishedState())
        page.assertTextVisible("Great job!")
    }

    @Test
    fun finished_showsDoneButton() {
        setContent(state = finishedState())
        page.assertDoneButtonVisible()
    }

    @Test
    fun finished_doesNotShowActiveTimerElements() {
        setContent(state = finishedState())
        page.assertActiveTimerElementsNotVisible()
    }

    @Test
    fun finished_doesNotShowCancelButton() {
        setContent(state = finishedState())
        page.assertCancelButtonNotExists()
    }

    // endregion

    // region Countdown formatting

    @Test
    fun countdown_showsSecondsOnly_whenUnder60() {
        setContent(state = workState(remainingSeconds = 45))
        page.assertCountdownShows("45")
    }

    @Test
    fun countdown_showsMinutesAndSeconds_whenOver60() {
        setContent(state = workState(remainingSeconds = 90))
        page.assertCountdownShows("1:30")
    }

    @Test
    fun countdown_padsSeconds_whenUnder10() {
        setContent(state = workState(remainingSeconds = 65))
        page.assertCountdownShows("1:05")
    }

    @Test
    fun countdown_showsSingleSecond_whenOneSecondLeft() {
        setContent(state = workState(remainingSeconds = 1))
        page.assertCountdownShows("1")
    }

    // endregion

    // region Rep counter

    @Test
    fun repCounter_showsRep1_whenOnFirstWork() {
        setContent(state = workState(currentPhaseIndex = 0))
        page.assertRepCounterShows("Rep 1 of 2")
    }

    @Test
    fun repCounter_showsRep2_whenOnSecondWork() {
        setContent(state = workState(currentPhaseIndex = 2))
        page.assertRepCounterShows("Rep 2 of 2")
    }

    @Test
    fun repCounter_showsRep1_duringRestAfterFirstWork() {
        setContent(state = restState())
        page.assertRepCounterShows("Rep 1 of 2")
    }

    // endregion

    // region Interactions

    @Test
    fun pauseButton_click_invokesOnStop() {
        var called = false
        setContent(state = workState(isRunning = true), onStop = { called = true })
        page.clickPause()
        assertTrue(called)
    }

    @Test
    fun resumeButton_click_invokesOnResume() {
        var called = false
        setContent(state = workState(isRunning = false), onResume = { called = true })
        page.clickResume()
        assertTrue(called)
    }

    @Test
    fun cancelButton_click_invokesOnCancel() {
        var called = false
        setContent(state = workState(), onCancel = { called = true })
        page.clickCancel()
        assertTrue(called)
    }

    @Test
    fun doneButton_click_invokesOnCancel() {
        var called = false
        setContent(state = finishedState(), onCancel = { called = true })
        page.clickDone()
        assertTrue(called)
    }

    @Test
    fun cancelButton_click_doesNotInvokeOnStop() {
        var stopCalled = false
        setContent(state = workState(), onStop = { stopCalled = true })
        page.clickCancel()
        assertFalse(stopCalled)
    }

    @Test
    fun pauseButton_click_doesNotInvokeOnCancel() {
        var cancelCalled = false
        setContent(state = workState(isRunning = true), onCancel = { cancelCalled = true })
        page.clickPause()
        assertFalse(cancelCalled)
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
        assertFalse(stopCalled)
    }

    @Test
    fun backPress_callCount_isExactlyOne() {
        var cancelCount = 0
        setContent(state = workState(), onCancel = { cancelCount++ })
        Espresso.pressBack()
        assertEquals(1, cancelCount)
    }

    // endregion

    // region Configuration Change (rotation)

    @Test
    fun rotation_workPhase_allElementsStillVisible() {
        setContent(state = workState(remainingSeconds = 25))
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent(state = workState(remainingSeconds = 25))
        page.assertActiveTimerVisible()
    }

    @Test
    fun rotation_preservesCountdownValue() {
        setContent(state = workState(remainingSeconds = 17))
        page.assertCountdownShows("17")
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent(state = workState(remainingSeconds = 17))
        page.assertCountdownShows("17")
    }

    @Test
    fun rotation_preservesWorkPhaseLabel() {
        setContent(state = workState())
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent(state = workState())
        page.assertPhaseLabelShows("WORK")
    }

    @Test
    fun rotation_preservesRestPhaseLabel() {
        setContent(state = restState())
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent(state = restState())
        page.assertPhaseLabelShows("REST")
    }

    @Test
    fun rotation_preservesPausedState() {
        setContent(state = workState(isRunning = false))
        page.assertResumeButtonVisible()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent(state = workState(isRunning = false))
        page.assertResumeButtonVisible().assertPauseButtonNotExists()
    }

    @Test
    fun rotation_preservesRunningState() {
        setContent(state = workState(isRunning = true))
        page.assertPauseButtonVisible()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent(state = workState(isRunning = true))
        page.assertPauseButtonVisible().assertResumeButtonNotExists()
    }

    @Test
    fun rotation_preservesFinishedState() {
        setContent(state = finishedState())
        page.assertFinishedMessageVisible()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent(state = finishedState())
        page.assertFinishedScreenVisible().assertPauseButtonNotExists()
    }

    @Test
    fun rotation_cancelButton_stillCallsOnCancel() {
        var cancelCalled = false
        fun content() {
            composeTestRule.setContent {
                BackHandler { cancelCalled = true }
                TimerContent(state = workState(), actions = object : TimerActions {
                    override fun onStop() = Unit
                    override fun onResume() = Unit
                    override fun onCancel() {
                        cancelCalled = true
                    }
                })
            }
        }
        content()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content()
        page.clickCancel()
        assertTrue(cancelCalled)
    }

    @Test
    fun rotation_pauseButton_stillCallsOnStop() {
        var stopCalled = false
        fun content() {
            composeTestRule.setContent {
                TimerContent(state = workState(isRunning = true), actions = object : TimerActions {
                    override fun onStop() {
                        stopCalled = true
                    }

                    override fun onResume() = Unit
                    override fun onCancel() = Unit
                })
            }
        }
        content()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content()
        page.clickPause()
        assertTrue(stopCalled)
    }

    @Test
    fun rotation_mutableState_survivesRecreation() {
        var isRunning by mutableStateOf(true)
        fun content() {
            composeTestRule.setContent {
                TimerContent(
                    state = workState(isRunning = isRunning),
                    actions = object : TimerActions {
                        override fun onStop() {
                            isRunning = false
                        }

                        override fun onResume() {
                            isRunning = true
                        }

                        override fun onCancel() = Unit
                    },
                )
            }
        }
        content()
        page.clickPause()
        composeTestRule.waitForIdle()
        assertFalse(isRunning)

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content()

        page.assertResumeButtonVisible().assertPauseButtonNotExists()
    }

    @Test
    fun multipleRotations_uiRemainsStable() {
        fun content() {
            setContent(state = workState(remainingSeconds = 42))
        }
        content()
        repeat(3) {
            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()
            content()
        }
        page.assertCountdownShows("42").assertPhaseLabelVisible().assertCancelButtonVisible()
    }

    // endregion

    // region Rapid Stop/Resume taps

    @Test
    fun rapidPause_callsOnStop_eachTime() {
        var stopCount = 0
        composeTestRule.setContent {
            TimerContent(state = workState(isRunning = true), actions = object : TimerActions {
                override fun onStop() {
                    stopCount++
                }

                override fun onResume() = Unit
                override fun onCancel() = Unit
            })
        }
        repeat(10) { page.clickPause() }
        assertEquals(10, stopCount)
    }

    @Test
    fun rapidResume_callsOnResume_eachTime() {
        var resumeCount = 0
        composeTestRule.setContent {
            TimerContent(state = workState(isRunning = false), actions = object : TimerActions {
                override fun onStop() = Unit
                override fun onResume() {
                    resumeCount++
                }

                override fun onCancel() = Unit
            })
        }
        repeat(10) { page.clickResume() }
        assertEquals(10, resumeCount)
    }

    @Test
    fun rapidAlternation_resumeButtonVisible_afterOddTaps() {
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                actions = object : TimerActions {
                    override fun onStop() {
                        isRunning = false
                    }

                    override fun onResume() {
                        isRunning = true
                    }

                    override fun onCancel() = Unit
                },
            )
        }
        repeat(9) {
            if (isRunning) page.clickPause() else page.clickResume()
            composeTestRule.waitForIdle()
        }
        assertFalse(isRunning)
        page.assertResumeButtonVisible()
    }

    @Test
    fun rapidAlternation_pauseButtonVisible_afterEvenTaps() {
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                actions = object : TimerActions {
                    override fun onStop() {
                        isRunning = false
                    }

                    override fun onResume() {
                        isRunning = true
                    }

                    override fun onCancel() = Unit
                },
            )
        }
        repeat(10) {
            if (isRunning) page.clickPause() else page.clickResume()
            composeTestRule.waitForIdle()
        }
        assertTrue(isRunning)
        page.assertPauseButtonVisible()
    }

    @Test
    fun rapidAlternation_correctButtonShown_afterEachTap() {
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                actions = object : TimerActions {
                    override fun onStop() {
                        isRunning = false
                    }

                    override fun onResume() {
                        isRunning = true
                    }

                    override fun onCancel() = Unit
                },
            )
        }
        repeat(6) {
            if (isRunning) {
                page.assertPauseButtonVisible().clickPause()
                composeTestRule.waitForIdle()
                page.assertResumeButtonVisible()
            } else {
                page.assertResumeButtonVisible().clickResume()
                composeTestRule.waitForIdle()
                page.assertPauseButtonVisible()
            }
        }
    }

    @Test
    fun rapidAlternation_neverShowsBothButtonsAtOnce() {
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                actions = object : TimerActions {
                    override fun onStop() {
                        isRunning = false
                    }

                    override fun onResume() {
                        isRunning = true
                    }

                    override fun onCancel() = Unit
                },
            )
        }
        repeat(8) {
            if (isRunning) page.clickPause() else page.clickResume()
            composeTestRule.waitForIdle()
            if (isRunning) page.assertResumeButtonNotExists().assertPauseButtonVisible()
            else page.assertPauseButtonNotExists().assertResumeButtonVisible()
        }
    }

    @Test
    fun rapidTaps_cancelButtonAlwaysVisible() {
        var isRunning by mutableStateOf(true)
        composeTestRule.setContent {
            TimerContent(
                state = workState(isRunning = isRunning),
                actions = object : TimerActions {
                    override fun onStop() {
                        isRunning = false
                    }

                    override fun onResume() {
                        isRunning = true
                    }

                    override fun onCancel() = Unit
                },
            )
        }
        repeat(10) {
            page.assertCancelButtonVisible()
            if (isRunning) page.clickPause() else page.clickResume()
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
                actions = object : TimerActions {
                    override fun onStop() {
                        isRunning = false
                    }

                    override fun onResume() {
                        isRunning = true
                    }

                    override fun onCancel() {
                        cancelCalled = true
                    }
                },
            )
        }
        repeat(3) {
            page.clickPause()
            composeTestRule.waitForIdle()
            page.clickResume()
            composeTestRule.waitForIdle()
        }
        page.clickCancel()
        assertTrue(cancelCalled)
    }

    // endregion
}
