package dev.ranzlappen.gadget.feature.radios.bt.automation

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.radios.bt.BluetoothAdapterWrapper
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [BtActionHandler] — `:feature:radios-bt`'s automation
 * `ActionHandler` seam. The handler only reads
 * [BluetoothAdapterWrapper.isEnabled], so — mirroring `CellActionHandlerTest`
 * / `SensorsActionHandlerTest` — the whole dispatch surface is reachable from
 * a plain JVM test with a mocked adapter.
 */
class BtActionHandlerTest {

    private val adapter = mockk<BluetoothAdapterWrapper>()
    private val handler = BtActionHandler(adapter)

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(BtActionHandler.FEATURE_ID, handler.featureId)
        assertEquals("bluetooth", handler.featureId)
    }

    @Test
    fun `declares exactly the check-enabled action`() {
        assertEquals(1, handler.actions.size)
        assertEquals(BtActionHandler.ACTION_CHECK_ENABLED, handler.actions.single().key)
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `bt_connect_check succeeds when the adapter is enabled`() = runTest {
        every { adapter.isEnabled() } returns true

        val result = handler.dispatch(BtActionHandler.ACTION_CHECK_ENABLED, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `bt_connect_check fails with a reason when the adapter is disabled`() = runTest {
        every { adapter.isEnabled() } returns false

        val result = handler.dispatch(BtActionHandler.ACTION_CHECK_ENABLED, emptyMap())

        assertEquals(ActionResult.Failure("Bluetooth is not enabled"), result)
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `bt_connect_check ignores unrecognised params`() = runTest {
        every { adapter.isEnabled() } returns true

        val result = handler.dispatch(
            BtActionHandler.ACTION_CHECK_ENABLED,
            mapOf("unused" to "value"),
        )

        assertEquals(ActionResult.Success, result)
    }
}
