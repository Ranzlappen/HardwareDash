package dev.ranzlappen.gadget.feature.audio.control


/**
 * Result returned by every [AudioRoutingController] privileged method.
 * Same shape as the Batch-7 controller result types.
 */
sealed class AudioRoutingControllerResult {
    data class Ok(val statusNote: String? = null) : AudioRoutingControllerResult()
    data object Unsupported : AudioRoutingControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : AudioRoutingControllerResult()
    data object OptedOut : AudioRoutingControllerResult()
    data class HardwareError(val message: String) : AudioRoutingControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : AudioRoutingControllerResult()

    data class VolumeSnapshot(
        val stream: AudioStreamType,
        val originalIndex: Int,
        val appliedIndex: Int,
        val maxIndex: Int,
    ) : AudioRoutingControllerResult()

    data class RoutingSnapshot(
        val priorTarget: AudioRoutingTarget,
        val appliedTarget: AudioRoutingTarget,
    ) : AudioRoutingControllerResult()

    data class AudioDumpExcerpt(val excerpt: String) : AudioRoutingControllerResult()
}
