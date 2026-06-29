package dev.ranzlappen.gadget.feature.bugreport.rooted

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.core.RootShell
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Force-grants a declared runtime permission to this app via `pm grant`,
 * bypassing the system permission dialog — the rooted one-up over the standard
 * Health screen's runtime request. Every grant clears [RootSafetyGate] first,
 * and the permission token is validated before it reaches the shell.
 */
@Singleton
class RootedPermissionGranter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyGate: RootSafetyGate,
    private val shell: RootShell,
) {
    suspend fun forceGrant(permission: String): PermissionGrantResult {
        if (!PermissionGrantCommands.isValidPermission(permission)) {
            return PermissionGrantResult.InvalidPermission
        }
        return when (val gate = safetyGate.check(RootFeatureKey.PermissionForceGrant)) {
            RootGateDecision.Allowed -> runGrant(permission).also { result ->
                if (result is PermissionGrantResult.Ok) {
                    safetyGate.recordInvocation(RootFeatureKey.PermissionForceGrant)
                }
            }
            RootGateDecision.BlockedByUser -> PermissionGrantResult.OptedOut
            is RootGateDecision.BlockedByLimiter -> PermissionGrantResult.RateLimited(gate.retryAfterMillis)
            RootGateDecision.Unsupported -> PermissionGrantResult.Unsupported
        }
    }

    private suspend fun runGrant(permission: String): PermissionGrantResult {
        val result = shell.exec(PermissionGrantCommands.grant(context.packageName, permission))
        return if (result.isSuccess) {
            PermissionGrantResult.Ok
        } else {
            PermissionGrantResult.Error(result.stderr.firstOrNull().orEmpty().ifBlank { "pm grant failed" })
        }
    }
}
