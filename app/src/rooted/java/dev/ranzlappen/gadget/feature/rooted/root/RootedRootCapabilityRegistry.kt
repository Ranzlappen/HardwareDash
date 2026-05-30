package dev.ranzlappen.gadget.feature.rooted.root

import dev.ranzlappen.gadget.core.root.*
import dev.ranzlappen.gadget.core.root.core.RootDetection
import dev.ranzlappen.gadget.core.root.core.RootDetector
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor [RootCapabilityRegistry]. Delegates to the libsu-backed
 * [RootDetector] and caches the result. The non-suspend [hasRootAccess]
 * reads from the cached detection so it's safe to call from any thread —
 * but it returns false until [probe] has resolved at least once
 * (LaunchGate is responsible for that single startup call).
 */
@Singleton
class RootedRootCapabilityRegistry @Inject constructor(
    private val detector: RootDetector,
) : RootCapabilityRegistry {

    @Volatile private var cached: RootDetection = RootDetection.None

    override val isRootedFlavor: Boolean = true

    override suspend fun probe(): RootDetection {
        val detection = detector.detect()
        cached = detection
        return detection
    }

    override fun hasRootAccess(): Boolean = cached is RootDetection.Available

    // TODO(batch-3): consult RootFeatureDescriptor table once features land.
    override fun isFeatureAvailable(feature: RootFeatureKey): Boolean =
        cached is RootDetection.Available
}
