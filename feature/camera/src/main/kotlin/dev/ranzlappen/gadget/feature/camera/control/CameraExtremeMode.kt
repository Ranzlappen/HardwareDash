package dev.ranzlappen.gadget.feature.camera.control

/**
 * Configuration for a high-FPS capture session driven via Camera2 directly,
 * bypassing the FPS clamps that CameraX applies. The implementation enforces
 * a hard 30-second ceiling on [durationMillis] and a 240 fps absolute cap on
 * [fps] regardless of what the sensor's `SCALER_AVAILABLE_MAX_FPS` reports.
 */
data class HighFpsConfig(
    val cameraId: String,
    val fps: Int,
    val durationMillis: Long,
)

/**
 * Manual exposure override. Any field left null retains the auto-exposure
 * default. [exposureTimeNanos] is clamped to 30 seconds; [isoSensitivity]
 * is clamped to the sensor's reported `SENSOR_INFO_SENSITIVITY_RANGE`
 * upper bound (no above-silicon boost — the rooted-tier benefit is that
 * CameraX often clamps narrower than the silicon allows).
 */
data class ManualExposureConfig(
    val cameraId: String,
    val isoSensitivity: Int? = null,
    val exposureTimeNanos: Long? = null,
    val focusDistanceDiopter: Float? = null,
    val durationMillis: Long = 0L,
)

/**
 * RAW DNG capture request. On vendor-locked devices that hide the
 * `RAW_SENSOR` capability the rooted impl will best-effort whitelist the
 * app via `setprop persist.camera.privapp.list` before retrying.
 */
data class RawCaptureConfig(
    val cameraId: String,
    val frameCount: Int,
)

/**
 * Multi-camera concurrent capture. Hard-capped at 3 concurrent streams and
 * 15 s per session. [cameraIds] must list at least two; the impl returns
 * [CameraControllerResult.Unsupported] if the device cannot satisfy.
 */
data class MultiCameraConfig(
    val cameraIds: List<String>,
    val durationMillis: Long,
)
