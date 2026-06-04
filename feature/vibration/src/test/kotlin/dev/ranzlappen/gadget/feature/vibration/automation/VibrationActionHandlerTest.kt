package dev.ranzlappen.gadget.feature.vibration.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.vibration.PatternRepository
import dev.ranzlappen.gadget.feature.vibration.PwmPulse
import dev.ranzlappen.gadget.feature.vibration.VibrationController
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.VibrationRootResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [VibrationActionHandler].
 *
 * The standard buzz / pattern-play branches now start
 * [dev.ranzlappen.gadget.feature.vibration.VibrationPlaybackService] via
 * `Context.startForegroundService` (so a background widget tap vibrates from a
 * foreground context). Like `TorchActionHandlerTest`'s strobe/morse branches,
 * those happy paths construct an `Intent` and need a real Android runtime, so
 * they live behind instrumented tests. The branches pinned here reach no
 * `Intent`: the pattern-validation failures, the rooted tier (which goes
 * straight to [VibrationRootCapabilities]), and the action metadata.
 */
class VibrationActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val controller = mockk<VibrationController>(relaxed = true)
    private val caps = mockk<VibrationRootCapabilities>(relaxed = true)
    private val patterns = mockk<PatternRepository>(relaxed = true)
    private val handler = VibrationActionHandler(context, controller, caps, patterns)

    @Test
    fun `unknown action is unsupported`() = runBlocking {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `pattern_play with a missing pattern fails`() = runBlocking {
        coEvery { patterns.get(any()) } returns null
        val result = handler.dispatch(
            VibrationActionHandler.ACTION_PATTERN_PLAY,
            mapOf(VibrationActionHandler.PARAM_PATTERN_ID to "nope"),
        )
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `pattern_play with a blank id fails`() = runBlocking {
        val result = handler.dispatch(
            VibrationActionHandler.ACTION_PATTERN_PLAY,
            mapOf(VibrationActionHandler.PARAM_PATTERN_ID to ""),
        )
        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `pattern_play action is declared with a pattern_id param`() {
        val action = handler.actions.first { it.key == VibrationActionHandler.ACTION_PATTERN_PLAY }
        assertTrue(action.params.any { it.name == VibrationActionHandler.PARAM_PATTERN_ID })
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
