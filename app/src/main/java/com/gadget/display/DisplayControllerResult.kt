package com.gadget.display

/**
 * Result returned by every [DisplayController] privileged method. Same
 * shape as the Batch-7 controller result types. Snapshot variants carry
 * the pre-mutation state so the UI can echo "old → new" in the status
 * line.
 */
sealed class DisplayControllerResult {
    data class Ok(val statusNote: String? = null) : DisplayControllerResult()
    data object Unsupported : DisplayControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : DisplayControllerResult()
    data object OptedOut : DisplayControllerResult()
    data class HardwareError(val message: String) : DisplayControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : DisplayControllerResult()

    data class BrightnessSnapshot(
        val originalRaw: Int,
        val appliedRaw: Int,
        val maxBrightness: Int,
    ) : DisplayControllerResult()

    data class RefreshRateSnapshot(
        val originalModeId: Int,
        val appliedModeId: Int,
    ) : DisplayControllerResult()

    data class DensitySnapshot(
        val originalDpi: Int?,
        val appliedDpi: Int,
    ) : DisplayControllerResult()

    data class SurfaceFlingerExcerpt(val excerpt: String) : DisplayControllerResult()
}
