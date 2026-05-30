package dev.ranzlappen.gadget.core.root

/**
 * Audit log entry emitted whenever the gate makes a decision. Surfaced through
 * Timber today; a later batch adds an in-app audit panel that reads from the
 * same stream.
 */
sealed class RootSafetyEvent {
    abstract val feature: RootFeatureKey
    abstract val timestampMillis: Long

    data class Granted(
        override val feature: RootFeatureKey,
        override val timestampMillis: Long,
    ) : RootSafetyEvent()

    data class Denied(
        override val feature: RootFeatureKey,
        override val timestampMillis: Long,
        val reason: RootGateDecision,
    ) : RootSafetyEvent()

    data class LimitTripped(
        override val feature: RootFeatureKey,
        override val timestampMillis: Long,
        val retryAfterMillis: Long,
    ) : RootSafetyEvent()
}
