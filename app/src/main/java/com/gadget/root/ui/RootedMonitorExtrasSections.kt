package com.gadget.root.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gadget.battery.BatteryControllerResult
import com.gadget.battery.ChargingProfileConfig
import com.gadget.battery.ChargingTypeOverrideConfig
import com.gadget.battery.HoldSocConfig
import com.gadget.battery.ThermalBypassConfig
import com.gadget.battery.WirelessCoilCurrentConfig
import com.gadget.sensors.FusionOverrideConfig
import com.gadget.sensors.HighPollingConfig
import com.gadget.sensors.OverclockConfig
import com.gadget.sensors.RawUnfilteredConfig
import com.gadget.sensors.SensorsControllerResult
import kotlinx.coroutines.launch

private const val DEMO_SENSOR_TAG_ACCEL = "accel"
private const val DEMO_SENSOR_HZ = 800
private const val DEMO_SENSOR_DURATION_MS = 5_000L
private const val DEMO_SENSOR_RAW_DURATION_MS = 5_000L
private const val DEMO_SENSOR_FUSION_DURATION_MS = 8_000L
private const val DEMO_SENSOR_OVERCLOCK_BUS = 1
private const val DEMO_SENSOR_OVERCLOCK_ADDR = 0x68
private const val DEMO_SENSOR_OVERCLOCK_REG = 0x19
private const val DEMO_SENSOR_OVERCLOCK_VALUE = 0x07
private const val DEMO_SENSOR_OVERCLOCK_DURATION_MS = 8_000L

private const val DEMO_BATTERY_CHARGING_CURRENT_MICROAMPS = 2_000_000L
private const val DEMO_BATTERY_CHARGING_VOLTAGE_MICROVOLTS = 4_400_000L
private const val DEMO_BATTERY_CHARGING_DURATION_MS = 10_000L
private const val DEMO_BATTERY_THERMAL_DURATION_MS = 10_000L
private const val DEMO_BATTERY_USB_TYPE = "USB_DCP"
private const val DEMO_BATTERY_USB_DURATION_MS = 5_000L

private const val DEMO_BATTERY_HOLD_SOC_PERCENT = 80
private const val DEMO_BATTERY_HOLD_SOC_DURATION_MS = 60_000L
private const val DEMO_BATTERY_WIRELESS_COIL_UA = 1_000_000L
private const val DEMO_BATTERY_WIRELESS_COIL_DURATION_MS = 15_000L

/**
 * Card for the Sensors "Root extras" surface. Fits inside SensorsScreen's
 * outer Column, mirroring the Torch/Vibration/Mic Card pattern from
 * Batches 3 and 4. Renders nothing on standard / no-root rooted.
 */
