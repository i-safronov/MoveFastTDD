package com.mobile.finsolve.app.movefasttdd.presentation.timer

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mobile.finsolve.app.movefasttdd.domain.model.TimerPhase
import com.mobile.finsolve.app.movefasttdd.domain.model.WorkoutConfig
import com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel.OnEvent
import com.mobile.finsolve.app.movefasttdd.presentation.theme.MoveFastTDDTheme
import com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model.TimerContract
import com.mobile.finsolve.app.movefasttdd.presentation.timer.view_model.TimerViewModel

object TimerScreenTags {
    const val PHASE_LABEL = "timer_phase_label"
    const val COUNTDOWN = "timer_countdown"
    const val REP_COUNTER = "timer_rep_counter"
    const val PAUSE_BUTTON = "timer_pause_button"
    const val RESUME_BUTTON = "timer_resume_button"
    const val CANCEL_BUTTON = "timer_cancel_button"
    const val FINISHED_MESSAGE = "timer_finished_message"
    const val DONE_BUTTON = "timer_done_button"
}

object TimerScreen : Screen {

    @Composable
    override fun Content() {
        val viewModel = hiltViewModel<TimerViewModel>()
        val navigator = LocalNavigator.currentOrThrow

        BackHandler {
            viewModel.dispatch(TimerContract.Executor.Cancel)
        }

        OnEvent(viewModel.events) { event ->
            when (event) {
                TimerContract.Event.NavigateBack -> navigator.pop()
                TimerContract.Event.WorkoutFinished -> Unit
            }
        }

        TimerContent(
            state = viewModel.state,
            onStop = { viewModel.dispatch(TimerContract.Executor.Stop) },
            onResume = { viewModel.dispatch(TimerContract.Executor.Resume) },
            onCancel = { viewModel.dispatch(TimerContract.Executor.Cancel) },
        )
    }
}

@Composable
internal fun TimerContent(
    state: TimerContract.State,
    onStop: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    if (state.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val isFinished = state.currentPhase is TimerPhase.Finished

    val phaseColor by animateColorAsState(
        targetValue = when {
            isFinished -> MaterialTheme.colorScheme.tertiary
            state.currentPhase is TimerPhase.Work -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.secondary
        },
        animationSpec = tween(durationMillis = 400),
        label = "phase_color",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isFinished) {
            FinishedContent(onDone = onCancel)
        } else {
            ActiveTimerContent(
                state = state,
                phaseColor = phaseColor,
                onStop = onStop,
                onResume = onResume,
                onCancel = onCancel,
            )
        }
    }
}

@Composable
private fun ActiveTimerContent(
    state: TimerContract.State,
    phaseColor: androidx.compose.ui.graphics.Color,
    onStop: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
) {
    Text(
        text = when (state.currentPhase) {
            is TimerPhase.Work -> "WORK"
            is TimerPhase.Rest -> "REST"
            TimerPhase.Finished -> ""
        },
        style = MaterialTheme.typography.labelLarge,
        color = phaseColor,
        letterSpacing = 4.sp,
        modifier = Modifier.testTag(TimerScreenTags.PHASE_LABEL),
    )

    Spacer(Modifier.height(24.dp))

    Text(
        text = state.remainingSeconds.formatTime(),
        fontSize = 80.sp,
        fontWeight = FontWeight.Bold,
        color = phaseColor,
        modifier = Modifier.testTag(TimerScreenTags.COUNTDOWN),
    )

    Spacer(Modifier.height(16.dp))

    Text(
        text = "Rep ${state.currentRep} of ${state.totalReps}",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag(TimerScreenTags.REP_COUNTER),
    )

    Spacer(Modifier.height(32.dp))

    LinearProgressIndicator(
        progress = { state.progress },
        modifier = Modifier.fillMaxWidth(),
        color = phaseColor,
        trackColor = phaseColor.copy(alpha = 0.2f),
    )

    Spacer(Modifier.height(48.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier
                .weight(1f)
                .testTag(TimerScreenTags.CANCEL_BUTTON),
        ) {
            Text("Cancel")
        }

        if (state.isRunning) {
            Button(
                onClick = onStop,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TimerScreenTags.PAUSE_BUTTON),
                colors = ButtonDefaults.buttonColors(containerColor = phaseColor),
            ) {
                Text("Pause")
            }
        } else {
            Button(
                onClick = onResume,
                modifier = Modifier
                    .weight(1f)
                    .testTag(TimerScreenTags.RESUME_BUTTON),
                colors = ButtonDefaults.buttonColors(containerColor = phaseColor),
            ) {
                Text("Resume")
            }
        }
    }
}

@Composable
private fun FinishedContent(onDone: () -> Unit) {
    Text(
        text = "Workout Complete!",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.testTag(TimerScreenTags.FINISHED_MESSAGE),
    )

    Spacer(Modifier.height(8.dp))

    Text(
        text = "Great job!",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(48.dp))

    Button(
        onClick = onDone,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TimerScreenTags.DONE_BUTTON),
    ) {
        Text("Done")
    }
}

internal fun Int.formatTime(): String {
    val minutes = this / 60
    val seconds = this % 60
    return if (minutes > 0) "$minutes:${seconds.toString().padStart(2, '0')}"
    else "$this"
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TimerWorkPreview() {
    MoveFastTDDTheme {
        TimerContent(
            state = TimerContract.State(
                config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10),
                phases = listOf(
                    TimerPhase.Work(30),
                    TimerPhase.Rest(10),
                    TimerPhase.Work(30),
                    TimerPhase.Finished
                ),
                currentPhaseIndex = 0,
                remainingSeconds = 24,
                isRunning = true,
            ),
            onStop = {}, onResume = {}, onCancel = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TimerRestPreview() {
    MoveFastTDDTheme {
        TimerContent(
            state = TimerContract.State(
                config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10),
                phases = listOf(
                    TimerPhase.Work(30),
                    TimerPhase.Rest(10),
                    TimerPhase.Work(30),
                    TimerPhase.Finished
                ),
                currentPhaseIndex = 1,
                remainingSeconds = 8,
                isRunning = true,
            ),
            onStop = {}, onResume = {}, onCancel = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TimerPausedPreview() {
    MoveFastTDDTheme {
        TimerContent(
            state = TimerContract.State(
                config = WorkoutConfig(reps = 3, repDuration = 30, restDuration = 10),
                phases = listOf(
                    TimerPhase.Work(30),
                    TimerPhase.Rest(10),
                    TimerPhase.Work(30),
                    TimerPhase.Finished
                ),
                currentPhaseIndex = 0,
                remainingSeconds = 15,
                isRunning = false,
            ),
            onStop = {}, onResume = {}, onCancel = {},
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun TimerFinishedPreview() {
    MoveFastTDDTheme {
        TimerContent(
            state = TimerContract.State(
                config = WorkoutConfig(reps = 2, repDuration = 30, restDuration = 10),
                phases = listOf(TimerPhase.Work(30), TimerPhase.Rest(10), TimerPhase.Finished),
                currentPhaseIndex = 2,
                remainingSeconds = 0,
                isRunning = false,
            ),
            onStop = {}, onResume = {}, onCancel = {},
        )
    }
}
