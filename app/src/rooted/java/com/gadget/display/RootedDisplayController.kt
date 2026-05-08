package com.gadget.display

import com.gadget.root.RootFeatureKey
import com.gadget.root.RootGateDecision
import com.gadget.root.RootSafetyGate
import com.gadget.root.sysfs.SysfsMutationLog
import javax.inject.Inject
import javax.inject.Singleton

private val DISPLAY_RESET_PREFIXES = listOf("sysfs-backlight://", "cmd-display://")
private val DISPLAY_SCREEN_EXIT_PREFIXES = listOf("sysfs-backlight://", "cmd-display://")

/**
 * Rooted-flavor Display controller. Wires the safety gate to the four
 * display helpers. Auto-revert and reset filter the
 * `sysfs-backlight://` and `cmd-display://` prefixes so screen-exit
 * disposes a brightness override / refresh-rate override / density
 * override even after the per-helper finally has already restored the
 * value (idempotent: the second restore is a no-op).
 */
@Singleton
class RootedDisplayController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val backlight: BacklightSysfsHelper,
    private val displayCmd: DisplayCommandHelper,
    private val density: DensityHelper,
    private val surfaceFlinger: SurfaceFlingerDumpHelper,
    private val mutationLog: SysfsMutationLog,
) : DisplayController {

    override suspend fun overrideBrightness(
        config: BrightnessOverrideConfig,
    ): DisplayControllerResult = runGated(RootFeatureKey.DisplayBrightnessOverride) {
        when (val outcome = backlight.overrideBrightness(config.percent, config.activeWindowMillis)) {
            BrightnessOutcome.Unavailable -> DisplayControllerResult.Unsupported
            BrightnessOutcome.WriteFailed ->
                DisplayControllerResult.HardwareError("backlight sysfs write rejected")
            is BrightnessOutcome.Applied -> DisplayControllerResult.BrightnessSnapshot(
                originalRaw = outcome.originalRaw,
                appliedRaw = outcome.appliedRaw,
                maxBrightness = outcome.maxBrightness,
            )
        }
    }

    override suspend fun overrideRefreshRate(
        config: RefreshRateOverrideConfig,
    ): DisplayControllerResult = runGated(RootFeatureKey.DisplayRefreshRateOverride) {
        if (!displayCmd.isApiSupported()) {
            return@runGated DisplayControllerResult.Unsupported
        }
        when (val outcome = displayCmd.setDisplayMode(config.displayId, config.targetModeId)) {
            SetModeOutcome.Unsupported -> DisplayControllerResult.Unsupported
            is SetModeOutcome.WriteFailed -> DisplayControllerResult.HardwareError(outcome.message)
            is SetModeOutcome.Applied -> DisplayControllerResult.RefreshRateSnapshot(
                originalModeId = outcome.originalModeId,
                appliedModeId = outcome.appliedModeId,
            )
        }
    }

    override suspend fun overrideDensity(
        config: DensityOverrideConfig,
    ): DisplayControllerResult = runGated(RootFeatureKey.DisplayDensityOverride) {
        when (val outcome = density.overrideDensity(config.dpi)) {
            DensityOutcome.WriteFailed ->
                DisplayControllerResult.HardwareError("wm density rejected the write")
            is DensityOutcome.Applied -> DisplayControllerResult.DensitySnapshot(
                originalDpi = outcome.originalDpi,
                appliedDpi = outcome.appliedDpi,
            )
        }
    }

    override suspend fun surfaceFlingerSnapshot(): DisplayControllerResult =
        runGated(RootFeatureKey.DisplaySurfaceFlingerSnapshot) {
            val excerpt = surfaceFlinger.snapshot()
                ?: return@runGated DisplayControllerResult.HardwareError(
                    "dumpsys SurfaceFlinger failed",
                )
            DisplayControllerResult.SurfaceFlingerExcerpt(excerpt)
        }

    override suspend fun resetAllDisplayMutations(): DisplayControllerResult {
        density.resetDensity()
        val outcome = mutationLog.revertAll(DISPLAY_RESET_PREFIXES)
        return DisplayControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertOnScreenExit(): DisplayControllerResult {
        density.resetDensity()
        val outcome = mutationLog.revertAll(DISPLAY_SCREEN_EXIT_PREFIXES)
        return DisplayControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> DisplayControllerResult,
    ): DisplayControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is DisplayControllerResult.Ok ||
                it is DisplayControllerResult.BrightnessSnapshot ||
                it is DisplayControllerResult.RefreshRateSnapshot ||
                it is DisplayControllerResult.DensitySnapshot ||
                it is DisplayControllerResult.SurfaceFlingerExcerpt
            ) {
                safetyGate.recordInvocation(feature)
            }
        }
        RootGateDecision.BlockedByUser -> DisplayControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            DisplayControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> DisplayControllerResult.Unsupported
    }
}
