package dev.ranzlappen.gadget.feature.torch.widget

import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Smoke test for [TorchWidgetConfig]'s `@Serializable` round-trip.
 *
 * The repository's actual disk persistence goes through
 * [dev.ranzlappen.gadget.core.datastore.FeaturePreferences] which has
 * its own integration test using a real DataStore on a tmpdir. This
 * test pins the JSON shape so an accidental field rename or default-
 * value drift surfaces immediately instead of as a silent on-disk
 * incompatibility for users upgrading across batches.
 */
class TorchWidgetConfigTest {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun `flashlight config encodes and decodes losslessly`() {
        val original = TorchWidgetConfig(
            type = WidgetType.Flashlight,
            displayName = "My flashlight",
        )

        val encoded = json.encodeToString(TorchWidgetConfig.serializer(), original)
        val decoded = json.decodeFromString(TorchWidgetConfig.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `strobe config with custom rate and SOS round-trips`() {
        val original = TorchWidgetConfig(
            type = WidgetType.Strobe,
            displayName = "Bright SOS",
            rateHz = 12f,
            sosMode = true,
        )

        val encoded = json.encodeToString(TorchWidgetConfig.serializer(), original)
        val decoded = json.decodeFromString(TorchWidgetConfig.serializer(), encoded)

        assertEquals(original, decoded)
    }

    @Test
    fun `decoding json with unknown fields tolerates them`() {
        // Forward-compatibility: a future batch that adds a field
        // should still be able to load older on-disk JSON without
        // failing. Test exercises that the decoder configuration
        // (`ignoreUnknownKeys = true`) is plumbed in.
        val withExtra = """
            {"type":"Flashlight","displayName":"Old","rateHz":5.0,"sosMode":false,"futureField":42}
        """.trimIndent()

        val decoded = json.decodeFromString(TorchWidgetConfig.serializer(), withExtra)

        assertEquals(
            TorchWidgetConfig(type = WidgetType.Flashlight, displayName = "Old"),
            decoded,
        )
    }
}
