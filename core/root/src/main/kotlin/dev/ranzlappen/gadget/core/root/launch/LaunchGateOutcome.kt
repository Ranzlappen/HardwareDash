package dev.ranzlappen.gadget.core.root.launch

/**
 * Decision returned by [LaunchGate.check]. The standard flavor always returns
 * [Allowed]; the rooted flavor returns [DeniedFatal] when no usable root
 * manager is present, which causes [dev.ranzlappen.gadget.root.ui.FatalLaunchScreen] to
 * replace the normal navigation graph for the lifetime of the process.
 */
sealed class LaunchGateOutcome {
    data object Allowed : LaunchGateOutcome()
    data class DeniedFatal(val reason: FatalReason) : LaunchGateOutcome()
}

sealed class FatalReason {
    data object NoRootDetected : FatalReason()
    data class RootRequestDenied(val providerName: String) : FatalReason()
    data class IncompatibleProvider(val providerName: String) : FatalReason()
}
