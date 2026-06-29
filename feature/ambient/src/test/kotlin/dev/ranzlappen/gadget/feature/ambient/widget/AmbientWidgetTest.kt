package dev.ranzlappen.gadget.feature.ambient.widget

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AmbientWidgetTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `config round-trips through json`() {
        val config = AmbientWidgetConfig(sizePreset = WidgetSizePreset.Small, displayName = "Desk")
        val decoded = json.decodeFromString(
            AmbientWidgetConfig.serializer(),
            json.encodeToString(AmbientWidgetConfig.serializer(), config),
        )
        assertEquals(config, decoded)
    }

    @Test
    fun `brightnessPercent is 0 at or below dark and rises on a log scale`() {
        assertEquals(0, AmbientBrightness.brightnessPercent(0f))
        assertEquals(0, AmbientBrightness.brightnessPercent(-5f))
        val dim = AmbientBrightness.brightnessPercent(50f)
        val indoor = AmbientBrightness.brightnessPercent(500f)
        val bright = AmbientBrightness.brightnessPercent(5_000f)
        assertTrue(dim in 1..99)
        assertTrue(dim < indoor)
        assertTrue(indoor < bright)
    }

    @Test
    fun `brightnessPercent clamps to 100 in direct sun`() {
        assertEquals(100, AmbientBrightness.brightnessPercent(200_000f))
    }

    @Test
    fun `level buckets follow the lux thresholds`() {
        assertEquals(AmbientBrightness.Level.Dark, AmbientBrightness.level(5f))
        assertEquals(AmbientBrightness.Level.Dim, AmbientBrightness.level(50f))
        assertEquals(AmbientBrightness.Level.Indoor, AmbientBrightness.level(500f))
        assertEquals(AmbientBrightness.Level.Bright, AmbientBrightness.level(5_000f))
        assertEquals(AmbientBrightness.Level.Sunlight, AmbientBrightness.level(50_000f))
    }
}
