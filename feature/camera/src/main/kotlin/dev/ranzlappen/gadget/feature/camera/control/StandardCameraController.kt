package dev.ranzlappen.gadget.feature.camera.control

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor Camera controller. Every extreme-tier method returns
 * [CameraControllerResult.Unsupported] — the standard APK has no privileged
 * shell and no Camera HAL bypass surface, so there is no way to drive
 * Camera2 outside the framework's clamps regardless of permissions.
 */
@Singleton
class StandardCameraController @Inject constructor() : CameraController {

    override suspend fun highFpsCapture(config: HighFpsConfig): CameraControllerResult =
        CameraControllerResult.Unsupported

    override suspend fun manualOverride(config: ManualExposureConfig): CameraControllerResult =
        CameraControllerResult.Unsupported

    override suspend fun rawCapture(config: RawCaptureConfig): CameraControllerResult =
        CameraControllerResult.Unsupported

    override suspend fun multiCameraCapture(config: MultiCameraConfig): CameraControllerResult =
        CameraControllerResult.Unsupported

    override suspend fun halBypassFrame(): CameraControllerResult =
        CameraControllerResult.Unsupported

    override suspend fun setShutterSoundEnabled(enabled: Boolean): CameraControllerResult =
        CameraControllerResult.Unsupported
}
