package dev.ranzlappen.gadget.feature.standard.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.core.root.core.RootDetection

/**
 * Standard-flavor [RootCapabilityRegistry]: every capability is reported as
 * unavailable. The standard APK never executes rooted code paths.
 */
class NoOpRootCapabilityRegistry : RootCapabilityRegistry {
    override val isRootedFlavor: Boolean = false
    override suspend fun probe(): RootDetection = RootDetection.None
    override fun hasRootAccess(): Boolean = false
    override fun isFeatureAvailable(feature: RootFeatureKey): Boolean = false
}
