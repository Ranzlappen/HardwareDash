package com.gadget.notification

/**
 * Result returned by every [NotificationController] privileged method.
 */
sealed class NotificationControllerResult {
    data class Ok(val statusNote: String? = null) : NotificationControllerResult()
    data object Unsupported : NotificationControllerResult()
    data class RateLimited(val retryAfterMillis: Long) : NotificationControllerResult()
    data object OptedOut : NotificationControllerResult()
    data class HardwareError(val message: String) : NotificationControllerResult()
    data class ResetCompleted(val restored: Int, val failed: Int) : NotificationControllerResult()
    data class ChannelImportanceSnapshot(
        val channelId: String,
        val previousImportance: Int,
        val newImportance: Int,
    ) : NotificationControllerResult()
}
