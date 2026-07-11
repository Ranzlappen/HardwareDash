package dev.ranzlappen.gadget.feature.radios.wifi.control

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [StandardWifiController] — the standard-flavor
 * [WifiController] implementation. Mirrors `StandardBluetoothControllerTest`:
 * every extreme-tier probe/mutation method is unconditionally
 * [WifiControllerResult.Unsupported] (no rooted tool chain to probe on this
 * flavor), while the two revert paths report a no-op
 * [WifiControllerResult.ResetCompleted] rather than `Unsupported` — there
 * being nothing to revert is a successful outcome, not a failure, so callers
 * (e.g. `RadiosScreen` dispose) don't need a flavor check before invoking them.
 */
class StandardWifiControllerTest {

    private val controller = StandardWifiController()

    @Test
    fun `rfkillToggle is unsupported`() = runTest {
        val result = controller.rfkillToggle(RfkillConfig(block = true, durationMillis = 1_000))
        assertEquals(WifiControllerResult.Unsupported, result)
    }

    @Test
    fun `txPowerOverride is unsupported`() = runTest {
        val result = controller.txPowerOverride(TxPowerConfig(targetDbm = 20, durationMillis = 1_000))
        assertEquals(WifiControllerResult.Unsupported, result)
    }

    @Test
    fun `channelOverride is unsupported`() = runTest {
        val result = controller.channelOverride(ChannelConfig(channel = 6, durationMillis = 1_000))
        assertEquals(WifiControllerResult.Unsupported, result)
    }

    @Test
    fun `probeInjectionCapability is unsupported`() = runTest {
        assertEquals(WifiControllerResult.Unsupported, controller.probeInjectionCapability())
    }

    @Test
    fun `resetAllWifiMutations reports a no-op completion`() = runTest {
        assertEquals(
            WifiControllerResult.ResetCompleted(restored = 0, failed = 0),
            controller.resetAllWifiMutations(),
        )
    }

    @Test
    fun `revertTxPowerOnly reports a no-op completion`() = runTest {
        assertEquals(
            WifiControllerResult.ResetCompleted(restored = 0, failed = 0),
            controller.revertTxPowerOnly(),
        )
    }
}
