package dev.ranzlappen.gadget.feature.gps.automation

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.automation.ActionHandler
import dev.ranzlappen.gadget.core.automation.ActionParam
import dev.ranzlappen.gadget.core.automation.ActionParamType
import dev.ranzlappen.gadget.core.automation.ActionResult
import dev.ranzlappen.gadget.core.automation.ModuleAction
import dev.ranzlappen.gadget.core.root.RootGateDecision
import dev.ranzlappen.gadget.feature.gps.GpsLocationTracker
import dev.ranzlappen.gadget.feature.gps.R
import dev.ranzlappen.gadget.feature.gps.control.GpsController
import dev.ranzlappen.gadget.feature.gps.control.GpsControllerResult
import dev.ranzlappen.gadget.feature.gps.control.NmeaTapConfig
import dev.ranzlappen.gadget.feature.gps.spoof.GpsSpoofController
import dev.ranzlappen.gadget.feature.gps.spoof.SpoofConfig
import dev.ranzlappen.gadget.feature.gps.spoof.SpoofResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GPS's invocable-action surface for automation. Reuses the existing
 * [GpsLocationTracker] (standard fix acquisition) and [GpsSpoofController]
 * (mock-location playback) rather than re-implementing location control.
 *
 * The two diagnostic reads and the mutation reset are dispatched through
 * [GpsController] — the standard/rooted seam bound per-flavor in `:app`
 * (`RootBindings`), never a `BuildConfig.IS_ROOTED` branch here. Standard
 * flavor's [GpsController] returns [GpsControllerResult.Unsupported] for the
 * two reads, so those actions carry `requiresRoot = true`; [GpsSpoofController]
 * works on both flavors (standard drives `LocationManager.addTestProvider`,
 * rooted layers on libsu + the bundled LSPosed module) so spoofing is not
 * root-gated here.
 */
