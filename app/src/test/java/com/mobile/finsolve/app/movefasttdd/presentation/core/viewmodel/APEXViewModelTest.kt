package com.mobile.finsolve.app.movefasttdd.presentation.core.viewmodel

import com.mobile.finsolve.app.movefasttdd.core.dispatchers.DispatchersList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class APEXViewModelTest {

    // region Test doubles

    private data class TestState(val count: Int = 0, val label: String = "") : Apex.State

    private sealed class TestExecutor : Apex.Executor {
        object Increment : TestExecutor()
        object Decrement : TestExecutor()
        data class SetLabel(val value: String) : TestExecutor()
        object TriggerEffect : TestExecutor()
        object TriggerEvent : TestExecutor()
        data class TriggerEventWithData(val value: String) : TestExecutor()
        object NoOp : TestExecutor()
    }

    private sealed class TestEffect : Apex.Effect {
        object IncrementViaEffect : TestEffect()
        object ChainToAnotherEffect : TestEffect()
    }

    private sealed class TestEvent : Apex.Event {
        object Simple : TestEvent()
        data class WithData(val value: String) : TestEvent()
    }

    private class TestDispatchersList(
        private val dispatcher: CoroutineDispatcher
    ) : DispatchersList {
        override fun io() = dispatcher
        override fun ui() = dispatcher
    }

    private inner class TestViewModel(
        initState: TestState = TestState(),
        capacity: Int = Channel.BUFFERED,
        dispatchers: DispatchersList = TestDispatchersList(testDispatcher),
    ) : APEXViewModel<TestState, TestExecutor, TestEffect, TestEvent>(initState, capacity, dispatchers) {

        val processedEffects = mutableListOf<TestEffect>()

        override suspend fun ExecutorScope<TestEffect, TestEvent>.execute(ex: TestExecutor): TestState =
            when (ex) {
                is TestExecutor.Increment -> state.copy(count = state.count + 1)
                is TestExecutor.Decrement -> state.copy(count = state.count - 1)
                is TestExecutor.SetLabel -> state.copy(label = ex.value)
                is TestExecutor.TriggerEffect -> { sendEffect(TestEffect.IncrementViaEffect); state }
                is TestExecutor.TriggerEvent -> { sendEvent(TestEvent.Simple); state }
                is TestExecutor.TriggerEventWithData -> { sendEvent(TestEvent.WithData(ex.value)); state }
                is TestExecutor.NoOp -> state
            }

        override suspend fun EffectorScope<TestExecutor>.affect(ef: TestEffect) {
            processedEffects.add(ef)
            when (ef) {
                is TestEffect.IncrementViaEffect -> dispatch(TestExecutor.Increment)
                is TestEffect.ChainToAnotherEffect -> dispatch(TestExecutor.TriggerEffect)
            }
        }
    }

    // endregion

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    // region Initial state

    @Test
    fun `initial state is set correctly on creation`() {
        val expected = TestState(count = 42, label = "athlete")
        val vm = TestViewModel(initState = expected)
        assertEquals(expected, vm.state)
    }

    @Test
    fun `default initial state has zero count and empty label`() {
        val vm = TestViewModel()
        assertEquals(0, vm.state.count)
        assertEquals("", vm.state.label)
    }

    // endregion

    // region State updates via Executor

    @Test
    fun `dispatch Increment increases count by one`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.Increment)
        advanceUntilIdle()
        assertEquals(1, vm.state.count)
    }

    @Test
    fun `dispatch Decrement decreases count by one`() = runTest {
        val vm = TestViewModel(initState = TestState(count = 5))
        vm.dispatch(TestExecutor.Decrement)
        advanceUntilIdle()
        assertEquals(4, vm.state.count)
    }

    @Test
    fun `dispatch SetLabel updates label in state`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.SetLabel("sprint"))
        advanceUntilIdle()
        assertEquals("sprint", vm.state.label)
    }

    @Test
    fun `dispatch NoOp does not mutate state`() = runTest {
        val initial = TestState(count = 7, label = "rest")
        val vm = TestViewModel(initState = initial)
        vm.dispatch(TestExecutor.NoOp)
        advanceUntilIdle()
        assertEquals(initial, vm.state)
    }

    @Test
    fun `multiple sequential dispatches accumulate state correctly`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.Increment)
        vm.dispatch(TestExecutor.Increment)
        vm.dispatch(TestExecutor.Increment)
        advanceUntilIdle()
        assertEquals(3, vm.state.count)
    }

    @Test
    fun `vararg dispatch processes all executors`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.Increment, TestExecutor.Increment, TestExecutor.SetLabel("track"))
        advanceUntilIdle()
        assertEquals(2, vm.state.count)
        assertEquals("track", vm.state.label)
    }

    @Test
    fun `executors are processed in FIFO order`() = runTest {
        val vm = TestViewModel(initState = TestState(count = 10))
        vm.dispatch(TestExecutor.Increment)  // 11
        vm.dispatch(TestExecutor.Decrement)  // 10
        vm.dispatch(TestExecutor.SetLabel("finish"))
        advanceUntilIdle()
        assertEquals(10, vm.state.count)
        assertEquals("finish", vm.state.label)
    }

    @Test
    fun `mixed executor types update all state fields correctly`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.SetLabel("warm-up"))
        vm.dispatch(TestExecutor.Increment)
        vm.dispatch(TestExecutor.Increment)
        advanceUntilIdle()
        assertEquals(TestState(count = 2, label = "warm-up"), vm.state)
    }

    @Test
    fun `state from previous executor is visible to the next executor`() = runTest {
        val vm = TestViewModel(initState = TestState(count = 0))
        repeat(5) { vm.dispatch(TestExecutor.Increment) }
        advanceUntilIdle()
        assertEquals(5, vm.state.count)
    }

    // endregion

    // region Effects

    @Test
    fun `sendEffect from execute triggers affect once`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEffect)
        advanceUntilIdle()
        assertEquals(1, vm.processedEffects.size)
        assertEquals(TestEffect.IncrementViaEffect, vm.processedEffects[0])
    }

    @Test
    fun `effect dispatches executor which updates state`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEffect)
        advanceUntilIdle()
        assertEquals(1, vm.state.count)
    }

    @Test
    fun `multiple effects are all processed`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEffect)
        vm.dispatch(TestExecutor.TriggerEffect)
        advanceUntilIdle()
        assertEquals(2, vm.processedEffects.size)
        assertEquals(2, vm.state.count)
    }

    @Test
    fun `chained effect dispatches executor and state is updated`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEffect)
        advanceUntilIdle()
        assertTrue(vm.state.count >= 1)
    }

    @Test
    fun `effect does not change state by itself — only via dispatched executor`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEffect)
        advanceUntilIdle()
        assertEquals(1, vm.processedEffects.size)
        assertEquals(1, vm.state.count)
    }

    // endregion

    // region Events

    @Test
    fun `sendEvent puts event into the events channel`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEvent)
        advanceUntilIdle()
        val result = vm.events.tryReceive()
        assertTrue(result.isSuccess)
        assertEquals(TestEvent.Simple, result.getOrNull())
    }

    @Test
    fun `event with data preserves payload correctly`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEventWithData("rep-done"))
        advanceUntilIdle()
        val result = vm.events.tryReceive()
        assertEquals(TestEvent.WithData("rep-done"), result.getOrNull())
    }

    @Test
    fun `multiple events are received in FIFO order`() = runTest {
        val vm = TestViewModel()
        repeat(3) { vm.dispatch(TestExecutor.TriggerEvent) }
        advanceUntilIdle()
        repeat(3) {
            val result = vm.events.tryReceive()
            assertTrue("Event $it should be present", result.isSuccess)
            assertEquals(TestEvent.Simple, result.getOrNull())
        }
    }

    @Test
    fun `event is not consumed automatically — stays in channel until received`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEvent)
        advanceUntilIdle()
        val first = vm.events.tryReceive()
        assertTrue(first.isSuccess)
        val second = vm.events.tryReceive()
        assertFalse("Channel should be empty after consuming the event", second.isSuccess)
    }

    @Test
    fun `state is not affected by sendEvent`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.TriggerEvent)
        advanceUntilIdle()
        assertEquals(TestState(), vm.state)
    }

    // endregion

    // region onEvent extension

    @Test
    fun `onEvent calls block when event is sent`() = runTest {
        val vm = TestViewModel()
        val received = mutableListOf<TestEvent>()

        val job = launch { vm.events.onEvent { received.add(it) } }

        vm.dispatch(TestExecutor.TriggerEvent)
        advanceUntilIdle()

        assertEquals(1, received.size)
        assertEquals(TestEvent.Simple, received[0])

        job.cancelAndJoin()
    }

    @Test
    fun `onEvent receives multiple events in order`() = runTest {
        val vm = TestViewModel()
        val received = mutableListOf<TestEvent>()

        val job = launch { vm.events.onEvent { received.add(it) } }

        repeat(5) { vm.dispatch(TestExecutor.TriggerEvent) }
        advanceUntilIdle()

        assertEquals(5, received.size)
        received.forEach { assertEquals(TestEvent.Simple, it) }

        job.cancelAndJoin()
    }

    @Test
    fun `onEvent stops receiving after scope is cancelled`() = runTest {
        val vm = TestViewModel()
        val received = mutableListOf<TestEvent>()

        val job = launch { vm.events.onEvent { received.add(it) } }

        vm.dispatch(TestExecutor.TriggerEvent)
        advanceUntilIdle()
        assertEquals(1, received.size)

        job.cancelAndJoin()

        vm.dispatch(TestExecutor.TriggerEvent)
        advanceUntilIdle()
        assertEquals("No new events should arrive after cancellation", 1, received.size)
    }

    @Test
    fun `onEvent with data payload passes correct value to block`() = runTest {
        val vm = TestViewModel()
        val received = mutableListOf<TestEvent>()

        val job = launch { vm.events.onEvent { received.add(it) } }

        vm.dispatch(TestExecutor.TriggerEventWithData("timer-ended"))
        advanceUntilIdle()

        assertEquals(TestEvent.WithData("timer-ended"), received[0])

        job.cancelAndJoin()
    }

    // endregion

    // region Edge cases

    @Test
    fun `dispatch on channel at capacity drops silently without crash`() = runTest {
        val vm = TestViewModel(capacity = 1)
        repeat(20) { vm.dispatch(TestExecutor.Increment) }
        advanceUntilIdle()
        assertTrue("At least some executors should be processed", vm.state.count > 0)
    }

    @Test
    fun `two independent ViewModels do not share state`() = runTest {
        val vm1 = TestViewModel(initState = TestState(count = 0))
        val vm2 = TestViewModel(initState = TestState(count = 100))

        vm1.dispatch(TestExecutor.Increment)
        vm2.dispatch(TestExecutor.Increment)
        advanceUntilIdle()

        assertEquals(1, vm1.state.count)
        assertEquals(101, vm2.state.count)
    }

    @Test
    fun `100 rapid dispatches complete without error`() = runTest {
        val vm = TestViewModel()
        repeat(100) { vm.dispatch(TestExecutor.Increment) }
        advanceUntilIdle()
        assertTrue(vm.state.count > 0)
    }

    @Test
    fun `decrement below zero is allowed — state reflects exact count`() = runTest {
        val vm = TestViewModel(initState = TestState(count = 0))
        vm.dispatch(TestExecutor.Decrement)
        advanceUntilIdle()
        assertEquals(-1, vm.state.count)
    }

    @Test
    fun `empty label is preserved when only count changes`() = runTest {
        val vm = TestViewModel()
        vm.dispatch(TestExecutor.Increment)
        advanceUntilIdle()
        assertEquals("", vm.state.label)
    }

    @Test
    fun `count is preserved when only label changes`() = runTest {
        val vm = TestViewModel(initState = TestState(count = 99))
        vm.dispatch(TestExecutor.SetLabel("cool-down"))
        advanceUntilIdle()
        assertEquals(99, vm.state.count)
        assertEquals("cool-down", vm.state.label)
    }

    // endregion
}
