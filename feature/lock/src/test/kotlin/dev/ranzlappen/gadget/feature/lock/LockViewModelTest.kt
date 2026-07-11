package dev.ranzlappen.gadget.feature.lock

import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [LockViewModel] — the one piece of real logic beyond
 * passthrough is stamping [RootCapabilityRegistry.isRootedFlavor] onto every
 * [LockState] emitted by [LockMonitor.state], including the initial value
 * (constructed independently of the monitor's flow) and subsequent updates.
 */
class LockViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(isRootedFlavor: Boolean, monitorState: MutableStateFlow<LockState>): LockViewModel {
        val monitor = mockk<LockMonitor>()
        every { monitor.state } returns monitorState
        val registry = mockk<RootCapabilityRegistry>()
        every { registry.isRootedFlavor } returns isRootedFlavor
        return LockViewModel(monitor, registry)
    }

    @Test
    fun `initial state carries isRootedFlavor before the monitor flow is collected`() {
        val vm = viewModel(isRootedFlavor = true, monitorState = MutableStateFlow(LockState()))

        assertEquals(true, vm.state.value.isRootedFlavor)
    }

    @Test
    fun `initial state is not rooted on the standard flavor`() {
        val vm = viewModel(isRootedFlavor = false, monitorState = MutableStateFlow(LockState()))

        assertEquals(false, vm.state.value.isRootedFlavor)
    }

    @Test
    fun `stamps isRootedFlavor onto every monitor state update`() {
        val monitorState = MutableStateFlow(LockState(isLocked = false))
        val vm = viewModel(isRootedFlavor = true, monitorState = monitorState)

        monitorState.value = LockState(isLocked = true, isSecure = true, hasBiometric = true)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            LockState(isLocked = true, isSecure = true, hasBiometric = true, isRootedFlavor = true),
            vm.state.value,
        )
    }

    @Test
    fun `does not mutate the monitor's own isRootedFlavor field`() {
        // The monitor never sets isRootedFlavor itself (that's the view model's job) —
        // guard against a future regression where the mapping is dropped.
        val monitorState = MutableStateFlow(LockState(isLocked = true, isRootedFlavor = false))
        val vm = viewModel(isRootedFlavor = true, monitorState = monitorState)

        assertEquals(true, vm.state.value.isRootedFlavor)
    }
}
