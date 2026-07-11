package dev.ranzlappen.gadget.feature.actuators.automation

import android.content.Context
import android.os.Vibrator
import dev.ranzlappen.gadget.core.automation.ActionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [ActuatorsActionHandler]. `Build.VERSION.SDK_INT` resolves
 * to the stub jar's default of `0` in a plain JVM unit test (no Robolectric
 * shadow), so the constructor always resolves the vibrator via the legacy
 * `Context.VIBRATOR_SERVICE` lookup (never `VIBRATOR_MANAGER_SERVICE`), and
 * `dispatch`'s haptic actions always take the pre-O `vibrate(Long)` branch —
 * mirroring the established repo convention (see `AudioActionHandlerTest`)
 * of only exercising the branch reachable under `SDK_INT == 0`.
 */
class ActuatorsActionHandlerTest {

    private fun handlerWith(vibrator: Vibrator?): ActuatorsActionHandler {
        val context = mockk<Context>()
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator
        return ActuatorsActionHandler(context)
    }

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(ActuatorsActionHandler.FEATURE_ID, handlerWith(null).featureId)
        assertEquals("actuators", handlerWith(null).featureId)
    }

    @Test
    fun `declares haptic-click, haptic-heavy and assert-available actions`() {
        val handler = handlerWith(null)

        assertEquals(
            setOf(
                ActuatorsActionHandler.ACTION_HAPTIC_CLICK,
                ActuatorsActionHandler.ACTION_HAPTIC_HEAVY,
                ActuatorsActionHandler.ACTION_ASSERT_AVAILABLE,
            ),
            handler.actions.map { it.key }.toSet(),
        )
        assertTrue(handler.actions.none { it.requiresRoot })
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handlerWith(mockk(relaxed = true)).dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    @Test
    fun `every action fails when there is no vibrator on the device`() = runTest {
        val handler = handlerWith(null)

        for (action in listOf(
            ActuatorsActionHandler.ACTION_HAPTIC_CLICK,
            ActuatorsActionHandler.ACTION_HAPTIC_HEAVY,
            ActuatorsActionHandler.ACTION_ASSERT_AVAILABLE,
        )) {
            assertEquals(ActionResult.Failure("No vibrator on this device"), handler.dispatch(action, emptyMap()))
        }
    }

    // ---- actuators_haptic_click ----

    @Test
    fun `haptic-click vibrates for 50ms via the legacy API and succeeds`() = runTest {
        val vibrator = mockk<Vibrator>(relaxed = true)

        val result = handlerWith(vibrator).dispatch(ActuatorsActionHandler.ACTION_HAPTIC_CLICK, emptyMap())

        assertEquals(ActionResult.Success, result)
        verify { vibrator.vibrate(50L) }
    }

    // ---- actuators_haptic_heavy ----

    @Test
    fun `haptic-heavy vibrates for 150ms via the legacy API and succeeds`() = runTest {
        val vibrator = mockk<Vibrator>(relaxed = true)

        val result = handlerWith(vibrator).dispatch(ActuatorsActionHandler.ACTION_HAPTIC_HEAVY, emptyMap())

        assertEquals(ActionResult.Success, result)
        verify { vibrator.vibrate(150L) }
    }

    // ---- actuators_assert_available ----

    @Test
    fun `assert-available succeeds when the vibrator reports available`() = runTest {
        val vibrator = mockk<Vibrator>()
        every { vibrator.hasVibrator() } returns true

        val result = handlerWith(vibrator).dispatch(ActuatorsActionHandler.ACTION_ASSERT_AVAILABLE, emptyMap())

        assertEquals(ActionResult.Success, result)
    }

    @Test
    fun `assert-available fails when the vibrator reports unavailable`() = runTest {
        val vibrator = mockk<Vibrator>()
        every { vibrator.hasVibrator() } returns false

        val result = handlerWith(vibrator).dispatch(ActuatorsActionHandler.ACTION_ASSERT_AVAILABLE, emptyMap())

        assertEquals(ActionResult.Failure("Vibrator not available"), result)
    }

    @Test
    fun `assert-available ignores unrecognised params`() = runTest {
        val vibrator = mockk<Vibrator>()
        every { vibrator.hasVibrator() } returns true

        val result = handlerWith(vibrator).dispatch(
            ActuatorsActionHandler.ACTION_ASSERT_AVAILABLE,
            mapOf("unused" to "value"),
        )

        assertEquals(ActionResult.Success, result)
    }
}
