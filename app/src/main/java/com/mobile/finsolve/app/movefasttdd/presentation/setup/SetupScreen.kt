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

object SetupScreenTags {
    const val REPS_FIELD = "setup_reps_field"
    const val REP_DURATION_FIELD = "setup_rep_duration_field"
    const val REST_DURATION_FIELD = "setup_rest_duration_field"
    const val START_BUTTON = "setup_start_button"
    const val ERROR_MESSAGE = "setup_error_message"
}
