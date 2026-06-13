package dev.ranzlappen.gadget.feature.battery

import android.os.BatteryManager
import androidx.compose.runtime.Immutable

@Immutable
data class BatteryState(
    val level: Int = -1,
    val isCharging: Boolean = false,
    val chargingStatus: BatteryChargingStatus = BatteryChargingStatus.Unknown,
    val pluggedType: BatteryPlugType = BatteryPlugType.None,
    val health: BatteryHealth = BatteryHealth.Unknown,
    val temperatureCelsius: Float = 0f,
    val voltageMv: Int = 0,
    val isAvailable: Boolean = false,
)

enum class BatteryChargingStatus { Unknown, Charging, Discharging, NotCharging, Full }
enum class BatteryPlugType { None, AC, USB, Wireless }
enum class BatteryHealth { Unknown, Good, Overheat, Dead, OverVoltage, UnspecifiedFailure, Cold }

internal fun Int.toBatteryChargingStatus(): BatteryChargingStatus = when (this) {
    BatteryManager.BATTERY_STATUS_CHARGING -> BatteryChargingStatus.Charging
    BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryChargingStatus.Discharging
    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryChargingStatus.NotCharging
    BatteryManager.BATTERY_STATUS_FULL -> BatteryChargingStatus.Full
    else -> BatteryChargingStatus.Unknown
}

internal fun Int.toBatteryPlugType(): BatteryPlugType = when (this) {
    BatteryManager.BATTERY_PLUGGED_AC -> BatteryPlugType.AC
    BatteryManager.BATTERY_PLUGGED_USB -> BatteryPlugType.USB
    BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryPlugType.Wireless
    else -> BatteryPlugType.None
}

internal fun Int.toBatteryHealth(): BatteryHealth = when (this) {
    BatteryManager.BATTERY_HEALTH_GOOD -> BatteryHealth.Good
    BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryHealth.Overheat
    BatteryManager.BATTERY_HEALTH_DEAD -> BatteryHealth.Dead
    BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryHealth.OverVoltage
    BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> BatteryHealth.UnspecifiedFailure
    BatteryManager.BATTERY_HEALTH_COLD -> BatteryHealth.Cold
    else -> BatteryHealth.Unknown
}
