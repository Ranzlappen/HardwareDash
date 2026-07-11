package dev.ranzlappen.gadget.feature.camera.automation

import android.content.Context
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.feature.camera.ScanHistoryRepository
import dev.ranzlappen.gadget.feature.camera.control.CameraController
import dev.ranzlappen.gadget.feature.camera.control.CameraControllerResult
import dev.ranzlappen.gadget.feature.camera.control.HighFpsConfig
import dev.ranzlappen.gadget.feature.camera.control.ManualExposureConfig
import dev.ranzlappen.gadget.feature.camera.control.MultiCameraConfig
import dev.ranzlappen.gadget.feature.camera.control.RawCaptureConfig
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [CameraActionHandler] — `:feature:camera`'s automation
 * `ActionHandler` seam. Mirrors `NotificationActionHandlerTest`'s shape:
 * the [CameraController]-backed branches are pinned here against a mocked
 * controller (the real rooted/standard impls have their own tests), and
 * every argument-parsing edge case (`camera_id` fallback, blank-means-auto
 * manual-override fields, `camera_ids` CSV splitting, `set_shutter_sound`'s
 * lenient boolean parsing) is exercised directly since `dispatch` is where
 * that parsing actually lives.
 *
 * `Context.getString` is stubbed to `""` (relaxed mock) — [CameraActionHandler]
 * resolves every [dev.ranzlappen.gadget.core.automation.ModuleAction] label
 * eagerly in its `actions` initializer, so even tests that never touch labels
 * need construction not to throw the plain-JVM-unit-test "not mocked" error.
 */
class CameraActionHandlerTest {

    private val context = mockk<Context>(relaxed = true) {
        every { getString(any()) } returns ""
    }
    private val cameraController = mockk<CameraController>()
    private val scanHistoryRepository = mockk<ScanHistoryRepository>(relaxed = true)
    private val handler = CameraActionHandler(context, cameraController, scanHistoryRepository)

    @Test
    fun `featureId matches the contracted FEATURE_ID constant`() {
        assertEquals(CameraActionHandler.FEATURE_ID, handler.featureId)
        assertEquals("camera", handler.featureId)
    }

    @Test
    fun `declares seven actions with clear_scan_history as the only non-root one`() {
        assertEquals(
            setOf(
                CameraActionHandler.ACTION_CLEAR_SCAN_HISTORY,
                CameraActionHandler.ACTION_HIGH_FPS_CAPTURE,
                CameraActionHandler.ACTION_MANUAL_OVERRIDE,
                CameraActionHandler.ACTION_RAW_CAPTURE,
                CameraActionHandler.ACTION_MULTI_CAMERA_CAPTURE,
                CameraActionHandler.ACTION_HAL_BYPASS_FRAME,
                CameraActionHandler.ACTION_SET_SHUTTER_SOUND,
            ),
            handler.actions.map { it.key }.toSet(),
        )
        val byKey = handler.actions.associateBy { it.key }
        assertFalse(byKey.getValue(CameraActionHandler.ACTION_CLEAR_SCAN_HISTORY).requiresRoot)
        (handler.actions - byKey.getValue(CameraActionHandler.ACTION_CLEAR_SCAN_HISTORY)).forEach {
            assertTrue(it.requiresRoot, "${it.key} should require root")
        }
    }

    @Test
    fun `unknown action returns Unsupported`() = runTest {
        val result = handler.dispatch("not-a-real-action", emptyMap())
        assertEquals(ActionResult.Unsupported, result)
    }

    // ---- clear_scan_history ----

    @Test
    fun `clear_scan_history clears the repository and returns Success`() = runTest {
        val result = handler.dispatch(CameraActionHandler.ACTION_CLEAR_SCAN_HISTORY, emptyMap())

        assertEquals(ActionResult.Success, result)
        coVerify { scanHistoryRepository.clear() }
    }

    // ---- high_fps_capture ----

    @Test
    fun `high_fps_capture forwards parsed params and defaults the camera id`() = runTest {
        coEvery { cameraController.highFpsCapture(any()) } returns CameraControllerResult.Ok

        val result = handler.dispatch(
            CameraActionHandler.ACTION_HIGH_FPS_CAPTURE,
            mapOf(
                CameraActionHandler.PARAM_FPS to "240",
                CameraActionHandler.PARAM_DURATION_MS to "10000",
            ),
        )

        assertEquals(ActionResult.Success, result)
        coVerify {
            cameraController.highFpsCapture(
                HighFpsConfig(cameraId = "0", fps = 240, durationMillis = 10_000),
            )
        }
    }

