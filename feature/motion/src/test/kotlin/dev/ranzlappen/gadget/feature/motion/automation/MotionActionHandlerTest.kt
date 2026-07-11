package dev.ranzlappen.gadget.feature.motion.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.motion.MotionDetectedMetricSource
import dev.ranzlappen.gadget.feature.motion.RotationRateMetricSource
import dev.ranzlappen.gadget.feature.motion.StepCounterMetricSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [MotionActionHandler] — `:feature:motion`'s automation
 * `ActionHandler` seam. Motion has no actuator of its own, so every action
 * is a threshold **assert** that samples an existing `MetricSource` directly;
 * coverage focuses on that dispatch/threshold-comparison logic. `context` is
 * relaxed-mocked since the constructor eagerly resolves each action's label
 * via `context.getString(...)` and this suite doesn't care about label text.
 */
class MotionActionHandlerTest {

    private val context = mockk<Context>(relaxed = true)
    private val rotationRate = mockk<RotationRateMetricSource>()
    private val stepCounter = mockk<StepCounterMetricSource>()
    private val motionDetected = mockk<MotionDetectedMetricSource>()

    private val handler = MotionActionHandler(context, rotationRate, stepCounter, motionDetected)

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(MotionActionHandler.FEATURE_ID, handler.featureId)
        assertEquals("motion", handler.featureId)
    }

    @Test
    fun `declares all four assert actions with no root requirement`() {
        assertEquals(
            setOf(
                MotionActionHandler.ACTION_ASSERT_MOTION_DETECTED,
                MotionActionHandler.ACTION_ASSERT_MOTION_IDLE,
                MotionActionHandler.ACTION_ASSERT_STEPS_ABOVE,
                MotionActionHandler.ACTION_ASSERT_ROTATION_ABOVE,
            ),
            handler.actions.map { it.key }.toSet(),
        )
        assertTrue(handler.actions.none { it.requiresRoot })
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        assertEquals(ActionResult.Unsupported, handler.dispatch("not-a-real-action", emptyMap()))
    }

    // ---- assert_motion_detected / assert_motion_idle ----

    @Test
    fun `assert-motion-detected succeeds when the sensor reports above the 0-5 threshold`() = runTest {
        coEvery { motionDetected.sample() } returns 1f

        val result = handler.dispatch(MotionActionHandler.ACTION_ASSERT_MOTION_DETECTED, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-motion-detected fails when idle`() = runTest {
        coEvery { motionDetected.sample() } returns 0f

        val result = handler.dispatch(MotionActionHandler.ACTION_ASSERT_MOTION_DETECTED, emptyMap())

        assertEquals(ActionResult.Failure("no motion detected"), result)
    }

    @Test
    fun `assert-motion-idle succeeds when the sensor is idle`() = runTest {
        coEvery { motionDetected.sample() } returns 0f

        val result = handler.dispatch(MotionActionHandler.ACTION_ASSERT_MOTION_IDLE, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-motion-idle fails when motion is detected`() = runTest {
        coEvery { motionDetected.sample() } returns 1f

        val result = handler.dispatch(MotionActionHandler.ACTION_ASSERT_MOTION_IDLE, emptyMap())

        assertEquals(ActionResult.Failure("motion detected"), result)
    }

    // ---- assert_steps_above ----

    @Test
    fun `assert-steps-above succeeds when the step count meets the given threshold`() = runTest {
        coEvery { stepCounter.sample() } returns 1500f

        val result = handler.dispatch(
            MotionActionHandler.ACTION_ASSERT_STEPS_ABOVE,
            mapOf(MotionActionHandler.PARAM_THRESHOLD_STEPS to "1000"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-steps-above fails when the step count is below the given threshold`() = runTest {
        coEvery { stepCounter.sample() } returns 500f

        val result = handler.dispatch(
            MotionActionHandler.ACTION_ASSERT_STEPS_ABOVE,
            mapOf(MotionActionHandler.PARAM_THRESHOLD_STEPS to "1000"),
        )

        assertEquals(ActionResult.Failure("step count 500.0 is below threshold 1000.0"), result)
    }

    @Test
    fun `assert-steps-above falls back to the 1000-step default when the param is missing`() = runTest {
        coEvery { stepCounter.sample() } returns 1000f

        val result = handler.dispatch(MotionActionHandler.ACTION_ASSERT_STEPS_ABOVE, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-steps-above falls back to the default when the param is not a number`() = runTest {
        coEvery { stepCounter.sample() } returns 999f

        val result = handler.dispatch(
            MotionActionHandler.ACTION_ASSERT_STEPS_ABOVE,
            mapOf(MotionActionHandler.PARAM_THRESHOLD_STEPS to "not-a-number"),
        )

        // Default threshold is 1000 — 999 falls short of it.
        assertEquals(ActionResult.Failure("step count 999.0 is below threshold 1000.0"), result)
    }

    @Test
    fun `assert-steps-above succeeds exactly at the threshold`() = runTest {
        coEvery { stepCounter.sample() } returns 1000f

        val result = handler.dispatch(
            MotionActionHandler.ACTION_ASSERT_STEPS_ABOVE,
            mapOf(MotionActionHandler.PARAM_THRESHOLD_STEPS to "1000"),
        )

        assertEquals(ActionResult.Success, result)
    }

    // ---- assert_rotation_above ----

    @Test
    fun `assert-rotation-above succeeds when the rotation rate meets the given threshold`() = runTest {
        coEvery { rotationRate.sample() } returns 2.5f

        val result = handler.dispatch(
            MotionActionHandler.ACTION_ASSERT_ROTATION_ABOVE,
            mapOf(MotionActionHandler.PARAM_THRESHOLD_RAD_S to "2.0"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-rotation-above fails when below the given threshold`() = runTest {
        coEvery { rotationRate.sample() } returns 0.5f

        val result = handler.dispatch(
            MotionActionHandler.ACTION_ASSERT_ROTATION_ABOVE,
            mapOf(MotionActionHandler.PARAM_THRESHOLD_RAD_S to "1.0"),
        )

        assertEquals(ActionResult.Failure("rotation rate 0.5 rad/s is below threshold 1.0 rad/s"), result)
    }

    @Test
    fun `assert-rotation-above falls back to the 1_0-default when the param is missing`() = runTest {
        coEvery { rotationRate.sample() } returns 1.0f

        val result = handler.dispatch(MotionActionHandler.ACTION_ASSERT_ROTATION_ABOVE, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-rotation-above ignores unrecognised params`() = runTest {
        coEvery { rotationRate.sample() } returns 5f

        val result = handler.dispatch(
            MotionActionHandler.ACTION_ASSERT_ROTATION_ABOVE,
            mapOf("unused" to "value"),
        )

        assertEquals(ActionResult.Success, result)
    }
}
