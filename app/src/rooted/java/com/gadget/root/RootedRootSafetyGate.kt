package com.gadget.root

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor [RootSafetyGate]. Batch 2 placeholder: returns `Unsupported`
 * so no rooted-only feature can yet execute even on the rooted APK. Batch 3
 * replaces this with the real implementation backed by [RootSafetyPreferences]
 * and [RootSoftLimiter] now that the root core layer (detector / shell) exists.
 */
@Singleton
class RootedRootSafetyGate @Inject constructor() : RootSafetyGate {
    override suspend fun check(feature: RootFeatureKey): RootGateDecision =
        RootGateDecision.Unsupported

    override suspend fun recordInvocation(feature: RootFeatureKey) = Unit
}