    @Test
    fun `high_fps_capture falls back to fps and duration defaults when params are missing`() = runTest {
        coEvery { cameraController.highFpsCapture(any()) } returns CameraControllerResult.Ok

        handler.dispatch(CameraActionHandler.ACTION_HIGH_FPS_CAPTURE, emptyMap())

        coVerify {
            cameraController.highFpsCapture(
                HighFpsConfig(cameraId = "0", fps = 120, durationMillis = 5_000),
            )
        }
    }

    @Test
    fun `high_fps_capture uses a blank camera_id fallback rather than an empty string`() = runTest {
        coEvery { cameraController.highFpsCapture(any()) } returns CameraControllerResult.Ok

        handler.dispatch(
            CameraActionHandler.ACTION_HIGH_FPS_CAPTURE,
            mapOf(CameraActionHandler.PARAM_CAMERA_ID to "   "),
        )

        coVerify { cameraController.highFpsCapture(match { it.cameraId == "0" }) }
    }

    // ---- CameraControllerResult -> ActionResult mapping (exercised once via high_fps_capture) ----

    @Test
    fun `controller Unsupported maps to a root-required Failure`() = runTest {
        coEvery { cameraController.highFpsCapture(any()) } returns CameraControllerResult.Unsupported

        val result = handler.dispatch(CameraActionHandler.ACTION_HIGH_FPS_CAPTURE, emptyMap())

        assertEquals(ActionResult.Failure("requires the rooted app version"), result)
    }

    @Test
    fun `controller OptedOut maps to a Settings-opt-out Failure`() = runTest {
        coEvery { cameraController.highFpsCapture(any()) } returns CameraControllerResult.OptedOut

        val result = handler.dispatch(CameraActionHandler.ACTION_HIGH_FPS_CAPTURE, emptyMap())

        assertEquals(ActionResult.Failure("turned off in Settings"), result)
    }

    @Test
    fun `controller RateLimited maps to a Failure carrying the retry delay`() = runTest {
        coEvery { cameraController.highFpsCapture(any()) } returns CameraControllerResult.RateLimited(2_500)

        val result = handler.dispatch(CameraActionHandler.ACTION_HIGH_FPS_CAPTURE, emptyMap())

        assertEquals(ActionResult.Failure("rate-limited; retry in 2500ms"), result)
    }

    @Test
    fun `controller HardwareError maps to a Failure carrying its message`() = runTest {
        coEvery { cameraController.highFpsCapture(any()) } returns
            CameraControllerResult.HardwareError("sensor busy")

        val result = handler.dispatch(CameraActionHandler.ACTION_HIGH_FPS_CAPTURE, emptyMap())

        assertEquals(ActionResult.Failure("sensor busy"), result)
    }

    // ---- manual_override ----

    @Test
    fun `manual_override parses provided iso, exposure and focus values`() = runTest {
        coEvery { cameraController.manualOverride(any()) } returns CameraControllerResult.Ok

        handler.dispatch(
            CameraActionHandler.ACTION_MANUAL_OVERRIDE,
            mapOf(
                CameraActionHandler.PARAM_CAMERA_ID to "1",
                CameraActionHandler.PARAM_ISO to "800",
                CameraActionHandler.PARAM_EXPOSURE_NANOS to "16000000",
                CameraActionHandler.PARAM_FOCUS_DIOPTER to "2.5",
                CameraActionHandler.PARAM_DURATION_MS to "3000",
            ),
        )

        coVerify {
            cameraController.manualOverride(
                ManualExposureConfig(
                    cameraId = "1",
                    isoSensitivity = 800,
                    exposureTimeNanos = 16_000_000L,
                    focusDistanceDiopter = 2.5f,
                    durationMillis = 3_000,
                ),
            )
        }
    }

    @Test
    fun `manual_override treats blank fields as auto (null)`() = runTest {
        coEvery { cameraController.manualOverride(any()) } returns CameraControllerResult.Ok

        handler.dispatch(
            CameraActionHandler.ACTION_MANUAL_OVERRIDE,
            mapOf(
                CameraActionHandler.PARAM_ISO to "",
                CameraActionHandler.PARAM_EXPOSURE_NANOS to "  ",
            ),
        )

        coVerify {
            cameraController.manualOverride(
                ManualExposureConfig(
                    cameraId = "0",
                    isoSensitivity = null,
                    exposureTimeNanos = null,
                    focusDistanceDiopter = null,
                    durationMillis = 0,
                ),
            )
        }
    }

