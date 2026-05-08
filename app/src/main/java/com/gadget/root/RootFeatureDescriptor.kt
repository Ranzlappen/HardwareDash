package com.gadget.root

/**
 * Static metadata about a rooted feature. The rooted flavor's registry will
 * expose one of these per [RootFeatureKey]; the standard flavor returns null.
 */
data class RootFeatureDescriptor(
    val key: RootFeatureKey,
    val defaultOn: Boolean,
    val limit: RootLimitPolicy?,
    val requiresExplicitConfirm: Boolean,
)
