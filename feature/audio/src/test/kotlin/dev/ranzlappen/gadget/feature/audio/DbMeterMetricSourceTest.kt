package dev.ranzlappen.gadget.feature.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dev.ranzlappen.gadget.core.model.MetricCategory
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [DbMeterMetricSource]. The permission gate is the only
 * branching logic reachable from a plain JVM test — the RMS→dB conversion
 * and clamping live inline inside the `callbackFlow` block and need a live
 * `AudioRecord` capture (real microphone hardware / an instrumented test) to
 * exercise meaningfully, so they are intentionally not covered here.
 *
 * `ContextCompat.checkSelfPermission` is mocked via `mockkStatic` — same
 * technique already used for `AppCompatDelegate`/`LocaleListCompat` in
 * `SettingsViewModelTest`.
 */
class DbMeterMetricSourceTest {

    private val context = mockk<Context>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun newSource() = DbMeterMetricSource(context)

    @Test
    fun `descriptor advertises the microphone level contract`() {
        val descriptor = newSource().descriptor

        assertEquals("db_meter", DbMeterMetricSource.METRIC_KEY)
        assertEquals(DbMeterMetricSource.METRIC_KEY, descriptor.metricKey)
        assertEquals("Microphone level", descriptor.displayName)
        assertEquals("dB", descriptor.unit)
        assertEquals(0f, descriptor.min)
        assertEquals(60f, descriptor.max)
        assertEquals(MetricCategory.Sensor, descriptor.category)
    }

    @Test
    fun `stream returns null without the RECORD_AUDIO permission`() {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_DENIED

        assertNull(newSource().stream())
    }

    @Test
    fun `sample falls back to zero when there is no stream`() = runBlocking {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_DENIED

        assertEquals(0f, newSource().sample())
    }
}
