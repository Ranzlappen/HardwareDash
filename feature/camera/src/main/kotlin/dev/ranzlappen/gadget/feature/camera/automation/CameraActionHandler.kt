package dev.ranzlappen.gadget.feature.camera.automation

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.feature.camera.R
import dev.ranzlappen.gadget.feature.camera.ScanHistoryRepository
import dev.ranzlappen.gadget.feature.camera.control.CameraController
import dev.ranzlappen.gadget.feature.camera.control.CameraControllerResult
import dev.ranzlappen.gadget.feature.camera.control.HighFpsConfig
import dev.ranzlappen.gadget.feature.camera.control.ManualExposureConfig
import dev.ranzlappen.gadget.feature.camera.control.MultiCameraConfig
import dev.ranzlappen.gadget.feature.camera.control.RawCaptureConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Camera's invocable-action surface for the future automation tool.
 *
 * Baseline camera use in this module (barcode-scanner preview + its
 * in-preview torch toggle) only runs against a live `CameraControl` bound
 * to an on-screen `PreviewView` inside [dev.ranzlappen.gadget.feature.camera.CameraScreen] —
 * there is no headless capture path (no `ImageCapture` use case is ever
 * bound) and no persistent camera session a background dispatch could reach,
 * so neither "take a photo" nor "toggle torch" is exposed here. That mirrors
 * [CameraController]'s own docstring: baseline capture stays in CameraX/
 * `CameraScreen`, this controller (and this handler) is for the extreme-tier
 * surface only.
 *
 * [CameraController] itself already plays the role [dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities]
 * plays for vibration: a single interface, standard-flavor-bound to a
 * no-op `StandardCameraController` and rooted-flavor-bound to
 * `RootedCameraController` via the app-level `:core:root` Hilt seam
 * (`RootBindings`), so this handler injects it directly and never branches
 * on `BuildConfig.IS_ROOTED`. Every one of its six actions carries
 * `requiresRoot = true`, and [CameraControllerResult] is mapped onto
 * [ActionResult] with the same shape [dev.ranzlappen.gadget.feature.vibration.automation.VibrationActionHandler]
 * uses for `VibrationRootResult`.
 *
 * The one non-root action, clearing scan history, reaches
 * [ScanHistoryRepository] directly — it's a plain DataStore write, not a
 * hardware call, so it carries no foreground-service requirement the way
 * torch's strobe or vibration's playback do.
 */
