package dev.ranzlappen.gadget.feature.standard.root

import dev.ranzlappen.gadget.core.root.*
/**
 * Standard-flavor [RootSoftLimiter]: nothing to limit because no rooted code
 * runs. Always reports the request as granted; the gate has already denied
 * upstream by the time anything else reaches it.
 */
class NoOpRootSoftLimiter : RootSoftLimiter {
    override suspend fun tryAcquire(
        feature: RootFeatureKey,
        policy: RootLimitPolicy,
    ): RootSoftLimiter.LimitOutcome = RootSoftLimiter.LimitOutcome.Granted
}
