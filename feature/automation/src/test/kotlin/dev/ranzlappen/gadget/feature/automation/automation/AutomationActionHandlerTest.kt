package dev.ranzlappen.gadget.feature.automation.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.automation.control.AutomationController
import dev.ranzlappen.gadget.feature.automation.control.AutomationControllerResult
import dev.ranzlappen.gadget.feature.automation.control.PrivilegedIntentConfig
import dev.ranzlappen.gadget.feature.automation.control.PrivilegedIntentVerb
import dev.ranzlappen.gadget.feature.automation.control.SystemSettingsOverrideConfig
import dev.ranzlappen.gadget.feature.automation.control.SystemSettingsScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AutomationActionHandler]. Every branch reaches only the
 * injected [AutomationController], so the whole surface is reachable from a
 * plain JVM test with a mocked controller.
 */
class AutomationActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val controller = mockk<AutomationController>()
    private val handler = AutomationActionHandler(context, controller)

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(AutomationActionHandler.FEATURE_ID, handler.featureId)
    }

    @Test
    fun `every action requires root`() {
        assertTrue(handler.actions.isNotEmpty())
        assertTrue(handler.actions.all { it.requiresRoot })
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `fire_privileged_intent fails fast without an action param`() = runTest {
        val result = handler.dispatch(AutomationActionHandler.ACTION_FIRE_INTENT, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `fire_privileged_intent dispatches a broadcast by default`() = runTest {
        coEvery { controller.firePrivilegedIntent(any()) } returns AutomationControllerResult.IntentResult(0, "")

        val result = handler.dispatch(
            AutomationActionHandler.ACTION_FIRE_INTENT,
            mapOf(AutomationActionHandler.PARAM_ACTION to "android.intent.action.MY_ACTION"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify {
            controller.firePrivilegedIntent(
                PrivilegedIntentConfig(
                    verb = PrivilegedIntentVerb.BROADCAST,
                    action = "android.intent.action.MY_ACTION",
                    componentFlatten = null,
                ),
            )
        }
    }

    @Test
    fun `fire_privileged_intent honours verb and component params`() = runTest {
        coEvery { controller.firePrivilegedIntent(any()) } returns AutomationControllerResult.IntentResult(0, "")

        handler.dispatch(
            AutomationActionHandler.ACTION_FIRE_INTENT,
            mapOf(
                AutomationActionHandler.PARAM_VERB to PrivilegedIntentVerb.START_SERVICE.name,
                AutomationActionHandler.PARAM_ACTION to "com.example.ACTION",
                AutomationActionHandler.PARAM_COMPONENT to "com.example/.MyService",
            ),
        )

        coVerify {
            controller.firePrivilegedIntent(
                PrivilegedIntentConfig(
                    verb = PrivilegedIntentVerb.START_SERVICE,
                    action = "com.example.ACTION",
                    componentFlatten = "com.example/.MyService",
                ),
            )
        }
    }

    @Test
    fun `fire_privileged_intent HardwareError maps to a Failure with the message`() = runTest {
        coEvery { controller.firePrivilegedIntent(any()) } returns
            AutomationControllerResult.HardwareError("action REBOOT is on the deny-list")

        val result = handler.dispatch(
            AutomationActionHandler.ACTION_FIRE_INTENT,
            mapOf(AutomationActionHandler.PARAM_ACTION to "REBOOT"),
        )

        assertEquals(ActionResult.Failure("action REBOOT is on the deny-list"), result)
    }

    @Test
    fun `override_system_setting fails fast without key or value params`() = runTest {
        val result = handler.dispatch(AutomationActionHandler.ACTION_OVERRIDE_SETTING, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `override_system_setting dispatches config from params`() = runTest {
        coEvery { controller.overrideSystemSetting(any()) } returns AutomationControllerResult.Ok()

        val result = handler.dispatch(
            AutomationActionHandler.ACTION_OVERRIDE_SETTING,
            mapOf(
                AutomationActionHandler.PARAM_SCOPE to SystemSettingsScope.GLOBAL.name,
                AutomationActionHandler.PARAM_KEY to "adb_enabled",
                AutomationActionHandler.PARAM_VALUE to "1",
            ),
        )

        assertEquals(ActionResult.Success, result)
        coVerify {
            controller.overrideSystemSetting(
                SystemSettingsOverrideConfig(scope = SystemSettingsScope.GLOBAL, key = "adb_enabled", value = "1"),
            )
        }
    }

    @Test
    fun `dumpsys_snapshot maps DumpsysExcerpt to Success`() = runTest {
        coEvery { controller.dumpsysSnapshot() } returns AutomationControllerResult.DumpsysExcerpt(emptyMap())

        val result = handler.dispatch(AutomationActionHandler.ACTION_DUMPSYS_SNAPSHOT, emptyMap())

        assertEquals(ActionResult.Success, result)
        coVerify { controller.dumpsysSnapshot() }
    }

    @Test
    fun `reset_all_mutations maps ResetCompleted to Success`() = runTest {
        coEvery { controller.resetAllAutomationMutations() } returns
            AutomationControllerResult.ResetCompleted(restored = 2, failed = 0)

        val result = handler.dispatch(AutomationActionHandler.ACTION_RESET_ALL, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `Unsupported maps to a Failure`() = runTest {
        coEvery { controller.dumpsysSnapshot() } returns AutomationControllerResult.Unsupported

        val result = handler.dispatch(AutomationActionHandler.ACTION_DUMPSYS_SNAPSHOT, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `RateLimited maps to a Failure`() = runTest {
        coEvery { controller.dumpsysSnapshot() } returns AutomationControllerResult.RateLimited(2_000)

        val result = handler.dispatch(AutomationActionHandler.ACTION_DUMPSYS_SNAPSHOT, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `OptedOut maps to a Failure`() = runTest {
        coEvery { controller.dumpsysSnapshot() } returns AutomationControllerResult.OptedOut

        val result = handler.dispatch(AutomationActionHandler.ACTION_DUMPSYS_SNAPSHOT, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }
}