@Singleton
class GpsActionHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tracker: GpsLocationTracker,
    private val spoofController: GpsSpoofController,
    private val gpsController: GpsController,
) : ActionHandler {

    override val featureId: String = FEATURE_ID

    override val actions: List<ModuleAction> = listOf(
        ModuleAction(ACTION_TRACK_START, context.getString(R.string.gps_action_track_start)),
        ModuleAction(ACTION_TRACK_STOP, context.getString(R.string.gps_action_track_stop)),
        ModuleAction(
            key = ACTION_SPOOF_START,
            label = context.getString(R.string.gps_action_spoof_start),
            params = listOf(
                ActionParam(PARAM_LAT, ActionParamType.Float, "0", -90f, 90f),
                ActionParam(PARAM_LON, ActionParamType.Float, "0", -180f, 180f),
                ActionParam(PARAM_ALT, ActionParamType.Float, "0", -500f, 8_849f),
            ),
        ),
        ModuleAction(ACTION_SPOOF_STOP, context.getString(R.string.gps_action_spoof_stop)),
        ModuleAction(
            key = ACTION_NMEA_RAW_TAP,
            label = context.getString(R.string.gps_action_nmea_raw_tap),
            requiresRoot = true,
            params = listOf(
                ActionParam(PARAM_DURATION_MS, ActionParamType.Int, "5000", 1_000f, 30_000f),
            ),
        ),
        ModuleAction(
            key = ACTION_CONSTELLATION_DUMP,
            label = context.getString(R.string.gps_action_constellation_dump),
            requiresRoot = true,
        ),
        ModuleAction(
            key = ACTION_RESET_GPS_MUTATIONS,
            label = context.getString(R.string.gps_action_reset_gps_mutations),
            requiresRoot = true,
        ),
    )

    override suspend fun dispatch(actionKey: String, params: Map<String, String>): ActionResult =
        when (actionKey) {
            ACTION_TRACK_START -> {
                tracker.startTracking()
                if (tracker.state.value.permissionGranted) {
                    ActionResult.Success
                } else {
                    ActionResult.Failure("location permission not granted")
                }
            }
            ACTION_TRACK_STOP -> { tracker.stopTracking(); ActionResult.Success }
            ACTION_SPOOF_START -> {
                val lat = params[PARAM_LAT]?.toDoubleOrNull()
                val lon = params[PARAM_LON]?.toDoubleOrNull()
                if (lat == null || lon == null) {
                    ActionResult.Failure("lat/lon required")
                } else {
                    val alt = params[PARAM_ALT]?.toDoubleOrNull() ?: 0.0
                    spoofController.start(SpoofConfig.Static(lat = lat, lon = lon, alt = alt)).toActionResult()
                }
            }
            ACTION_SPOOF_STOP -> spoofController.stop().toActionResult()
            ACTION_NMEA_RAW_TAP -> gpsController.nmeaRawTap(
                NmeaTapConfig(durationMillis = params.longOr(PARAM_DURATION_MS, DEFAULT_NMEA_DURATION_MS)),
            ).toActionResult()
            ACTION_CONSTELLATION_DUMP -> gpsController.constellationDump().toActionResult()
            ACTION_RESET_GPS_MUTATIONS -> gpsController.resetAllGpsMutations().toActionResult()
            else -> ActionResult.Unsupported
        }

    private fun SpoofResult.toActionResult(): ActionResult = when (this) {
        SpoofResult.Ok -> ActionResult.Success
        is SpoofResult.Unsupported -> ActionResult.Failure(reason)
        is SpoofResult.Blocked -> ActionResult.Failure(decision.toReadableReason())
        is SpoofResult.Failed -> ActionResult.Failure(message)
        SpoofResult.LegalNotAcknowledged -> ActionResult.Failure("legal disclaimer not acknowledged")
    }

    private fun RootGateDecision.toReadableReason(): String = when (this) {
        RootGateDecision.Allowed -> "blocked"
        RootGateDecision.BlockedByUser -> "turned off in Settings"
        is RootGateDecision.BlockedByLimiter -> "rate-limited; retry in ${retryAfterMillis}ms"
        RootGateDecision.Unsupported -> "requires the rooted app version"
    }

    private fun GpsControllerResult.toActionResult(): ActionResult = when (this) {
        is GpsControllerResult.Ok -> ActionResult.Success
        GpsControllerResult.Unsupported -> ActionResult.Failure("requires the rooted app version")
        is GpsControllerResult.RateLimited -> ActionResult.Failure("rate-limited; retry in ${retryAfterMillis}ms")
        GpsControllerResult.OptedOut -> ActionResult.Failure("turned off in Settings")
        is GpsControllerResult.HardwareError -> ActionResult.Failure(message)
        is GpsControllerResult.ResetCompleted -> ActionResult.Success
        is GpsControllerResult.NmeaSnapshot -> ActionResult.Success
        is GpsControllerResult.ConstellationSnapshot -> ActionResult.Success
    }

    private fun Map<String, String>.longOr(key: String, fallback: Long): Long =
        this[key]?.toLongOrNull() ?: fallback

    companion object {
        const val FEATURE_ID = "gps"
        const val ACTION_TRACK_START = "track_start"
        const val ACTION_TRACK_STOP = "track_stop"
        const val ACTION_SPOOF_START = "spoof_start"
        const val ACTION_SPOOF_STOP = "spoof_stop"
        const val ACTION_NMEA_RAW_TAP = "nmea_raw_tap"
        const val ACTION_CONSTELLATION_DUMP = "constellation_dump"
        const val ACTION_RESET_GPS_MUTATIONS = "reset_gps_mutations"
        const val PARAM_LAT = "lat"
        const val PARAM_LON = "lon"
        const val PARAM_ALT = "alt"
        const val PARAM_DURATION_MS = "duration_ms"
        const val DEFAULT_NMEA_DURATION_MS = 5_000L
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface GpsActionModule {

    @Binds
    @IntoMap
    @StringKey(GpsActionHandler.FEATURE_ID)
    fun bindGpsActionHandler(handler: GpsActionHandler): ActionHandler
}
