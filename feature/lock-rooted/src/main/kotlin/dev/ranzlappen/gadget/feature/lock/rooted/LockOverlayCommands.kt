package dev.ranzlappen.gadget.feature.lock.rooted

/**
 * Pure helpers for the rooted secure-overlay flow — the privileged shell
 * command and the duration clamp. Kept Android-free so the genuinely
 * fallible bits (command shape, bounds) round-trip in a plain JVM test.
 */
object LockOverlayCommands {

    /** Shortest overlay that is still perceivable. */
    const val MIN_DURATION_MILLIS = 1_000L

    /**
     * Absolute ceiling on how long the overlay can sit above the keyguard,
     * matching the legacy helper — a cancelled coroutine still tears it down
     * via the `NonCancellable` finally, but the ceiling bounds the happy path.
     */
    const val HARD_CEILING_MILLIS = 60_000L

    /** Default duration when an automation rule omits the parameter. */
    const val DEFAULT_DURATION_MILLIS = 3_000L

    /**
     * Self-grant `SYSTEM_ALERT_WINDOW` to [packageName] via root appops so the
     * overlay works even when the user never toggled the Settings permission —
     * the genuinely root-only step in the flow.
     */
    fun grantOverlayPermission(packageName: String): String =
        "appops set $packageName SYSTEM_ALERT_WINDOW allow"

    /** Clamp a requested duration into the perceivable / safe window. */
    fun clampDuration(requestedMillis: Long): Long =
        requestedMillis.coerceIn(MIN_DURATION_MILLIS, HARD_CEILING_MILLIS)
}
