package dev.ranzlappen.gadget.feature.radios.nfc.control

/**
 * Result returned by every [NfcController] extreme-tier method. Same
 * shape as the other Batch-6 surface result types.
 */
sealed class NfcControllerResult {
    data class Ok(val statusNote: String? = null) : NfcControllerResult()
    data object Unsupported : NfcControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : NfcControllerResult()
    data object OptedOut : NfcControllerResult()
    data class HardwareError(val message: String) : NfcControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : NfcControllerResult()
    data class NciResponse(val responseHex: String) : NfcControllerResult()
}
