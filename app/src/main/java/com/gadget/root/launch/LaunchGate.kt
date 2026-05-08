package com.gadget.root.launch

/**
 * Hard-fail gate consulted by [com.gadget.MainActivity] before the normal
 * navigation graph is mounted. Standard flavor's no-op implementation always
 * returns [LaunchGateOutcome.Allowed] on the first composition, so it adds
 * zero overhead to the non-rooted launch path.
 */
interface LaunchGate {
    suspend fun check(): LaunchGateOutcome
}
