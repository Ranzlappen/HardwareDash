package dev.ranzlappen.gadget.feature.camera

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.camera.core.CameraControl
import dev.ranzlappen.gadget.core.root.RootCapabilityRegistry
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [CameraViewModel]'s non-passthrough logic:
 *
 *  - seeding [CameraViewModel.isRootedFlavor] from [RootCapabilityRegistry] at
 *    construction time (mirrors `AudioViewModelTest` / `BtViewModelTest`'s
 *    identical seeding coverage),
 *  - [CameraViewModel.onScanDetected] updating `latestScan` *and* clearing any
 *    prior `error` together while persisting the scan via [ScanHistoryRepository],
 *  - [CameraViewModel.toggleTorch]'s actual toggle (not just a setter — it reads
 *    the current `isTorchOn` to compute the next value) and forwarding to
 *    [CameraControl.enableTorch], and
 *  - [CameraViewModel.copyToClipboard] building the clipboard payload from the
 *    scan's raw value.
 *
 * [CameraViewModel.onPermissionResult] is pure passthrough and deliberately not
 * covered here, matching this repo's convention of skipping passthrough setters.
 * [CameraViewModel.clearHistory] has no computed values to pin beyond "it calls
 * `repository.clear()`", which is covered briefly for completeness.
 *
 * `ClipData.newPlainText` is intercepted via `mockkStatic` — same technique
 * `SettingsViewModelTest` uses for `AppCompatDelegate` — and `CameraControl` is a
 * plain interface from `camera-core`, so it mocks cleanly with no Android stub
 * jar involved.
 */
class CameraViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val repository = mockk<ScanHistoryRepository>(relaxed = true)
    private val rootCapabilityRegistry = mockk<RootCapabilityRegistry>(relaxed = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { repository.history } returns MutableStateFlow(emptyList())
    }

    @After
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    private fun createViewModel(): CameraViewModel = CameraViewModel(repository, rootCapabilityRegistry)

    private fun barcode(id: String = "1") = BarcodeResult(
        id = id,
        rawValue = "value-$id",
        format = "QR_CODE",
        displayType = "Text",
        timestamp = 1_000L,
    )

    // ---- constructor seeding of isRootedFlavor ----

    @Test
    fun `isRootedFlavor is true for the rooted flavor`() {
        every { rootCapabilityRegistry.isRootedFlavor } returns true

        assertTrue(createViewModel().isRootedFlavor)
    }

    @Test
    fun `isRootedFlavor is false for the standard flavor`() {
        every { rootCapabilityRegistry.isRootedFlavor } returns false

        assertFalse(createViewModel().isRootedFlavor)
    }

    // ---- onScanDetected ----

    @Test
    fun `onScanDetected records the latest scan, clears any prior error, and persists it`() {
        val viewModel = createViewModel()
        val scan = barcode()

        viewModel.onScanDetected(scan)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(scan, viewModel.state.value.latestScan)
        assertNull(viewModel.state.value.error)
        coVerify { repository.add(scan) }
    }

    // ---- toggleTorch ----

    @Test
    fun `toggleTorch flips isTorchOn from off to on and enables the torch`() {
        val viewModel = createViewModel()
        val cameraControl = mockk<CameraControl>(relaxed = true)

        viewModel.toggleTorch(cameraControl)

        assertTrue(viewModel.state.value.isTorchOn)
        verify { cameraControl.enableTorch(true) }
    }

    @Test
    fun `toggleTorch flips isTorchOn back off on a second call`() {
        val viewModel = createViewModel()
        val cameraControl = mockk<CameraControl>(relaxed = true)

        viewModel.toggleTorch(cameraControl)
        viewModel.toggleTorch(cameraControl)

        assertFalse(viewModel.state.value.isTorchOn)
        verify { cameraControl.enableTorch(false) }
    }

    // ---- clearHistory ----

    @Test
    fun `clearHistory clears the repository`() {
        val viewModel = createViewModel()

        viewModel.clearHistory()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.clear() }
    }

    // ---- copyToClipboard ----

    @Test
    fun `copyToClipboard copies the scan's raw value onto the system clipboard`() {
        mockkStatic(ClipData::class)
        val clipData = mockk<ClipData>()
        every { ClipData.newPlainText("barcode", "value-1") } returns clipData
        val clipboard = mockk<ClipboardManager>(relaxed = true)
        val context = mockk<Context>(relaxed = true) {
            every { getSystemService(Context.CLIPBOARD_SERVICE) } returns clipboard
        }
        val viewModel = createViewModel()

        viewModel.copyToClipboard(context, barcode())

        verify { clipboard.setPrimaryClip(clipData) }
    }
}
