package dev.ranzlappen.gadget.feature.notification.control


/**
 * Override the importance of an existing notification channel. The
 * impl ONLY ever raises importance (LOW → HIGH); never silences.
 */
data class StickyOverrideConfig(
    val channelId: String,
)

/**
 * Show a `TYPE_SYSTEM_ALERT` overlay above the keyguard for a bounded
 * duration. Hard 60 s active-window ceiling enforced inside the helper.
 */
data class LockScreenOverlayConfig(
    val message: String,
    val durationMillis: Long,
)
