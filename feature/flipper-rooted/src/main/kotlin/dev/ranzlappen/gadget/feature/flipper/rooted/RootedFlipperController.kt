package dev.ranzlappen.gadget.feature.flipper.rooted

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.feature.flipper.transport.FlipperUsbLink
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Root-grants USB access to an attached Flipper Zero by relaxing its device
 * node's permissions (`chmod 666 /dev/bus/usb/BBB/DDD`), so the standard
 * [FlipperUsbLink.open] path opens the port without the per-attach permission
 * dialog. Every grant clears [RootSafetyGate] first.
 */
@Singleton
class RootedFlipperController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
) {
    suspend fun grantUsbAccess(): FlipperRootResult =
        when (val gate = safetyGate.check(RootFeatureKey.FlipperUsbGrant)) {
            RootGateDecision.Allowed -> relaxAttachedNodes().also { result ->
                if (result is FlipperRootResult.Ok) {
                    safetyGate.recordInvocation(RootFeatureKey.FlipperUsbGrant)
                }
            }
            RootGateDecision.BlockedByUser -> FlipperRootResult.OptedOut
            is RootGateDecision.BlockedByLimiter -> FlipperRootResult.RateLimited(gate.retryAfterMillis)
            RootGateDecision.Unsupported -> FlipperRootResult.Unsupported
        }

    private suspend fun relaxAttachedNodes(): FlipperRootResult {
        val nodes = FlipperUsbLink.listDevices(context).map { it.deviceName }
        if (nodes.isEmpty()) return FlipperRootResult.NoDevice

        var anyOk = false
        var lastError: String? = null
        for (node in nodes) {
            val result = shell.exec(FlipperRootCommands.relaxUsbNode(node))
            if (result.isSuccess) anyOk = true else lastError = result.stderr.firstOrNull()
        }
        return when {
            anyOk -> FlipperRootResult.Ok
            else -> FlipperRootResult.Error(lastError ?: "chmod failed")
        }
    }
}
