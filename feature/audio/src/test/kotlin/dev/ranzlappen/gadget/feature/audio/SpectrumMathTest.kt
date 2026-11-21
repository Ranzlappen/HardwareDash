package dev.ranzlappen.gadget.feature.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SpectrumMathTest {

    private val rate = MicAnalyzer.SAMPLE_RATE
    private val n = MicAnalyzer.FFT_SIZE

    private fun sine(hz: Double, amplitude: Double = 0.9) =
        ShortArray(n) { i -> (Short.MAX_VALUE * amplitude * sin(2 * PI * hz * i / rate)).toInt().toShort() }

    @Test
    fun `a 1 kHz sine peaks within one bin of 1 kHz`() {
        val mags = SpectrumMath.magnitudes(sine(1_000.0), n)
        val binHz = rate.toFloat() / n

        assertEquals(n / 2, mags.size)
        assertTrue(abs(SpectrumMath.peakHz(mags, rate) - 1_000f) <= binHz)
        assertTrue(mags.max() > 0.5f, "a loud sine should be near full scale, was ${mags.max()}")
    }

    @Test
    fun `silence gives empty bands and zero dB`() {
        val silence = ShortArray(n)
        val bands = SpectrumMath.bands(SpectrumMath.magnitudes(silence, n), rate, 32)

        assertEquals(32, bands.size)
        assertTrue(bands.all { it == 0f })
        assertEquals(0f, SpectrumMath.rmsDb(silence))
    }

    @Test
    fun `the band holding the tone is the loudest band`() {
        val bands = SpectrumMath.bands(SpectrumMath.magnitudes(sine(3_000.0), n), rate, 32, 40f, 16_000f)
        val loudest = bands.indices.maxBy { bands[it] }
        // Log spacing: 3 kHz sits at log(75)/log(400) ≈ 72 % of the range.
        assertEquals(23, loudest, "bands=$bands")
        assertTrue(bands[loudest] in 0.5f..1f)
    }

    @Test
    fun `rmsDb uses the 0 to 60 meter scale`() {
        val fullScale = ShortArray(n) { Short.MAX_VALUE }
        assertEquals(60f, SpectrumMath.rmsDb(fullScale), 0.01f)
        val quiet = ShortArray(n) { 33 } // ≈ -60 dBFS
        assertEquals(0f, SpectrumMath.rmsDb(quiet), 0.5f)
    }

    @Test
    fun `formatHz switches to kHz at 1000`() {
        assertEquals("440 Hz", SpectrumMath.formatHz(440f))
        assertEquals(String.format("%.1f kHz", 16f), SpectrumMath.formatHz(16_000f))
    }

    @Test
    fun `wav helpers derive duration and names`() {
        // 44-byte header + one second of 44.1 kHz mono 16-bit.
        assertEquals(1_000L, wavDurationMs(44L + 88_200L))
        assertEquals(0L, wavDurationMs(10L))
        assertEquals("1:05", formatDuration(65_000))
        assertEquals("1:00:01", formatDuration(3_601_000))
        assertTrue(Regex("""Gadget_\d{8}_\d{6}\.wav""").matches(recordingFileName(0L)))
    }
}