@Singleton
class CameraActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cameraController: CameraController,
    private val scanHistoryRepository: ScanHistoryRepository,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(
            key = ACTION_CLEAR_SCAN_HISTORY,
            label = context.getString(R.string.camera_action_clear_scan_history),
        ),
        ModuleAction(
            key = ACTION_HIGH_FPS_CAPTURE,
            label = context.getString(R.string.camera_action_high_fps_capture),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_CAMERA_ID, ActionParamType.Text, DEFAULT_CAMERA_ID),
                ActionParam(PARAM_FPS, ActionParamType.Int, "120", 1f, 240f),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "5000", 100f, 30_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_MANUAL_OVERRIDE,
            label = context.getString(R.string.camera_action_manual_override),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_CAMERA_ID, ActionParamType.Text, DEFAULT_CAMERA_ID),
                // Blank means "leave this field on auto" — mirrors
                // ManualExposureConfig's own null-means-auto contract.
                ActionParam(PARAM_ISO, ActionParamType.Text, ""),
                ActionParam(PARAM_EXPOSURE_NANOS, ActionParamType.Text, ""),
                ActionParam(PARAM_FOCUS_DIOPTER, ActionParamType.Text, ""),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "0", 0f, 30_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_RAW_CAPTURE,
            label = context.getString(R.string.camera_action_raw_capture),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_CAMERA_ID, ActionParamType.Text, DEFAULT_CAMERA_ID),
                ActionParam(PARAM_FRAME_COUNT, ActionParamType.Int, "1", 1f, 50f),
            ),
        ),
        ModuleAction(
            key = ACTION_MULTI_CAMERA_CAPTURE,
            label = context.getString(R.string.camera_action_multi_camera_capture),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_CAMERA_IDS, ActionParamType.Text, "0,1"),
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "5000", 100f, 15_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_HAL_BYPASS_FRAME,
            label = context.getString(R.string.camera_action_hal_bypass_frame),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_SET_SHUTTER_SOUND,
            label = context.getString(R.string.camera_action_set_shutter_sound),
            requiresRoot = true,
            params = listOf(ActionParam(PARAM_ENABLED, ActionParamType.Bool, "true")),
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_CLEAR_SCAN_HISTORY -> {
                scanHistoryRepository.clear()
                ActionResult.Success
            }
            ACTION_HIGH_FPS_CAPTURE -> cameraController.highFpsCapture(
                HighFpsConfig(
                    cameraId = params.textOr(PARAM_CAMERA_ID, DEFAULT_CAMERA_ID),
                    fps = params.intOr(PARAM_FPS, 120),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 5_000),
                ),
            ).toActionResult()
            ACTION_MANUAL_OVERRIDE -> cameraController.manualOverride(
                ManualExposureConfig(
                    cameraId = params.textOr(PARAM_CAMERA_ID, DEFAULT_CAMERA_ID),
                    isoSensitivity = params.intOrNull(PARAM_ISO),
                    exposureTimeNanos = params.longOrNull(PARAM_EXPOSURE_NANOS),
                    focusDistanceDiopter = params.floatOrNull(PARAM_FOCUS_DIOPTER),
                    durationMillis = params.longOr(PARAM_DURATION_MS, 0),
                ),
            ).toActionResult()
            ACTION_RAW_CAPTURE -> cameraController.rawCapture(
                RawCaptureConfig(
                    cameraId = params.textOr(PARAM_CAMERA_ID, DEFAULT_CAMERA_ID),
                    frameCount = params.intOr(PARAM_FRAME_COUNT, 1),
                ),
            ).toActionResult()
            ACTION_MULTI_CAMERA_CAPTURE -> {
                val ids = params[PARAM_CAMERA_IDS]
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.takeIf { it.size >= 2 }
                if (ids != null) {
                    cameraController.multiCameraCapture(
                        MultiCameraConfig(
                            cameraIds = ids,
                            durationMillis = params.longOr(PARAM_DURATION_MS, 5_000),
                        ),
                    ).toActionResult()
                } else {
                    ActionResult.Failure("multi_camera_ids requires at least two comma-separated camera ids")
                }
            }
            ACTION_HAL_BYPASS_FRAME -> cameraController.halBypassFrame().toActionResult()
            ACTION_SET_SHUTTER_SOUND -> cameraController.setShutterSoundEnabled(
                params[PARAM_ENABLED]?.toBooleanStrictOrNull() ?: true,
            ).toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun CameraControllerResult.toActionResult(): ActionResult = when (this) {
        CameraControllerResult.Ok -> ActionResult.Success
        CameraControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        CameraControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is CameraControllerResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        is CameraControllerResult.HardwareError -> ActionResult.Failure(message)
    }

    private fun Map<String, String>.textOr(key: String, fallback: String): String =
        this[key]?.takeIf { it.isNotBlank() } ?: fallback

    private fun Map<String, String>.intOr(key: String, fallback: Int): Int =
        this[key]?.toIntOrNull() ?: fallback

    private fun Map<String, String>.longOr(key: String, fallback: Long): Long =
        this[key]?.toLongOrNull() ?: fallback

    private fun Map<String, String>.intOrNull(key: String): Int? =
        this[key]?.takeIf { it.isNotBlank() }?.toIntOrNull()

    private fun Map<String, String>.longOrNull(key: String): Long? =
        this[key]?.takeIf { it.isNotBlank() }?.toLongOrNull()

    private fun Map<String, String>.floatOrNull(key: String): Float? =
        this[key]?.takeIf { it.isNotBlank() }?.toFloatOrNull()

    companion object {
        const val FEATURE_ID = "camera"
        const val ACTION_CLEAR_SCAN_HISTORY = "clear_scan_history"
        const val ACTION_HIGH_FPS_CAPTURE = "high_fps_capture"
        const val ACTION_MANUAL_OVERRIDE = "manual_override"
        const val ACTION_RAW_CAPTURE = "raw_capture"
        const val ACTION_MULTI_CAMERA_CAPTURE = "multi_camera_capture"
        const val ACTION_HAL_BYPASS_FRAME = "hal_bypass_frame"
        const val ACTION_SET_SHUTTER_SOUND = "set_shutter_sound"
        const val PARAM_CAMERA_ID = "camera_id"
        const val PARAM_CAMERA_IDS = "camera_ids"
        const val PARAM_FPS = "fps"
        const val PARAM_DURATION_MS = "duration_ms"
        const val PARAM_ISO = "iso"
        const val PARAM_EXPOSURE_NANOS = "exposure_nanos"
        const val PARAM_FOCUS_DIOPTER = "focus_diopter"
        const val PARAM_FRAME_COUNT = "frame_count"
        const val PARAM_ENABLED = "enabled"
        const val DEFAULT_CAMERA_ID = "0"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface CameraActionModule {

    @Binds
    @IntoMap
    @StringKey(CameraActionHandler.FEATURE_ID)
    fun bindCameraActionHandler(handler: CameraActionHandler): ActionHandler
}
