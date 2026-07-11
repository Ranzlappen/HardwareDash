package dev.ranzlappen.gadget.feature.radios.ir.automation

import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.radios.ir.IrHardware
import dev.ranzlappen.gadget.feature.radios.ir.IrProtocol
import dev.ranzlappen.gadget.feature.radios.ir.IrSignal
import dev.ranzlappen.gadget.feature.radios.ir.IrSignalRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [IrActionHandler] — `:feature:radios-ir`'s automation
 * `ActionHandler` seam. `transmit_ir` either replays a saved signal looked up
 * by (case-insensitive) name, or — when no name is given, or the name
 * doesn't match anything in the library — builds an ad-hoc [IrSignal] from
 * the raw `protocol` / `payload` / `carrier_hz` / `repeats` params, with
 * NEC / 38000Hz / 1 repeat as the parse-failure defaults. Either way the
 * final dispatch to [IrHardware.transmit] decides success/failure.
 */
class IrActionHandlerTest {

    private val hardware = mockk<IrHardware>()
    private val repository = mockk<IrSignalRepository>()
    private val handler = IrActionHandler(hardware, repository)

    // ---- metadata ----

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(IrActionHandler.FEATURE_ID, handler.featureId)
        assertEquals("ir", handler.featureId)
    }

    @Test
    fun `declares exactly the transmit_ir action with its five params`() {
        assertEquals(1, handler.actions.size)
        val action = handler.actions.single()

        assertEquals(IrActionHandler.ACTION_TRANSMIT_IR, action.key)
        assertTrue(action.params.all { it.type == ActionParamType.Text })
        assertEquals(
            setOf(
                IrActionHandler.PARAM_NAME,
                IrActionHandler.PARAM_PROTOCOL,
                IrActionHandler.PARAM_PAYLOAD,
                IrActionHandler.PARAM_CARRIER_HZ,
                IrActionHandler.PARAM_REPEATS,
            ),
            action.params.map { it.name }.toSet(),
        )
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch("not-a-real-action", emptyMap())

        assertEquals(ActionResult.Unsupported, result)
    }

    // ---- saved-signal-by-name path ----

    @Test
    fun `transmit_ir replays a saved signal matched by name case-insensitively`() = runTest {
        val saved = IrSignal(
            id = "1",
            name = "Living Room TV Power",
            protocol = IrProtocol.NEC,
            payload = "0x20DF10EF",
            carrierHz = 38_000,
            repeats = 1,
        )
        every { repository.signals } returns flowOf(listOf(saved))
        coEvery { hardware.transmit(saved) } returns null

        val result = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(IrActionHandler.PARAM_NAME to "living room tv power"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { hardware.transmit(saved) }
    }

    @Test
    fun `transmit_ir reports a failure when the hardware transmit fails for a saved signal`() = runTest {
        val saved = IrSignal(id = "1", name = "TV Power", protocol = IrProtocol.NEC, payload = "0x1")
        every { repository.signals } returns flowOf(listOf(saved))
        coEvery { hardware.transmit(saved) } returns "No IR emitter on this device"

        val result = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(IrActionHandler.PARAM_NAME to "TV Power"),
        )

        assertEquals(ActionResult.Failure("No IR emitter on this device"), result)
    }

    // ---- ad-hoc signal path ----

    @Test
    fun `transmit_ir fails when the name doesn't match a saved signal and no payload is given`() = runTest {
        every { repository.signals } returns flowOf(emptyList())

        val result = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(IrActionHandler.PARAM_NAME to "Unknown Signal"),
        )

        assertEquals(
            ActionResult.Failure("payload is required when no saved signal name is given"),
            result,
        )
    }

    @Test
    fun `transmit_ir fails when neither a name nor a payload is given`() = runTest {
        val result = handler.dispatch(IrActionHandler.ACTION_TRANSMIT_IR, emptyMap())

        assertEquals(
            ActionResult.Failure("payload is required when no saved signal name is given"),
            result,
        )
    }

    @Test
    fun `transmit_ir builds an ad-hoc signal from raw params when no name is given`() = runTest {
        coEvery { hardware.transmit(any()) } returns null

        val result = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(
                IrActionHandler.PARAM_PAYLOAD to "0xABCD",
                IrActionHandler.PARAM_PROTOCOL to "raw",
                IrActionHandler.PARAM_CARRIER_HZ to "40000",
                IrActionHandler.PARAM_REPEATS to "3",
            ),
        )

        assertEquals(ActionResult.Success, result)
        coVerify {
            hardware.transmit(
                match {
                    it.name == "Automation" &&
                        it.protocol == IrProtocol.RAW &&
                        it.payload == "0xABCD" &&
                        it.carrierHz == 40_000 &&
                        it.repeats == 3
                },
            )
        }
    }

    @Test
    fun `transmit_ir defaults protocol to NEC when the protocol param is missing or unrecognised`() = runTest {
        coEvery { hardware.transmit(any()) } returns null

        val result = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(IrActionHandler.PARAM_PAYLOAD to "0x1", IrActionHandler.PARAM_PROTOCOL to "not-a-protocol"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { hardware.transmit(match { it.protocol == IrProtocol.NEC }) }
    }

    @Test
    fun `transmit_ir defaults carrier and repeats when their params are missing or unparsable`() = runTest {
        coEvery { hardware.transmit(any()) } returns null

        val result = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(
                IrActionHandler.PARAM_PAYLOAD to "0x1",
                IrActionHandler.PARAM_CARRIER_HZ to "not-a-number",
                IrActionHandler.PARAM_REPEATS to "not-a-number",
            ),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { hardware.transmit(match { it.carrierHz == 38_000 && it.repeats == 1 }) }
    }

    @Test
    fun `transmit_ir clamps repeats to the 1 to 10 range`() = runTest {
        coEvery { hardware.transmit(any()) } returns null

        val tooMany = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(IrActionHandler.PARAM_PAYLOAD to "0x1", IrActionHandler.PARAM_REPEATS to "99"),
        )
        val tooFew = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(IrActionHandler.PARAM_PAYLOAD to "0x1", IrActionHandler.PARAM_REPEATS to "0"),
        )

        assertEquals(ActionResult.Success, tooMany)
        assertEquals(ActionResult.Success, tooFew)
        coVerify { hardware.transmit(match { it.repeats == 10 }) }
        coVerify { hardware.transmit(match { it.repeats == 1 }) }
    }

    @Test
    fun `transmit_ir fails when the payload param is blank`() = runTest {
        val result = handler.dispatch(
            IrActionHandler.ACTION_TRANSMIT_IR,
            mapOf(IrActionHandler.PARAM_PAYLOAD to "   "),
        )

        assertEquals(
            ActionResult.Failure("payload is required when no saved signal name is given"),
            result,
        )
    }
}
