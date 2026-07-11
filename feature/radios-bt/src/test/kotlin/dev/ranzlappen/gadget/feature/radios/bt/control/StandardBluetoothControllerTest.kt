package dev.ranzlappen.gadget.feature.radios.bt.control

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [StandardBluetoothController] — the standard-flavor
 * [BluetoothController] implementation. Every extreme-tier probe/mutation
 * method is unconditionally [BluetoothControllerResult.Unsupported] (no
 * rooted tool chain to probe on this flavor), while the two revert paths
 * report a no-op [BluetoothControllerResult.ResetCompleted] rather than
 * `Unsupported` — there being nothing to revert is a successful outcome,
 * not a failure, so callers (e.g. `RadiosScreen` dispose) don't need a
 * flavor check before invoking them.
 */
class StandardBluetoothControllerTest {

    private val controller = StandardBluetoothController()

    @Test
    fun `rfkillToggle is unsupported`() = runTest {
        val result = controller.rfkillToggle(BluetoothRfkillConfig(block = true, durationMillis = 1_000))
        assertEquals(BluetoothControllerResult.Unsupported, result)
    }

    @Test
    fun `txPowerOverride is unsupported`() = runTest {
        val result = controller.txPowerOverride(BluetoothTxPowerConfig(targetDbm = 4, durationMillis = 1_000))
        assertEquals(BluetoothControllerResult.Unsupported, result)
    }

    @Test
    fun `hciSnoopDump is unsupported`() = runTest {
        assertEquals(BluetoothControllerResult.Unsupported, controller.hciSnoopDump())
    }

    @Test
    fun `resetAllBluetoothMutations reports a no-op completion`() = runTest {
        assertEquals(
            BluetoothControllerResult.ResetCompleted(restored = 0, failed = 0),
            controller.resetAllBluetoothMutations(),
        )
    }

    @Test
    fun `revertTxPowerOnly reports a no-op completion`() = runTest {
        assertEquals(
            BluetoothControllerResult.ResetCompleted(restored = 0, failed = 0),
            controller.revertTxPowerOnly(),
        )
    }
}
