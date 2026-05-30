package dev.ranzlappen.gadget.feature.torch.rooted

import dev.ranzlappen.gadget.feature.torch.legacy.LegacyTorchController
import dev.ranzlappen.gadget.feature.torch.legacy.LegacyTorchControllerResult
import dev.ranzlappen.gadget.core.root.core.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

internal const val THERMAL_OVERRIDE_HARD_CEILING_MILLIS = 45_000L
private const val THERMAL_MONITOR_INTERVAL_MILLIS = 500L

/**
 * Temporarily disables the OS thermal throttling for the LED-driver thermal
 * zone. The hard 45-second absolute ceiling is enforced inside the function
 * — callers cannot extend it by passing a larger timeout.
 *
 * Restoration is wrapped in a `NonCancellable` finally so the original
 * mode is always written back even on coroutine cancellation. A monitor
 * coroutine reads the zone temperature every 500 ms and aborts the block
 * if the trip point is breached.
 */
@Singleton
class ThermalOverrideController @Inject constructor(
    private val shell: RootShell,
) {
    suspend fun withOverride(
        timeoutMillis: Long,
        block: suspend () -> Unit,
    ): LegacyTorchControllerResult = coroutineScope {
        val effectiveTimeout = timeoutMillis.coerceAtMost(THERMAL_OVERRIDE_HARD_CEILING_MILLIS)
        val zone = locateLedThermalZone()
            ?: return@coroutineScope LegacyTorchControllerResult.Unsupported
        val originalMode = readMode(zone)
            ?: return@coroutineScope LegacyTorchControllerResult.HardwareError("read mode failed")
        val tripPoint = readTripPoint(zone)

        var abortReason: String? = null
        var monitor: Job? = null

        try {
            shell.exec("echo disabled > \"${zone.path}/mode\"")
            monitor = launch(Dispatchers.IO) {
                while (isActive) {
                    delay(THERMAL_MONITOR_INTERVAL_MILLIS)
                    val temp = readTemp(zone) ?: continue
                    if (tripPoint != null && temp >= tripPoint) {
                        abortReason = "Thermal trip breached: ${temp}m°C ≥ ${tripPoint}m°C"
                        return@launch
                    }
                }
            }
            withTimeoutOrNull(effectiveTimeout) { block() }
            abortReason?.let { LegacyTorchControllerResult.HardwareError(it) } ?: LegacyTorchControllerResult.Ok
        } finally {
            withContext(NonCancellable + Dispatchers.IO) {
                monitor?.cancel()
                shell.exec("echo $originalMode > \"${zone.path}/mode\"")
            }
        }
    }

    private suspend fun locateLedThermalZone(): ThermalZone? {
        val list = shell.exec("ls -d /sys/class/thermal/thermal_zone* 2>/dev/null")
        if (!list.isSuccess) return null
        val zonePaths = list.stdout
            .flatMap { it.trim().split(Regex("\\s+")) }
            .filter { it.isNotBlank() }
        for (path in zonePaths) {
            val type = shell.exec("cat \"$path/type\"").stdout.firstOrNull()?.trim().orEmpty()
            if (type.containsAny(LED_TYPE_KEYWORDS)) {
                return ThermalZone(path = path, type = type)
            }
        }
        return null
    }

    private suspend fun readMode(zone: ThermalZone): String? =
        shell.exec("cat \"${zone.path}/mode\"").stdout.firstOrNull()?.trim()

    private suspend fun readTemp(zone: ThermalZone): Long? =
        shell.exec("cat \"${zone.path}/temp\"").stdout.firstOrNull()?.trim()?.toLongOrNull()

    private suspend fun readTripPoint(zone: ThermalZone): Long? =
        shell.exec("cat \"${zone.path}/trip_point_0_temp\"")
            .stdout.firstOrNull()?.trim()?.toLongOrNull()

    private fun String.containsAny(needles: List<String>): Boolean =
        needles.any { contains(it, ignoreCase = true) }

    private companion object {
        val LED_TYPE_KEYWORDS = listOf("flash", "torch", "led", "flashlight")
    }
}

data class ThermalZone(val path: String, val type: String)
