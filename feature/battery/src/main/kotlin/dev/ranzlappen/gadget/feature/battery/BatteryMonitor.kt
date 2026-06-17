package dev.ranzlappen.gadget.feature.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Observes [Intent.ACTION_BATTERY_CHANGED] and exposes decoded
 * [BatteryState] as a hot [StateFlow].
 *
 * [ACTION_BATTERY_CHANGED] is a sticky broadcast — registering with a null
 * receiver returns the most-recent value immediately, so the [StateFlow] is
 * populated before the first collector subscribes.
 *
 * No permissions are required; `BatteryManager` extras are readable by all
 * apps on all API levels.
 */
@Singleton
class BatteryMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow(BatteryState())
    val state: StateFlow<BatteryState> = _state.asStateFlow()

    init {
        // Read the sticky broadcast for immediate initial state.
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?.let { updateState(it) }

        // Live receiver for subsequent updates. Singleton lives for the entire
        // process lifetime, so the receiver is intentionally not unregistered.
        ContextCompat.registerReceiver(
            context,
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) = updateState(intent)
            },
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun updateState(intent: Intent) {
        val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(
            BatteryManager.EXTRA_STATUS,
            BatteryManager.BATTERY_STATUS_UNKNOWN,
        )
        _state.value = BatteryState(
            level = if (scale > 0) raw * 100 / scale else -1,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL,
            chargingStatus = status.toBatteryChargingStatus(),
            pluggedType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0).toBatteryPlugType(),
            health = intent.getIntExtra(
                BatteryManager.EXTRA_HEALTH,
                BatteryManager.BATTERY_HEALTH_UNKNOWN,
            ).toBatteryHealth(),
            temperatureCelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f,
            voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
            isAvailable = true,
        )
    }
}
