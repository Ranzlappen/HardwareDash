package dev.ranzlappen.gadget.core.root

import kotlin.time.Duration

/**
 * Per-feature rolling-window invocation limiter. Returns a decision the gate
 * can translate into [RootGateDecision]. Implementation is deferred to a later
 * batch; this batch only fixes the contract.
 */
interface RootSoftLimiter {
    suspend fun tryAcquire(feature: RootFeatureKey, policy: RootLimitPolicy): LimitOutcome

    sealed class LimitOutcome {
        data object Granted : LimitOutcome()
        data class Throttled(val retryAfterMillis: Long) : LimitOutcome()
    }
}

/**
 * Soft cap of [maxInvocations] per [window]. `null` policies on a feature mean
 * "no rate-limit, the user-opt-out is the only gate".
 */
data class RootLimitPolicy(
    val window: Duration,
    val maxInvocations: Int,
)
