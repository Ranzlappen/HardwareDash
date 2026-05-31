package dev.ranzlappen.gadget.feature.vibration

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Serialization round-trip + [VibrationRootToolsConfig.coercedTo] clamping for
 * the rooted-tools settings record. Mirrors torch's `RootToolsConfigTest`.
 */
class VibrationRootToolsConfigTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `config round-trips through json`() {
        val original = VibrationRootToolsConfig(
            extremeAmplitudePercent = 90,
            extremeBurstMs = 2_000L,
            pwmOnMicros = 6_000L,
            pwmOffMicros = 9_000L,
            pwmPulses = 30,
            dualPhaseOffsetMicros = 4_000L,
            rumbleDurationMs = 45_000L,
            rumbleAmplitudePercent = 50,
        )
        val decoded = json.decodeFromString(
            VibrationRootToolsConfig.serializer(),
            json.encodeToString(VibrationRootToolsConfig.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `empty object decodes to defaults`() {
        assertEquals(
            VibrationRootToolsConfig(),
            json.decodeFromString(VibrationRootToolsConfig.serializer(), "{}"),
        )
    }

    @Test
    fun `coercedTo clamps amplitude to the live ceiling`() {
        val coerced = VibrationRootToolsConfig(extremeAmplitudePercent = 100, rumbleAmplitudePercent = 100)
            .coercedTo(maxAmplitudePercent = 80)
        assertEquals(80, coerced.extremeAmplitudePercent)
        assertEquals(80, coerced.rumbleAmplitudePercent)
    }

    @Test
    fun `coercedTo clamps burst + rumble durations to their hard ceilings`() {
        val coerced = VibrationRootToolsConfig(extremeBurstMs = 10_000L, rumbleDurationMs = 10L * 60_000L)
            .coercedTo(maxAmplitudePercent = 100)
        assertEquals(VibrationRootToolsConfig.MAX_BURST_MS, coerced.extremeBurstMs)
        assertEquals(VibrationRootToolsConfig.MAX_RUMBLE_MS, coerced.rumbleDurationMs)
    }

    @Test
    fun `coercedTo floors the PWM off-time`() {
        val coerced = VibrationRootToolsConfig(pwmOffMicros = 100L).coercedTo(maxAmplitudePercent = 100)
        assertEquals(VibrationRootToolsConfig.MIN_PWM_OFF_MICROS, coerced.pwmOffMicros)
    }
}
