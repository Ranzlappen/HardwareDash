package dev.ranzlappen.gadget.feature.sensors.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.sensors.AccelerationMetricSource
import dev.ranzlappen.gadget.feature.sensors.LightMetricSource
import dev.ranzlappen.gadget.feature.sensors.ProximityMetricSource
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [SensorsActionHandler]. Sensors has no controller to
 * command (a pure [dev.ranzlappen.gadget.core.model.MetricSource] read
 * side), so unlike torch/vibration there's no service `Intent` to keep
 * behind instrumented tests — every branch here reaches only the injected
 * `MetricSource`s and is safe to pin directly.
 */
class SensorsActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val proximity = mockk<ProximityMetricSource>(relaxed = true)
    private val light = mockk<LightMetricSource>(relaxed = true)
    private val acceleration = mockk<AccelerationMetricSource>(relaxed = true)
    private val handler = SensorsActionHandler(context, proximity, light, acceleration)

    @Test
    fun `unknown action is unsupported`() = runBlocking {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `proximity_assert_near succeeds when the reading is at or below the threshold`() = runBlocking {
        every { proximity.stream() } returns flowOf(1f)
        coEvery { proximity.sample() } returns 2f

        val result = handler.dispatch(
            SensorsActionHandler.ACTION_PROXIMITY_NEAR,
            mapOf(SensorsActionHandler.PARAM_THRESHOLD_CM to "5"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `proximity_assert_near fails when the reading is above the threshold`() = runBlocking {
        every { proximity.stream() } returns flowOf(1f)
        coEvery { proximity.sample() } returns 8f

        val result = handler.dispatch(
            SensorsActionHandler.ACTION_PROXIMITY_NEAR,
            mapOf(SensorsActionHandler.PARAM_THRESHOLD_CM to "5"),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `an absent sensor fails rather than trusting the absent-value sample`() = runBlocking {
        // MetricSource.sample() returns 0f (the absent-value) when the
        // sensor doesn't exist, which would otherwise satisfy a
        // below-threshold assertion by accident. stream() == null is the
        // module's own signal for "not present" (SensorsViewModel uses the
        // same check).
        every { light.stream() } returns null

        val result = handler.dispatch(SensorsActionHandler.ACTION_LIGHT_DARK, emptyMap())

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `acceleration_assert_above succeeds once the reading clears the shake threshold`() = runBlocking {
        every { acceleration.stream() } returns flowOf(9.8f)
        coEvery { acceleration.sample() } returns 20f

        val result = handler.dispatch(
            SensorsActionHandler.ACTION_ACCELERATION_ABOVE,
            mapOf(SensorsActionHandler.PARAM_THRESHOLD_MS2 to "15"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(SensorsActionHandler.FEATURE_ID, handler.featureId)
    }
}
