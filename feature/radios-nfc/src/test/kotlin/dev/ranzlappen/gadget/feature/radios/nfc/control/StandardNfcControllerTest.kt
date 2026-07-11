package dev.ranzlappen.gadget.feature.radios.nfc.control

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [StandardNfcController] — the standard-flavor [NfcController]
 * implementation. `sendRawNciCommand` is unconditionally
 * [NfcControllerResult.Unsupported] (no rooted sysfs access on this flavor),
 * while `resetAllNfcMutations` reports a no-op [NfcControllerResult.ResetCompleted]
 * rather than `Unsupported` — there being nothing to revert is a successful
 * outcome, not a failure, mirroring `StandardBluetoothControllerTest`'s revert
 * paths.
 */
class StandardNfcControllerTest {

    private val controller = StandardNfcController()

    @Test
    fun `sendRawNciCommand is unsupported`() = runTest {
        val result = controller.sendRawNciCommand(RawNciCommandConfig(payloadHex = "00A1B2"))

        assertEquals(NfcControllerResult.Unsupported, result)
    }

    @Test
    fun `resetAllNfcMutations reports a no-op completion`() = runTest {
        assertEquals(
            NfcControllerResult.ResetCompleted(restored = 0, failed = 0),
            controller.resetAllNfcMutations(),
        )
    }
}
