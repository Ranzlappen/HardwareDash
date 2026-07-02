package dev.ranzlappen.gadget.feature.gps.spoof

import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standard-flavor controller. Drives `LocationManager.addTestProvider` gated
 * on the user enabling HardwareDash as the Mock Location App in Developer
 * Options. Returns [SpoofResult.Unsupported] when the AppOp isn't held —
 * with the helper intent action so the UI can deep-link the user to the
 * right Settings page.
 *
 * The rooted-only methods ([installLsposedModule] / [uninstallLsposedModule])
 * always return [SpoofResult.Unsupported].
 */
@Singleton
class StandardGpsSpoofController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val engine: SpoofEngine,
    private val legal: LegalAcknowledgement,
) : GpsSpoofController {

    override val state: StateFlow<SpoofState> = engine.state

    override suspend fun capabilities(): SpoofCapabilities {
        // Probe whether we hold the Mock Location App slot by attempting a
        // throwaway addTestProvider in a try/catch. A successful add we
        // immediately remove; failure with SecurityException means we don't
        // hold the slot.
        val mockLocationAppSelected = probeMockLocationGrant()

        val perApiCaveats = buildList {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                add("Pre-API 31: Location.isMock() does not exist; mock-detection is partial.")
            }
        }

        val activeModes = if (mockLocationAppSelected) setOf(SpoofMode.TestProviderUserGranted)
        else emptySet()

        return SpoofCapabilities(
            mockLocationAppSelected = mockLocationAppSelected,
            competingMockLocationAppActive = false,
            competingMockLocationAppPackage = null,
            rootGranted = false,
            lsposedFrameworkActive = false,
            lsposedModuleInstalled = false,
            lsposedModuleLoaded = false,
            lsposedBundledVersion = 0,
            lsposedInstalledVersion = 0,
            activeModes = activeModes,
            perApiCaveats = perApiCaveats,
        )
    }

    override suspend fun start(config: SpoofConfig): SpoofResult {
        if (!legal.isAcknowledged()) return SpoofResult.LegalNotAcknowledged
        return engine.start(config, activeModes = setOf(SpoofMode.TestProviderUserGranted))
    }

    override suspend fun stop(): SpoofResult = engine.stop()

    override suspend fun installLsposedModule(): SpoofResult =
        SpoofResult.Unsupported(reason = "Requires the rooted flavor")

    override suspend fun uninstallLsposedModule(): SpoofResult =
        SpoofResult.Unsupported(reason = "Requires the rooted flavor")

    override suspend fun isLegalAcknowledged(): Boolean = legal.isAcknowledged()

    override suspend fun acknowledgeLegal() = legal.acknowledge()

    /**
     * We can't query the Dev-Options selection directly. The cheapest
     * reliable probe is to attempt addTestProvider on a throwaway name and
     * see if SecurityException is thrown. We pick PASSIVE_PROVIDER (least
     * disruptive) and immediately remove the registration.
     */
    private fun probeMockLocationGrant(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val probeName = LocationManager.PASSIVE_PROVIDER
        return try {
            // We can't easily roll back if anyone else also registered as a
            // test provider, so just attempt and clean up best-effort.
            try { lm.removeTestProvider(probeName) } catch (_: Exception) {}
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val props = android.location.provider.ProviderProperties.Builder().build()
                lm.addTestProvider(probeName, props)
            } else {
                @Suppress("DEPRECATION")
                lm.addTestProvider(
                    probeName,
                    false, false, false, false, false, false, false,
                    android.location.Criteria.POWER_LOW,
                    android.location.Criteria.ACCURACY_FINE,
                )
            }
            try { lm.removeTestProvider(probeName) } catch (_: Exception) {}
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            // System-defined provider name conflict — treat as "not the mock app".
            false
        }
    }
}
