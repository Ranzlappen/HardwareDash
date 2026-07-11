package dev.ranzlappen.gadget.feature.lock

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dev.ranzlappen.gadget.core.model.MetricCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [LockStateMetricSource] — `:feature:lock`'s monitoring
 * `MetricSource` seam. Covers the poll path ([LockStateMetricSource.sample])
 * against a mocked [KeyguardManager], and the push path
 * ([LockStateMetricSource.stream]) by capturing the [BroadcastReceiver] passed
 * to the statically-mocked `ContextCompat.registerReceiver` and driving it
 * manually — the same "capture + drive the callback" technique used for
 * `BtEnabledMetricSourceTest`/`WifiEnabledMetricSourceTest`'s
 * `Context.registerReceiver`, adapted for this module's
 * `ContextCompat.registerReceiver` (needed for the `RECEIVER_NOT_EXPORTED`
 * flag on API 33+).
 */
class LockStateMetricSourceTest {

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun sourceWith(keyguard: KeyguardManager?, context: Context = mockk(relaxed = true)): LockStateMetricSource {
        every { context.getSystemService(Context.KEYGUARD_SERVICE) } returns keyguard
        return LockStateMetricSource(context)
    }

    @Test
    fun `descriptor advertises the lock_state metric key as a Device metric`() {
        val source = sourceWith(null)

        assertEquals(LockStateMetricSource.METRIC_KEY, source.descriptor.metricKey)
        assertEquals("lock_state", source.descriptor.metricKey)
        assertEquals(MetricCategory.Device, source.descriptor.category)
        assertEquals(0f, source.descriptor.min)
        assertEquals(1f, source.descriptor.max)
    }

    @Test
    fun `sample reports 1 when the keyguard is locked`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns true

        assertEquals(1f, sourceWith(keyguard).sample())
    }

    @Test
    fun `sample reports 0 when the keyguard is not locked`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns false

        assertEquals(0f, sourceWith(keyguard).sample())
    }

    @Test
    fun `sample reports 0 when there is no keyguard service`() = runTest {
        assertEquals(0f, sourceWith(null).sample())
    }

    @Test
    fun `stream emits the current state on subscribe then follows broadcasts`() = runTest {
        val keyguard = mockk<KeyguardManager>()
        every { keyguard.isKeyguardLocked } returns false
        val context = mockk<Context>(relaxed = true)
        val source = sourceWith(keyguard, context)

        val receiverSlot = slot<BroadcastReceiver>()
        every {
            ContextCompat.registerReceiver(context, capture(receiverSlot), any(), any())
        } returns null

        val values = mutableListOf<Float>()
        val job = launch { source.stream().toList(values) }
        advanceUntilIdle()

        assertEquals(listOf(0f), values)

        every { keyguard.isKeyguardLocked } returns true
        receiverSlot.captured.onReceive(context, mockk<Intent>())
        advanceUntilIdle()

        assertEquals(listOf(0f, 1f), values)

        job.cancel()
        advanceUntilIdle()
        verify { context.unregisterReceiver(receiverSlot.captured) }
    }
}
