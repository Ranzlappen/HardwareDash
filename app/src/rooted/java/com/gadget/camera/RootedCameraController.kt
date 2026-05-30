package com.gadget.camera

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor Camera controller. Each public method routes through
 * [RootSafetyGate] (capability + per-feature opt-out + rolling-window
 * limiter) before delegating to a dedicated `class` helper for the
 * actual privileged work. Hard cutoffs (FPS duration, multi-camera
 * duration, RAW frame count, etc.) are enforced inside the helpers.
 */
@Singleton
class RootedCameraController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val camera2: Camera2OverrideHelper,
    private val rawSession: RawCaptureSession,
    private val multiCamera: MultiCameraOrchestrator,
    private val halBypass: HalBypassChannel,
    private val sysfsOverrides: CameraSysfsOverrides,
) : CameraController {

    override suspend fun highFpsCapture(config: HighFpsConfig): CameraControllerResult =
        runGated(RootFeatureKey.CameraHighFps) { camera2.runHighFps(config) }

    override suspend fun manualOverride(config: ManualExposureConfig): CameraControllerResult =
        runGated(RootFeatureKey.CameraManualOverride) { camera2.runManualOverride(config) }

    override suspend fun rawCapture(config: RawCaptureConfig): CameraControllerResult =
        runGated(RootFeatureKey.CameraRawCapture) { rawSession.capture(config) }

    override suspend fun multiCameraCapture(config: MultiCameraConfig): CameraControllerResult =
        runGated(RootFeatureKey.CameraMultiSimultaneous) { multiCamera.capture(config) }

    override suspend fun halBypassFrame(): CameraControllerResult =
        runGated(RootFeatureKey.CameraHalBypass) { halBypass.probeAndCapture() }

    override suspend fun setShutterSoundEnabled(enabled: Boolean): CameraControllerResult =
        runGated(RootFeatureKey.CameraShutterSoundOverride) {
            sysfsOverrides.setShutterSoundEnabled(enabled)
        }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> CameraControllerResult,
    ): CameraControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is CameraControllerResult.Ok) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> CameraControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            CameraControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> CameraControllerResult.Unsupported
    }
}
