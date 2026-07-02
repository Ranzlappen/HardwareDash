package dev.ranzlappen.gadget.feature.adbdebug.rooted.control

import dev.ranzlappen.gadget.core.root.RootFeatureKey
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.core.root.RootSafetyGate
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingController
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbDebuggingControllerResult
import dev.ranzlappen.gadget.feature.adbdebug.control.AdbNetworkConfig
import dev.ranzlappen.gadget.feature.adbdebug.control.SetPropConfig
import javax.inject.Inject
import javax.inject.Singleton

private val ADB_RESET_PREFIXES = listOf(
    "adb-toggle://",
    "setprop://",
)
private val ADB_SCREEN_EXIT_PREFIXES = listOf(
    "adb-toggle://",
    "setprop://",
)

/**
 * Rooted-flavor ADB Debugging controller. Wires the safety gate to the
 * four ADB helpers. Auto-revert on screen exit filters `adb-toggle://`
 * and `setprop://` so navigating away while a toggle is flipped puts the
 * device back into a known state.
 */
@Singleton
class RootedAdbDebuggingController @Inject constructor(
    private val safetyGate: RootSafetyGate,
    private val adbSettingsHelper: AdbSettingsHelper,
    private val adbNetworkHelper: AdbNetworkHelper,
    private val propDumpHelper: PropDumpHelper,
    private val setPropHelper: SetPropHelper,
    private val mutationLog: SysfsMutationLog,
) : AdbDebuggingController {

    override suspend fun toggleAdbEnabled(enabled: Boolean): AdbDebuggingControllerResult =
        runGated(RootFeatureKey.AdbToggleEnabled) { adbSettingsHelper.setEnabled(enabled) }

    override suspend fun toggleAdbOverNetwork(
        config: AdbNetworkConfig,
    ): AdbDebuggingControllerResult =
        runGated(RootFeatureKey.AdbOverNetwork) { adbNetworkHelper.apply(config) }

    override suspend fun dumpProperties(persist: Boolean): AdbDebuggingControllerResult =
        runGated(RootFeatureKey.AdbDumpProperties) {
            val excerpt = propDumpHelper.snapshot()
                ?: return@runGated AdbDebuggingControllerResult.HardwareError("getprop failed")
            val persistedFile = if (persist) {
                propDumpHelper.persistToLogbook(excerpt)?.absolutePath
            } else {
                null
            }
            AdbDebuggingControllerResult.PropertyDump(
                excerpt = excerpt,
                persistedFile = persistedFile,
            )
        }

    override suspend fun overrideSystemProperty(
        config: SetPropConfig,
    ): AdbDebuggingControllerResult =
        runGated(RootFeatureKey.AdbSetpropOverride) { setPropHelper.apply(config) }

    override suspend fun resetAllAdbMutations(): AdbDebuggingControllerResult {
        val outcome = mutationLog.revertAll(ADB_RESET_PREFIXES)
        return AdbDebuggingControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    override suspend fun revertOnScreenExit(): AdbDebuggingControllerResult {
        val outcome = mutationLog.revertAll(ADB_SCREEN_EXIT_PREFIXES)
        return AdbDebuggingControllerResult.ResetCompleted(
            restored = outcome.restored,
            failed = outcome.failed,
        )
    }

    private suspend inline fun runGated(
        feature: RootFeatureKey,
        crossinline block: suspend () -> AdbDebuggingControllerResult,
    ): AdbDebuggingControllerResult = when (val gate = safetyGate.check(feature)) {
        RootGateDecision.Allowed -> block().also {
            if (it !is AdbDebuggingControllerResult.OptedOut &&
                it !is AdbDebuggingControllerResult.Unsupported &&
                it !is AdbDebuggingControllerResult.RateLimited &&
                it !is AdbDebuggingControllerResult.HardwareError
            ) {
                safetyGate.recordInvocation(feature)
            }
        }
        RootGateDecision.BlockedByUser -> AdbDebuggingControllerResult.OptedOut
        is RootGateDecision.BlockedByLimiter ->
            AdbDebuggingControllerResult.RateLimited(gate.retryAfterMillis)
        RootGateDecision.Unsupported -> AdbDebuggingControllerResult.Unsupported
    }
}