@Composable
fun SensorsRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val sensors = entryPoint.sensorsController()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Root extras (Sensors)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Direct IIO + i2c access, hidden sensor enumeration, fusion override. " +
                    "All writes snapshot+restore in finally; mutation log persists across calls.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeSensorsResult(
                            sensors.highPolling(
                                HighPollingConfig(
                                    sensorTag = DEMO_SENSOR_TAG_ACCEL,
                                    requestedHz = DEMO_SENSOR_HZ,
                                    durationMillis = DEMO_SENSOR_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("High-rate poll (${DEMO_SENSOR_HZ}Hz target)") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeSensorsResult(
                            sensors.rawUnfiltered(
                                RawUnfilteredConfig(
                                    sensorTag = DEMO_SENSOR_TAG_ACCEL,
                                    durationMillis = DEMO_SENSOR_RAW_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("Disable hardware filtering (${DEMO_SENSOR_RAW_DURATION_MS / 1000}s)") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch { status = describeSensorsResult(sensors.readSysfs()) }
                },
            ) { Text("Read raw IIO sysfs") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeSensorsResult(
                            sensors.overclock(
                                OverclockConfig(
                                    sensorTag = DEMO_SENSOR_TAG_ACCEL,
                                    i2cBus = DEMO_SENSOR_OVERCLOCK_BUS,
                                    i2cAddress = DEMO_SENSOR_OVERCLOCK_ADDR,
                                    odrRegister = DEMO_SENSOR_OVERCLOCK_REG,
                                    odrValue = DEMO_SENSOR_OVERCLOCK_VALUE,
                                    durationMillis = DEMO_SENSOR_OVERCLOCK_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("Overclock via i2cset (explicit-confirm)") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeSensorsResult(
                            sensors.fusionOverride(
                                FusionOverrideConfig(durationMillis = DEMO_SENSOR_FUSION_DURATION_MS),
                            ),
                        )
                    }
                },
            ) { Text("Disable HAL fusion") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch { status = describeSensorsResult(sensors.enumerateHidden()) }
                },
            ) { Text("Enumerate hidden sensors") }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch { status = describeSensorsResult(sensors.resetAllSensorMutations()) }
                },
            ) { Text("Reset all sensor mutations") }
            status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

/**
 * Card for the Battery Monitor "Root extras" surface. Same Card pattern as
 * Sensors. Renders nothing on standard / no-root rooted.
 */
@Composable
fun BatteryRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val battery = entryPoint.batteryController()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Root extras (Battery)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Fuel-gauge raw, per-cell readings, charging profile + thermal bypass " +
                    "(both gated by explicit-confirm and a thermal-zone monitor that aborts " +
                    "+ restores on trip-point breach). Full diagnostic dump lands as JSON " +
                    "in the logbook folder.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { scope.launch { status = describeBatteryResult(battery.fuelGaugeRaw()) } },
            ) { Text("Fuel gauge raw dump") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { scope.launch { status = describeBatteryResult(battery.cellMonitor()) } },
            ) { Text("Per-cell snapshot") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeBatteryResult(
                            battery.chargingProfile(
                                ChargingProfileConfig(
                                    maxCurrentMicroAmps = DEMO_BATTERY_CHARGING_CURRENT_MICROAMPS,
                                    maxVoltageMicroVolts = DEMO_BATTERY_CHARGING_VOLTAGE_MICROVOLTS,
                                    durationMillis = DEMO_BATTERY_CHARGING_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("Charging profile override (explicit-confirm)") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeBatteryResult(
                            battery.thermalBypass(
                                ThermalBypassConfig(durationMillis = DEMO_BATTERY_THERMAL_DURATION_MS),
                            ),
                        )
                    }
                },
            ) { Text("Thermal throttle bypass (explicit-confirm)") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeBatteryResult(
                            battery.chargingTypeOverride(
                                ChargingTypeOverrideConfig(
                                    type = DEMO_BATTERY_USB_TYPE,
                                    durationMillis = DEMO_BATTERY_USB_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("Charging-type override → $DEMO_BATTERY_USB_TYPE") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { scope.launch { status = describeBatteryResult(battery.fullDump()) } },
            ) { Text("Full diagnostic dump → logbook") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeBatteryResult(
                            battery.holdStateOfCharge(
                                HoldSocConfig(
                                    targetSocPercent = DEMO_BATTERY_HOLD_SOC_PERCENT,
                                    durationMillis = DEMO_BATTERY_HOLD_SOC_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("Hold SOC at ${DEMO_BATTERY_HOLD_SOC_PERCENT}% (60s)") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch { status = describeBatteryResult(battery.batteryHealthDeepDump()) }
                },
            ) { Text("Battery health deep-dump → logbook") }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        status = describeBatteryResult(
                            battery.wirelessCoilCurrent(
                                WirelessCoilCurrentConfig(
                                    maxCurrentMicroAmps = DEMO_BATTERY_WIRELESS_COIL_UA,
                                    durationMillis = DEMO_BATTERY_WIRELESS_COIL_DURATION_MS,
                                ),
                            ),
                        )
                    }
                },
            ) { Text("Wireless coil cap @ 1.0 A (15s)") }
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch { status = describeBatteryResult(battery.resetAllBatteryMutations()) }
                },
            ) { Text("Reset all battery mutations") }
            status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}

private fun describeSensorsResult(result: SensorsControllerResult): String = when (result) {
    is SensorsControllerResult.Ok -> result.statusNote ?: "OK"
    SensorsControllerResult.Unsupported -> "Unsupported on this device"
    SensorsControllerResult.OptedOut -> "Disabled — enable in Settings"
    is SensorsControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is SensorsControllerResult.HardwareError -> "Hardware error: ${result.message}"
    is SensorsControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is SensorsControllerResult.EnumerationCompleted ->
        "Found ${result.nodes.size} sensor nodes"
    is SensorsControllerResult.SysfsRead ->
        "Read ${result.nodeReadings.size} IIO triples"
}

private fun describeBatteryResult(result: BatteryControllerResult): String = when (result) {
    is BatteryControllerResult.Ok -> result.statusNote ?: "OK"
    BatteryControllerResult.Unsupported -> "Unsupported on this device"
    BatteryControllerResult.OptedOut -> "Disabled — enable in Settings"
    is BatteryControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is BatteryControllerResult.HardwareError -> "Hardware error: ${result.message}"
    is BatteryControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is BatteryControllerResult.FuelGaugeReading ->
        "Read ${result.nodes.size} fuel-gauge nodes"
    is BatteryControllerResult.CellSnapshot ->
        "Read ${result.cells.size} cells"
    is BatteryControllerResult.DumpWritten -> "Dump written → ${result.absolutePath}"
    is BatteryControllerResult.DangerousAborted -> "Aborted: ${result.reason}"
    is BatteryControllerResult.HoldSocSnapshot ->
        "Held SOC at ${result.appliedTargetSocPercent}% for " +
            "${result.appliedDurationMillis / 1000}s (start=${result.initialSocPercent ?: "?"}%)"
    is BatteryControllerResult.BatteryHealthReading ->
        "Health: cycles=${result.cycleCount ?: "?"} " +
            "design=${result.designCapacityUah ?: "?"}uAh full=${result.fullChargeCapacityUah ?: "?"}uAh"
    is BatteryControllerResult.WirelessCoilSnapshot ->
        "Coil cap applied: ${result.appliedCoilCurrentMicroAmps}uA " +
            "(prior=${result.priorCoilCurrentMicroAmps ?: "?"}uA)"
}
