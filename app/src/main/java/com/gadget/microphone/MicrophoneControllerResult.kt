package com.gadget.microphone

/**
 * Result returned by every [MicrophoneController] extreme-tier method.
 * Same shape as `CameraControllerResult` / `TorchControllerResult`.
 */
sealed class MicrophoneControllerResult {
    data object Ok : MicrophoneControllerResult()
    data object Unsupported : MicrophoneControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : MicrophoneControllerResult()
    data object OptedOut : MicrophoneControllerResult()
    data class HardwareError(val message: String) : MicrophoneControllerResult()
}
