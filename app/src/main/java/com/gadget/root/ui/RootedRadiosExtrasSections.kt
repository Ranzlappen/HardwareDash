package com.gadget.root.ui

import dev.ranzlappen.gadget.core.root.*
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothControllerResult
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothRfkillConfig
import dev.ranzlappen.gadget.feature.radios.bt.control.BluetoothTxPowerConfig
import com.gadget.cell.CellControllerResult
import com.gadget.gps.GpsControllerResult
import com.gadget.gps.NmeaTapConfig
import com.gadget.ir.IrCarrierConfig
import com.gadget.ir.IrControllerResult
import com.gadget.ir.IrRawPatternConfig
import com.gadget.nfc.NfcControllerResult
import com.gadget.nfc.RawNciCommandConfig
import com.gadget.wifi.ChannelConfig
import com.gadget.wifi.RfkillConfig
import com.gadget.wifi.TxPowerConfig
import com.gadget.wifi.WifiControllerResult
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

private const val DEMO_WIFI_RFKILL_DURATION_MS = 8_000L
private const val DEMO_WIFI_TX_POWER_DBM = 18
private const val DEMO_WIFI_TX_POWER_DURATION_MS = 30_000L
private const val DEMO_WIFI_CHANNEL = 6
private const val DEMO_WIFI_CHANNEL_DURATION_MS = 15_000L

private const val DEMO_BT_RFKILL_DURATION_MS = 8_000L
private const val DEMO_BT_TX_POWER_DBM = 8
private const val DEMO_BT_TX_POWER_DURATION_MS = 30_000L

private const val DEMO_NFC_PAYLOAD_HEX = "20000100"

private const val DEMO_IR_CARRIER_HZ = 56_000
private const val DEMO_IR_CARRIER_DURATION_MS = 10_000L
private const val DEMO_IR_RAW_ON_MS = 100L
private const val DEMO_IR_RAW_OFF_MS = 100L
private const val DEMO_IR_RAW_TOTAL_MS = 3_000L

private const val DEMO_GPS_NMEA_DURATION_MS = 10_000L

/**
 * Top-level regulatory + safety disclaimer Card. Rendered once at the
 * top of every Batch-6 surface Card so the user sees a single,
 * prominent legal reminder no matter which radio they expand first.
 */
@Composable
private fun RadiosRootDisclaimerCard() {
    RootExtrasDisclaimerCard(
        text = "TX power overrides and raw commands may violate local " +
            "regulations (FCC/ETSI). Use at your own risk. The developer " +
            "assumes no liability.",
    )
}

/**
 * Wi-Fi root extras Card. Rendered inside `RadiosScreen` after the
 * Wi-Fi signal-details section. Includes auto-revert of TX-power on
 * screen dispose via [DisposableEffect] — belt-and-suspenders on top
 * of the helper's local snapshot+restore and the global mutation log.
 */
