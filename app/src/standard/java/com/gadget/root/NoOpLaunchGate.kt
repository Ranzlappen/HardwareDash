package com.gadget.root

import com.gadget.root.launch.LaunchGate
import com.gadget.root.launch.LaunchGateOutcome

/**
 * Standard-flavor [LaunchGate]: always allows the launch instantly. The
 * suspend signature returns without yielding so the standard APK's startup
 * path is behaviourally identical to pre-Batch-2.
 */
class NoOpLaunchGate : LaunchGate {
    override suspend fun check(): LaunchGateOutcome = LaunchGateOutcome.Allowed
}
