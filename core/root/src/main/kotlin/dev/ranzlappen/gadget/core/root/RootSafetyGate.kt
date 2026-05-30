package dev.ranzlappen.gadget.core.root

/**
 * Authoritative gatekeeper consulted before any rooted operation runs. Combines
 * three checks: capability availability, the user's per-feature opt-out
 * preference, and a soft rate-limiter window.
 *
 * Callers receive a [RootGateDecision] and render UI accordingly — never a raw
 * boolean — so denial reasons surface to the user instead of silently failing.
 */
interface RootSafetyGate {
    suspend fun check(feature: RootFeatureKey): RootGateDecision

    /**
     * Records that [feature] just executed successfully. Drives the soft
     * limiter's rolling window so subsequent [check] calls can throttle.
     */
    suspend fun recordInvocation(feature: RootFeatureKey)
}

sealed class RootGateDecision {
    data object Allowed : RootGateDecision()
    data object BlockedByUser : RootGateDecision()
    data class BlockedByLimiter(val retryAfterMillis: Long) : RootGateDecision()
    data object Unsupported : RootGateDecision()
}
