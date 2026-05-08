package com.gadget.root

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor [RootSoftLimiter]. Placeholder: always grants because the
 * gate above is still denying everything in Batch 2. Batch 3 implements the
 * real rolling-window counter.
 */
@Singleton
class RootedRootSoftLimiter @Inject constructor() : RootSoftLimiter {
    override suspend fun tryAcquire(
        feature: RootFeatureKey,
        policy: RootLimitPolicy,
    ): RootSoftLimiter.LimitOutcome = RootSoftLimiter.LimitOutcome.Granted
}
