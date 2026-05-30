package dev.ranzlappen.gadget.feature.rooted.root.emergency

import dev.ranzlappen.gadget.core.root.emergency.*
import android.content.Context
import com.gadget.keepalive.KeepAliveController
import com.gadget.keepalive.KeepAliveControllerResult
import dev.ranzlappen.gadget.core.root.RootFeatureToggles
import dev.ranzlappen.gadget.core.root.core.RootShell
import dev.ranzlappen.gadget.core.root.sysfs.SysfsMutationLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val KEEP_ALIVE_PREFS = "gadget_keep_alive"
private const val KEY_KEEP_ALIVE_ENABLED = "keep_alive_enabled"

/**
 * Rooted-flavor global emergency reset. Bypasses [dev.ranzlappen.gadget.core.root.RootSafetyGate]
 * intentionally — the gate check itself depends on state this method is
 * meant to repair, so gating the reset would be a footgun.
 *
 * Every step is wrapped in a try/catch so a single broken step doesn't
 * abort the rest. The aggregated [EmergencyResetCoordinatorResult.Ok]
 * surfaces partial successes so the UI can render which steps actually
 * worked.
 *
 * Wrapped in `withContext(NonCancellable)` so a screen-dispose during
 * the dialog confirmation doesn't leave the device half-reverted.
 */
@Singleton
class RootedEmergencyResetCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mutationLog: SysfsMutationLog,
    private val keepAliveController: KeepAliveController,
    private val featureToggles: RootFeatureToggles,
    private val shell: RootShell,
) : EmergencyResetCoordinator {

    override suspend fun resetEverything(
        options: EmergencyResetOptions,
    ): EmergencyResetCoordinatorResult = withContext(NonCancellable) {
        var sysfsRestored = 0
        var sysfsFailed = 0
        var keepAliveStopped = false
        var dozeReset = false
        var batteryOptReset = false
        var perFeatureCleared = 0
        var fatalError: String? = null

        if (options.revertAllSysfsMutations) {
            try {
                val outcome = mutationLog.revertAll(emptyList())
                sysfsRestored = outcome.restored
                sysfsFailed = outcome.failed
            } catch (t: Throwable) {
                fatalError = "mutationLog.revertAll: ${t.message ?: t.javaClass.simpleName}"
            }
        }

        if (options.stopKeepAliveService) {
            try {
                val outcome = keepAliveController.disableAndStopService()
                keepAliveStopped = outcome is KeepAliveControllerResult.Ok ||
                    outcome is KeepAliveControllerResult.ResetCompleted
                clearKeepAlivePreference()
                if (keepAliveStopped) dozeReset = true
            } catch (t: Throwable) {
                if (fatalError == null) {
                    fatalError = "keepAlive.disableAndStopService: ${t.message ?: t.javaClass.simpleName}"
                }
            }
        }

        if (options.reEnableDozeAndBatteryOptimization) {
            try {
                val pkg = context.packageName
                val result = shell.exec(
                    "cmd appops set \"$pkg\" RUN_ANY_IN_BACKGROUND default",
                )
                batteryOptReset = result.isSuccess
            } catch (t: Throwable) {
                if (fatalError == null) {
                    fatalError = "appops reset: ${t.message ?: t.javaClass.simpleName}"
                }
            }
        }

        if (options.resetAllPerFeatureOptOuts) {
            try {
                perFeatureCleared = featureToggles.resetAllToDefault()
            } catch (t: Throwable) {
                if (fatalError == null) {
                    fatalError = "featureToggles.resetAllToDefault: ${t.message ?: t.javaClass.simpleName}"
                }
            }
        }

        val ok = EmergencyResetCoordinatorResult.Ok(
            sysfsMutationsRestored = sysfsRestored,
            sysfsMutationsFailed = sysfsFailed,
            keepAliveStopped = keepAliveStopped,
            dozeReset = dozeReset,
            batteryOptimizationReset = batteryOptReset,
            perFeatureOptOutsCleared = perFeatureCleared,
        )
        if (fatalError != null) {
            EmergencyResetCoordinatorResult.HardwareError(message = fatalError, partial = ok)
        } else {
            ok
        }
    }

    private fun clearKeepAlivePreference() {
        context.getSharedPreferences(KEEP_ALIVE_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_KEEP_ALIVE_ENABLED, false)
            .apply()
    }
}