@Composable
fun WifiRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val wifi = entryPoint.wifiController()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            // MainScope so the coroutine survives RadiosScreen disposal.
            MainScope().launch { wifi.revertTxPowerOnly() }
        }
    }

    Column(modifier = modifier) {
        RadiosRootDisclaimerCard()
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Root extras (Wi-Fi)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "rfkill toggle, TX-power override (capped at 20 dBm), explicit channel " +
                        "override (allow-listed 1–14 + standard 5 GHz). All writes snapshot+restore.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeWifiResult(
                                wifi.rfkillToggle(
                                    RfkillConfig(
                                        block = true,
                                        durationMillis = DEMO_WIFI_RFKILL_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("rfkill block Wi-Fi (${DEMO_WIFI_RFKILL_DURATION_MS / 1000}s)") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeWifiResult(
                                wifi.txPowerOverride(
                                    TxPowerConfig(
                                        targetDbm = DEMO_WIFI_TX_POWER_DBM,
                                        durationMillis = DEMO_WIFI_TX_POWER_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("TX power → ${DEMO_WIFI_TX_POWER_DBM}dBm (explicit-confirm)") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeWifiResult(
                                wifi.channelOverride(
                                    ChannelConfig(
                                        channel = DEMO_WIFI_CHANNEL,
                                        durationMillis = DEMO_WIFI_CHANNEL_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("Set channel → $DEMO_WIFI_CHANNEL (explicit-confirm)") }
                Text(
                    text = "Note: monitor / injection mode is a read-only capability probe. " +
                        "Actual packet injection requires a custom kernel module (not included).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeWifiResult(wifi.probeInjectionCapability())
                        }
                    },
                ) { Text("Probe injection capability (read-only)") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeWifiResult(wifi.resetAllWifiMutations())
                        }
                    },
                ) { Text("Reset all Wi-Fi mutations") }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/**
 * Bluetooth root extras Card. Same DisposableEffect auto-revert
 * pattern as Wi-Fi.
 */
@Composable
fun BluetoothRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val bluetooth = entryPoint.bluetoothController()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            MainScope().launch { bluetooth.revertTxPowerOnly() }
        }
    }

    Column(modifier = modifier) {
        RadiosRootDisclaimerCard()
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Root extras (Bluetooth)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "rfkill toggle, TX-power override (Class-1 cap at 10 dBm), HCI snoop tail. " +
                        "BlueZ CLI is often absent on modern Android — falls back gracefully.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeBluetoothResult(
                                bluetooth.rfkillToggle(
                                    BluetoothRfkillConfig(
                                        block = true,
                                        durationMillis = DEMO_BT_RFKILL_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("rfkill block BT (${DEMO_BT_RFKILL_DURATION_MS / 1000}s)") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeBluetoothResult(
                                bluetooth.txPowerOverride(
                                    BluetoothTxPowerConfig(
                                        targetDbm = DEMO_BT_TX_POWER_DBM,
                                        durationMillis = DEMO_BT_TX_POWER_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("TX power → ${DEMO_BT_TX_POWER_DBM}dBm (explicit-confirm)") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeBluetoothResult(bluetooth.hciSnoopDump())
                        }
                    },
                ) { Text("HCI snoop log tail (read-only)") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeBluetoothResult(bluetooth.resetAllBluetoothMutations())
                        }
                    },
                ) { Text("Reset all Bluetooth mutations") }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/** NFC root extras Card. Raw NCI command exchange only. */
@Composable
fun NfcRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val nfc = entryPoint.nfcController()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        RadiosRootDisclaimerCard()
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Root extras (NFC)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Raw NCI command exchange via /sys/class/nfc/. 256-byte payload " +
                        "ceiling, 5 s read timeout, explicit-confirm.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeNfcResult(
                                nfc.sendRawNciCommand(RawNciCommandConfig(payloadHex = DEMO_NFC_PAYLOAD_HEX)),
                            )
                        }
                    },
                ) { Text("Send NCI CORE_RESET demo (${DEMO_NFC_PAYLOAD_HEX})") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { status = describeNfcResult(nfc.resetAllNfcMutations()) }
                    },
                ) { Text("Reset all NFC mutations") }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/** IR root extras Card. Custom carrier + raw GPIO LED toggling. */
@Composable
fun IrRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val ir = entryPoint.irController()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        RadiosRootDisclaimerCard()
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Root extras (IR)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Custom carrier (clamped 20–100 kHz) + direct GPIO LED toggling " +
                        "(≤ 50 % duty, 5 s burst ceiling).",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeIrResult(
                                ir.customCarrier(
                                    IrCarrierConfig(
                                        carrierHz = DEMO_IR_CARRIER_HZ,
                                        durationMillis = DEMO_IR_CARRIER_DURATION_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("Custom carrier ${DEMO_IR_CARRIER_HZ / 1000}kHz") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeIrResult(
                                ir.rawGpioPattern(
                                    IrRawPatternConfig(
                                        onMillis = DEMO_IR_RAW_ON_MS,
                                        offMillis = DEMO_IR_RAW_OFF_MS,
                                        totalDurationMillis = DEMO_IR_RAW_TOTAL_MS,
                                    ),
                                ),
                            )
                        }
                    },
                ) { Text("Raw GPIO pulse (${DEMO_IR_RAW_TOTAL_MS / 1000}s)") }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { status = describeIrResult(ir.resetAllIrMutations()) }
                    },
                ) { Text("Reset all IR mutations") }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/** Cellular root extras Card. Read-only modem + signal diagnostics. */
@Composable
fun CellRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val cell = entryPoint.cellController()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        RadiosRootDisclaimerCard()
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Root extras (Cellular)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Read-only modem-diagnostic dump (Qualcomm-specific) + deep RSRP/RSRQ " +
                        "per-band breakdown where vendor exposes the nodes.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scope.launch { status = describeCellResult(cell.rawModemDump()) } },
                ) { Text("Modem-diagnostic dump") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { scope.launch { status = describeCellResult(cell.signalDeepDump()) } },
                ) { Text("Signal deep dump") }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

