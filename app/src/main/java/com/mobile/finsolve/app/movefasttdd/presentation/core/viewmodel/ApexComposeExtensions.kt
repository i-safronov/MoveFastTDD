package com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.channels.Channel

@Composable
fun <EV: Apex.Event> OnEvent(events: Channel<EV>, block: (EV) -> Unit) {
    LaunchedEffect(Unit) {
        events.onEvent(
            block = block
        )
    }
}