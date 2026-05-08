package com.gadget.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.DngCreator
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import com.gadget.root.core.RootShell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val RAW_FRAME_HARD_CEILING = 20
internal const val RAW_MIN_FREE_DISK_BYTES = 500L * 1024L * 1024L
private const val RAW_HANDLER_THREAD_NAME = "RawCaptureSession"
private const val RAW_BUFFER_COUNT = 2
private const val DNG_FILE_PREFIX = "gadget-raw-"
private const val DNG_FILE_EXTENSION = ".dng"

/**
 * RAW DNG capture via Camera2. On vendor-locked devices (Samsung knox-locked,
 * some Xiaomi MIUI builds) the `RAW_SENSOR` capability is hidden from
 * userspace; the impl best-effort whitelists the app via `setprop
 * persist.camera.privapp.list ${packageName}` and retries the capability
 * check. If RAW remains unavailable, returns [CameraControllerResult.Unsupported]
 * cleanly.
 *
 * Frames land in the app's external files dir as `gadget-raw-<timestamp>.dng`.
 * Free disk is checked before each capture; aborts to [CameraControllerResult.HardwareError]
 * below the 500 MB threshold.
 */
@Singleton
class RawCaptureSession @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: RootShell,
) {
    suspend fun capture(config: RawCaptureConfig): CameraControllerResult {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return CameraControllerResult.Unsupported
        val characteristics = ensureRawCapability(cameraManager, config.cameraId)
            ?: return CameraControllerResult.Unsupported

        val rawSize = characteristics
            .get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
            ?: return CameraControllerResult.HardwareError("sensor pixel array size unknown")

        val frameCount = config.frameCount.coerceIn(1, RAW_FRAME_HARD_CEILING)
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DCIM)
            ?: return CameraControllerResult.HardwareError("no external files dir")

        val handlerThread = HandlerThread(RAW_HANDLER_THREAD_NAME).apply { start() }
        val handler = Handler(handlerThread.looper)

        val device = try {
            openCamera(cameraManager, config.cameraId, handler)
        } catch (e: CameraAccessException) {
            handlerThread.quitSafely()
            return CameraControllerResult.HardwareError("openCamera RAW: ${e.reason}")
        } catch (e: SecurityException) {
            handlerThread.quitSafely()
            return CameraControllerResult.HardwareError("camera permission missing: ${e.message}")
        }

        val reader = ImageReader.newInstance(
            rawSize.width,
            rawSize.height,
            ImageFormat.RAW_SENSOR,
            RAW_BUFFER_COUNT,
        )

        return try {
            val session = createSession(device, reader.surface, handler)
            captureFrames(session, device, reader, characteristics, frameCount, outputDir, handler)
        } catch (e: CameraAccessException) {
            CameraControllerResult.HardwareError("RAW capture: ${e.reason}")
        } finally {
            withContext(NonCancellable) {
                runCatching { reader.close() }
                runCatching { device.close() }
                handlerThread.quitSafely()
            }
        }
    }

    private suspend fun ensureRawCapability(
        cameraManager: CameraManager,
        cameraId: String,
    ): CameraCharacteristics? {
        val first = runCatching { cameraManager.getCameraCharacteristics(cameraId) }.getOrNull()
            ?: return null
        if (first.supportsRaw()) return first

        shell.exec("setprop persist.camera.privapp.list ${context.packageName}")
        val second = runCatching { cameraManager.getCameraCharacteristics(cameraId) }.getOrNull()
        return second?.takeIf { it.supportsRaw() }
    }

    private fun CameraCharacteristics.supportsRaw(): Boolean {
        val caps = get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return false
        return caps.contains(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
    }

    private suspend fun captureFrames(
        session: CameraCaptureSession,
        device: CameraDevice,
        reader: ImageReader,
        characteristics: CameraCharacteristics,
        frameCount: Int,
        outputDir: File,
        handler: Handler,
    ): CameraControllerResult {
        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
            addTarget(reader.surface)
        }.build()

        repeat(frameCount) { index ->
            if (outputDir.usableSpace < RAW_MIN_FREE_DISK_BYTES) {
                return CameraControllerResult.HardwareError(
                    "free disk below ${RAW_MIN_FREE_DISK_BYTES / 1024 / 1024}MB at frame $index",
                )
            }
            val result = captureSingleFrame(session, request, reader, characteristics, outputDir, handler)
            if (result is CameraControllerResult.HardwareError) return result
        }
        session.close()
        return CameraControllerResult.Ok
    }

    private suspend fun captureSingleFrame(
        session: CameraCaptureSession,
        request: CaptureRequest,
        reader: ImageReader,
        characteristics: CameraCharacteristics,
        outputDir: File,
        handler: Handler,
    ): CameraControllerResult = suspendCancellableCoroutine { cont ->
        val callback = object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                s: CameraCaptureSession,
                req: CaptureRequest,
                result: TotalCaptureResult,
            ) {
                val image = reader.acquireNextImage()
                if (image == null) {
                    if (cont.isActive) cont.resume(CameraControllerResult.HardwareError("no image"))
                    return
                }
                try {
                    val file = File(
                        outputDir,
                        "$DNG_FILE_PREFIX${System.currentTimeMillis()}$DNG_FILE_EXTENSION",
                    )
                    DngCreator(characteristics, result).use { dng ->
                        FileOutputStream(file).use { out -> dng.writeImage(out, image) }
                    }
                    if (cont.isActive) cont.resume(CameraControllerResult.Ok)
                } catch (e: Throwable) {
                    if (cont.isActive) cont.resume(CameraControllerResult.HardwareError("dng: ${e.message}"))
                } finally {
                    image.close()
                }
            }

            override fun onCaptureFailed(
                s: CameraCaptureSession,
                req: CaptureRequest,
                failure: android.hardware.camera2.CaptureFailure,
            ) {
                if (cont.isActive) {
                    cont.resume(CameraControllerResult.HardwareError("capture failed: reason=${failure.reason}"))
                }
            }
        }
        try {
            session.capture(request, callback, handler)
        } catch (e: CameraAccessException) {
            if (cont.isActive) cont.resumeWithException(e)
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
                        CameraAccessException(CameraAccessException.CAMERA_ERROR, "RAW session configure failed"),
                    )
                }
            }
        }
        device.createCaptureSession(
            SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                listOf(OutputConfiguration(surface)),
                { runnable -> handler.post(runnable) },
                callback,
            ),
        )
    }

}
