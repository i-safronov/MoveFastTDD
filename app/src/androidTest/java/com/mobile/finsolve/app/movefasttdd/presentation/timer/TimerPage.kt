package com.mobile.finsolve.app.movefasttdd.presentation.timer

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule

class TimerPage(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>,
) {
    // region Nodes

    private val phaseLabel get() = rule.onNodeWithTag(TimerScreenTags.PHASE_LABEL)
    private val countdown get() = rule.onNodeWithTag(TimerScreenTags.COUNTDOWN)
    private val repCounter get() = rule.onNodeWithTag(TimerScreenTags.REP_COUNTER)
    private val pauseButton get() = rule.onNodeWithTag(TimerScreenTags.PAUSE_BUTTON)
    private val resumeButton get() = rule.onNodeWithTag(TimerScreenTags.RESUME_BUTTON)
    private val cancelButton get() = rule.onNodeWithTag(TimerScreenTags.CANCEL_BUTTON)
    private val finishedMessage get() = rule.onNodeWithTag(TimerScreenTags.FINISHED_MESSAGE)
    private val doneButton get() = rule.onNodeWithTag(TimerScreenTags.DONE_BUTTON)

    // endregion

    // region Actions

    fun clickPause(): TimerPage {
        pauseButton.performClick()
        return this
    }

    fun clickResume(): TimerPage {
        resumeButton.performClick()
        return this
    }

    fun clickCancel(): TimerPage {
        cancelButton.performClick()
        return this
    }

    fun clickDone(): TimerPage {
        doneButton.performClick()
        return this
    }

    // endregion

    // region Assertions — element visibility

    fun assertPhaseLabelVisible(): TimerPage {
        phaseLabel.assertIsDisplayed()
        return this
    }

    fun assertPhaseLabelNotExists(): TimerPage {
        phaseLabel.assertDoesNotExist()
        return this
    }

    fun assertCountdownVisible(): TimerPage {
        countdown.assertIsDisplayed()
        return this
    }

    fun assertCountdownNotExists(): TimerPage {
        countdown.assertDoesNotExist()
        return this
    }

    fun assertRepCounterVisible(): TimerPage {
        repCounter.assertIsDisplayed()
        return this
    }

    fun assertRepCounterNotExists(): TimerPage {
        repCounter.assertDoesNotExist()
        return this
    }

    fun assertPauseButtonVisible(): TimerPage {
        pauseButton.assertIsDisplayed()
        return this
    }

    fun assertPauseButtonNotExists(): TimerPage {
        pauseButton.assertDoesNotExist()
        return this
    }

    fun assertResumeButtonVisible(): TimerPage {
        resumeButton.assertIsDisplayed()
        return this
    }

    fun assertResumeButtonNotExists(): TimerPage {
        resumeButton.assertDoesNotExist()
        return this
    }

    fun assertCancelButtonVisible(): TimerPage {
        cancelButton.assertIsDisplayed()
        return this
    }

    fun assertCancelButtonNotExists(): TimerPage {
        cancelButton.assertDoesNotExist()
        return this
    }

    fun assertFinishedMessageVisible(): TimerPage {
        finishedMessage.assertIsDisplayed()
        return this
    }

    fun assertFinishedMessageNotExists(): TimerPage {
        finishedMessage.assertDoesNotExist()
        return this
    }

    fun assertDoneButtonVisible(): TimerPage {
        doneButton.assertIsDisplayed()
        return this
    }

    fun assertDoneButtonNotExists(): TimerPage {
        doneButton.assertDoesNotExist()
        return this
    }

    // endregion

    // region Assertions — text content

    fun assertTextVisible(text: String): TimerPage {
        rule.onNodeWithText(text).assertIsDisplayed()
        return this
    }

    fun assertCountdownShows(value: String): TimerPage = assertTextVisible(value)
    fun assertPhaseLabelShows(label: String): TimerPage = assertTextVisible(label)
    fun assertRepCounterShows(text: String): TimerPage = assertTextVisible(text)

    // endregion

    // region Compound assertions

    fun assertActiveTimerVisible(): TimerPage =
        assertPhaseLabelVisible()
            .assertCountdownVisible()
            .assertRepCounterVisible()
            .assertCancelButtonVisible()

    fun assertFinishedScreenVisible(): TimerPage =
        assertFinishedMessageVisible()
            .assertDoneButtonVisible()

    fun assertActiveTimerElementsNotVisible(): TimerPage =
        assertPhaseLabelNotExists()
            .assertCountdownNotExists()
            .assertRepCounterNotExists()
            .assertPauseButtonNotExists()

    // endregion
}
