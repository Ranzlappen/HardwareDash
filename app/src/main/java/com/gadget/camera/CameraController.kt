package com.gadget.camera

/**
 * Rooted-only Camera capability surface. The standard-flavor implementation
 * always returns [CameraControllerResult.Unsupported] so shared UI uses one
 * code path for both flavors.
 *
 * Every method routes through `com.gadget.root.RootSafetyGate` before doing
 * anything privileged. Hard cutoffs (FPS duration, multi-camera duration,
 * RAW frame count, etc.) are enforced inside the impl and cannot be
 * extended by callers.
 *
 * The interface deliberately exposes ONLY extreme-tier operations. Baseline
 * camera capture continues to flow through CameraX in `CameraScreen` —
 * this controller is for the rooted "Root extras" surface only.
 */
interface CameraController {

    /**
     * Captures at [HighFpsConfig.fps] for [HighFpsConfig.durationMillis] on
     * the named camera. The impl bypasses CameraX's framework-imposed FPS
     * clamps via direct Camera2; some Qualcomm sensors additionally accept
     * vendor sysfs writes for above-spec FPS but those are best-effort.
     * Hard 30-second per-call ceiling.
     */
    suspend fun highFpsCapture(config: HighFpsConfig): CameraControllerResult

    /**
     * Drives Camera2 with `CONTROL_AE_MODE = OFF` and the manual values in
     * [config]. Useful for astro / long-exposure / fixed-ISO surveillance.
     * Hard caps: 30 s shutter, ISO clamped to silicon max.
     */
    suspend fun manualOverride(config: ManualExposureConfig): CameraControllerResult

    /**
     * Captures [RawCaptureConfig.frameCount] DNG frames. On vendor-locked
     * devices the impl attempts a `setprop` whitelist before retrying.
     * Auto-stops if free disk drops below ~500 MB.
     */
    suspend fun rawCapture(config: RawCaptureConfig): CameraControllerResult

    /**
     * Opens 2–3 [CameraDevice]s concurrently and binds an `ImageReader` per
     * device. Returns [CameraControllerResult.Unsupported] if the HAL can't
     * sustain concurrent open. Hard 15 s per-session ceiling.
     */
    suspend fun multiCameraCapture(config: MultiCameraConfig): CameraControllerResult

    /**
     * Best-effort raw v4l2 frame access via `/dev/video*`. Most modern
     * Android devices route through HAL3 and don't expose useful v4l2 — in
     * those cases the impl returns [CameraControllerResult.Unsupported].
     * Aborts immediately if the privacy LED is queryable and not asserted.
     */
    suspend fun halBypassFrame(): CameraControllerResult

    /**
     * Toggles the system shutter sound via `setprop`. Persisted as a
     * setting until the user re-toggles. Mandatory legal-warning confirm
     * is enforced via `requiresExplicitConfirm` on the descriptor —
     * shutter-sound is legally required in some jurisdictions (Japan,
     * Korea, parts of EU). The privacy LED is **never** touched.
     */
    suspend fun setShutterSoundEnabled(enabled: Boolean): CameraControllerResult
}
