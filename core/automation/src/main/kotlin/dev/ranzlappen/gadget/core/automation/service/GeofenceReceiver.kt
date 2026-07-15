package dev.ranzlappen.gadget.core.automation.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.automation.RuleRepository
import dev.ranzlappen.gadget.core.automation.model.GeofenceTransition
import dev.ranzlappen.gadget.core.automation.model.Trigger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Landing point for `GeofencingClient` transition callbacks (the PendingIntent
 * armed by [GeofenceRegistrar]). Reads the [GeofencingEvent], maps the OS
 * transition constant back onto [GeofenceTransition], and fires every enabled
 * [Trigger.Geofence] rule whose id is in the triggering set **and** whose
 * configured transition matches the event.
 *
 * **Not** `@AndroidEntryPoint` — the whole `:core:automation` module is
 * Kotlin-only and Hilt's receiver ASM transform breaks on it (see
 * [AutomationSystemEventReceiver]); the Hilt graph is reached via
 * [EntryPointAccessors] instead, and the fire hand-off goes through
 * [RuleFireExecutor] exactly like the alarm / power-event receivers.
 */
class GeofenceReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface GeofenceEntryPoint {
        fun ruleRepository(): RuleRepository
        fun fireExecutor(): RuleFireExecutor
    }

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val transition = when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> GeofenceTransition.Enter
            Geofence.GEOFENCE_TRANSITION_EXIT -> GeofenceTransition.Exit
            else -> return
        }
        val triggeredIds = event.triggeringGeofences
            ?.map { it.requestId }
            ?.toSet()
            .orEmpty()
        if (triggeredIds.isEmpty()) return

        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GeofenceEntryPoint::class.java,
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                entry.ruleRepository().observeRules().first()
                    .filter { rule ->
                        rule.enabled &&
                            rule.id in triggeredIds &&
                            (rule.trigger as? Trigger.Geofence)?.transition == transition
                    }
                    .forEach { entry.fireExecutor().fire(it) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_GEOFENCE_EVENT = "dev.ranzlappen.gadget.automation.GEOFENCE_EVENT"
    }
}
