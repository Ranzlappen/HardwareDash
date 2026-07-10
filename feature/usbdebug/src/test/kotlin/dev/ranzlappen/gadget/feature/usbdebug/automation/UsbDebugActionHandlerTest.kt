package dev.ranzlappen.gadget.feature.usbdebug.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingController
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbDebuggingControllerResult
import dev.ranzlappen.gadget.feature.usbdebug.control.UsbFunctionType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [UsbDebugActionHandler]. Mirrors `TorchActionHandlerTest`'s
 * shape: a relaxed [Context] mock (only `getString` return values matter for
 * [UsbDebugActionHandler.actions] labels, which these tests don't assert on)
 * and a mocked controller so every branch — including the
 * [UsbDebuggingControllerResult] → [ActionResult] mapping — is reachable
 * without a real Android runtime.
 */
class UsbDebugActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val controller = mockk<UsbDebuggingController>(relaxed = true)
    private val handler = UsbDebugActionHandler(context, controller)

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(UsbDebugActionHandler.FEATURE_ID, handler.featureId)
    }

    @Test
    fun `all four actions are declared and require root`() {
        val keys = handler.actions.map { it.key }
        assertEquals(
            setOf(
                UsbDebugActionHandler.ACTION_SWITCH_FUNCTION,
                UsbDebugActionHandler.ACTION_DUMP_USB,
                UsbDebugActionHandler.ACTION_DUMP_SERIAL_SERVICE,
                UsbDebugActionHandler.ACTION_DUMP_USB_DEVICES_DEBUG,
            ),
            keys.toSet(),
        )
        assertTrue(handler.actions.all { it.requiresRoot })
    }

    @Test
    fun `unknown action is unsupported`() = runBlocking {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `switch_function parses the wire-name param and dispatches to the controller`() = runBlocking {
        coEvery { controller.switchUsbFunction(UsbFunctionType.RNDIS) } returns
            UsbDebuggingControllerResult.UsbFunctionSnapshot(
                appliedFunction = UsbFunctionType.RNDIS,
                priorFunction = "mtp",
            )

        val result = handler.dispatch(
            UsbDebugActionHandler.ACTION_SWITCH_FUNCTION,
            mapOf(UsbDebugActionHandler.PARAM_FUNCTION to "rndis"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { controller.switchUsbFunction(UsbFunctionType.RNDIS) }
    }

    @Test
    fun `switch_function falls back to MTP when the param is missing`() = runBlocking {
        coEvery { controller.switchUsbFunction(UsbFunctionType.MTP) } returns
            UsbDebuggingControllerResult.UsbFunctionSnapshot(
                appliedFunction = UsbFunctionType.MTP,
                priorFunction = null,
            )

        val result = handler.dispatch(UsbDebugActionHandler.ACTION_SWITCH_FUNCTION, emptyMap())

        assertEquals(ActionResult.Success, result)
        coVerify { controller.switchUsbFunction(UsbFunctionType.MTP) }
    }

    @Test
    fun `dump_usb maps an excerpt result to Success`() = runBlocking {
        coEvery { controller.dumpUsb() } returns
            UsbDebuggingControllerResult.UsbDumpExcerpt(excerpt = "…", source = "dumpsys usb")

        val result = handler.dispatch(UsbDebugActionHandler.ACTION_DUMP_USB, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `dump_serial_service maps Unsupported to a Failure`() = runBlocking {
        coEvery { controller.dumpSerialService() } returns UsbDebuggingControllerResult.Unsupported

        val result = handler.dispatch(UsbDebugActionHandler.ACTION_DUMP_SERIAL_SERVICE, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `dump_usb_devices_debug maps a HardwareError to a Failure carrying the message`() = runBlocking {
        coEvery { controller.dumpUsbDevicesDebug() } returns
            UsbDebuggingControllerResult.HardwareError("debugfs read failed")

        val result = handler.dispatch(UsbDebugActionHandler.ACTION_DUMP_USB_DEVICES_DEBUG, emptyMap())

        assertEquals(ActionResult.Failure("debugfs read failed"), result)
    }

    @Test
    fun `switch_function maps OptedOut to a Failure`() = runBlocking {
        coEvery { controller.switchUsbFunction(any()) } returns UsbDebuggingControllerResult.OptedOut

        val result = handler.dispatch(UsbDebugActionHandler.ACTION_SWITCH_FUNCTION, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `switch_function maps RateLimited to a Failure`() = runBlocking {
        coEvery { controller.switchUsbFunction(any()) } returns
            UsbDebuggingControllerResult.RateLimited(retryAfterMillis = 5_000)

        val result = handler.dispatch(UsbDebugActionHandler.ACTION_SWITCH_FUNCTION, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }
}
