package com.gadget.root

/**
 * Rooted-flavor [RootCapabilityRegistry]. For Batch 1 this is intentionally a
 * pass-through stub that mirrors the standard flavor — the rooted APK
 * compiles, runs, and behaves identically to standard until Batch 2 wires up
 * the real su-probe and per-feature availability table.
 */
class RootedRootCapabilityRegistry : RootCapabilityRegistry {
    override val isRootedFlavor: Boolean = true

    // TODO(batch-2): probe for su via Shell.SU.available() (or libsu) and cache.
    override fun hasRootAccess(): Boolean = false

    // TODO(batch-2): consult RootFeatureDescriptor table once features land.
    override fun isFeatureAvailable(feature: RootFeatureKey): Boolean = false
}
