package dev.ranzlappen.gadget.core.root

/**
 * Static metadata about a rooted feature. The rooted flavor's registry will
 * expose one of these per [RootFeatureKey]; the standard flavor returns null.
 *
 * [isWriteCapable] flags features that mutate sysfs / driver / system state.
 * `RootSafetyGate` short-circuits write-capable features when the user has
 * enabled "Rooted Monitor Safety Mode" — read-only diagnostics stay live.
 * Default `false` so legacy descriptors (and any new read-only features)
 * keep working without a per-call audit.
 */
data class RootFeatureDescriptor(
    val key: RootFeatureKey,
    val defaultOn: Boolean,
    val limit: RootLimitPolicy?,
    val requiresExplicitConfirm: Boolean,
    val isWriteCapable: Boolean = false,
)
