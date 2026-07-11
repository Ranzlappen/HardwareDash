package dev.ranzlappen.gadget.feature.actuators.monitor

import android.content.Context
import android.os.Vibrator
import dev.ranzlappen.gadget.core.model.MetricCategory
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [ActuatorsMetricSource]. `Build.VERSION.SDK_INT` resolves to
 * `0` in a plain JVM unit test, so the constructor always resolves the
 * vibrator via the legacy `Context.VIBRATOR_SERVICE` lookup (never
 * `VIBRATOR_MANAGER_SERVICE`) — matching `ActuatorsActionHandlerTest`.
 */
class ActuatorsMetricSourceTest {

    private fun sourceWith(vibrator: Vibrator?): ActuatorsMetricSource {
        val context = mockk<Context>(relaxed = true)
        every { context.getSystemService(Context.VIBRATOR_SERVICE) } returns vibrator
        return ActuatorsMetricSource(context)
    }

    @Test
    fun `descriptor advertises the vibrator_available metric key as an Actuator metric`() {
        val descriptor = sourceWith(null).descriptor

        assertEquals(ActuatorsMetricSource.METRIC_KEY, descriptor.metricKey)
        assertEquals("vibrator_available", descriptor.metricKey)
        assertEquals(MetricCategory.Actuator, descriptor.category)
        assertEquals(0f, descriptor.min)
        assertEquals(1f, descriptor.max)
    }

    @Test
    fun `sample reports 1 when the vibrator is available`() = runTest {
        val vibrator = mockk<Vibrator>()
        every { vibrator.hasVibrator() } returns true

        assertEquals(1f, sourceWith(vibrator).sample())
    }

    @Test
    fun `sample reports 0 when the vibrator reports unavailable`() = runTest {
        val vibrator = mockk<Vibrator>()
        every { vibrator.hasVibrator() } returns false

        assertEquals(0f, sourceWith(vibrator).sample())
    }

    @Test
    fun `sample reports 0 when there is no vibrator service`() = runTest {
        assertEquals(0f, sourceWith(null).sample())
    }
}
