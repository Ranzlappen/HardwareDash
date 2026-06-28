package dev.ranzlappen.gadget.feature.lock.rooted

/**
 * Outcome of a rooted secure-overlay request. Mirrors the result shapes the
 * other rooted controllers use (`TorchSysfsControllerResult`) so the gate
 * branches map cleanly onto an automation `ActionResult`.
 */
sealed interface LockOverlayResult {
    /** The overlay was shown for its full bounded duration and torn down. */
    data object Ok : LockOverlayResult

    /** The user has disabled this rooted feature in safety preferences. */
    data object OptedOut : LockOverlayResult

    /** The soft limiter rejected the call; retry after [retryAfterMillis]. */
    data class RateLimited(val retryAfterMillis: Long) : LockOverlayResult

    /** No rooted capability / no WindowManager on this build. */
    data object Unsupported : LockOverlayResult

    /** The window manager refused the overlay (e.g. permission still denied). */
    data class Error(val reason: String) : LockOverlayResult
}
