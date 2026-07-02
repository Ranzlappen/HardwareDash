package dev.ranzlappen.gadget.feature.radios.ir.control

/**
 * Result returned by every [IrController] extreme-tier method.
 */
sealed class IrControllerResult {
    data class Ok(val statusNote: String? = null) : IrControllerResult()
    data object Unsupported : IrControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : IrControllerResult()
    data object OptedOut : IrControllerResult()
    data class HardwareError(val message: String) : IrControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : IrControllerResult()
}
