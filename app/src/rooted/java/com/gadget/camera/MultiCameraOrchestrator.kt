package com.gadget.camera

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal const val MULTI_CAMERA_HARD_CEILING_MILLIS = 15_000L
internal const val MULTI_CAMERA_MAX_CONCURRENT = 3
private const val MULTI_BUFFER_COUNT = 2
private const val MULTI_IMAGE_WIDTH = 1280
private const val MULTI_IMAGE_HEIGHT = 720
private const val MULTI_HANDLER_THREAD_NAME_PREFIX = "MultiCam-"

/**
 * Concurrent multi-camera capture. Opens up to [MULTI_CAMERA_MAX_CONCURRENT]
 * `CameraDevice`s in parallel and binds a minimal preview pipeline per
 * device. The HAL on most modern devices supports concurrent open only for
 * specific subsets reported by `CameraManager.getConcurrentCameraIds()`;
 * if the requested set isn't satisfiable the impl returns
 * [CameraControllerResult.Unsupported].
 *
 * Hard 15-second per-session ceiling, enforced inside the coroutineScope.
 */
@Singleton
class MultiCameraOrchestrator @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun capture(config: MultiCameraConfig): CameraControllerResult = coroutineScope {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return@coroutineScope CameraControllerResult.Unsupported
        val ids = config.cameraIds.distinct().take(MULTI_CAMERA_MAX_CONCURRENT)
        if (ids.size < 2) return@coroutineScope CameraControllerResult.Unsupported
        if (!isConcurrencySupported(cameraManager, ids)) {
            return@coroutineScope CameraControllerResult.Unsupported
        }

        val effectiveDuration = config.durationMillis.coerceAtMost(MULTI_CAMERA_HARD_CEILING_MILLIS)
        val sessions = mutableListOf<MultiCameraSession>()

        try {
            val openJobs = ids.mapIndexed { idx, id ->
                async { openOneCamera(cameraManager, id, idx) }
            }
            val opened = openJobs.awaitAll()
            sessions.addAll(opened.filterNotNull())
            if (sessions.size != ids.size) {
                return@coroutineScope CameraControllerResult.HardwareError(
                    "only ${sessions.size}/${ids.size} cameras opened",
                )
            }
            sessions.forEach { it.startPreview() }
            delay(effectiveDuration)
            CameraControllerResult.Ok
        } catch (e: CameraAccessException) {
            CameraControllerResult.HardwareError("multi-camera: ${e.reason}")
        } finally {
            withContext(NonCancellable) { sessions.forEach { it.tearDown() } }
        }
    }

    private fun isConcurrencySupported(cameraManager: CameraManager, ids: List<String>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        val supported = runCatching { cameraManager.concurrentCameraIds }.getOrNull() ?: return true
        if (supported.isEmpty()) return true
        return supported.any { set -> set.containsAll(ids) }
    }

    private suspend fun openOneCamera(
        cameraManager: CameraManager,
        cameraId: String,
        index: Int,
    ): MultiCameraSession? {
        val handlerThread = HandlerThread("$MULTI_HANDLER_THREAD_NAME_PREFIX$index").apply { start() }
        val handler = Handler(handlerThread.looper)
        val device = try {
            openCamera(cameraManager, cameraId, handler)
        } catch (e: CameraAccessException) {
            handlerThread.quitSafely()
            return null
        } catch (e: SecurityException) {
            handlerThread.quitSafely()
            return null
        }
        val reader = ImageReader.newInstance(
            MULTI_IMAGE_WIDTH,
            MULTI_IMAGE_HEIGHT,
            ImageFormat.YUV_420_888,
            MULTI_BUFFER_COUNT,
        )
        val session = createSession(device, reader.surface, handler)
        return MultiCameraSession(device, session, reader, handlerThread, handler)
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
                        CameraAccessException(CameraAccessException.CAMERA_ERROR, "multi-cam session configure failed"),
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

class MultiCameraSession(
    private val device: CameraDevice,
    private val session: CameraCaptureSession,
    private val reader: ImageReader,
    private val handlerThread: HandlerThread,
    private val handler: Handler,
) {
    fun startPreview() {
        val request = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(reader.surface)
        }.build()
        runCatching { session.setRepeatingRequest(request, null, handler) }
    }

    fun tearDown() {
        runCatching { session.stopRepeating() }
        runCatching { session.close() }
        runCatching { reader.close() }
        runCatching { device.close() }
        handlerThread.quitSafely()
    }
}
