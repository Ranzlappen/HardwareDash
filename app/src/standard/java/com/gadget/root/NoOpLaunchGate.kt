package com.gadget.root

import dev.ranzlappen.gadget.core.root.launch.LaunchGate
import dev.ranzlappen.gadget.core.root.launch.LaunchGateOutcome

/**
 * Standard-flavor [LaunchGate]: always allows the launch instantly. The
 * suspend signature returns without yielding so the standard APK's startup
 * path is behaviourally identical to pre-Batch-2.
 */
class NoOpLaunchGate : LaunchGate {
    override suspend fun check(): LaunchGateOutcome = LaunchGateOutcome.Allowed
}
