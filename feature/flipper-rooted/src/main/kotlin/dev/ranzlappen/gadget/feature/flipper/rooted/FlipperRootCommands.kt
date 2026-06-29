package dev.ranzlappen.gadget.feature.flipper.rooted

/**
 * Pure helpers for the rooted Flipper flow. Kept Android-free so the command
 * shape round-trips in a plain JVM test.
 */
object FlipperRootCommands {

    /**
     * Relax a USB device node so the app can open it without the framework
     * permission dialog. [nodePath] is the `UsbDevice.getDeviceName()` path,
     * e.g. `/dev/bus/usb/001/002`.
     */
    fun relaxUsbNode(nodePath: String): String = "chmod 666 $nodePath"
}

/** Outcome of a rooted Flipper USB-grant request. */
sealed interface FlipperRootResult {
    /** At least one attached Flipper's node was relaxed. */
    data object Ok : FlipperRootResult

    /** No Flipper is currently attached over USB. */
    data object NoDevice : FlipperRootResult

    /** The user has disabled this rooted feature in safety preferences. */
    data object OptedOut : FlipperRootResult

    /** The soft limiter rejected the call; retry after [retryAfterMillis]. */
    data class RateLimited(val retryAfterMillis: Long) : FlipperRootResult

    /** No rooted capability on this build. */
    data object Unsupported : FlipperRootResult

    /** The chmod failed on every attached node. */
    data class Error(val reason: String) : FlipperRootResult
}
