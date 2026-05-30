package dev.ranzlappen.gadget.core.root.core

/**
 * Identifies which root-management framework is active on the device. The
 * rooted-flavor detector probes for each provider in priority order; the
 * first match wins. `Unknown` covers exotic / legacy `su` setups that don't
 * announce themselves through any of the well-known markers.
 */
sealed class RootProvider(val displayName: String) {
    data object Magisk : RootProvider("Magisk")
    data object KernelSu : RootProvider("KernelSU")
    data object APatch : RootProvider("APatch")
    data object Unknown : RootProvider("Unknown")
}
