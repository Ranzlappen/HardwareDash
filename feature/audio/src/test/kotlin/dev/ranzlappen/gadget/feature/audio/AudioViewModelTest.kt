package dev.ranzlappen.gadget.feature.audio

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import io.mockk.EqMatcher
import io.mockk.anyConstructed
import io.mockk.constructedWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
 * [AudioViewModel.onPermissionResult] and the two `init`-block StateFlow
 * collectors (`recorder.isRecording` / `dbMeter.stream()` mirrored straight
 * into [AudioState]) are pure passthrough and deliberately not covered here,
 * matching this repo's convention of skipping passthrough setters.
 *
 * [AudioRecorder] / [DbMeterMetricSource] are mocked collaborators — mockk
 * mocks final Kotlin classes out of the box on the JVM, no `open` needed.
 * `Intent` construction is intercepted via `mockkConstructor` and
 * `ContextCompat.checkSelfPermission` via `mockkStatic`, the same
 * techniques `SettingsViewModelTest` and `DbMeterMetricSourceTest` use.
 */
class AudioViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var recorder: AudioRecorder
    private lateinit var dbMeter: DbMeterMetricSource
    private lateinit var rootCapabilityRegistry: RootCapabilityRegistry

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(ContextCompat::class)
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setPackage(any()) } returns mockk(relaxed = true)

        context = mockk(relaxed = true)
        recorder = mockk(relaxed = true)
        dbMeter = mockk(relaxed = true)
        rootCapabilityRegistry = mockk(relaxed = true)

        every { recorder.isRecording } returns MutableStateFlow(false)
        every { dbMeter.stream() } returns null
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
        AudioViewModel(context, recorder, dbMeter, rootCapabilityRegistry)

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
            constructedWith<Intent>(EqMatcher(AudioRecordService.ACTION_START_RECORD)).setPackage(any())
        }
        verify { context.startService(any()) }
    }

    @Test
    fun `stopRecording fires an Intent carrying the stop action`() {
        val viewModel = createViewModel()

        viewModel.stopRecording()

        verify {
            constructedWith<Intent>(EqMatcher(AudioRecordService.ACTION_STOP_RECORD)).setPackage(any())
        }
        verify { context.startService(any()) }
    }
}
