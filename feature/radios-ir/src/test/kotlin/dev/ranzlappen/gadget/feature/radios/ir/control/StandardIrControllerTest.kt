package dev.ranzlappen.gadget.feature.radios.ir.control

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [StandardIrController] — the standard-flavor [IrController]
 * implementation. `customCarrier` and `rawGpioPattern` are unconditionally
 * [IrControllerResult.Unsupported] (no rooted LIRC sysfs / GPIO access on this
 * flavor), while `resetAllIrMutations` reports a no-op [IrControllerResult.ResetCompleted]
 * rather than `Unsupported` — there being nothing to revert is a successful
 * outcome, not a failure, mirroring `StandardNfcControllerTest`.
 */
class StandardIrControllerTest {

    private val controller = StandardIrController()

    @Test
    fun `customCarrier is unsupported`() = runTest {
        val result = controller.customCarrier(IrCarrierConfig(carrierHz = 40_000, durationMillis = 1_000))

        assertEquals(IrControllerResult.Unsupported, result)
    }

    @Test
    fun `rawGpioPattern is unsupported`() = runTest {
        val result = controller.rawGpioPattern(
            IrRawPatternConfig(onMillis = 100, offMillis = 100, totalDurationMillis = 1_000),
        )

        assertEquals(IrControllerResult.Unsupported, result)
    }

    @Test
    fun `resetAllIrMutations reports a no-op completion`() = runTest {
        assertEquals(
            IrControllerResult.ResetCompleted(restored = 0, failed = 0),
            controller.resetAllIrMutations(),
        )
    }
}
