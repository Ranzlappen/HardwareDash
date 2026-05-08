package com.gadget.root

/**
 * Standard-flavor [RootCapabilityRegistry]: every capability is reported as
 * unavailable. The standard APK never executes rooted code paths.
 */
class NoOpRootCapabilityRegistry : RootCapabilityRegistry {
    override val isRootedFlavor: Boolean = false
    override fun hasRootAccess(): Boolean = false
    override fun isFeatureAvailable(feature: RootFeatureKey): Boolean = false
}
