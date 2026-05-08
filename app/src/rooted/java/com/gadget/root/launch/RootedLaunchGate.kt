package com.gadget.root.launch

import com.gadget.root.RootCapabilityRegistry
import com.gadget.root.core.RootDetection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor launch gate. Routes the first launch through
 * [RootCapabilityRegistry.probe] (which itself drives the libsu detector)
 * and translates the result into a [LaunchGateOutcome] that
 * [com.gadget.MainActivity] uses to decide between the normal nav graph and
 * [com.gadget.root.ui.FatalLaunchScreen].
 */
@Singleton
class RootedLaunchGate @Inject constructor(
    private val registry: RootCapabilityRegistry,
) : LaunchGate {

    override suspend fun check(): LaunchGateOutcome =
        when (val detection = registry.probe()) {
            RootDetection.None -> LaunchGateOutcome.DeniedFatal(FatalReason.NoRootDetected)
            is RootDetection.AvailableButDenied ->
                LaunchGateOutcome.DeniedFatal(
                    FatalReason.RootRequestDenied(detection.provider.displayName),
                )
            is RootDetection.Available -> LaunchGateOutcome.Allowed
        }
}