/** GPS root extras Card. Read-only NMEA tap + constellation enumeration. */
@Composable
fun GpsRootExtrasSection(modifier: Modifier = Modifier) {
    val entryPoint = rememberRootFeatures()
    var rootAvailable by remember { mutableStateOf(false) }
    LaunchedEffect(entryPoint) {
        rootAvailable = entryPoint.capabilityRegistry().hasRootAccess()
    }
    if (!rootAvailable) return

    val gps = entryPoint.gpsController()
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier) {
        RadiosRootDisclaimerCard()
        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Root extras (GPS)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Read-only NMEA tap (30 s ceiling) + constellation enumeration showing " +
                        "every visible satellite the framework filters from GpsStatus.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            status = describeGpsResult(
                                gps.nmeaRawTap(NmeaTapConfig(durationMillis = DEMO_GPS_NMEA_DURATION_MS)),
                            )
                        }
                    },
                ) { Text("NMEA tap (${DEMO_GPS_NMEA_DURATION_MS / 1000}s)") }
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch { status = describeGpsResult(gps.constellationDump()) }
                    },
                ) { Text("Constellation enumeration") }
                status?.let { Text(text = it, style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}

private fun describeWifiResult(result: WifiControllerResult): String = when (result) {
    is WifiControllerResult.Ok -> result.statusNote ?: "OK"
    WifiControllerResult.Unsupported -> "Unsupported on this device"
    WifiControllerResult.OptedOut -> "Disabled — enable in Settings"
    is WifiControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is WifiControllerResult.HardwareError -> "Hardware error: ${result.message}"
    is WifiControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is WifiControllerResult.RfkillState ->
        if (result.blocked) "Wi-Fi is blocked" else "Wi-Fi is unblocked"
    is WifiControllerResult.InjectionCapabilityProbe ->
        "Monitor=${result.supportsMonitor}, IBSS=${result.supportsIbss}"
}

private fun describeBluetoothResult(result: BluetoothControllerResult): String = when (result) {
    is BluetoothControllerResult.Ok -> result.statusNote ?: "OK"
    BluetoothControllerResult.Unsupported -> "Unsupported on this device"
    BluetoothControllerResult.OptedOut -> "Disabled — enable in Settings"
    is BluetoothControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is BluetoothControllerResult.HardwareError -> "Hardware error: ${result.message}"
    is BluetoothControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is BluetoothControllerResult.HciSnoopExcerpt ->
        "Read ${result.tailLines.size} HCI lines"
}

private fun describeNfcResult(result: NfcControllerResult): String = when (result) {
    is NfcControllerResult.Ok -> result.statusNote ?: "OK"
    NfcControllerResult.Unsupported -> "Unsupported on this device"
    NfcControllerResult.OptedOut -> "Disabled — enable in Settings"
    is NfcControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is NfcControllerResult.HardwareError -> "Hardware error: ${result.message}"
    is NfcControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is NfcControllerResult.NciResponse ->
        "NCI response: ${result.responseHex.take(64).ifEmpty { "(empty)" }}"
}

private fun describeIrResult(result: IrControllerResult): String = when (result) {
    is IrControllerResult.Ok -> result.statusNote ?: "OK"
    IrControllerResult.Unsupported -> "Unsupported on this device"
    IrControllerResult.OptedOut -> "Disabled — enable in Settings"
    is IrControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is IrControllerResult.HardwareError -> "Hardware error: ${result.message}"
    is IrControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
}

private fun describeCellResult(result: CellControllerResult): String = when (result) {
    is CellControllerResult.Ok -> result.statusNote ?: "OK"
    CellControllerResult.Unsupported -> "Unsupported on this device"
    CellControllerResult.OptedOut -> "Disabled — enable in Settings"
    is CellControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is CellControllerResult.HardwareError -> "Hardware error: ${result.message}"
    is CellControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is CellControllerResult.ModemDump ->
        "Read ${result.nodes.size} modem nodes"
    is CellControllerResult.SignalDeepDump ->
        "Read ${result.perBand.size} signal nodes"
}

private fun describeGpsResult(result: GpsControllerResult): String = when (result) {
    is GpsControllerResult.Ok -> result.statusNote ?: "OK"
    GpsControllerResult.Unsupported -> "Unsupported on this device"
    GpsControllerResult.OptedOut -> "Disabled — enable in Settings"
    is GpsControllerResult.RateLimited ->
        "Cooling down — try again in ${result.retryAfterMillis / 1000}s"
    is GpsControllerResult.HardwareError -> "Hardware error: ${result.message}"
    is GpsControllerResult.ResetCompleted ->
        "Reset: ${result.restored} restored, ${result.failed} failed"
    is GpsControllerResult.NmeaSnapshot ->
        "Tapped ${result.sentences.size} NMEA sentences"
    is GpsControllerResult.ConstellationSnapshot ->
        "Found ${result.satellites.size} satellites"
}
