package dev.ranzlappen.gadget.feature.radios.cell.automation

import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.radios.cell.CellSignalMetricSource
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [CellActionHandler]. `:feature:radios-cell` has no write
 * path to command (its `CellController` is deliberately read-only), so
 * — mirroring `SensorsActionHandlerTest` / `AmbientActionHandlerTest` —
 * every branch here reaches only the injected [CellSignalMetricSource] and
 * is safe to pin directly without an instrumented test.
 */
class CellActionHandlerTest {

    private val signal = mockk<CellSignalMetricSource>(relaxed = true)
    private val handler = CellActionHandler(signal)

    @Test
    fun `unknown action is unsupported`() = runBlocking {
        assertEquals(ActionResult.Unsupported, handler.dispatch("nope", emptyMap()))
    }

    @Test
    fun `assert_above succeeds when the reading is at or above the threshold`() = runBlocking {
        coEvery { signal.sample() } returns 3f

        val result = handler.dispatch(
            CellActionHandler.ACTION_ASSERT_ABOVE,
            mapOf(CellActionHandler.PARAM_THRESHOLD_BARS to "2"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert_above fails when the reading is below the threshold`() = runBlocking {
        coEvery { signal.sample() } returns 1f

        val result = handler.dispatch(
            CellActionHandler.ACTION_ASSERT_ABOVE,
            mapOf(CellActionHandler.PARAM_THRESHOLD_BARS to "2"),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `assert_below succeeds when the reading is at or below the threshold`() = runBlocking {
        coEvery { signal.sample() } returns 1f

        val result = handler.dispatch(
            CellActionHandler.ACTION_ASSERT_BELOW,
            mapOf(CellActionHandler.PARAM_THRESHOLD_BARS to "2"),
        )

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert_below fails when the reading is above the threshold`() = runBlocking {
        coEvery { signal.sample() } returns 4f

        val result = handler.dispatch(
            CellActionHandler.ACTION_ASSERT_BELOW,
            mapOf(CellActionHandler.PARAM_THRESHOLD_BARS to "2"),
        )

        assertTrue(result is ActionResult.Failure)
    }

    @Test
    fun `missing threshold param falls back to the default`() = runBlocking {
        coEvery { signal.sample() } returns CellActionHandler.DEFAULT_THRESHOLD_BARS

        val result = handler.dispatch(CellActionHandler.ACTION_ASSERT_ABOVE, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(CellActionHandler.FEATURE_ID, handler.featureId)
    }
}
