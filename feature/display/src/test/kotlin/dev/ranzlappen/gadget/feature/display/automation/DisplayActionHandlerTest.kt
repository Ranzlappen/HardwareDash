package dev.ranzlappen.gadget.feature.display.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.display.control.BrightnessOverrideConfig
import dev.ranzlappen.gadget.feature.display.control.DensityOverrideConfig
import dev.ranzlappen.gadget.feature.display.control.DisplayController
import dev.ranzlappen.gadget.feature.display.control.DisplayControllerResult
import dev.ranzlappen.gadget.feature.display.control.RefreshRateOverrideConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [DisplayActionHandler]. [DisplayActionHandler.ACTION_SET_BRIGHTNESS]
 * writes `Settings.System` directly, which needs a real content provider to
 * observe — so its happy path is left for an instrumented test; the
 * branches pinned here reach only the injected [DisplayController] mock
 * and the action metadata, mirroring `VibrationActionHandlerTest`'s split
 * between controller-only branches and service/Intent branches.
 */
class DisplayActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val controller = mockk<DisplayController>(relaxed = true)
    private val handler = DisplayActionHandler(context, controller)

    @Test
    fun `unknown action is unsupported`() = runTest {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(DisplayActionHandler.FEATURE_ID, handler.featureId)
    }

    @Test
    fun `rooted actions are flagged requiresRoot, brightness set is not`() {
        val rootActions = handler.actions.filter { it.requiresRoot }.map { it.key }
        assertTrue(DisplayActionHandler.ACTION_OVERRIDE_BRIGHTNESS_EXTREME in rootActions)
        assertTrue(DisplayActionHandler.ACTION_OVERRIDE_REFRESH_RATE in rootActions)
        assertTrue(DisplayActionHandler.ACTION_OVERRIDE_DENSITY in rootActions)
        assertTrue(DisplayActionHandler.ACTION_RESET_ALL in rootActions)
        assertTrue(DisplayActionHandler.ACTION_SET_BRIGHTNESS !in rootActions)
    }

    @Test
    fun `override density clamps the dpi param and maps Ok to Success`() = runTest {
        coEvery { controller.overrideDensity(DensityOverrideConfig(dpi = 560)) } returns DisplayControllerResult.Ok()

        val result = handler.dispatch(
            DisplayActionHandler.ACTION_OVERRIDE_DENSITY,
            mapOf(DisplayActionHandler.PARAM_DPI to "9999"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify { controller.overrideDensity(DensityOverrideConfig(dpi = 560)) }
    }

    @Test
    fun `override refresh rate maps Unsupported to a Failure with reason`() = runTest {
        coEvery { controller.overrideRefreshRate(RefreshRateOverrideConfig(targetModeId = 2)) } returns
            DisplayControllerResult.Unsupported

        val result = handler.dispatch(
            DisplayActionHandler.ACTION_OVERRIDE_REFRESH_RATE,
            mapOf(DisplayActionHandler.PARAM_TARGET_MODE_ID to "2"),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `override brightness extreme maps RateLimited to a Failure with the retry reason`() = runTest {
        coEvery { controller.overrideBrightness(BrightnessOverrideConfig(130, 60_000L)) } returns
            DisplayControllerResult.RateLimited(5_000L)

        val result = handler.dispatch(
            DisplayActionHandler.ACTION_OVERRIDE_BRIGHTNESS_EXTREME,
            mapOf(
                DisplayActionHandler.PARAM_PERCENT to "130",
                DisplayActionHandler.PARAM_ACTIVE_WINDOW_MILLIS to "60000",
            ),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `reset all maps ResetCompleted to Success`() = runTest {
        coEvery { controller.resetAllDisplayMutations() } returns DisplayControllerResult.ResetCompleted(2, 0)

        val result = handler.dispatch(DisplayActionHandler.ACTION_RESET_ALL, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `reset all maps OptedOut to a Failure`() = runTest {
        coEvery { controller.resetAllDisplayMutations() } returns DisplayControllerResult.OptedOut

        val result = handler.dispatch(DisplayActionHandler.ACTION_RESET_ALL, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `reset all maps HardwareError to a Failure carrying the message`() = runTest {
        coEvery { controller.resetAllDisplayMutations() } returns
            DisplayControllerResult.HardwareError("shell died")

        val result = handler.dispatch(DisplayActionHandler.ACTION_RESET_ALL, emptyMap())

        assertEquals(ActionResult.Failure("shell died"), result)
    }
}
