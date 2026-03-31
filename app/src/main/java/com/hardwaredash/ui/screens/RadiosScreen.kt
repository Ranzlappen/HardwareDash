package com.hardwaredash.ui.screens

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RadiosScreen() {
    val context = LocalContext.current

    // ── Live state, polled every second ───────────────────────────────────────
    var wifiEnabled by remember { mutableStateOf(false) }
    var wifiSsid    by remember { mutableStateOf("—") }
    var btEnabled   by remember { mutableStateOf(false) }
    var btName      by remember { mutableStateOf("—") }
    var nfcEnabled  by remember { mutableStateOf(false) }
    var hasNfc      by remember { mutableStateOf(false) }
    var mobileData  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            // WiFi
            val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiEnabled = wm.isWifiEnabled
            @Suppress("DEPRECATION")
            wifiSsid = wm.connectionInfo?.ssid?.removeSurrounding("\"") ?: "—"

            // Bluetooth
            val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bt: BluetoothAdapter? = bm.adapter
            btEnabled = bt?.isEnabled == true
            btName = try { bt?.name ?: "—" } catch (_: SecurityException) { "Permission needed" }

            // NFC
            val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
            hasNfc     = nfcAdapter != null
            nfcEnabled = nfcAdapter?.isEnabled == true

            // Mobile data (checks if cellular is the active network)
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val active = cm.activeNetwork
            mobileData = cm.getNetworkCapabilities(active)
                ?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

            delay(1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Radio & Network Status",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "⚠️  Android 10+ prevents apps from toggling WiFi/BT directly.\n" +
            "Use the buttons below to open the relevant Settings panel.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        // ── Status cards ──────────────────────────────────────────────────────
        RadioCard(
            title   = "Wi-Fi",
            icon    = Icons.Default.Wifi,
            enabled = wifiEnabled,
            detail  = if (wifiEnabled && wifiSsid != "<unknown ssid>") "Connected: $wifiSsid" else "Disconnected",
            onSettings = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
        )

        RadioCard(
            title   = "Bluetooth",
            icon    = Icons.Default.Bluetooth,
            enabled = btEnabled,
            detail  = if (btEnabled) "Device: $btName" else "Off",
            onSettings = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
        )

        RadioCard(
            title   = "Mobile Data",
            icon    = Icons.Default.SignalCellularAlt,
            enabled = mobileData,
            detail  = if (mobileData) "Active transport" else "Not active / WiFi preferred",
            onSettings = { context.startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)) },
        )

        RadioCard(
            title   = "NFC",
            icon    = Icons.Default.Nfc,
            enabled = nfcEnabled,
            detail  = when {
                !hasNfc     -> "Not available on this device"
                nfcEnabled  -> "Ready to scan"
                else        -> "Disabled"
            },
            onSettings = {
                context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS).also {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
            },
        )

        HorizontalDivider()

        // ── Quick panel launcher (Android 12+) ────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Text("Quick Toggles (System Panel)", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.Panel.ACTION_WIFI))
                }) { Text("WiFi Panel") }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }) { Text("BT Settings") }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                }) { Text("Internet Panel") }
            }
        }
    }
}

@Composable
private fun RadioCard(
    title: String, icon: ImageVector, enabled: Boolean,
    detail: String, onSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = if (enabled)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon, title,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.SemiBold)
                    Text(detail, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (enabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 8.dp),
                ) {
                    Text(
                        if (enabled) "ON" else "OFF",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, "Open settings")
                }
            }
        }
    }
}
