package dev.ranzlappen.gadget.feature.radios.cell.control

/**
 * Result returned by every [CellController] extreme-tier method. All
 * Batch-6 cellular methods are read-only.
 */
sealed class CellControllerResult {
    data class Ok(val statusNote: String? = null) : CellControllerResult()
    data object Unsupported : CellControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : CellControllerResult()
    data object OptedOut : CellControllerResult()
    data class HardwareError(val message: String) : CellControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : CellControllerResult()
    data class ModemDump(val nodes: Map<String, String>) : CellControllerResult()
    data class SignalDeepDump(val perBand: Map<String, String>) : CellControllerResult()
}
