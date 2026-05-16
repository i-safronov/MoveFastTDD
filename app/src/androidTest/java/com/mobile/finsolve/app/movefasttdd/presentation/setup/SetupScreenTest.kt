package com.mobile.finsolve.app.movefasttdd.presentation.setup

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mobile.finsolve.app.movefasttdd.presentation.setup.view_model.SetupContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val hasError = SemanticsMatcher.keyIsDefined(SemanticsProperties.Error)
    private val hasNoError = SemanticsMatcher.keyNotDefined(SemanticsProperties.Error)

    private fun setContent(
        state: SetupContract.State = SetupContract.State(),
        onRepsChange: (Int) -> Unit = {},
        onRepDurationChange: (Int) -> Unit = {},
        onRestDurationChange: (Int) -> Unit = {},
        onStart: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            SetupContent(
                state = state,
                onRepsChange = onRepsChange,
                onRepDurationChange = onRepDurationChange,
                onRestDurationChange = onRestDurationChange,
                onStart = onStart,
            )
        }
    }

    // region Display

    @Test
    fun repsField_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assertIsDisplayed()
    }

    @Test
    fun repDurationField_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assertIsDisplayed()
    }

    @Test
    fun restDurationField_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).assertIsDisplayed()
    }

    @Test
    fun startButton_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithTag(SetupScreenTags.START_BUTTON).assertIsDisplayed()
    }

    @Test
    fun title_isDisplayed() {
        setContent()
        composeTestRule.onNodeWithText("Interval Timer").assertIsDisplayed()
    }

    // endregion

    // region Error message visibility

    @Test
    fun errorMessage_isNotDisplayed_whenNoErrors() {
        setContent(state = SetupContract.State())
        composeTestRule.onNodeWithTag(SetupScreenTags.ERROR_MESSAGE).assertIsNotDisplayed()
    }

    @Test
    fun errorMessage_isDisplayed_whenRepsError() {
        setContent(state = SetupContract.State(repsError = true))
        composeTestRule.onNodeWithTag(SetupScreenTags.ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun errorMessage_isDisplayed_whenRepDurationError() {
        setContent(state = SetupContract.State(repDurationError = true))
        composeTestRule.onNodeWithTag(SetupScreenTags.ERROR_MESSAGE).assertIsDisplayed()
    }

    @Test
    fun errorMessage_isDisplayed_whenRestDurationError() {
        setContent(state = SetupContract.State(restDurationError = true))
        composeTestRule.onNodeWithTag(SetupScreenTags.ERROR_MESSAGE).assertIsDisplayed()
    }

    // endregion

    // region Per-field error state

    @Test
    fun repsField_hasError_whenRepsErrorIsTrue() {
        setContent(state = SetupContract.State(repsError = true))
        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasError)
    }

    @Test
    fun repsField_hasNoError_whenRepsErrorIsFalse() {
        setContent(state = SetupContract.State(repsError = false))
        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasNoError)
    }

    @Test
    fun repDurationField_hasError_whenRepDurationErrorIsTrue() {
        setContent(state = SetupContract.State(repDurationError = true))
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assert(hasError)
    }

    @Test
    fun repDurationField_hasNoError_whenRepDurationErrorIsFalse() {
        setContent(state = SetupContract.State(repDurationError = false))
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assert(hasNoError)
    }

    @Test
    fun restDurationField_hasError_whenRestDurationErrorIsTrue() {
        setContent(state = SetupContract.State(restDurationError = true))
        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).assert(hasError)
    }

    @Test
    fun restDurationField_hasNoError_whenRestDurationErrorIsFalse() {
        setContent(state = SetupContract.State(restDurationError = false))
        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).assert(hasNoError)
    }

    @Test
    fun onlyRepsField_hasError_whenOnlyRepsErrorIsTrue() {
        setContent(state = SetupContract.State(repsError = true))

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assert(hasNoError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).assert(hasNoError)
    }

    @Test
    fun onlyRepDurationField_hasError_whenOnlyRepDurationErrorIsTrue() {
        setContent(state = SetupContract.State(repDurationError = true))

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasNoError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assert(hasError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).assert(hasNoError)
    }

    @Test
    fun multipleFields_haveErrors_whenMultipleErrorsAreTrue() {
        setContent(state = SetupContract.State(repsError = true, repDurationError = true))

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assert(hasError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).assert(hasNoError)
    }

    @Test
    fun noFields_haveErrors_whenStateHasNoErrors() {
        setContent(state = SetupContract.State())

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasNoError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assert(hasNoError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).assert(hasNoError)
    }

    // endregion

    // region Interactions

    @Test
    fun clickingStartButton_invokesOnStart() {
        var startCalled = false
        setContent(onStart = { startCalled = true })

        composeTestRule.onNodeWithTag(SetupScreenTags.START_BUTTON).performClick()

        assertTrue(startCalled)
    }

    @Test
    fun typingInRepsField_invokesOnRepsChange() {
        var capturedValue: Int? = null
        setContent(
            state = SetupContract.State(reps = 0),
            onRepsChange = { capturedValue = it },
        )

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).apply {
            performTextClearance()
            performTextInput("5")
        }

        assertEquals(5, capturedValue)
    }

    @Test
    fun typingInRepDurationField_invokesOnRepDurationChange() {
        var capturedValue: Int? = null
        setContent(
            state = SetupContract.State(repDuration = 0),
            onRepDurationChange = { capturedValue = it },
        )

        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).apply {
            performTextClearance()
            performTextInput("45")
        }

        assertEquals(45, capturedValue)
    }

    @Test
    fun typingInRestDurationField_invokesOnRestDurationChange() {
        var capturedValue: Int? = null
        setContent(
            state = SetupContract.State(restDuration = 0),
            onRestDurationChange = { capturedValue = it },
        )

        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).apply {
            performTextClearance()
            performTextInput("10")
        }

        assertEquals(10, capturedValue)
    }

    @Test
    fun nonDigitInput_isIgnored_andCallbackNotInvokedWithInvalidValue() {
        var capturedValue: Int? = null
        setContent(
            state = SetupContract.State(reps = 0),
            onRepsChange = { capturedValue = it },
        )

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).apply {
            performTextClearance()
            performTextInput("abc")
        }

        // Буквы фильтруются — пустая строка не конвертируется в Int, колбэк не вызван
        assertEquals(null, capturedValue)
    }

    // endregion

    // region Configuration Change

    @Test
    fun rotation_allFieldsStillVisible() {
        setContent()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent()

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SetupScreenTags.REST_DURATION_FIELD).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SetupScreenTags.START_BUTTON).assertIsDisplayed()
    }

    @Test
    fun rotation_preservesFieldValues() {
        var reps by mutableStateOf(5)
        fun content() {
            composeTestRule.setContent {
                SetupContent(
                    state = SetupContract.State(reps = reps),
                    onRepsChange = { reps = it },
                    onRepDurationChange = {},
                    onRestDurationChange = {},
                    onStart = {},
                )
            }
        }
        content()
        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).apply {
            performTextClearance()
            performTextInput("7")
        }
        assertEquals(7, reps)

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content()

        composeTestRule.onNodeWithText("7").assertIsDisplayed()
    }

    @Test
    fun rotation_preservesErrorState() {
        fun content(repsError: Boolean) {
            composeTestRule.setContent {
                SetupContent(
                    state = SetupContract.State(repsError = repsError),
                    onRepsChange = {},
                    onRepDurationChange = {},
                    onRestDurationChange = {},
                    onStart = {},
                )
            }
        }
        content(repsError = true)
        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasError)

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content(repsError = true)

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasError)
        composeTestRule.onNodeWithTag(SetupScreenTags.REP_DURATION_FIELD).assert(hasNoError)
    }

    @Test
    fun rotation_noErrorStatePreservedWhenNoError() {
        fun content() {
            composeTestRule.setContent {
                SetupContent(
                    state = SetupContract.State(),
                    onRepsChange = {},
                    onRepDurationChange = {},
                    onRestDurationChange = {},
                    onStart = {},
                )
            }
        }
        content()
        composeTestRule.onNodeWithTag(SetupScreenTags.ERROR_MESSAGE).assertIsNotDisplayed()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content()

        composeTestRule.onNodeWithTag(SetupScreenTags.ERROR_MESSAGE).assertIsNotDisplayed()
    }

    @Test
    fun rotation_startButtonStillClickable() {
        var startCalled = false
        fun content() {
            composeTestRule.setContent {
                SetupContent(
                    state = SetupContract.State(),
                    onRepsChange = {},
                    onRepDurationChange = {},
                    onRestDurationChange = {},
                    onStart = { startCalled = true },
                )
            }
        }
        content()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content()

        composeTestRule.onNodeWithTag(SetupScreenTags.START_BUTTON).performClick()
        assertTrue(startCalled)
    }

    @Test
    fun landscapeToPortrait_multipleRotations_uiStaysCorrect() {
        fun content(repsError: Boolean = false) {
            composeTestRule.setContent {
                SetupContent(
                    state = SetupContract.State(repsError = repsError),
                    onRepsChange = {},
                    onRepDurationChange = {},
                    onRestDurationChange = {},
                    onStart = {},
                )
            }
        }
        content(repsError = true)

        repeat(3) {
            composeTestRule.activityRule.scenario.recreate()
            composeTestRule.waitForIdle()
            content(repsError = true)
        }

        composeTestRule.onNodeWithTag(SetupScreenTags.REPS_FIELD).assert(hasError)
        composeTestRule.onNodeWithTag(SetupScreenTags.START_BUTTON).assertIsDisplayed()
    }

    // endregion
}
