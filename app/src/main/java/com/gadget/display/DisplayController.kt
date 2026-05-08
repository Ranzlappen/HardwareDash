package com.gadget.display

/**
 * Rooted-only Display tuning surface. Standard flavor returns
 * [DisplayControllerResult.Unsupported] for every method.
 *
 * Privileged paths: direct `/sys/class/leds/lcd-backlight/brightness` writes
 * past the framework cap (clamped to 130 % of `max_brightness`),
 * `cmd display set-display-mode` and `cmd display set-color-mode`
 * (API 30+; guarded), `wm density <dpi>` runtime DPI override (clamped
 * 120–560), and a read-only `dumpsys SurfaceFlinger` snapshot.
 */
interface DisplayController {

    /**
     * Drives the LCD backlight sysfs node directly. The helper reads
     * `max_brightness` and clamps the request to 130 % of that value.
     * Snapshot+restore via the shared mutation log under
     * `sysfs-backlight://<path>`. The 60 s active-window auto-cutoff
     * lives inside the helper.
     */
    suspend fun overrideBrightness(config: BrightnessOverrideConfig): DisplayControllerResult

    /**
     * Snapshots the active display mode via
     * `cmd display get-active-display-mode` and switches to the requested
     * mode id via `cmd display set-display-mode`. API 30+; the helper
     * surfaces [DisplayControllerResult.Unsupported] on lower SDKs.
     */
    suspend fun overrideRefreshRate(config: RefreshRateOverrideConfig): DisplayControllerResult

    /**
     * Runtime DPI override via `wm density <dpi>` clamped to 120–560.
     * Revert is `wm density reset` regardless of stored snapshot.
     */
    suspend fun overrideDensity(config: DensityOverrideConfig): DisplayControllerResult

    /** Read-only `dumpsys SurfaceFlinger` snapshot, tail-capped to 8 KB. */
    suspend fun surfaceFlingerSnapshot(): DisplayControllerResult

    /** Reverts every Display-surface mutation registered with the log. */
    suspend fun resetAllDisplayMutations(): DisplayControllerResult

    /**
     * Auto-revert path called on `TorchScreen` dispose. Filters by
     * `sysfs-backlight://` + `cmd-display://` prefixes.
     */
    suspend fun revertOnScreenExit(): DisplayControllerResult
}
