package com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

interface ExecutorScope<EF : Apex.Effect, EV : Apex.Event> {
    fun sendEffect(ef: EF)
    fun sendEvent(event: EV)
}

interface EffectorScope<EX : Apex.Executor> {
    fun dispatch(vararg ex: EX)
}

suspend fun <EV : Apex.Event> Channel<EV>.onEvent(block: (EV) -> Unit) {
    for (event in this) block(event)
}

abstract class APEXViewModel<S : Apex.State, EX : Apex.Executor, EF : Apex.Effect, EV : Apex.Event>(
    initState: S,
    capacity: Int = Channel.BUFFERED,
    private val dispatchers: DispatchersList = DispatchersList.Base(),
) : ViewModel() {

    private val executorScope = ExecutorScopeImpl()
    private val effectorScope = EffectorScopeImpl()
    private val executors = Channel<EX>(capacity)
    private val effectors = Channel<EF>(capacity)

    private val _state = mutableStateOf(initState)
    val state get() = _state.value

    val events = Channel<EV>(capacity)

    init {
        proceed()
        effector()
    }

    fun dispatch(vararg ex: EX) {
        ex.forEach { executors.trySend(it) }
    }

    private fun proceed() = viewModelScope.launch(dispatchers.ui()) {
        for (ex in executors) {
            _state.value = executorScope.execute(ex)
        }
    }

    private fun effector() = viewModelScope.launch(dispatchers.io()) {
        for (ef in effectors) {
            launch { effectorScope.affect(ef) }
        }
    }

    private inner class ExecutorScopeImpl : ExecutorScope<EF, EV> {
        override fun sendEffect(ef: EF) {
            effectors.trySend(ef)
        }

        override fun sendEvent(event: EV) {
            events.trySend(event)
        }
    }

    private inner class EffectorScopeImpl : EffectorScope<EX> {
        override fun dispatch(vararg ex: EX) {
            ex.forEach { executors.trySend(it) }
        }
    }

    protected abstract suspend fun ExecutorScope<EF, EV>.execute(ex: EX): S

    protected abstract suspend fun EffectorScope<EX>.affect(ef: EF)
}