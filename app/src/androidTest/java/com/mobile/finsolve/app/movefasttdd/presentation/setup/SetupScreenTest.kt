package com.mobile.finsolve.app.movefasttdd.presentation.setup

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mobile.finsolve.app.movefasttdd.presentation.setup.view_model.SetupContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SetupScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val page = SetupPage(composeTestRule)

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
        page.assertRepsFieldVisible()
    }

    @Test
    fun repDurationField_isDisplayed() {
        setContent()
        page.assertRepDurationFieldVisible()
    }

    @Test
    fun restDurationField_isDisplayed() {
        setContent()
        page.assertRestDurationFieldVisible()
    }

    @Test
    fun startButton_isDisplayed() {
        setContent()
        page.assertStartButtonVisible()
    }

    @Test
    fun title_isDisplayed() {
        setContent()
        page.assertTitleVisible()
    }

    // endregion

    // region Error message visibility

    @Test
    fun errorMessage_isNotDisplayed_whenNoErrors() {
        setContent(state = SetupContract.State())
        page.assertErrorMessageHidden()
    }

    @Test
    fun errorMessage_isDisplayed_whenRepsError() {
        setContent(state = SetupContract.State(repsError = true))
        page.assertErrorMessageVisible()
    }

    @Test
    fun errorMessage_isDisplayed_whenRepDurationError() {
        setContent(state = SetupContract.State(repDurationError = true))
        page.assertErrorMessageVisible()
    }

    @Test
    fun errorMessage_isDisplayed_whenRestDurationError() {
        setContent(state = SetupContract.State(restDurationError = true))
        page.assertErrorMessageVisible()
    }

    // endregion

    // region Per-field error state

    @Test
    fun repsField_hasError_whenRepsErrorIsTrue() {
        setContent(state = SetupContract.State(repsError = true))
        page.assertRepsHasError()
    }

    @Test
    fun repsField_hasNoError_whenRepsErrorIsFalse() {
        setContent(state = SetupContract.State(repsError = false))
        page.assertRepsHasNoError()
    }

    @Test
    fun repDurationField_hasError_whenRepDurationErrorIsTrue() {
        setContent(state = SetupContract.State(repDurationError = true))
        page.assertRepDurationHasError()
    }

    @Test
    fun repDurationField_hasNoError_whenRepDurationErrorIsFalse() {
        setContent(state = SetupContract.State(repDurationError = false))
        page.assertRepDurationHasNoError()
    }

    @Test
    fun restDurationField_hasError_whenRestDurationErrorIsTrue() {
        setContent(state = SetupContract.State(restDurationError = true))
        page.assertRestDurationHasError()
    }

    @Test
    fun restDurationField_hasNoError_whenRestDurationErrorIsFalse() {
        setContent(state = SetupContract.State(restDurationError = false))
        page.assertRestDurationHasNoError()
    }

    @Test
    fun onlyRepsField_hasError_whenOnlyRepsErrorIsTrue() {
        setContent(state = SetupContract.State(repsError = true))
        page.assertRepsHasError()
            .assertRepDurationHasNoError()
            .assertRestDurationHasNoError()
    }

    @Test
    fun onlyRepDurationField_hasError_whenOnlyRepDurationErrorIsTrue() {
        setContent(state = SetupContract.State(repDurationError = true))
        page.assertRepsHasNoError()
            .assertRepDurationHasError()
            .assertRestDurationHasNoError()
    }

    @Test
    fun multipleFields_haveErrors_whenMultipleErrorsAreTrue() {
        setContent(state = SetupContract.State(repsError = true, repDurationError = true))
        page.assertRepsHasError()
            .assertRepDurationHasError()
            .assertRestDurationHasNoError()
    }

    @Test
    fun noFields_haveErrors_whenStateHasNoErrors() {
        setContent(state = SetupContract.State())
        page.assertNoFieldsHaveErrors()
    }

    // endregion

    // region Interactions

    @Test
    fun clickingStartButton_invokesOnStart() {
        var startCalled = false
        setContent(onStart = { startCalled = true })
        page.clickStart()
        assertTrue(startCalled)
    }

    @Test
    fun typingInRepsField_invokesOnRepsChange() {
        var capturedValue: Int? = null
        setContent(
            state = SetupContract.State(reps = 0),
            onRepsChange = { capturedValue = it },
        )
        page.typeReps("5")
        assertEquals(5, capturedValue)
    }

    @Test
    fun typingInRepDurationField_invokesOnRepDurationChange() {
        var capturedValue: Int? = null
        setContent(
            state = SetupContract.State(repDuration = 0),
            onRepDurationChange = { capturedValue = it },
        )
        page.typeRepDuration("45")
        assertEquals(45, capturedValue)
    }

    @Test
    fun typingInRestDurationField_invokesOnRestDurationChange() {
        var capturedValue: Int? = null
        setContent(
            state = SetupContract.State(restDuration = 0),
            onRestDurationChange = { capturedValue = it },
        )
        page.typeRestDuration("10")
        assertEquals(10, capturedValue)
    }

    @Test
    fun nonDigitInput_isIgnored_andCallbackNotInvokedWithInvalidValue() {
        var capturedValue: Int? = null
        setContent(
            state = SetupContract.State(reps = 0),
            onRepsChange = { capturedValue = it },
        )
        page.typeReps("abc")
        assertEquals(null, capturedValue)
    }

    // endregion

    // region Configuration Change (rotation)

    @Test
    fun rotation_allFieldsStillVisible() {
        setContent()
        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        setContent()
        page.assertAllFieldsVisible()
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
        page.typeReps("7")
        assertEquals(7, reps)

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content()

        page.assertCountdownText("7")
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
        page.assertRepsHasError()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content(repsError = true)

        page.assertRepsHasError().assertRepDurationHasNoError()
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
        page.assertErrorMessageHidden()

        composeTestRule.activityRule.scenario.recreate()
        composeTestRule.waitForIdle()
        content()

        page.assertErrorMessageHidden()
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

        page.clickStart()
        assertTrue(startCalled)
    }

    @Test
    fun multipleRotations_uiStaysCorrect() {
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

        page.assertRepsHasError().assertStartButtonVisible()
    }

    // endregion
}
