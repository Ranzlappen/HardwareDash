package dev.ranzlappen.gadget.feature.apps.rooted.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.apps.root.AppsRootController
import dev.ranzlappen.gadget.feature.apps.root.AppsRootControllerResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AppsRootActionHandler]. Every branch reaches only the
 * injected [AppsRootController], so — like `TorchActionHandlerTest` /
 * `AdbDebugActionHandlerTest` — the whole surface is reachable from a
 * plain JVM test with a relaxed [Context] mock (the `getString` calls in
 * the `actions` list just resolve to `""`) and a mocked controller; the
 * deny-list / RootSafetyGate plumbing lives in `RootedAppsRootController`
 * and isn't exercised here.
 */
class AppsRootActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val controller = mockk<AppsRootController>()
    private val handler = AppsRootActionHandler(context, controller)

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(AppsRootActionHandler.FEATURE_ID, handler.featureId)
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch(
            "not-a-real-action",
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.example.app"),
        )
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `every action requires root and takes a package_name param`() {
        assertTrue(handler.actions.isNotEmpty())
        assertTrue(handler.actions.all { it.requiresRoot })
        assertTrue(
            handler.actions.all { action ->
                action.params.any { it.name == AppsRootActionHandler.PARAM_PACKAGE_NAME }
            },
        )
    }

    @Test
    fun `dispatch fails fast without a package_name param`() = runTest {
        val result = handler.dispatch(AppsRootActionHandler.ACTION_FREEZE, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `freeze dispatches to the controller and maps Ok to Success`() = runTest {
        coEvery { controller.freezeApp("com.example.app") } returns
            AppsRootControllerResult.Ok(statusNote = "com.example.app disabled")

        val result = handler.dispatch(
            AppsRootActionHandler.ACTION_FREEZE,
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.example.app"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { controller.freezeApp("com.example.app") }
    }

    @Test
    fun `unfreeze dispatches to the controller`() = runTest {
        coEvery { controller.unfreezeApp("com.example.app") } returns
            AppsRootControllerResult.Ok(statusNote = "com.example.app enabled")

        val result = handler.dispatch(
            AppsRootActionHandler.ACTION_UNFREEZE,
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.example.app"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { controller.unfreezeApp("com.example.app") }
    }

    @Test
    fun `force_stop dispatches to the controller`() = runTest {
        coEvery { controller.forceStopApp("com.example.app") } returns
            AppsRootControllerResult.Ok(statusNote = "com.example.app force-stopped")

        val result = handler.dispatch(
            AppsRootActionHandler.ACTION_FORCE_STOP,
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.example.app"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { controller.forceStopApp("com.example.app") }
    }

    @Test
    fun `Denied maps to a Failure with the message`() = runTest {
        coEvery { controller.freezeApp("com.android.systemui") } returns
            AppsRootControllerResult.Denied("com.android.systemui is a protected system package")

        val result = handler.dispatch(
            AppsRootActionHandler.ACTION_FREEZE,
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.android.systemui"),
        )

        assertEquals(
            ActionResult.Failure("com.android.systemui is a protected system package"),
            result,
        )
    }

    @Test
    fun `Unsupported maps to a Failure`() = runTest {
        coEvery { controller.freezeApp(any()) } returns AppsRootControllerResult.Unsupported

        val result = handler.dispatch(
            AppsRootActionHandler.ACTION_FREEZE,
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.example.app"),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `OptedOut maps to a Failure`() = runTest {
        coEvery { controller.freezeApp(any()) } returns AppsRootControllerResult.OptedOut

        val result = handler.dispatch(
            AppsRootActionHandler.ACTION_FREEZE,
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.example.app"),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `RateLimited maps to a Failure`() = runTest {
        coEvery { controller.freezeApp(any()) } returns AppsRootControllerResult.RateLimited(2_000)

        val result = handler.dispatch(
            AppsRootActionHandler.ACTION_FREEZE,
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.example.app"),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `HardwareError maps to a Failure with the message`() = runTest {
        coEvery { controller.forceStopApp("com.example.app") } returns
            AppsRootControllerResult.HardwareError("am force-stop exited non-zero")

        val result = handler.dispatch(
            AppsRootActionHandler.ACTION_FORCE_STOP,
            mapOf(AppsRootActionHandler.PARAM_PACKAGE_NAME to "com.example.app"),
        )

        assertEquals(ActionResult.Failure("am force-stop exited non-zero"), result)
    }
}
