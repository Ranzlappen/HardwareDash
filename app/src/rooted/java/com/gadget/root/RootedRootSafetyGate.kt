package com.gadget.root

/**
 * Rooted-flavor [RootSafetyGate]. Batch 1 placeholder: returns `Unsupported`
 * so no rooted-only code path can yet execute even on the rooted APK. Batch 2
 * replaces this with the real implementation backed by [RootSafetyPreferences]
 * and [RootSoftLimiter].
 */
class RootedRootSafetyGate : RootSafetyGate {
    override suspend fun check(feature: RootFeatureKey): RootGateDecision =
        RootGateDecision.Unsupported

    override suspend fun recordInvocation(feature: RootFeatureKey) = Unit
}
