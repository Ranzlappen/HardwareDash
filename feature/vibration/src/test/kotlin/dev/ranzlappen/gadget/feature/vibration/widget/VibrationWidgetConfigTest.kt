package dev.ranzlappen.gadget.feature.vibration.widget

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Serialization round-trip for [VibrationWidgetConfig] (pins the JSON shape so
 * a field rename surfaces as a test failure, not a silent on-disk
 * incompatibility). Mirror of torch's `TorchWidgetConfigTest`.
 */
class VibrationWidgetConfigTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `vibrate config round-trips`() {
        val original = VibrationWidgetConfig(
            type = WidgetType.Vibrate,
            displayName = "Buzz",
            amplitudePercent = 75,
            durationMillis = 400L,
        )
        val decoded = json.decodeFromString(
            VibrationWidgetConfig.serializer(),
            json.encodeToString(VibrationWidgetConfig.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `pattern config round-trips with a patternId`() {
        val original = VibrationWidgetConfig(
            type = WidgetType.Pattern,
            displayName = "SOS pattern",
            patternId = "pattern-123",
        )
        val decoded = json.decodeFromString(
            VibrationWidgetConfig.serializer(),
            json.encodeToString(VibrationWidgetConfig.serializer(), original),
        )
        assertEquals(original, decoded)
    }

    @Test
    fun `decoding tolerates unknown fields`() {
        val withExtra = """{"type":"Vibrate","displayName":"X","futureField":42}"""
        val decoded = json.decodeFromString(VibrationWidgetConfig.serializer(), withExtra)
        assertEquals(WidgetType.Vibrate, decoded.type)
        assertEquals("X", decoded.displayName)
    }
}
