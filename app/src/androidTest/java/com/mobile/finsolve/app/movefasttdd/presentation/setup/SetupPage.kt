package com.mobile.finsolve.app.movefasttdd.presentation.setup

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.rules.ActivityScenarioRule

class SetupPage(
    private val rule: AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>,
) {
    private val hasError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
    private val hasNoError = SemanticsMatcher.keyNotDefined(SemanticsProperties.Error)

    // region Nodes

    private val repsField get() = rule.onNodeWithTag(SetupScreenTags.REPS_FIELD)
    private val repDurationField get() = rule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD)
    private val restDurationField get() = rule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD)
    private val startButton get() = rule.onNodeWithTag(SetupScreenTags.START_BUTTON)
    private val errorMessage get() = rule.onNodeWithTag(SetupScreenTags.ERROR_MESSAGE)
    private val title get() = rule.onNodeWithText("Interval Timer")

    // endregion

    // region Actions

    fun typeReps(text: String): SetupPage {
        repsField.performTextClearance()
        repsField.performTextInput(text)
        return this
    }

    fun typeRepDuration(text: String): SetupPage {
        repDurationField.performTextClearance()
        repDurationField.performTextInput(text)
        return this
    }

    fun typeRestDuration(text: String): SetupPage {
        restDurationField.performTextClearance()
        restDurationField.performTextInput(text)
        return this
    }

    fun clickStart(): SetupPage {
        startButton.performClick()
        return this
    }

    // endregion

    // region Assertions — visibility

    fun assertTitleVisible(): SetupPage {
        title.assertIsDisplayed()
        return this
    }

    fun assertRepsFieldVisible(): SetupPage {
        repsField.assertIsDisplayed()
        return this
    }

    fun assertRepDurationFieldVisible(): SetupPage {
        repDurationField.assertIsDisplayed()
        return this
    }

    fun assertRestDurationFieldVisible(): SetupPage {
        restDurationField.assertIsDisplayed()
        return this
    }

    fun assertStartButtonVisible(): SetupPage {
        startButton.assertIsDisplayed()
        return this
    }

    fun assertAllFieldsVisible(): SetupPage =
        assertRepsFieldVisible()
            .assertRepDurationFieldVisible()
            .assertRestDurationFieldVisible()
            .assertStartButtonVisible()

    fun assertErrorMessageVisible(): SetupPage {
        errorMessage.assertIsDisplayed()
        return this
    }

    fun assertErrorMessageHidden(): SetupPage {
        errorMessage.assertIsNotDisplayed()
        return this
    }

    // endregion

    // region Assertions — error state per field

    fun assertRepsHasError(): SetupPage {
        repsField.assert(hasError)
        return this
    }

    fun assertRepsHasNoError(): SetupPage {
        repsField.assert(hasNoError)
        return this
    }

    fun assertRepDurationHasError(): SetupPage {
        repDurationField.assert(hasError)
        return this
    }

    fun assertRepDurationHasNoError(): SetupPage {
        repDurationField.assert(hasNoError)
        return this
    }

    fun assertRestDurationHasError(): SetupPage {
        restDurationField.assert(hasError)
        return this
    }

    fun assertRestDurationHasNoError(): SetupPage {
        restDurationField.assert(hasNoError)
        return this
    }

    fun assertNoFieldsHaveErrors(): SetupPage =
        assertRepsHasNoError()
            .assertRepDurationHasNoError()
            .assertRestDurationHasNoError()

    // endregion

    // region Assertions — text content

    fun assertCountdownText(expected: String): SetupPage {
        rule.onNodeWithText(expected).assertIsDisplayed()
        return this
    }

    // endregion
}
