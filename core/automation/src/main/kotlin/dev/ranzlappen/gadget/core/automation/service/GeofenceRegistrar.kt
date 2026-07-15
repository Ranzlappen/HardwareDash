package dev.ranzlappen.gadget.core.automation.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.model.GeofenceTransition
import dev.ranzlappen.gadget.core.automation.model.Rule
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers one platform geofence per enabled [Trigger.Geofence] rule with the
 * OS-hosted `GeofencingClient`. The fence's `requestId` is the rule id, so
 * transitions delivered to [GeofenceReceiver] map straight back to the rule.
 *
 * Mirrors [AutomationScheduler]'s shape (one OS-hosted registration per rule,
 * a single shared `PendingIntent`, `register`/`unregister`/`rearmAll`) — and
 * like alarms, geofences do **not** survive reboot, so [rearmAll] runs on the
 * boot re-arm path. No resident service is needed: the OS fires our
 * `PendingIntent` even when the process is dead, exactly like `AlarmManager`.
 *
 * Registration silently no-ops when `ACCESS_FINE_LOCATION` isn't granted (the
 * rule-editor hint tells the user to grant "Allow all the time"); the rule
 * persists and arms once the permission is present and [rearmAll] re-runs.
 */
@Singleton
class GeofenceRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ruleRepository: RuleRepository,
) {
    private val client: GeofencingClient by lazy { LocationServices.getGeofencingClient(context) }

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceReceiver::class.java)
            .setAction(GeofenceReceiver.ACTION_GEOFENCE_EVENT)
        // MUTABLE: Play Services fills the GeofencingEvent extras into the
        // broadcast; an immutable PendingIntent would arrive empty.
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    /**
     * (Re)register [rule]'s fence. A disabled or non-geofence rule is
     * unregistered instead — so this is safe to call for every rule.
     */
    @SuppressLint("MissingPermission") // guarded by hasLocationPermission()
    fun register(rule: Rule) {
        val trigger = rule.trigger as? Trigger.Geofence
        if (trigger == null || !rule.enabled) {
            unregister(rule.id)
            return
        }
        if (!hasLocationPermission()) return
        val geofence = Geofence.Builder()
            .setRequestId(rule.id)
            .setCircularRegion(trigger.latitude, trigger.longitude, trigger.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(trigger.transition.toTransitionType())
            .build()
        val request = GeofencingRequest.Builder()
            // No initial trigger: arming while already inside the fence must
            // not fire the rule — only a real crossing does.
            .setInitialTrigger(0)
            .addGeofence(geofence)
            .build()
        runCatching { client.addGeofences(request, pendingIntent) }
            .onFailure { Log.w(TAG, "addGeofences failed for rule ${rule.id}", it) }
    }

    /** Remove the fence for [ruleId]; a no-op if none is registered. */
    fun unregister(ruleId: String) {
        runCatching { client.removeGeofences(listOf(ruleId)) }
    }

    /** Re-register every enabled geofence rule — the boot / rule-set-change path. */
    suspend fun rearmAll() {
        ruleRepository.observeRules().first().forEach(::register)
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun GeofenceTransition.toTransitionType(): Int = when (this) {
        GeofenceTransition.Enter -> Geofence.GEOFENCE_TRANSITION_ENTER
        GeofenceTransition.Exit -> Geofence.GEOFENCE_TRANSITION_EXIT
    }

    private companion object {
        const val TAG = "GeofenceRegistrar"
    }
}
