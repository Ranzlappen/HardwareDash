package dev.ranzlappen.gadget.feature.standard.root

import dev.ranzlappen.gadget.core.root.*
/**
 * Standard-flavor [RootSafetyGate]: every check denies as `Unsupported`.
 * Recording invocations is a no-op.
 */
class NoOpRootSafetyGate : RootSafetyGate {
    override suspend fun check(feature: RootFeatureKey): RootGateDecision =
        RootGateDecision.Unsupported

    override suspend fun recordInvocation(feature: RootFeatureKey) = Unit
}
