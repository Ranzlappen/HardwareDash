package dev.ranzlappen.gadget.feature.vibration

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Serialization round-trip + derived-property checks for [VibrationPattern].
 */
class VibrationPatternTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `pattern round-trips through json`() {
        val original = VibrationPattern(
            id = "abc",
            name = "Heartbeat",
            timingsMillis = listOf(0L, 100L, 50L, 100L),
            amplitudes = listOf(0, 200, 0, 255),
        )
        val decoded = json.decodeFromString(
            VibrationPattern.serializer(),
            json.encodeToString(VibrationPattern.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `totalMillis sums the segment timings`() {
        val pattern = VibrationPattern("a", "n", listOf(0L, 100L, 50L, 100L), listOf(0, 200, 0, 255))
        assertEquals(250L, pattern.totalMillis)
    }

    @Test
    fun `peakPercent maps the strongest raw amplitude to a percent`() {
        val pattern = VibrationPattern("a", "n", listOf(0L, 100L), listOf(0, 255))
        assertEquals(100, pattern.peakPercent)
    }
}
