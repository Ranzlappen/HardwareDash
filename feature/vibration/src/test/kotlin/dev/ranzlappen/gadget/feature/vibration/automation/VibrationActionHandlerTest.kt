package dev.ranzlappen.gadget.feature.vibration.automation

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.vibration.PwmPulse
import dev.ranzlappen.gadget.feature.vibration.VibrationController
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.VibrationRootResult
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VibrationActionHandlerTest {

    private val controller = mockk<VibrationController>(relaxed = true)
    private val caps = mockk<VibrationRootCapabilities>(relaxed = true)
    private val handler = VibrationActionHandler(controller, caps)

    @Test
    fun `oneshot parses params and delegates to the controller`() = runBlocking {
        every { controller.oneShot(any(), any()) } just Runs
        val result = handler.dispatch(
            VibrationActionHandler.ACTION_ONESHOT,
            mapOf(
                VibrationActionHandler.PARAM_AMPLITUDE to "80",
                VibrationActionHandler.PARAM_DURATION_MS to "500",
            ),
        )
        assertEquals(ActionResult.Success, result)
        verify { controller.oneShot(80, 500L) }
    }

    @Test
    fun `oneshot falls back to defaults on bad params`() = runBlocking {
        every { controller.oneShot(any(), any()) } just Runs
        handler.dispatch(VibrationActionHandler.ACTION_ONESHOT, mapOf(VibrationActionHandler.PARAM_AMPLITUDE to "x"))
        verify { controller.oneShot(60, 300L) }
    }

    @Test
    fun `unknown action is unsupported`() = runBlocking {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `rooted actions are flagged requiresRoot`() {
        val rootActions = handler.actions.filter { it.requiresRoot }.map { it.key }
        assertTrue(VibrationActionHandler.ACTION_EXTREME_AMPLITUDE in rootActions)
        assertTrue(VibrationActionHandler.ACTION_DIRECT_PWM in rootActions)
        assertTrue(VibrationActionHandler.ACTION_SUSTAINED_RUMBLE in rootActions)
    }

    @Test
    fun `direct pwm builds the pulse list from params and maps Ok to Success`() = runBlocking {
        val expected = List(3) { PwmPulse(8000, 12000) }
        coEvery { caps.directPwm(expected) } returns VibrationRootResult.Ok
        val result = handler.dispatch(
            VibrationActionHandler.ACTION_DIRECT_PWM,
            mapOf(
                VibrationActionHandler.PARAM_PWM_ON_MICROS to "8000",
                VibrationActionHandler.PARAM_PWM_OFF_MICROS to "12000",
                VibrationActionHandler.PARAM_PWM_PULSES to "3",
            ),
        )
        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `rooted Unsupported maps to a Failure with reason`() = runBlocking {
        coEvery { caps.extremeAmplitude(any(), any()) } returns VibrationRootResult.Unsupported
        val result = handler.dispatch(VibrationActionHandler.ACTION_EXTREME_AMPLITUDE, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `rooted OptedOut maps to a Failure`() = runBlocking {
        coEvery { caps.sustainedRumble(any(), any()) } returns VibrationRootResult.OptedOut
        val result = handler.dispatch(VibrationActionHandler.ACTION_SUSTAINED_RUMBLE, emptyMap())
        assertTrue(result is ActionResult.Failure)
    }
}
