package dev.ranzlappen.gadget.feature.radios.ir

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
 * JVM tests for [IrSignalRepository]'s persistence logic: `save()` upserts by
 * [IrSignal.id] — a new id is appended, and a matching id is first filtered
 * out and then re-appended, so re-saving an existing id moves it to the END
 * of the list rather than leaving it in its original position — and
 * `delete()` removes by id. Uses a real `DataStore<Preferences>` on disk via
 * a mocked [Context] pointed at a temp dir, same intent as
 * `ScanHistoryRepositoryTest`.
 *
 * Important wrinkle (same as `ScanHistoryRepositoryTest` / `IrSignalRepository`'s
 * own doc comment): [IrSignalRepository] reaches its `DataStore` through the
 * file-scoped `Context.irSignalsDataStore by preferencesDataStore(name = ...)`
 * property. That delegate caches its `DataStore` instance on the delegate
 * object itself, created ONCE for the whole JVM/classloader on first access —
 * NOT per [Context] instance. So every [IrSignalRepository] built anywhere in
 * this test class shares the same on-disk file; the backing directory is a
 * class-level [ClassRule] and every test starts by draining any signals left
 * over from a previous test.
 */
class IrSignalRepositoryTest {

    companion object {
        @ClassRule
        @JvmField
        val tempFolder = TemporaryFolder()
    }

    private lateinit var repository: IrSignalRepository

    @Before
    fun setUp() {
        val context = mockk<Context>(relaxed = true)
        every { context.applicationContext } returns context
        every { context.filesDir } returns tempFolder.root

        repository = IrSignalRepository(context)
        runTest {
            repository.signals.first().forEach { repository.delete(it.id) }
        }
    }

    private fun signal(id: String, name: String = "Signal $id") = IrSignal(
        id = id,
        name = name,
        protocol = IrProtocol.NEC,
        payload = "0x$id",
        carrierHz = 38_000,
        repeats = 1,
    )

    @Test
    fun `signals is empty before anything is saved`() = runTest {
        assertEquals(emptyList(), repository.signals.first())
    }

    @Test
    fun `save stores a signal retrievable from signals`() = runTest {
        val theSignal = signal("1")

        repository.save(theSignal)

        assertEquals(listOf(theSignal), repository.signals.first())
    }

    @Test
    fun `save appends new signals after existing ones`() = runTest {
        repository.save(signal("1"))
        repository.save(signal("2"))
        repository.save(signal("3"))

        assertEquals(listOf("1", "2", "3"), repository.signals.first().map { it.id })
    }

    @Test
    fun `save with an existing id replaces that entry in place`() = runTest {
        repository.save(signal("1"))
        repository.save(signal("2"))
        repository.save(signal("1", name = "Renamed"))

        val signals = repository.signals.first()
        assertEquals(listOf("2", "1"), signals.map { it.id })
        assertEquals("Renamed", signals.single { it.id == "1" }.name)
    }

    @Test
    fun `delete removes the matching signal`() = runTest {
        repository.save(signal("1"))
        repository.save(signal("2"))

        repository.delete("1")

        assertEquals(listOf("2"), repository.signals.first().map { it.id })
    }

    @Test
    fun `delete of an unknown id is a no-op`() = runTest {
        repository.save(signal("1"))

        repository.delete("does-not-exist")

        assertEquals(listOf("1"), repository.signals.first().map { it.id })
    }

    @Test
    fun `delete on an empty library is a no-op`() = runTest {
        repository.delete("anything")

        assertTrue(repository.signals.first().isEmpty())
    }
}
