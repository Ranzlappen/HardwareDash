package dev.ranzlappen.gadget.feature.ambient.automation

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.ambient.AmbientSensor
import dev.ranzlappen.gadget.feature.ambient.AmbientState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [AmbientActionHandler] — `:feature:ambient`'s automation
 * `ActionHandler` seam. Both actions read the cached
 * [AmbientSensor.state]`.luxLevel` and compare it against a caller-supplied
 * (or default) threshold; coverage focuses on that dispatch/threshold
 * comparison logic, mirroring `MotionActionHandlerTest` (the same
 * read-only-sensor-assert shape this handler is the reference for).
 */
class AmbientActionHandlerTest {

    private val sensor = mockk<AmbientSensor>()
    private val handler = AmbientActionHandler(sensor)

    private fun stateWith(luxLevel: Float?) {
        every { sensor.state } returns MutableStateFlow(AmbientState(luxLevel = luxLevel))
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(AmbientActionHandler.FEATURE_ID, handler.featureId)
        assertEquals("ambient", handler.featureId)
    }

    @Test
    fun `declares the two assert actions with no root requirement`() {
        assertEquals(
            setOf(AmbientActionHandler.ACTION_ASSERT_BRIGHT, AmbientActionHandler.ACTION_ASSERT_DARK),
            handler.actions.map { it.key }.toSet(),
        )
        assertTrue(handler.actions.none { it.requiresRoot })
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        stateWith(luxLevel = 50f)

        assertEquals(ActionResult.Unsupported, handler.dispatch("not-a-real-action", emptyMap()))
    }

    // ---- ambient_assert_bright ----

    @Test
    fun `assert-bright succeeds when the reading meets the given threshold`() = runTest {
        stateWith(luxLevel = 150f)

        val result = handler.dispatch(
            AmbientActionHandler.ACTION_ASSERT_BRIGHT,
            mapOf("threshold_lux" to "100"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-bright succeeds exactly at the threshold`() = runTest {
        stateWith(luxLevel = 100f)

        val result = handler.dispatch(
            AmbientActionHandler.ACTION_ASSERT_BRIGHT,
            mapOf("threshold_lux" to "100"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-bright fails when the reading is below the given threshold`() = runTest {
        stateWith(luxLevel = 40f)

        val result = handler.dispatch(
            AmbientActionHandler.ACTION_ASSERT_BRIGHT,
            mapOf("threshold_lux" to "100"),
        )

        assertEquals(ActionResult.Failure("Ambient light 40.0 lux is below threshold 100.0 lux"), result)
    }

    @Test
    fun `assert-bright falls back to the 100-lux default when the param is missing`() = runTest {
        stateWith(luxLevel = 150f)

        val result = handler.dispatch(AmbientActionHandler.ACTION_ASSERT_BRIGHT, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-bright falls back to the default when the param is not a number`() = runTest {
        stateWith(luxLevel = 50f)

        val result = handler.dispatch(
            AmbientActionHandler.ACTION_ASSERT_BRIGHT,
            mapOf("threshold_lux" to "not-a-number"),
        )

        // Default threshold is 100 — 50 falls short of it.
        assertEquals(ActionResult.Failure("Ambient light 50.0 lux is below threshold 100.0 lux"), result)
    }

    @Test
    fun `assert-bright treats a never-reported lux level as 0`() = runTest {
        stateWith(luxLevel = null)

        val result = handler.dispatch(AmbientActionHandler.ACTION_ASSERT_BRIGHT, emptyMap())

        assertEquals(ActionResult.Failure("Ambient light 0.0 lux is below threshold 100.0 lux"), result)
    }

    // ---- ambient_assert_dark ----

    @Test
    fun `assert-dark succeeds when the reading meets the given threshold`() = runTest {
        stateWith(luxLevel = 2f)

        val result = handler.dispatch(
            AmbientActionHandler.ACTION_ASSERT_DARK,
            mapOf("threshold_lux" to "10"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-dark succeeds exactly at the threshold`() = runTest {
        stateWith(luxLevel = 10f)

        val result = handler.dispatch(
            AmbientActionHandler.ACTION_ASSERT_DARK,
            mapOf("threshold_lux" to "10"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-dark fails when the reading is above the given threshold`() = runTest {
        stateWith(luxLevel = 500f)

        val result = handler.dispatch(
            AmbientActionHandler.ACTION_ASSERT_DARK,
            mapOf("threshold_lux" to "10"),
        )

        assertEquals(ActionResult.Failure("Ambient light 500.0 lux is above threshold 10.0 lux"), result)
    }

    @Test
    fun `assert-dark falls back to the 10-lux default when the param is missing`() = runTest {
        stateWith(luxLevel = 2f)

        val result = handler.dispatch(AmbientActionHandler.ACTION_ASSERT_DARK, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-dark treats a never-reported lux level as 0, which is always at-or-below any threshold`() = runTest {
        stateWith(luxLevel = null)

        val result = handler.dispatch(AmbientActionHandler.ACTION_ASSERT_DARK, emptyMap())

        assertEquals(ActionResult.Success, result)
    }
}
