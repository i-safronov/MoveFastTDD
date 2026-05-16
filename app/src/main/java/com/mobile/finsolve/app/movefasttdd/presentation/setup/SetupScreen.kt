package com.mobile.finsolve.app.movefasttdd.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.OnEvent
import com.mobile.finsolve.app.movefasttdd.presentation.setup.view_model.SetupContract
import com.mobile.finsolve.app.movefasttdd.presentation.setup.view_model.SetupViewModel
import com.mobile.finsolve.app.movefasttdd.presentation.timer.TimerScreen

object SetupScreenTags {
    const val REPS_FIELD = "setup_reps_field"
    const val REP_DURATION_FIELD = "setup_rep_duration_field"
    const val REST_DURATION_FIELD = "setup_rest_duration_field"
    const val START_BUTTON = "setup_start_button"
    const val ERROR_MESSAGE = "setup_error_message"
}

class SetupScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<SetupViewModel>()
        val navigator = LocalNavigator.currentOrThrow

        OnEvent(viewModel.events) { event ->
            when (event) {
                is SetupContract.Event.NavigateToTimer ->
                    navigator.push(TimerScreen(event.config))
            }
        }

        SetupContent(
            state = viewModel.state,
            onRepsChange = { viewModel.dispatch(SetupContract.Executor.UpdateReps(it)) },
            onRepDurationChange = { viewModel.dispatch(SetupContract.Executor.UpdateRepDuration(it)) },
            onRestDurationChange = { viewModel.dispatch(SetupContract.Executor.UpdateRestDuration(it)) },
            onStart = { viewModel.dispatch(SetupContract.Executor.Start) },
        )
    }
}

@Composable
internal fun SetupContent(
    state: SetupContract.State,
    onRepsChange: (Int) -> Unit,
    onRepDurationChange: (Int) -> Unit,
    onRestDurationChange: (Int) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SetupHeader()

        Spacer(Modifier.height(32.dp))

        WorkoutTextField(
            label = "Reps",
            value = state.reps,
            isError = state.repsError,
            testTag = SetupScreenTags.REPS_FIELD,
            onChange = onRepsChange,
        )

        Spacer(Modifier.height(12.dp))

        WorkoutTextField(
            label = "Rep duration (sec)",
            value = state.repDuration,
            isError = state.repDurationError,
            testTag = SetupScreenTags.REP_DURATION_FIELD,
            onChange = onRepDurationChange,
        )

        Spacer(Modifier.height(12.dp))

        WorkoutTextField(
            label = "Rest between reps (sec)",
            value = state.restDuration,
            isError = state.restDurationError,
            testTag = SetupScreenTags.REST_DURATION_FIELD,
            onChange = onRestDurationChange,
        )

        Spacer(Modifier.height(8.dp))

        if (state.hasError) {
            SetupErrorMessage()
        } else {
            Spacer(Modifier.height(20.dp))
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SetupScreenTags.START_BUTTON),
        ) {
            Text(
                text = "Start Workout",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun SetupHeader() {
    Text(
        text = "Interval Timer",
        style = MaterialTheme.typography.headlineMedium,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = "Configure your workout",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WorkoutTextField(
    label: String,
    value: Int,
    isError: Boolean,
    testTag: String,
    onChange: (Int) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value.toString()) }

    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() }
            text = filtered
            filtered.toIntOrNull()?.let { onChange(it) }
        },
        label = { Text(label) },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

@Composable
private fun SetupErrorMessage() {
    Text(
        text = "Highlighted fields are required and must be greater than zero",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, top = 4.dp)
            .testTag(SetupScreenTags.ERROR_MESSAGE),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SetupContentPreview() {
    com.mobile.finsolve.app.movefasttdd.ui.theme.MoveFastTDDTheme {
        SetupContent(
            state = SetupContract.State(),
            onRepsChange = {},
            onRepDurationChange = {},
            onRestDurationChange = {},
            onStart = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SetupContentErrorPreview() {
    com.mobile.finsolve.app.movefasttdd.ui.theme.MoveFastTDDTheme {
        SetupContent(
            state = SetupContract.State(reps = 0, repDuration = 0, repsError = true, repDurationError = true),
            onRepsChange = {},
            onRepDurationChange = {},
            onRestDurationChange = {},
            onStart = {},
        )
    }
}
