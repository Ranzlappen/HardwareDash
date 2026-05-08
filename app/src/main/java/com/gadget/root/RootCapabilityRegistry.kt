package com.gadget.root

/**
 * Single source of truth for "is the rooted code path available on this build
 * and this device?". Implementations are flavor-scoped: the standard flavor
 * always reports unsupported; the rooted flavor will, in a later batch,
 * actually probe for su.
 *
 * Feature code MUST NOT branch on `BuildConfig.IS_ROOTED` — it must consult
 * this interface (and [RootSafetyGate]) so flavor differences flow through a
 * single Hilt-bound implementation.
 */
interface RootCapabilityRegistry {
    /** Whether this build was compiled as the rooted flavor. */
    val isRootedFlavor: Boolean

    /** Whether the runtime environment actually grants root access. */
    fun hasRootAccess(): Boolean

    /** Whether [feature] has a real (non-stub) implementation in this build. */
    fun isFeatureAvailable(feature: RootFeatureKey): Boolean
}
