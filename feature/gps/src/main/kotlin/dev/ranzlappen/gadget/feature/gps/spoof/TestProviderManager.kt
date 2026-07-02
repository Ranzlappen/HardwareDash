package dev.ranzlappen.gadget.feature.gps.spoof

import android.content.Context
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.location.provider.ProviderProperties
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns lifecycle for ALL three test providers: GPS_PROVIDER, NETWORK_PROVIDER,
 * PASSIVE_PROVIDER. Apps that subscribe only to NETWORK_PROVIDER (a common
 * "I just want a cheap fix" pattern) won't see a GPS-only spoof, so we drive
 * all three simultaneously.
 *
 * Crash-recovery: every `addTestProvider` call is preceded by an idempotent
 * `removeTestProvider` (swallowing IllegalArgumentException), so a session
 * left dangling by a force-stop is cleaned up cleanly on the next start.
 *
 * Thrown SecurityException → the caller's responsibility (the standard
 * controller surfaces it as `SpoofResult.Unsupported` with the helper
 * intent).
 */
@Singleton
class TestProviderManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val locationManager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    /**
     * Registers all three providers and enables them. Idempotent — safe to
     * call after a previous session was force-killed.
     *
     * @throws SecurityException when the caller is neither the user-selected
     *   Mock Location App (standard) nor holding the AppOp via root.
     */
    fun start() {
        val lm = locationManager
        for (provider in LocationAdapter.ALL_PROVIDERS) {
            // Idempotent recovery — remove any stale registration first.
            try {
                lm.removeTestProvider(provider)
            } catch (_: IllegalArgumentException) {
                // Provider wasn't registered as a test provider — fine.
            } catch (_: SecurityException) {
                // Bubble up below via the addTestProvider call.
            }

            addProviderApiSafe(lm, provider)
            try {
                lm.setTestProviderEnabled(provider, true)
            } catch (e: IllegalArgumentException) {
                // addTestProvider succeeded but enable failed — best-effort.
            }
        }
    }

    /**
     * Pushes [base] (a Static SpoofConfig built once by the controller) to
     * every registered test provider, refreshing `time` /
     * `elapsedRealtimeNanos` per provider so each looks "fresh".
     */
    fun emitStatic(cfg: SpoofConfig.Static) {
        val lm = locationManager
        for (provider in LocationAdapter.ALL_PROVIDERS) {
            val loc = LocationAdapter.fromStatic(provider, cfg)
            try {
                lm.setTestProviderLocation(provider, loc)
            } catch (_: SecurityException) {
                // Provider may have been pulled out from under us; caller will recover.
            } catch (_: IllegalArgumentException) {
                // Provider wasn't registered for some reason; skip.
            }
        }
    }

    /** Pushes a single [Location] (with provider already set) to every test provider. */
    fun emitSample(sample: RouteEngine.Sample, accuracy: Float = 5f) {
        val lm = locationManager
        for (provider in LocationAdapter.ALL_PROVIDERS) {
            val loc = LocationAdapter.fromSample(provider, sample, accuracy)
            try {
                lm.setTestProviderLocation(provider, loc)
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    fun emitRaw(loc: Location) {
        val lm = locationManager
        for (provider in LocationAdapter.ALL_PROVIDERS) {
            val cloned = Location(loc)
            cloned.provider = provider
            try {
                lm.setTestProviderLocation(provider, cloned)
            } catch (_: SecurityException) {
            } catch (_: IllegalArgumentException) {
            }
        }
    }

    /** Disables and removes all three providers. Idempotent. */
    fun stop() {
        val lm = locationManager
        for (provider in LocationAdapter.ALL_PROVIDERS) {
            try {
                lm.setTestProviderEnabled(provider, false)
            } catch (_: IllegalArgumentException) {
            } catch (_: SecurityException) {
            }
            try {
                lm.removeTestProvider(provider)
            } catch (_: IllegalArgumentException) {
            } catch (_: SecurityException) {
            }
        }
    }

    private fun addProviderApiSafe(lm: LocationManager, provider: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // API 31+ has the typed ProviderProperties overload.
            val props = ProviderProperties.Builder()
                .setHasNetworkRequirement(false)
                .setHasSatelliteRequirement(provider == LocationManager.GPS_PROVIDER)
                .setHasCellRequirement(false)
                .setHasMonetaryCost(false)
                .setHasAltitudeSupport(true)
                .setHasSpeedSupport(true)
                .setHasBearingSupport(true)
                .setPowerUsage(ProviderProperties.POWER_USAGE_LOW)
                .setAccuracy(ProviderProperties.ACCURACY_FINE)
                .build()
            try {
                lm.addTestProvider(provider, props)
            } catch (e: SecurityException) {
                throw e
            } catch (_: IllegalArgumentException) {
                // Provider name unknown to system — skip silently.
            }
        } else {
            @Suppress("DEPRECATION")
            try {
                lm.addTestProvider(
                    provider,
                    /* requiresNetwork = */ false,
                    /* requiresSatellite = */ provider == LocationManager.GPS_PROVIDER,
                    /* requiresCell = */ false,
                    /* hasMonetaryCost = */ false,
                    /* supportsAltitude = */ true,
                    /* supportsSpeed = */ true,
                    /* supportsBearing = */ true,
                    /* powerRequirement = */ Criteria.POWER_LOW,
                    /* accuracy = */ Criteria.ACCURACY_FINE,
                )
            } catch (e: SecurityException) {
                throw e
            } catch (_: IllegalArgumentException) {
            }
        }
    }
}
