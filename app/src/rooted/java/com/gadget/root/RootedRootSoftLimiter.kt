package com.gadget.root

/**
 * Rooted-flavor [RootSoftLimiter]. Batch 1 placeholder: always grants because
 * the gate above is still denying everything. Batch 2 implements the real
 * rolling-window counter.
 */
class RootedRootSoftLimiter : RootSoftLimiter {
    override suspend fun tryAcquire(
        feature: RootFeatureKey,
        policy: RootLimitPolicy,
    ): RootSoftLimiter.LimitOutcome = RootSoftLimiter.LimitOutcome.Granted
}
