package dev.ranzlappen.gadget.feature.files

import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val store = mockk<FileMetadataStore>()
    private val uri = mockk<Uri>()
    private val photo = FileMetadata(
        name = "a.jpg",
        sizeBytes = 10,
        mimeType = "image/jpeg",
        modifiedMs = null,
        exif = mapOf(ExifInterface.TAG_ARTIST to "Alice"),
        location = 1.0 to 2.0,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { store.read(uri) } returns photo
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `open reads the file and seeds the editable fields`() = runTest(dispatcher) {
        val vm = FilesViewModel(store)
        vm.open(uri)
        assertTrue(vm.state.value.loading)
        advanceUntilIdle()

        assertEquals(photo, vm.state.value.metadata)
        assertEquals("Alice", vm.state.value.edits[ExifInterface.TAG_ARTIST])
        assertEquals("", vm.state.value.edits[ExifInterface.TAG_COPYRIGHT])
        assertTrue(vm.state.value.changes.isEmpty())
    }

    @Test
    fun `save writes only the changes, then re-reads`() = runTest(dispatcher) {
        coEvery { store.writeExif(uri, any()) } returns true
        val vm = FilesViewModel(store)
        vm.open(uri)
        advanceUntilIdle()

        vm.edit(ExifInterface.TAG_ARTIST, "Bob")
        vm.save()
        advanceUntilIdle()

        coVerify { store.writeExif(uri, mapOf(ExifInterface.TAG_ARTIST to "Bob")) }
        coVerify(exactly = 2) { store.read(uri) }
        assertEquals(FilesMessage.Saved, vm.state.value.message)
    }

    @Test
    fun `a refused write keeps the edits and reports it`() = runTest(dispatcher) {
        coEvery { store.writeExif(uri, any()) } returns false
        val vm = FilesViewModel(store)
        vm.open(uri)
        advanceUntilIdle()

        vm.edit(ExifInterface.TAG_ARTIST, "Bob")
        vm.save()
        advanceUntilIdle()

        assertEquals(FilesMessage.SaveFailed, vm.state.value.message)
        assertEquals("Bob", vm.state.value.edits[ExifInterface.TAG_ARTIST])
    }

    @Test
    fun `revert restores the file's values`() = runTest(dispatcher) {
        val vm = FilesViewModel(store)
        vm.open(uri)
        advanceUntilIdle()
        vm.edit(ExifInterface.TAG_ARTIST, "Bob")

        vm.revert()

        assertTrue(vm.state.value.changes.isEmpty())
    }

    @Test
    fun `removeLocation reports success and an unreadable file reports failure`() = runTest(dispatcher) {
        coEvery { store.removeLocation(uri) } returns true
        val vm = FilesViewModel(store)
        vm.open(uri)
        advanceUntilIdle()
        vm.removeLocation()
        advanceUntilIdle()
        assertEquals(FilesMessage.LocationRemoved, vm.state.value.message)

        coEvery { store.read(uri) } returns null
        vm.open(uri)
        advanceUntilIdle()
        assertNull(vm.state.value.metadata)
        assertEquals(FilesMessage.ReadFailed, vm.state.value.message)
    }
}
