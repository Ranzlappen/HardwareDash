package com.gadget.gps.spoof

import dev.ranzlappen.gadget.core.root.RootGateDecision

/**
 * Common result shape for every controller method that mutates state.
 * Mirrors the existing `*ControllerResult` patterns in the rooted modules
 * (e.g. `KeepAliveControllerResult`).
 */
sealed interface SpoofResult {

    data object Ok : SpoofResult

    /**
     * The current device + permission state can't fulfil this. [reason] is a
     * short, user-facing string (already localized by the caller via
     * Strings.kt). [helperIntentAction] (when non-null) is an
     * `android.provider.Settings.ACTION_*` constant the UI can deep-link to
     * (e.g. ACTION_APPLICATION_DEVELOPMENT_SETTINGS).
     */
    data class Unsupported(
        val reason: String,
        val helperIntentAction: String? = null,
    ) : SpoofResult

    /** RootSafetyGate refused (off in toggles, rate-limited, etc.). */
    data class Blocked(val decision: RootGateDecision) : SpoofResult

    /** Operational failure (e.g. SecurityException, file too big, parse error). */
    data class Failed(val message: String, val cause: Throwable? = null) : SpoofResult

    /** User has not yet acknowledged the legal disclaimer. UI shows the modal. */
    data object LegalNotAcknowledged : SpoofResult
}
