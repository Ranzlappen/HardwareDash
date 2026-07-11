package dev.ranzlappen.gadget.feature.radios.nfc.template

import android.content.Context
import android.content.res.AssetManager
import io.mockk.every
import io.mockk.mockk
import java.io.ByteArrayInputStream
import java.io.IOException
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [NfcTemplateRepository]. `templates` is a `by lazy` **instance**
 * property (unlike `ScanHistoryRepository`'s file-scoped `by preferencesDataStore`
 * delegate) so there's no classloader-wide caching wrinkle here — each test builds
 * a fresh repository over a mocked [Context]/[AssetManager] pointed at its own
 * `nfc_templates.json` bytes.
 *
 * Covers the happy decode path, the `ignoreUnknownKeys = true` tolerance, and the
 * `runCatching { }.getOrDefault(emptyList())` fallback for both a missing asset
 * (`AssetManager.open` throwing [IOException]) and malformed JSON.
 */
class NfcTemplateRepositoryTest {

    private fun repositoryWithAsset(json: String): NfcTemplateRepository {
        val assets = mockk<AssetManager>()
        every { assets.open("nfc_templates.json") } returns ByteArrayInputStream(json.toByteArray())
        val context = mockk<Context>()
        every { context.assets } returns assets
        return NfcTemplateRepository(context)
    }

    private fun repositoryWithMissingAsset(): NfcTemplateRepository {
        val assets = mockk<AssetManager>()
        every { assets.open("nfc_templates.json") } throws IOException("asset not found")
        val context = mockk<Context>()
        every { context.assets } returns assets
        return NfcTemplateRepository(context)
    }

    @Test
    fun `templates decodes every entry from the asset JSON`() {
        val json = """
            [
              {"id":"wifi","name":"Wi-Fi login","category":"network","mode":"TEXT","template":"WIFI:S:{{ssid}};;"},
              {"id":"url","name":"Open URL","category":"link","mode":"URL","template":"https://{{host}}"}
            ]
        """.trimIndent()

        val templates = repositoryWithAsset(json).templates

        assertEquals(2, templates.size)
        assertEquals("wifi", templates[0].id)
        assertEquals("Wi-Fi login", templates[0].name)
        assertEquals("network", templates[0].category)
        assertEquals("TEXT", templates[0].mode)
        assertEquals("url", templates[1].id)
        assertEquals("URL", templates[1].mode)
    }

    @Test
    fun `templates tolerates unknown JSON keys`() {
        val json = """
            [{"id":"1","name":"N","category":"c","mode":"TEXT","template":"t","futureField":"ignored"}]
        """.trimIndent()

        val templates = repositoryWithAsset(json).templates

        assertEquals(1, templates.size)
        assertEquals("1", templates.single().id)
    }

    @Test
    fun `templates is empty when the asset is missing`() {
        assertTrue(repositoryWithMissingAsset().templates.isEmpty())
    }

    @Test
    fun `templates is empty when the asset JSON is malformed`() {
        assertTrue(repositoryWithAsset("not valid json").templates.isEmpty())
    }

    @Test
    fun `templates is empty for an empty JSON array`() {
        assertTrue(repositoryWithAsset("[]").templates.isEmpty())
    }

    @Test
    fun `templates is cached after the first access`() {
        val assets = mockk<AssetManager>()
        every { assets.open("nfc_templates.json") } returns
            ByteArrayInputStream("""[{"id":"1","name":"N","category":"c","mode":"TEXT","template":"t"}]""".toByteArray())
        val context = mockk<Context>()
        every { context.assets } returns assets
        val repository = NfcTemplateRepository(context)

        repository.templates
        repository.templates

        io.mockk.verify(exactly = 1) { assets.open("nfc_templates.json") }
    }
}
