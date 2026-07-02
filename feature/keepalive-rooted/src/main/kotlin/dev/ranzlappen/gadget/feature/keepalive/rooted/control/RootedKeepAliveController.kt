package dev.ranzlappen.gadget.feature.keepalive.rooted.control

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.keepalive.PersistentKeepAliveService
import dev.ranzlappen.gadget.feature.keepalive.control.KeepAliveController
import dev.ranzlappen.gadget.feature.keepalive.control.KeepAliveControllerResult
import dev.ranzlappen.gadget.feature.keepalive.control.PmGrantConfig
import dev.ranzlappen.gadget.feature.keepalive.control.PmGrantVerb
import javax.inject.Inject
import javax.inject.Singleton

private val KEEP_ALIVE_RESET_PREFIXES = listOf(
    "cmd-deviceidle://",
    "pm-grant://",
)

private val DEFAULT_KEEP_ALIVE_GRANTS = listOf(
    "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    "android.permission.WAKE_LOCK",
    "android.permission.FOREGROUND_SERVICE",
    "android.permission.FOREGROUND_SERVICE_DATA_SYNC",
    "android.permission.RECEIVE_BOOT_COMPLETED",
    "android.permission.POST_NOTIFICATIONS",
)

/**
 * Rooted-flavor keep-alive. [enable] starts the foreground service AND
 * issues `cmd deviceidle whitelist +<own-pkg>` and `pm grant` of the
 * default normal-permission allow-list — all gated by their own
 * RootSafetyGate descriptors.
 */
@Singleton
class RootedKeepAliveController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val safetyGate: RootSafetyGate,
    private val dozeHelper: DozeBypassHelper,
    private val pmHelper: PmGrantHelper,
    private val mutationLog: SysfsMutationLog,
) : KeepAliveController {

    override suspend fun enable(): KeepAliveControllerResult {
        ContextCompat.startForegroundService(
            context,
            Intent(context, PersistentKeepAliveService::class.java),
        )
        val pkg = context.packageName
        val dozeOutcome = runGated(RootFeatureKey.KeepAliveDozeBypass) { dozeHelper.whitelist(pkg) }
        val grantOutcome = runGated(RootFeatureKey.KeepAlivePmGrant) {
            pmHelper.grant(
                pkg,
                PmGrantConfig(grantOrRevoke = PmGrantVerb.GRANT, permissions = DEFAULT_KEEP_ALIVE_GRANTS),
            )
        }
        return when {
            dozeOutcome is KeepAliveControllerResult.Ok &&
                grantOutcome is KeepAliveControllerResult.Ok ->
                KeepAliveControllerResult.Ok(statusNote = "Service + Doze + grants applied")
            dozeOutcome is KeepAliveControllerResult.OptedOut ||
                grantOutcome is KeepAliveControllerResult.OptedOut ->
                KeepAliveControllerResult.OptedOut
            dozeOutcome is KeepAliveControllerResult.RateLimited ->
                dozeOutcome
            grantOutcome is KeepAliveControllerResult.RateLimited ->
                grantOutcome
            else ->
                KeepAliveControllerResult.Ok(
                    statusNote = "Service started; some privileged steps did not complete",
                )
        }
    }

    override suspend fun disable(): KeepAliveControllerResult {
        context.startService(
            Intent(context, PersistentKeepAliveService::class.java)
                .setAction(PersistentKeepAliveService.ACTION_STOP),
        )
        val outcome = mutationLog.revertAll(KEEP_ALIVE_RESET_PREFIXES)
        return KeepAliveControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun disableAndStopService(): KeepAliveControllerResult = disable()

    override suspend fun requestUserBatteryOptExemption(): KeepAliveControllerResult =
        KeepAliveControllerResult.Ok(statusNote = "Rooted: handled via cmd deviceidle whitelist")

    override suspend fun resetAllKeepAliveMutations(): KeepAliveControllerResult = disable()

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> KeepAliveControllerResult,
    ): KeepAliveControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it is KeepAliveControllerResult.Ok) safetyGate.recordInvocation(feature)
        }
        RootGateDecision.BlockedByUser -> KeepAliveControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            KeepAliveControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> KeepAliveControllerResult.Unsupported
    }
}
