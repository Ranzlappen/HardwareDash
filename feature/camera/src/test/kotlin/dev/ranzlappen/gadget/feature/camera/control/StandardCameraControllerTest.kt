package dev.ranzlappen.gadget.feature.camera.control

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Contract-pinning tests for [StandardCameraController] — the standard-flavor
 * implementation of [CameraController]. Mirrors
 * `StandardAudioRoutingControllerTest`'s shape: the standard APK has no
 * privileged shell and no Camera HAL bypass surface, so every extreme-tier
 * method must always report [CameraControllerResult.Unsupported], regardless
 * of the config passed in. Pinning this guards against a future edit
 * accidentally wiring one of these methods to real (unsafe, unprivileged)
 * behavior instead of the deliberate no-op.
 */
class StandardCameraControllerTest {

    private val controller = StandardCameraController()

    @Test
    fun `highFpsCapture is unsupported`() = runTest {
        val result = controller.highFpsCapture(HighFpsConfig(cameraId = "0", fps = 240, durationMillis = 5_000))

        assertEquals(CameraControllerResult.Unsupported, result)
    }

    @Test
    fun `manualOverride is unsupported`() = runTest {
        val result = controller.manualOverride(
            ManualExposureConfig(cameraId = "0", isoSensitivity = 800, durationMillis = 1_000),
        )

        assertEquals(CameraControllerResult.Unsupported, result)
    }

    @Test
    fun `rawCapture is unsupported`() = runTest {
        val result = controller.rawCapture(RawCaptureConfig(cameraId = "0", frameCount = 5))

        assertEquals(CameraControllerResult.Unsupported, result)
    }

    @Test
    fun `multiCameraCapture is unsupported`() = runTest {
        val result = controller.multiCameraCapture(
            MultiCameraConfig(cameraIds = listOf("0", "1"), durationMillis = 5_000),
        )

        assertEquals(CameraControllerResult.Unsupported, result)
    }

    @Test
    fun `halBypassFrame is unsupported`() = runTest {
        assertEquals(CameraControllerResult.Unsupported, controller.halBypassFrame())
    }

    @Test
    fun `setShutterSoundEnabled is unsupported regardless of the requested state`() = runTest {
        assertEquals(CameraControllerResult.Unsupported, controller.setShutterSoundEnabled(true))
        assertEquals(CameraControllerResult.Unsupported, controller.setShutterSoundEnabled(false))
    }
}
