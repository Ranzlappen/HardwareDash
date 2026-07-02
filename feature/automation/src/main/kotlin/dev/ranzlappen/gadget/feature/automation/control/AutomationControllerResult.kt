package dev.ranzlappen.gadget.feature.automation.control


/**
 * Result returned by every [AutomationController] privileged method.
 * Same shape as the Batch-6 controller result types.
 */
sealed class AutomationControllerResult {
    data class Ok(val statusNote: String? = null) : AutomationControllerResult()
    data object Unsupported : AutomationControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : AutomationControllerResult()
    data object OptedOut : AutomationControllerResult()
    data class HardwareError(val message: String) : AutomationControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : AutomationControllerResult()
    data class IntentResult(val exitCode: Int, val tail: String) : AutomationControllerResult()
    data class DumpsysExcerpt(val sections: Map<String, String>) : AutomationControllerResult()
}
