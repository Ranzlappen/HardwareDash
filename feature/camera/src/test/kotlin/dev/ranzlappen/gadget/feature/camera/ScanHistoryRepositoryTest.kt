package dev.ranzlappen.gadget.feature.camera

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JVM tests for [ScanHistoryRepository]'s persistence/ordering logic: `add()`
 * prepends the newest scan and truncates to the 20 most recent entries,
 * `clear()` empties it. Uses a real `DataStore<Preferences>` on disk (via a
 * mocked [Context] pointed at a temp dir) rather than a fake — same intent as
 * `FeaturePreferencesTest` in `:core:datastore`, which exercises the real
 * on-disk round trip instead of hiding encoding bugs behind a fake.
 *
 * Important wrinkle: [ScanHistoryRepository] reaches its `DataStore` through
 * the file-scoped `Context.scanHistoryDataStore by preferencesDataStore(name = ...)`
 * property (same pattern `IrSignalRepository` uses). That delegate caches its
 * `DataStore` instance on the delegate object itself, created ONCE on the
 * first-ever access for the whole JVM/classloader — NOT per [Context]
 * instance. So every [ScanHistoryRepository] built anywhere in this test
 * class shares the exact same on-disk file, no matter which mocked [Context]
 * constructed it. That's why the backing directory is a class-level
 * [ClassRule] (one directory for the whole test class, chosen once) instead
 * of a fresh `@Rule` per test, and why every test clears history in
 * [setUp] rather than assuming a clean slate.
 */
class ScanHistoryRepositoryTest {

    companion object {
        @ClassRule
        @JvmField
        val tempFolder = TemporaryFolder()
    }

    private lateinit var repository: ScanHistoryRepository

    @Before
    fun setUp() {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempFolder.root

        repository = ScanHistoryRepository(context)
        runTest { repository.clear() }
    }

    private fun barcode(id: String) = BarcodeResult(
        id = id,
        rawValue = "value-$id",
        format = "QR_CODE",
        displayType = "Text",
        timestamp = 1_000L,
    )

    @Test
    fun `history is empty before anything is added`() = runTest {
        assertEquals(emptyList(), repository.history.first())
    }

    @Test
    fun `add stores a result retrievable from history`() = runTest {
        val result = barcode("1")

        repository.add(result)

        assertEquals(listOf(result), repository.history.first())
    }

    @Test
    fun `add prepends the newest result ahead of older ones`() = runTest {
        repository.add(barcode("1"))
        repository.add(barcode("2"))
        repository.add(barcode("3"))

        assertEquals(listOf("3", "2", "1"), repository.history.first().map { it.id })
    }

    @Test
    fun `add truncates history to the 20 most recent entries`() = runTest {
        (1..25).forEach { i -> repository.add(barcode(i.toString())) }

        val history = repository.history.first()

        assertEquals(20, history.size)
        // Newest (25) first; entries 1..5 fell off the tail once the cap was hit.
        assertEquals((25 downTo 6).map { it.toString() }, history.map { it.id })
    }

    @Test
    fun `clear empties a populated history`() = runTest {
        repository.add(barcode("1"))
        repository.add(barcode("2"))

        repository.clear()

        assertTrue(repository.history.first().isEmpty())
    }
}
