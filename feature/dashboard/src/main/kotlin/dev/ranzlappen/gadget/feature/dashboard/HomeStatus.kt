package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.runtime.Immutable
import dev.ranzlappen.gadget.core.model.MetricSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.math.roundToInt

/**
 * Everything the Home status cards and quick actions show. Every field is
 * nullable: `null` means "not known yet" or "this metric isn't registered on
 * this device/flavor", and the card renders a placeholder instead of a guess.
 */
@Immutable
data class HomeStatus(
    val batteryPercent: Int? = null,
    val batteryTemperatureC: Float? = null,
    val storageUsedPercent: Int? = null,
    val storageFreeGb: Float? = null,
    val wifiEnabled: Boolean? = null,
    /** Connected-network RSSI; `null` when off, disconnected or unknown. */
    val wifiRssiDbm: Int? = null,
    /** Torch on/off; `null` when the torch metric/action isn't available. */
    val torchOn: Boolean? = null,
    val canBuzz: Boolean = false,
    val lastLog: LastLogEntry? = null,
)

@Immutable
data class LastLogEntry(val text: String, val timestampMillis: Long)

/**
 * The metric keys and action ids Home reads/dispatches. They are the
 * features' **persisted, stable** identifiers (rules and widgets store them
 * too), duplicated here as strings because `:feature:dashboard` must not
 * depend on feature modules. Renaming one on the feature side breaks saved
 * rules as well, so they don't drift in practice.
 */
internal object HomeKeys {
    const val BATTERY_LEVEL = "battery_level"
    const val BATTERY_TEMPERATURE = "battery_temperature"
    const val STORAGE_USED_PERCENT = "storage_used_percent"
    const val STORAGE_FREE_GB = "storage_free_gb"
    const val WIFI_ENABLED = "wifi_enabled"
    const val WIFI_SIGNAL = "wifi_signal"
    const val TORCH_INTENSITY = "torch_intensity"

    const val TORCH_FEATURE = "torch"
    const val TORCH_ON = "torch_on"
    const val TORCH_OFF = "torch_off"

    const val VIBRATION_FEATURE = "vibration"
    const val VIBRATION_ONESHOT = "oneshot"
    val BUZZ_PARAMS = mapOf("amplitude" to "80", "duration_ms" to "250")

    /** Metrics Home observes, with the poll interval used when a source has no push stream. */
    val OBSERVED: Map<String, Long> = linkedMapOf(
        BATTERY_LEVEL to 10_000L,
        BATTERY_TEMPERATURE to 10_000L,
        STORAGE_USED_PERCENT to 30_000L,
        STORAGE_FREE_GB to 30_000L,
        WIFI_ENABLED to 5_000L,
        WIFI_SIGNAL to 5_000L,
        // Polled fast so the quick-action button reflects a toggle promptly.
        TORCH_INTENSITY to 1_000L,
    )

    /** RSSI at or below this means "no usable signal / not connected". */
    const val NO_SIGNAL_DBM = -100
}

/** Push stream when the source has one, otherwise a poll loop at [pollIntervalMs]. */
internal fun MetricSource.values(pollIntervalMs: Long): Flow<Float> =
    stream() ?: flow {
        while (true) {
            emit(sample())
            delay(pollIntervalMs)
        }
    }

/**
 * Pure mapping from the latest raw metric values to [HomeStatus]. Kept free
 * of Android and Hilt so `HomeStatusTest` can pin it on the JVM.
 *
 * [values] holds the latest reading per [HomeKeys] metric key (`null` /
 * missing = unknown). [torchAvailable] / [buzzAvailable] say whether the
 * matching action handlers are registered.
 */
internal fun homeStatusOf(
    values: Map<String, Float?>,
    torchAvailable: Boolean,
    buzzAvailable: Boolean,
    lastLog: LastLogEntry?,
): HomeStatus {
    val wifiEnabled = values[HomeKeys.WIFI_ENABLED]?.let { it >= 0.5f }
    val rssi = values[HomeKeys.WIFI_SIGNAL]?.roundToInt()
        ?.takeIf { wifiEnabled == true && it > HomeKeys.NO_SIGNAL_DBM }
    val torchIntensity = values[HomeKeys.TORCH_INTENSITY]
    return HomeStatus(
        batteryPercent = values[HomeKeys.BATTERY_LEVEL]?.roundToInt()?.coerceIn(0, 100),
        batteryTemperatureC = values[HomeKeys.BATTERY_TEMPERATURE]?.takeIf { it > 0f },
        storageUsedPercent = values[HomeKeys.STORAGE_USED_PERCENT]?.roundToInt()?.coerceIn(0, 100),
        storageFreeGb = values[HomeKeys.STORAGE_FREE_GB]?.takeIf { it >= 0f },
        wifiEnabled = wifiEnabled,
        wifiRssiDbm = rssi,
        torchOn = if (torchAvailable && torchIntensity != null) torchIntensity > 0f else null,
        canBuzz = buzzAvailable,
        lastLog = lastLog,
    )
}
