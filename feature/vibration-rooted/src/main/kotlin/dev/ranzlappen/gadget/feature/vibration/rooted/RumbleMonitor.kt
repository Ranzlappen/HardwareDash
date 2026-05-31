package dev.ranzlappen.gadget.feature.vibration.rooted

import android.content.Context
import android.os.BatteryManager
import dev.ranzlappen.gadget.core.root.core.RootShell
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

internal const val RUMBLE_BATTERY_DRAIN_THRESHOLD_MA = 200.0
internal const val RUMBLE_MOTOR_TEMP_THRESHOLD_M_C = 50_000L
internal const val RUMBLE_ROLLING_WINDOW_MS = 30_000L
private const val RUMBLE_MONITOR_INTERVAL_MS = 1_000L
private const val MICROAMP_PER_MILLIAMP = 1000.0

/**
 * Polls battery current and motor temperature during a sustained rumble.
 * Aborts via [onAbort] callback if either exceeds threshold:
 *
 *   - Battery drain over a 30 s rolling window > 200 mA — likely the motor is
 *     stalling at high duty cycle.
 *   - Motor thermal zone > 50 °C — protect the LRA/ERM coil.
 *
 * The motor thermal zone is best-effort: many devices don't expose it. When
 * absent, only battery drain is monitored. Ported verbatim from the legacy
 * `com.gadget.vibration.RumbleMonitor`.
 */
@Singleton
class RumbleMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shell: RootShell,
) {
    private var motorThermalZonePath: String? = null
    private var thermalZoneProbed = false

    suspend fun monitor(durationMillis: Long, onAbort: (String) -> Unit) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return
        val zone = locateMotorThermalZone()
        val readings = ArrayDeque<BatteryReading>()
        val deadline = System.currentTimeMillis() + durationMillis

        while (coroutineContext.isActive && System.currentTimeMillis() < deadline) {
            delay(RUMBLE_MONITOR_INTERVAL_MS)
            val now = System.currentTimeMillis()

            val currentMicroAmps = batteryManager
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            readings.add(BatteryReading(now, currentMicroAmps))
            while (readings.isNotEmpty() && readings.first().timestampMs < now - RUMBLE_ROLLING_WINDOW_MS) {
                readings.removeFirst()
            }

            val drainMa = drainMagnitudeMa(readings)
            if (drainMa > RUMBLE_BATTERY_DRAIN_THRESHOLD_MA) {
                onAbort("battery drain ${drainMa.toInt()}mA exceeds threshold")
                return
            }
            if (zone != null) {
                val temp = readTempMicroC(zone)
                if (temp != null && temp > RUMBLE_MOTOR_TEMP_THRESHOLD_M_C) {
                    onAbort("motor temp ${temp}m°C exceeds threshold")
                    return
                }
            }
        }
    }

    private fun drainMagnitudeMa(readings: List<BatteryReading>): Double {
        if (readings.isEmpty()) return 0.0
        val avgMicroAmps = readings.sumOf { it.currentMicroAmps.toLong() } / readings.size.toDouble()
        // CURRENT_NOW is negative when discharging on most devices — flip sign.
        return -avgMicroAmps / MICROAMP_PER_MILLIAMP
    }

    private suspend fun locateMotorThermalZone(): String? {
        if (thermalZoneProbed) return motorThermalZonePath
        val list = shell.exec("ls -d /sys/class/thermal/thermal_zone* 2>/dev/null")
        if (list.isSuccess) {
            val zonePaths = list.stdout
                .flatMap { it.trim().split(Regex("\\s+")) }
                .filter { it.isNotBlank() }
            for (path in zonePaths) {
                val type = shell.exec("cat \"$path/type\"").stdout.firstOrNull()?.trim().orEmpty()
                if (type.contains("motor", ignoreCase = true) ||
                    type.contains("haptic", ignoreCase = true) ||
                    type.contains("vibrator", ignoreCase = true)
                ) {
                    motorThermalZonePath = path
                    break
                }
            }
        }
        thermalZoneProbed = true
        return motorThermalZonePath
    }

    private suspend fun readTempMicroC(path: String): Long? =
        shell.exec("cat \"$path/temp\"").stdout.firstOrNull()?.trim()?.toLongOrNull()

    private data class BatteryReading(val timestampMs: Long, val currentMicroAmps: Int)
}
