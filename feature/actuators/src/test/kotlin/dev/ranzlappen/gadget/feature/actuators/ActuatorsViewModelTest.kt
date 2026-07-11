package dev.ranzlappen.gadget.feature.actuators

import android.content.Context
import android.os.Vibrator
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [ActuatorsViewModel]'s initial-state computation — the one
 * piece of real branching logic in this otherwise-passthrough view model:
 * `vibratorAvailable` from `Vibrator.hasVibrator()`, `hasAmplitudeControl`
 * (always false under a plain JVM test, since `Build.VERSION.SDK_INT == 0`
 * short-circuits the `SDK_INT >= O` guard before `hasAmplitudeControl()` is
 * ever read), and `isRootedFlavor` stamped from [RootCapabilityRegistry].
 */
class ActuatorsViewModelTest {

    private fun viewModel(vibrator: Vibrator?, isRootedFlavor: Boolean): ActuatorsViewModel {
        val context = mockk<Context>()
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator
        val registry = mockk<RootCapabilityRegistry>()
        every { registry.isRootedFlavor } returns isRootedFlavor
        return ActuatorsViewModel(context, registry)
    }

    @Test
    fun `reports vibratorAvailable true when the vibrator is present`() {
        val vibrator = mockk<Vibrator>()
        every { vibrator.hasVibrator() } returns true

        val state = viewModel(vibrator, isRootedFlavor = false).state.value

        assertEquals(true, state.vibratorAvailable)
    }

    @Test
    fun `reports vibratorAvailable false when there is no vibrator service`() {
        val state = viewModel(null, isRootedFlavor = false).state.value

        assertEquals(false, state.vibratorAvailable)
    }

    @Test
    fun `hasAmplitudeControl is always false under SDK_INT 0, regardless of the vibrator's own report`() {
        val vibrator = mockk<Vibrator>()
        every { vibrator.hasVibrator() } returns true
        every { vibrator.hasAmplitudeControl() } returns true

        val state = viewModel(vibrator, isRootedFlavor = false).state.value

        assertEquals(false, state.hasAmplitudeControl)
    }

    @Test
    fun `stamps isRootedFlavor from the capability registry`() {
        val stateRooted = viewModel(null, isRootedFlavor = true).state.value
        val stateStandard = viewModel(null, isRootedFlavor = false).state.value

        assertEquals(true, stateRooted.isRootedFlavor)
        assertEquals(false, stateStandard.isRootedFlavor)
    }
}
