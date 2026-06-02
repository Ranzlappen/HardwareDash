package com.gadget.camera

/**
 * Result returned by every [CameraController] extreme-tier method.
 *
 * Same shape as `TorchControllerResult` —
 * a separate type per surface lets each evolve independently and gives
 * compile-time safety against passing a torch result to a camera handler.
 */
sealed class CameraControllerResult {
    data object Ok : CameraControllerResult()
    data object Unsupported : CameraControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : CameraControllerResult()
    data object OptedOut : CameraControllerResult()
    data class HardwareError(val message: String) : CameraControllerResult()
}
