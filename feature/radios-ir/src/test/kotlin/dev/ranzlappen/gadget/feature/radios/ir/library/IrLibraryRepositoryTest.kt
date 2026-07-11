package dev.ranzlappen.gadget.feature.radios.ir.library

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
 * Unit tests for [IrLibraryRepository]. `brands` is a `by lazy` **instance**
 * property (same shape as `NfcTemplateRepository.templates`), so each test
 * builds a fresh repository over a mocked [Context]/[AssetManager] pointed at
 * its own `ir_library.json` bytes. Covers the happy decode path, the
 * `ignoreUnknownKeys = true` tolerance, and the `runCatching { }.getOrDefault(emptyList())`
 * fallback for both a missing asset (`AssetManager.open` throwing [IOException])
 * and malformed JSON.
 */
class IrLibraryRepositoryTest {

    private fun repositoryWithAsset(json: String): IrLibraryRepository {
        val assets = mockk<AssetManager>()
        every { assets.open("ir_library.json") } returns ByteArrayInputStream(json.toByteArray())
        val context = mockk<Context>()
        every { context.assets } returns assets
        return IrLibraryRepository(context)
    }

    private fun repositoryWithMissingAsset(): IrLibraryRepository {
        val assets = mockk<AssetManager>()
        every { assets.open("ir_library.json") } throws IOException("asset not found")
        val context = mockk<Context>()
        every { context.assets } returns assets
        return IrLibraryRepository(context)
    }

    @Test
    fun `brands decodes every brand and its signals from the asset JSON`() {
        val json = """
            [
              {
                "brand": "Acme",
                "category": "TV",
                "signals": [
                  {"name": "Power", "protocol": "NEC", "payload": "0x20DF10EF"},
                  {"name": "Vol Up", "protocol": "NEC", "payload": "0x20DF40BF", "carrierHz": 40000, "repeats": 2}
                ]
              }
            ]
        """.trimIndent()

        val brands = repositoryWithAsset(json).brands

        assertEquals(1, brands.size)
        assertEquals("Acme", brands[0].brand)
        assertEquals("TV", brands[0].category)
        assertEquals(2, brands[0].signals.size)
        assertEquals("Power", brands[0].signals[0].name)
        assertEquals(38_000, brands[0].signals[0].carrierHz)
        assertEquals(1, brands[0].signals[0].repeats)
        assertEquals(40_000, brands[0].signals[1].carrierHz)
        assertEquals(2, brands[0].signals[1].repeats)
    }

    @Test
    fun `brands tolerates unknown JSON keys`() {
        val json = """
            [{"brand": "Acme", "category": "TV", "signals": [], "futureField": "ignored"}]
        """.trimIndent()

        val brands = repositoryWithAsset(json).brands

        assertEquals(1, brands.size)
        assertEquals("Acme", brands.single().brand)
    }

    @Test
    fun `brands is empty when the asset is missing`() {
        assertTrue(repositoryWithMissingAsset().brands.isEmpty())
    }

    @Test
    fun `brands is empty when the asset JSON is malformed`() {
        assertTrue(repositoryWithAsset("not valid json").brands.isEmpty())
    }

    @Test
    fun `brands is empty for an empty JSON array`() {
        assertTrue(repositoryWithAsset("[]").brands.isEmpty())
    }

    @Test
    fun `brands is cached after the first access`() {
        val assets = mockk<AssetManager>()
        every { assets.open("ir_library.json") } returns
            ByteArrayInputStream("""[{"brand":"Acme","category":"TV","signals":[]}]""".toByteArray())
        val context = mockk<Context>()
        every { context.assets } returns assets
        val repository = IrLibraryRepository(context)

        repository.brands
        repository.brands

        io.mockk.verify(exactly = 1) { assets.open("ir_library.json") }
    }
}
