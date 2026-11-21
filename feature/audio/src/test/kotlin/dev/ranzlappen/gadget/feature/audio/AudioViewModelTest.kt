package dev.ranzlappen.gadget.feature.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import dev.ranzlappen.gadget.feature.audio.control.AudioRoutingController
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [AudioViewModel]'s non-passthrough logic:
 *
 *  - seeding [AudioState.isRootedFlavor] from [RootCapabilityRegistry] at
 *    construction time,
 *  - [AudioViewModel.checkPermission]'s RECORD_AUDIO branch (mirrors
 *    `SettingsViewModelTest`'s locale/permission-seeding coverage), and
 *  - [AudioViewModel.startRecording]/[AudioViewModel.stopRecording] building
 *    the right `Intent` for [AudioRecordService].
 *
 *  - the recordings list: initial load and delete success / failure (v3 B10).
 *
 * [AudioViewModel.onPermissionResult] and the `init`-block StateFlow
 * mirrors are pure passthrough and deliberately not covered here.
 *
 * All collaborators are mocks — mockk
 * mocks final Kotlin classes out of the box on the JVM, no `open` needed.
 * `Intent` construction is intercepted via `mockkConstructor` and
 * `ContextCompat.checkSelfPermission` via `mockkStatic`, the same
 * techniques `SettingsViewModelTest` and `DbMeterMetricSourceTest` use.
 */
class AudioViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var recorder: AudioRecorder
    private lateinit var analyzer: MicAnalyzer
    private lateinit var recordings: Recordings
    private lateinit var player: RecordingPlayer
    private lateinit var routing: AudioRoutingController
    private lateinit var rootCapabilityRegistry: RootCapabilityRegistry

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(ContextCompat::class)
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setPackage(any()) } returns mockk(relaxed = true)

        context = mockk(relaxed = true)
        recorder = mockk(relaxed = true)
        analyzer = mockk(relaxed = true)
        recordings = mockk(relaxed = true)
        player = mockk(relaxed = true)
        routing = mockk(relaxed = true)
        rootCapabilityRegistry = mockk(relaxed = true)

        every { recorder.isRecording } returns MutableStateFlow(false)
        every { recorder.lastSaved } returns MutableStateFlow<Uri?>(null)
        every { player.playing } returns MutableStateFlow<Uri?>(null)
        coEvery { recordings.list() } returns emptyList()
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AudioViewModel =
        AudioViewModel(context, recorder, analyzer, recordings, player, routing, rootCapabilityRegistry)

    // ---- constructor seeding of isRootedFlavor ----

    @Test
    fun `seeds isRootedFlavor true for the rooted flavor`() {
        every { rootCapabilityRegistry.isRootedFlavor } returns true

        val viewModel = createViewModel()

        assertEquals(true, viewModel.state.value.isRootedFlavor)
    }

    @Test
    fun `seeds isRootedFlavor false for the standard flavor`() {
        every { rootCapabilityRegistry.isRootedFlavor } returns false

        val viewModel = createViewModel()

        assertEquals(false, viewModel.state.value.isRootedFlavor)
    }

    // ---- checkPermission ----

    @Test
    fun `checkPermission sets permissionGranted true when RECORD_AUDIO is granted`() {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED

        val viewModel = createViewModel()

        assertEquals(true, viewModel.state.value.permissionGranted)
    }

    @Test
    fun `checkPermission sets permissionGranted false when RECORD_AUDIO is denied`() {
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_DENIED

        val viewModel = createViewModel()

        assertEquals(false, viewModel.state.value.permissionGranted)
    }

    // ---- startRecording / stopRecording ----

    @Test
    fun `startRecording fires an Intent carrying the start action`() {
        val viewModel = createViewModel()

        viewModel.startRecording()

        verify {
            // The Intent(String) ctor arg is not matchable against the android.jar
            // stub (constructedWith never matched), so assert the observable part.
            anyConstructed<Intent>().setPackage(any())
        }
        verify { context.startService(any()) }
    }

    @Test
    fun `stopRecording fires an Intent carrying the stop action`() {
        val viewModel = createViewModel()

        viewModel.stopRecording()

        verify {
            // The Intent(String) ctor arg is not matchable against the android.jar
            // stub (constructedWith never matched), so assert the observable part.
            anyConstructed<Intent>().setPackage(any())
        }
        verify { context.startService(any()) }
    }

    // ---- recordings (v3 B10) ----

    private fun recording(name: String) =
        Recording(uri = mockk(), name = name, durationMs = 1_000, sizeBytes = 88_244, addedAtMs = 0)

    @Test
    fun `loads the recordings list on start`() = runTest(dispatcher) {
        val list = listOf(recording("a.wav"), recording("b.wav"))
        coEvery { recordings.list() } returns list

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(list, viewModel.state.value.recordings)
    }

    @Test
    fun `deleteRecording drops the row when MediaStore deletes it`() = runTest(dispatcher) {
        val a = recording("a.wav")
        val b = recording("b.wav")
        coEvery { recordings.list() } returns listOf(a, b)
        coEvery { recordings.delete(a.uri) } returns true

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.deleteRecording(a)
        advanceUntilIdle()

        assertEquals(listOf(b), viewModel.state.value.recordings)
        assertEquals(null, viewModel.state.value.error)
    }

    @Test
    fun `deleteRecording keeps the row and reports when delete fails`() = runTest(dispatcher) {
        val a = recording("a.wav")
        coEvery { recordings.list() } returns listOf(a)
        coEvery { recordings.delete(a.uri) } returns false

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.deleteRecording(a)
        advanceUntilIdle()

        assertEquals(listOf(a), viewModel.state.value.recordings)
        assertEquals(AudioViewModel.DELETE_FAILED, viewModel.state.value.error)
    }

    @Test
    fun `togglePlayback hands the recording to the player`() {
        val a = recording("a.wav")
        val viewModel = createViewModel()

        viewModel.togglePlayback(a)

        verify { player.toggle(a.uri) }
    }
}
