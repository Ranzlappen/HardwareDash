package dev.ranzlappen.gadget.feature.adbdebug.automation

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingController
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingControllerResult
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbNetworkConfig
import dev.ranzlappen.gadget.feature.adbdebug.control.SetPropConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AdbDebugActionHandler]. Every branch reaches only the
 * injected [AdbDebuggingController], so — like `GpsActionHandlerTest` — the
 * whole surface is reachable from a plain JVM test with a mocked controller.
 */
class AdbDebugActionHandlerTest {

    private val controller = mockk<AdbDebuggingController>()
    private val handler = AdbDebugActionHandler(controller)

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(AdbDebugActionHandler.FEATURE_ID, handler.featureId)
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `every action requires root`() {
        assertTrue(handler.actions.isNotEmpty())
        assertTrue(handler.actions.all { it.requiresRoot })
    }

    @Test
    fun `toggle_adb_enabled defaults to true and maps Ok to Success`() = runTest {
        coEvery { controller.toggleAdbEnabled(true) } returns
            AdbDebuggingControllerResult.AdbToggleSnapshot(appliedEnabled = true, priorEnabled = false)

        val result = handler.dispatch(AdbDebugActionHandler.ACTION_TOGGLE_ADB_ENABLED, emptyMap())

        assertEquals(ActionResult.Success, result)
        coVerify { controller.toggleAdbEnabled(true) }
    }

    @Test
    fun `toggle_adb_enabled honours the enabled param`() = runTest {
        coEvery { controller.toggleAdbEnabled(false) } returns
            AdbDebuggingControllerResult.AdbToggleSnapshot(appliedEnabled = false, priorEnabled = true)

        handler.dispatch(
            AdbDebugActionHandler.ACTION_TOGGLE_ADB_ENABLED,
            mapOf(AdbDebugActionHandler.PARAM_ENABLED to "false"),
        )

        coVerify { controller.toggleAdbEnabled(false) }
    }

    @Test
    fun `toggle_adb_enabled Unsupported maps to a Failure`() = runTest {
        coEvery { controller.toggleAdbEnabled(any()) } returns AdbDebuggingControllerResult.Unsupported

        val result = handler.dispatch(AdbDebugActionHandler.ACTION_TOGGLE_ADB_ENABLED, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `toggle_adb_over_network builds config from params`() = runTest {
        coEvery { controller.toggleAdbOverNetwork(any()) } returns
            AdbDebuggingControllerResult.AdbNetworkSnapshot(appliedPort = 5560, priorPort = null)

        val result = handler.dispatch(
            AdbDebugActionHandler.ACTION_TOGGLE_ADB_OVER_NETWORK,
            mapOf(
                AdbDebugActionHandler.PARAM_ENABLED to "true",
                AdbDebugActionHandler.PARAM_PORT to "5560",
            ),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { controller.toggleAdbOverNetwork(AdbNetworkConfig(enabled = true, port = 5560)) }
    }

    @Test
    fun `toggle_adb_over_network defaults port to 5555 when omitted`() = runTest {
        coEvery { controller.toggleAdbOverNetwork(any()) } returns
            AdbDebuggingControllerResult.AdbNetworkSnapshot(appliedPort = 5555, priorPort = null)

        handler.dispatch(
            AdbDebugActionHandler.ACTION_TOGGLE_ADB_OVER_NETWORK,
            mapOf(AdbDebugActionHandler.PARAM_ENABLED to "true"),
        )

        coVerify { controller.toggleAdbOverNetwork(AdbNetworkConfig(enabled = true, port = 5555)) }
    }

    @Test
    fun `dump_properties defaults persist to false`() = runTest {
        coEvery { controller.dumpProperties(false) } returns
            AdbDebuggingControllerResult.PropertyDump(excerpt = "[foo]: [bar]")

        val result = handler.dispatch(AdbDebugActionHandler.ACTION_DUMP_PROPERTIES, emptyMap())

        assertEquals(ActionResult.Success, result)
        coVerify { controller.dumpProperties(false) }
    }

    @Test
    fun `dump_properties honours the persist param`() = runTest {
        coEvery { controller.dumpProperties(true) } returns
            AdbDebuggingControllerResult.PropertyDump(excerpt = "[foo]: [bar]", persistedFile = "/x/y.json")

        handler.dispatch(
            AdbDebugActionHandler.ACTION_DUMP_PROPERTIES,
            mapOf(AdbDebugActionHandler.PARAM_PERSIST to "true"),
        )

        coVerify { controller.dumpProperties(true) }
    }

    @Test
    fun `override_setprop fails fast without a key param`() = runTest {
        val result = handler.dispatch(AdbDebugActionHandler.ACTION_OVERRIDE_SETPROP, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `override_setprop dispatches SetPropConfig from params`() = runTest {
        coEvery { controller.overrideSystemProperty(any()) } returns
            AdbDebuggingControllerResult.SetpropSnapshot(
                key = "log.tag.MyTag",
                appliedValue = "VERBOSE",
                priorValue = null,
            )

        val result = handler.dispatch(
            AdbDebugActionHandler.ACTION_OVERRIDE_SETPROP,
            mapOf(
                AdbDebugActionHandler.PARAM_KEY to "log.tag.MyTag",
                AdbDebugActionHandler.PARAM_VALUE to "VERBOSE",
            ),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { controller.overrideSystemProperty(SetPropConfig(key = "log.tag.MyTag", value = "VERBOSE")) }
    }

    @Test
    fun `override_setprop HardwareError maps to a Failure with the message`() = runTest {
        coEvery { controller.overrideSystemProperty(any()) } returns
            AdbDebuggingControllerResult.HardwareError("ro.foo is read-only at the kernel level")

        val result = handler.dispatch(
            AdbDebugActionHandler.ACTION_OVERRIDE_SETPROP,
            mapOf(AdbDebugActionHandler.PARAM_KEY to "ro.foo", AdbDebugActionHandler.PARAM_VALUE to "1"),
        )

        assertEquals(ActionResult.Failure("ro.foo is read-only at the kernel level"), result)
    }

    @Test
    fun `RateLimited maps to a Failure`() = runTest {
        coEvery { controller.toggleAdbEnabled(any()) } returns AdbDebuggingControllerResult.RateLimited(2_000)

        val result = handler.dispatch(AdbDebugActionHandler.ACTION_TOGGLE_ADB_ENABLED, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `OptedOut maps to a Failure`() = runTest {
        coEvery { controller.toggleAdbEnabled(any()) } returns AdbDebuggingControllerResult.OptedOut

        val result = handler.dispatch(AdbDebugActionHandler.ACTION_TOGGLE_ADB_ENABLED, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }
}
