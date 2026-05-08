package com.gadget.camera

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Range
import android.view.Surface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val FPS_HARD_CEILING = 240
internal const val EXPOSURE_HARD_CEILING_NANOS = 30_000_000_000L
internal const val HIGH_FPS_HARD_CEILING_MILLIS = 30_000L
private const val IMAGE_READER_WIDTH = 1280
private const val IMAGE_READER_HEIGHT = 720
private const val IMAGE_READER_FORMAT = android.graphics.ImageFormat.YUV_420_888
private const val IMAGE_READER_MAX_BUFFERS = 3
private const val EXECUTOR_THREAD_NAME = "Camera2OverrideHelper"

/**
 * Camera2 wrapper used by [RootedCameraController] for high-FPS and
 * manual-override capture. Bridges Camera2's callback API to coroutines
 * via [suspendCancellableCoroutine] and tears down the session +
 * device + handler thread in a `NonCancellable` finally so a cancelled
 * caller never leaks a privileged camera handle.
 */
@Singleton
class Camera2OverrideHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    suspend fun runHighFps(config: HighFpsConfig): CameraControllerResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return CameraControllerResult.Unsupported
        val characteristics = runCatching {
            cameraManager.getCameraCharacteristics(config.cameraId)
        }.getOrNull() ?: return CameraControllerResult.HardwareError("camera id not found")

        val targetFps = config.fps.coerceIn(1, FPS_HARD_CEILING)
        val effectiveDuration = config.durationMillis.coerceAtMost(HIGH_FPS_HARD_CEILING_MILLIS)
        val supportedRange = pickFpsRange(characteristics, targetFps)

        return runWithDevice(cameraManager, config.cameraId) { device, handler ->
            val reader = ImageReader.newInstance(
                IMAGE_READER_WIDTH,
                IMAGE_READER_HEIGHT,
                IMAGE_READER_FORMAT,
                IMAGE_READER_MAX_BUFFERS,
            )
            try {
                val session = createSession(device, reader.surface, handler)
                val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(reader.surface)
                    set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, supportedRange)
                }.build()
                session.setRepeatingRequest(request, null, handler)
                delay(effectiveDuration)
                session.stopRepeating()
                session.close()
                CameraControllerResult.Ok
            } catch (e: CameraAccessException) {
                CameraControllerResult.HardwareError("camera2 high-fps: ${e.reason}")
            } finally {
                reader.close()
            }
        }
    }

    suspend fun runManualOverride(config: ManualExposureConfig): CameraControllerResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return CameraControllerResult.Unsupported
        val characteristics = runCatching {
            cameraManager.getCameraCharacteristics(config.cameraId)
        }.getOrNull() ?: return CameraControllerResult.HardwareError("camera id not found")

        val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val effectiveIso = config.isoSensitivity?.let { iso ->
            isoRange?.let { range -> iso.coerceIn(range.lower, range.upper) } ?: iso
        }
        val effectiveExposure = config.exposureTimeNanos?.coerceIn(0L, EXPOSURE_HARD_CEILING_NANOS)

        return runWithDevice(cameraManager, config.cameraId) { device, handler ->
            val reader = ImageReader.newInstance(
                IMAGE_READER_WIDTH,
                IMAGE_READER_HEIGHT,
                IMAGE_READER_FORMAT,
                IMAGE_READER_MAX_BUFFERS,
            )
            try {
                val session = createSession(device, reader.surface, handler)
                val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                    addTarget(reader.surface)
                    set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF)
                    effectiveIso?.let { set(CaptureRequest.SENSOR_SENSITIVITY, it) }
                    effectiveExposure?.let { set(CaptureRequest.SENSOR_EXPOSURE_TIME, it) }
                    config.focusDistanceDiopter?.let {
                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                        set(CaptureRequest.LENS_FOCUS_DISTANCE, it)
                    }
                }.build()
                session.capture(request, null, handler)
                if (config.durationMillis > 0) {
                    delay(config.durationMillis.coerceAtMost(EXPOSURE_HARD_CEILING_NANOS / 1_000_000L))
                }
                session.close()
                CameraControllerResult.Ok
            } catch (e: CameraAccessException) {
                CameraControllerResult.HardwareError("camera2 manual: ${e.reason}")
            } finally {
                reader.close()
            }
        }
    }

    private fun pickFpsRange(
        characteristics: CameraCharacteristics,
        targetFps: Int,
    ): Range<Int> {
        val available = characteristics
            .get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
            ?: return Range(targetFps, targetFps)
        return available
            .filter { it.upper >= targetFps }
            .minByOrNull { it.upper - targetFps + (targetFps - it.lower) }
            ?: available.maxByOrNull { it.upper }
            ?: Range(targetFps, targetFps)
    }

    private suspend inline fun runWithDevice(
        cameraManager: CameraManager,
        cameraId: String,
        crossinline block: suspend (CameraDevice, Handler) -> CameraControllerResult,
    ): CameraControllerResult {
        val handlerThread = HandlerThread(EXECUTOR_THREAD_NAME).apply { start() }
        val handler = Handler(handlerThread.looper)
        val device = try {
            openCamera(cameraManager, cameraId, handler)
        } catch (e: CameraAccessException) {
            handlerThread.quitSafely()
            return CameraControllerResult.HardwareError("openCamera: ${e.reason}")
        } catch (e: SecurityException) {
            handlerThread.quitSafely()
            return CameraControllerResult.HardwareError("camera permission missing: ${e.message}")
        }
        return try {
            block(device, handler)
        } finally {
            withContext(NonCancellable) {
                runCatching { device.close() }
                handlerThread.quitSafely()
            }
        }
    }

    private suspend fun openCamera(
        cameraManager: CameraManager,
        cameraId: String,
        handler: Handler,
    ): CameraDevice = suspendCancellableCoroutine { cont ->
        cameraManager.openCamera(
            cameraId,
            object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (cont.isActive) cont.resume(camera)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    runCatching { camera.close() }
                    if (cont.isActive) {
                        cont.resumeWithException(CameraAccessException(CameraAccessException.CAMERA_DISCONNECTED))
                    }
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    runCatching { camera.close() }
                    if (cont.isActive) {
                        cont.resumeWithException(
                            CameraAccessException(CameraAccessException.CAMERA_ERROR, "error=$error"),
                        )
                    }
                }
            },
            handler,
        )
    }

    private suspend fun createSession(
        device: CameraDevice,
        surface: Surface,
        handler: Handler,
    ): CameraCaptureSession = suspendCancellableCoroutine { cont ->
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                if (cont.isActive) cont.resume(session)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                if (cont.isActive) {
                    cont.resumeWithException(
                        CameraAccessException(CameraAccessException.CAMERA_ERROR, "session configure failed"),
                    )
                }
            }
        }
        val outputs = listOf(OutputConfiguration(surface))
        device.createCaptureSession(
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                outputs,
                { runnable -> handler.post(runnable) },
                callback,
            ),
        )
    }
}
