package com.gadget.gps.spoof

import android.content.Context
import android.content.pm.PackageManager
import com.gadget.root.RootCapabilityRegistry
import com.gadget.root.RootFeatureKey
import com.gadget.root.RootGateDecision
import com.gadget.root.RootSafetyGate
import com.gadget.root.core.RootShell
import com.gadget.root.sysfs.SysfsMutationLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Rooted-flavor GPS spoofing controller. Wraps [SpoofEngine] with:
 *
 *   1. RootSafetyGate check on every state-changing entry point.
 *   2. Pre-flight `appops set com.gadget.root android:mock_location allow`
 *      via libsu so we don't need the user to enable us in Dev Options.
 *      ACCESS_MOCK_LOCATION is signature-only since API 23 — `pm grant`
 *      cannot grant it; only the AppOp set call works.
 *   3. Mutation-log entry whose restore action reverts the AppOp,
 *      removes all three test providers, and stops the foreground service.
 *      Survives force-kill via the existing reboot-resilience flow.
 *   4. Optional bundled LSPosed module install (separate user-initiated
 *      action; not auto-reverted).
 */
@Singleton
class RootedGpsSpoofController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: SpoofEngine,
    private val legal: LegalAcknowledgement,
    private val safetyGate: RootSafetyGate,
    private val capabilityRegistry: RootCapabilityRegistry,
    private val shell: RootShell,
    private val mutationLog: SysfsMutationLog,
    private val installer: LsposedModuleInstaller,
) : GpsSpoofController {

    override val state: StateFlow<SpoofState> = engine.state

    override suspend fun capabilities(): SpoofCapabilities {
        val rootGranted = capabilityRegistry.hasRootAccess()
        val lsposedFrameworkActive = isLsposedFrameworkInstalled()
        val moduleInstalled = installer.isModuleInstalled()
        val moduleLoaded = LsposedHandshake.isLoaded()
        val bundledVersion = installer.bundledVersion()
        val installedVersion = if (moduleInstalled) installer.installedVersion() else 0
        val mockLocationGranted = if (rootGranted) probeAppOp() else false

        val activeModes = buildSet {
            if (mockLocationGranted) add(SpoofMode.TestProviderRootGranted)
            if (moduleLoaded) add(SpoofMode.LsposedHookActive)
        }

        val perApiCaveats = buildList {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) {
                add("API < 31: Location.isMock() does not exist; LSPosed coverage is partial.")
            }
            if (!lsposedFrameworkActive) {
                add("LSPosed framework not detected. Anti-detection limited to test provider only.")
            }
        }

        return SpoofCapabilities(
            mockLocationAppSelected = mockLocationGranted,
            competingMockLocationAppActive = false,
            competingMockLocationAppPackage = null,
            rootGranted = rootGranted,
            lsposedFrameworkActive = lsposedFrameworkActive,
            lsposedModuleInstalled = moduleInstalled,
            lsposedModuleLoaded = moduleLoaded,
            lsposedBundledVersion = bundledVersion,
            lsposedInstalledVersion = installedVersion,
            activeModes = activeModes,
            perApiCaveats = perApiCaveats,
        )
    }

    override suspend fun start(config: SpoofConfig): SpoofResult {
        if (!legal.isAcknowledged()) return SpoofResult.LegalNotAcknowledged

        val featureKey: RootFeatureKey = when (config) {
            is SpoofConfig.Static -> RootFeatureKey.GpsLocationOverride
            else -> RootFeatureKey.GpsRouteSimulation
        }

        when (val gate = safetyGate.check(featureKey)) {
            RootGateDecision.Allowed -> Unit
            else -> return SpoofResult.Blocked(gate)
        }

        // Grant the AppOp via libsu. mutation-log records the original "default"
        // value so we can restore on stop / crash recovery.
        val grant = shell.exec(
            "appops set ${context.packageName} android:mock_location allow",
            timeoutMillis = 10_000L,
        )
        if (!grant.isSuccess) {
            return SpoofResult.Failed("Could not set mock_location AppOp: ${grant.stderr.firstOrNull().orEmpty()}")
        }
        mutationLog.register(MUTATION_PATH_APPOP, "default")

        val activeModes = buildSet {
            add(SpoofMode.TestProviderRootGranted)
            if (LsposedHandshake.isLoaded()) add(SpoofMode.LsposedHookActive)
        }

        val result = engine.start(config, activeModes = activeModes)
        if (result is SpoofResult.Ok) safetyGate.recordInvocation(featureKey)
        return result
    }

    override suspend fun stop(): SpoofResult {
        val result = engine.stop()
        // Revert the AppOp regardless of engine result.
        val revert = shell.exec(
            "appops set ${context.packageName} android:mock_location default",
            timeoutMillis = 10_000L,
        )
        if (revert.isSuccess) mutationLog.unregister(MUTATION_PATH_APPOP)
        return result
    }

    override suspend fun installLsposedModule(): SpoofResult {
        if (!legal.isAcknowledged()) return SpoofResult.LegalNotAcknowledged
        when (val gate = safetyGate.check(RootFeatureKey.GpsLsposedHookInstall)) {
            RootGateDecision.Allowed -> Unit
            else -> return SpoofResult.Blocked(gate)
        }
        val result = installer.install()
        if (result is SpoofResult.Ok) safetyGate.recordInvocation(RootFeatureKey.GpsLsposedHookInstall)
        return result
    }

    override suspend fun uninstallLsposedModule(): SpoofResult {
        // No gate check on uninstall — strictly less risky than install.
        return installer.uninstall()
    }

    override suspend fun isLegalAcknowledged(): Boolean = legal.isAcknowledged()

    override suspend fun acknowledgeLegal() = legal.acknowledge()

    private fun isLsposedFrameworkInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(LsposedModuleInstaller.LSPOSED_MANAGER_PACKAGE, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private suspend fun probeAppOp(): Boolean {
        // `appops get <pkg> android:mock_location` returns lines containing "allow" if granted.
        val r = shell.exec(
            "appops get ${context.packageName} android:mock_location",
            timeoutMillis = 5_000L,
        )
        if (!r.isSuccess) return false
        return r.stdout.any { it.contains("allow", ignoreCase = true) }
    }

    companion object {
        /** Pseudo-path used by SysfsMutationLog to remember the AppOp grant. */
        private const val MUTATION_PATH_APPOP = "gps_spoof:appop:android:mock_location"
    }
}