    // ---- raw_capture ----

    @Test
    fun `raw_capture defaults frame_count to 1`() = runTest {
        coEvery { cameraController.rawCapture(any()) } returns CameraControllerResult.Ok

        handler.dispatch(CameraActionHandler.ACTION_RAW_CAPTURE, emptyMap())

        coVerify { cameraController.rawCapture(RawCaptureConfig(cameraId = "0", frameCount = 1)) }
    }

    @Test
    fun `raw_capture forwards a provided frame_count`() = runTest {
        coEvery { cameraController.rawCapture(any()) } returns CameraControllerResult.Ok

        handler.dispatch(
            CameraActionHandler.ACTION_RAW_CAPTURE,
            mapOf(CameraActionHandler.PARAM_FRAME_COUNT to "12"),
        )

        coVerify { cameraController.rawCapture(RawCaptureConfig(cameraId = "0", frameCount = 12)) }
    }

    // ---- multi_camera_capture ----

    @Test
    fun `multi_camera_capture splits, trims and filters the camera_ids CSV`() = runTest {
        coEvery { cameraController.multiCameraCapture(any()) } returns CameraControllerResult.Ok

        val result = handler.dispatch(
            CameraActionHandler.ACTION_MULTI_CAMERA_CAPTURE,
            mapOf(CameraActionHandler.PARAM_CAMERA_IDS to " 0 ,1,, 2"),
        )

        assertEquals(ActionResult.Success, result)
        coVerify {
            cameraController.multiCameraCapture(
                MultiCameraConfig(cameraIds = listOf("0", "1", "2"), durationMillis = 5_000),
            )
        }
    }

    @Test
    fun `multi_camera_capture fails without calling the controller when fewer than two ids are given`() = runTest {
        val result = handler.dispatch(
            CameraActionHandler.ACTION_MULTI_CAMERA_CAPTURE,
            mapOf(CameraActionHandler.PARAM_CAMERA_IDS to "0"),
        )

        assertEquals(
            ActionResult.Failure("multi_camera_ids requires at least two comma-separated camera ids"),
            result,
        )
        coVerify(exactly = 0) { cameraController.multiCameraCapture(any()) }
    }

    @Test
    fun `multi_camera_capture fails when camera_ids is missing entirely`() = runTest {
        val result = handler.dispatch(CameraActionHandler.ACTION_MULTI_CAMERA_CAPTURE, emptyMap())

        assertTrue(result is ActionResult.Failure)
        coVerify(exactly = 0) { cameraController.multiCameraCapture(any()) }
    }

    // ---- hal_bypass_frame ----

    @Test
    fun `hal_bypass_frame dispatches directly to the controller`() = runTest {
        coEvery { cameraController.halBypassFrame() } returns CameraControllerResult.Ok

        val result = handler.dispatch(CameraActionHandler.ACTION_HAL_BYPASS_FRAME, emptyMap())

        assertEquals(ActionResult.Success, result)
        coVerify { cameraController.halBypassFrame() }
    }

    // ---- set_shutter_sound ----

    @Test
    fun `set_shutter_sound defaults enabled to true when the param is missing`() = runTest {
        coEvery { cameraController.setShutterSoundEnabled(any()) } returns CameraControllerResult.Ok

        handler.dispatch(CameraActionHandler.ACTION_SET_SHUTTER_SOUND, emptyMap())

        coVerify { cameraController.setShutterSoundEnabled(true) }
    }

    @Test
    fun `set_shutter_sound parses an explicit false`() = runTest {
        coEvery { cameraController.setShutterSoundEnabled(any()) } returns CameraControllerResult.Ok

        handler.dispatch(
            CameraActionHandler.ACTION_SET_SHUTTER_SOUND,
            mapOf(CameraActionHandler.PARAM_ENABLED to "false"),
        )

        coVerify { cameraController.setShutterSoundEnabled(false) }
    }

    @Test
    fun `set_shutter_sound falls back to true for an unparseable value`() = runTest {
        coEvery { cameraController.setShutterSoundEnabled(any()) } returns CameraControllerResult.Ok

        handler.dispatch(
            CameraActionHandler.ACTION_SET_SHUTTER_SOUND,
            mapOf(CameraActionHandler.PARAM_ENABLED to "maybe"),
        )

        coVerify { cameraController.setShutterSoundEnabled(true) }
    }
}
