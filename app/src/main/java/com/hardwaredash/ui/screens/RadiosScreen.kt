// CHANGE: Added WiFi/cellular signal strength, upload/download speed, NFC tag read/write
// REASON: Add signal monitoring, speed indicators, and full NFC capabilities
// DATE: 2026-04-02

package com.hardwaredash.ui.screens

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.nfc.*
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.compose.foundation.horizontalScroll
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

    // WiFi signal details
    var wifiRssi      by remember { mutableIntStateOf(0) }
    var wifiLinkSpeed by remember { mutableIntStateOf(0) }
    var wifiFreqMhz   by remember { mutableIntStateOf(0) }

    // Cellular signal
    var cellSignalDbm  by remember { mutableIntStateOf(0) }
    var cellSignalLevel by remember { mutableIntStateOf(0) }
    var networkType    by remember { mutableStateOf("—") }

    // Traffic stats
    var downloadSpeed by remember { mutableStateOf("— KB/s") }
    var uploadSpeed   by remember { mutableStateOf("— KB/s") }
    var prevRxBytes   by remember { mutableLongStateOf(TrafficStats.getTotalRxBytes()) }
    var prevTxBytes   by remember { mutableLongStateOf(TrafficStats.getTotalTxBytes()) }
    var prevTimestamp  by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // NFC tag state
    var nfcTagId      by remember { mutableStateOf<String?>(null) }
    var nfcTechList   by remember { mutableStateOf<List<String>>(emptyList()) }
    var nfcRecords    by remember { mutableStateOf<List<String>>(emptyList()) }
    var nfcTagCapacity by remember { mutableIntStateOf(0) }
    var nfcTagWritable by remember { mutableStateOf<Boolean?>(null) }
    var nfcWriteMsg   by remember { mutableStateOf("") }
    var nfcWriteStatus by remember { mutableStateOf("") }
    var currentTag    by remember { mutableStateOf<Tag?>(null) }
    var nfcReaderActive by remember { mutableStateOf(false) }
    var nfcWriteType  by remember { mutableStateOf("Text") } // Text, URI, MIME
    var nfcUriPrefix  by remember { mutableStateOf("https://") }
    var nfcMimeType   by remember { mutableStateOf("text/plain") }
    var showNfcInfo   by remember { mutableStateOf(false) }

    // Enable NFC reader mode
    DisposableEffect(nfcReaderActive) {
        if (!nfcReaderActive) return@DisposableEffect onDispose { }
        val activity = context as? Activity ?: return@DisposableEffect onDispose { }
        val nfcAdapter = NfcAdapter.getDefaultAdapter(context) ?: return@DisposableEffect onDispose { }

        val callback = NfcAdapter.ReaderCallback { tag ->
            currentTag = tag
            nfcTagId = tag.id?.joinToString(":") { "%02X".format(it) } ?: "Unknown"
            nfcTechList = tag.techList?.map { it.substringAfterLast('.') } ?: emptyList()

            // Read NDEF records + tag metadata
            val ndef = Ndef.get(tag)
            val records = mutableListOf<String>()
            if (ndef != null) {
                try {
                    ndef.connect()
                    nfcTagCapacity = ndef.maxSize
                    nfcTagWritable = ndef.isWritable
                    val msg = ndef.ndefMessage
                    if (msg != null && msg.records.isNotEmpty()) {
                        msg.records.forEachIndexed { idx, record ->
                            val tnf = when (record.tnf) {
                                NdefRecord.TNF_EMPTY -> "EMPTY"
                                NdefRecord.TNF_WELL_KNOWN -> "WELL_KNOWN"
                                NdefRecord.TNF_MIME_MEDIA -> "MIME"
                                NdefRecord.TNF_ABSOLUTE_URI -> "URI"
                                NdefRecord.TNF_EXTERNAL_TYPE -> "EXTERNAL"
                                else -> "OTHER(${record.tnf})"
                            }
                            val type = String(record.type, Charsets.UTF_8)
                            val payload = record.payload
                            val payloadStr = when {
                                record.tnf == NdefRecord.TNF_WELL_KNOWN && type == "U" -> {
                                    // URI record: first byte is prefix code
                                    if (payload.isNotEmpty()) {
                                        val prefixByte = payload[0].toInt()
                                        val uriPrefixes = arrayOf("", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.", "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://", "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:", "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://", "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:", "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:")
                                        val prefix = if (prefixByte < uriPrefixes.size) uriPrefixes[prefixByte] else ""
                                        prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
                                    } else ""
                                }
                                record.tnf == NdefRecord.TNF_WELL_KNOWN && type == "T" -> {
                                    // Text record: first byte = status (encoding + lang length)
                                    if (payload.isNotEmpty()) {
                                        val langLen = (payload[0].toInt() and 0x3F)
                                        if (payload.size > 1 + langLen) {
                                            String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
                                        } else String(payload, Charsets.UTF_8)
                                    } else ""
                                }
                                else -> String(payload, Charsets.UTF_8)
                            }
                            records.add("[${idx + 1}] TNF=$tnf  Type=$type")
                            records.add("    $payloadStr")
                        }
                    } else {
                        records.add("Tag is NDEF-formatted but empty")
                    }
                    ndef.close()
                } catch (e: Exception) {
                    records.add("Read error: ${e.message}")
                }
            } else {
                nfcTagCapacity = 0
                nfcTagWritable = null
                records.add("No NDEF data (tag may be unformatted)")
            }
            nfcRecords = records
        }

        nfcAdapter.enableReaderMode(
            activity,
            callback,
            NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NFC_BARCODE,
            null
        )
        onDispose {
            nfcAdapter.disableReaderMode(activity)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            // WiFi
            val wm = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiEnabled = wm.isWifiEnabled
            @Suppress("DEPRECATION")
            val connInfo = wm.connectionInfo
            wifiSsid = connInfo?.ssid?.removeSurrounding("\"") ?: "—"
            wifiRssi = connInfo?.rssi ?: 0
            wifiLinkSpeed = connInfo?.linkSpeed ?: 0
            wifiFreqMhz = connInfo?.frequency ?: 0

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

            // Cellular signal info
            try {
                val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                val sigStrength = tm.signalStrength
                cellSignalLevel = sigStrength?.level ?: 0
                cellSignalDbm = sigStrength?.cellSignalStrengths
                    ?.firstOrNull()?.dbm ?: 0
                networkType = when (tm.dataNetworkType) {
                    TelephonyManager.NETWORK_TYPE_LTE     -> "LTE"
                    TelephonyManager.NETWORK_TYPE_NR      -> "5G NR"
                    TelephonyManager.NETWORK_TYPE_HSDPA,
                    TelephonyManager.NETWORK_TYPE_HSUPA,
                    TelephonyManager.NETWORK_TYPE_HSPA    -> "HSPA"
                    TelephonyManager.NETWORK_TYPE_UMTS    -> "UMTS"
                    TelephonyManager.NETWORK_TYPE_EDGE    -> "EDGE"
                    TelephonyManager.NETWORK_TYPE_GPRS    -> "GPRS"
                    TelephonyManager.NETWORK_TYPE_CDMA    -> "CDMA"
                    TelephonyManager.NETWORK_TYPE_EVDO_0,
                    TelephonyManager.NETWORK_TYPE_EVDO_A  -> "EVDO"
                    TelephonyManager.NETWORK_TYPE_UNKNOWN -> "Unknown"
                    else -> "Other"
                }
            } catch (_: SecurityException) {
                networkType = "Permission needed"
            }

            // Traffic stats — compute speed
            val now       = System.currentTimeMillis()
            val rxBytes   = TrafficStats.getTotalRxBytes()
            val txBytes   = TrafficStats.getTotalTxBytes()
            val elapsed   = (now - prevTimestamp).coerceAtLeast(1L)
            val rxRate    = (rxBytes - prevRxBytes) * 1000 / elapsed
            val txRate    = (txBytes - prevTxBytes) * 1000 / elapsed
            downloadSpeed = formatSpeed(rxRate)
            uploadSpeed   = formatSpeed(txRate)
            prevRxBytes   = rxBytes
            prevTxBytes   = txBytes
            prevTimestamp  = now

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

        // WiFi signal details
        if (wifiEnabled) {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("WiFi Signal Details", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    val band = if (wifiFreqMhz > 4900) "5 GHz" else if (wifiFreqMhz > 0) "2.4 GHz" else "—"
                    Text("RSSI: $wifiRssi dBm  ·  Link: $wifiLinkSpeed Mbps  ·  Band: $band ($wifiFreqMhz MHz)",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    // Signal strength bar
                    val signalPercent = WifiManager.calculateSignalLevel(wifiRssi, 100) / 100f
                    LinearProgressIndicator(
                        progress = { signalPercent },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                }
            }
        }

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

        // Cellular signal details
        if (mobileData || cellSignalDbm != 0) {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Cellular Signal", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text("Network: $networkType  ·  Signal: $cellSignalDbm dBm  ·  Level: $cellSignalLevel/4",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    LinearProgressIndicator(
                        progress = { cellSignalLevel / 4f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                }
            }
        }

        // ── Traffic stats ─────────────────────────────────────────────────────
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Network Speed", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ArrowDownward, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text(downloadSpeed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Download", style = MaterialTheme.typography.labelSmall)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.ArrowUpward, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                        Text(uploadSpeed, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text("Upload", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        HorizontalDivider()

        // ── NFC section ───────────────────────────────────────────────────────
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

        if (hasNfc && nfcEnabled) {
            // NFC reader toggle
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("NFC Tag Reader", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Switch(checked = nfcReaderActive, onCheckedChange = {
                            nfcReaderActive = it
                            if (!it) {
                                nfcTagId = null
                                nfcTechList = emptyList()
                                nfcRecords = emptyList()
                                nfcTagCapacity = 0
                                nfcTagWritable = null
                                currentTag = null
                            }
                        })
                    }

                    if (nfcReaderActive) {
                        if (nfcTagId != null) {
                            Text("Tag ID: $nfcTagId", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Technologies: ${nfcTechList.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                            if (nfcTagCapacity > 0) {
                                Text("Capacity: $nfcTagCapacity bytes  |  Writable: ${nfcTagWritable ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary)
                            }
                            if (nfcRecords.isNotEmpty()) {
                                Text("NDEF Records:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                nfcRecords.forEach { record ->
                                    Text(record, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            Text("Hold an NFC tag near the device...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // NFC Writer — supports Text, URI, MIME
            if (nfcReaderActive && currentTag != null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Write to Tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        // Write type selector
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Text", "URI", "MIME").forEach { type ->
                                FilterChip(
                                    selected = nfcWriteType == type,
                                    onClick = { nfcWriteType = type },
                                    label = { Text(type) },
                                )
                            }
                        }

                        when (nfcWriteType) {
                            "Text" -> {
                                OutlinedTextField(
                                    value = nfcWriteMsg,
                                    onValueChange = { nfcWriteMsg = it },
                                    label = { Text("Text to write") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            "URI" -> {
                                // URI prefix selector
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                ) {
                                    listOf("https://", "http://", "tel:", "mailto:", "geo:", "sms:").forEach { prefix ->
                                        FilterChip(
                                            selected = nfcUriPrefix == prefix,
                                            onClick = { nfcUriPrefix = prefix },
                                            label = { Text(prefix, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.height(28.dp),
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = nfcWriteMsg,
                                    onValueChange = { nfcWriteMsg = it },
                                    label = { Text("URI path") },
                                    placeholder = { Text("example.com") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            "MIME" -> {
                                OutlinedTextField(
                                    value = nfcMimeType,
                                    onValueChange = { nfcMimeType = it },
                                    label = { Text("MIME type") },
                                    placeholder = { Text("text/plain") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                                OutlinedTextField(
                                    value = nfcWriteMsg,
                                    onValueChange = { nfcWriteMsg = it },
                                    label = { Text("Payload") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3,
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val tag = currentTag ?: run {
                                    nfcWriteStatus = "No tag present"
                                    return@Button
                                }
                                nfcWriteStatus = when (nfcWriteType) {
                                    "URI" -> writeNdefRecord(tag, NdefRecord.createUri(nfcUriPrefix + nfcWriteMsg))
                                    "MIME" -> writeNdefRecord(tag, NdefRecord.createMime(nfcMimeType, nfcWriteMsg.toByteArray()))
                                    else -> writeNdefRecord(tag, NdefRecord.createTextRecord("en", nfcWriteMsg))
                                }
                            },
                            enabled = nfcWriteMsg.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Write NDEF ${nfcWriteType}") }

                        if (nfcWriteStatus.isNotEmpty()) {
                            Text(nfcWriteStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (nfcWriteStatus.startsWith("OK")) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // NFC Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("NFC Guide", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { showNfcInfo = !showNfcInfo }, modifier = Modifier.size(24.dp)) {
                            Icon(
                                if (showNfcInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                "Toggle info",
                            )
                        }
                    }
                    if (showNfcInfo) {
                        Text("NDEF Record Types:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        Text("  Text — Plain text with language code\n" +
                             "  URI — URLs, phone numbers, emails (compact encoding)\n" +
                             "  MIME — Any MIME type with custom payload\n" +
                             "  Smart Poster — URI + metadata (title, icon)",
                            style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(4.dp))
                        Text("Common Tag Types:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        Text("  NTAG213 — 144 bytes, most common small tags\n" +
                             "  NTAG215 — 504 bytes, used for Amiibo\n" +
                             "  NTAG216 — 888 bytes, large capacity\n" +
                             "  Mifare Classic 1K — 1024 bytes, proprietary",
                            style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(4.dp))
                        Text("Common Uses:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        Text("  URLs — Share links by tapping\n" +
                             "  Wi-Fi — Share network credentials\n" +
                             "  vCard — Share contact information\n" +
                             "  App Launch — Open specific apps\n" +
                             "  Smart Home — Trigger automations",
                            style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(4.dp))
                        Text("Best Practices:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium)
                        Text("  - Use URI records for URLs (more compact than text)\n" +
                             "  - Keep payloads small for faster read/write\n" +
                             "  - Test with reader before writing to verify\n" +
                             "  - Lock tags after writing to prevent tampering",
                            style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        HorizontalDivider()

        // ── Quick panel launcher (Android 10+) ────────────────────────────────
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Text("Quick Toggles (System Panel)", style = MaterialTheme.typography.titleMedium)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.Panel.ACTION_WIFI))
                }) { Text("WiFi Panel", maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                }) { Text("BT Settings", maxLines = 1, softWrap = false) }
                OutlinedButton(onClick = {
                    context.startActivity(Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY))
                }) { Text("Internet Panel", maxLines = 1, softWrap = false) }
            }
        }
    }
}

private fun writeNdefRecord(tag: Tag, record: NdefRecord): String {
    val message = NdefMessage(arrayOf(record))

    // Try NDEF first
    val ndef = Ndef.get(tag)
    if (ndef != null) {
        return try {
            ndef.connect()
            if (!ndef.isWritable) {
                ndef.close()
                return "Tag is read-only"
            }
            if (message.toByteArray().size > ndef.maxSize) {
                ndef.close()
                return "Message too large (${message.toByteArray().size}B > ${ndef.maxSize}B max)"
            }
            ndef.writeNdefMessage(message)
            ndef.close()
            "OK Written successfully (${message.toByteArray().size} bytes)"
        } catch (e: Exception) {
            "Write error: ${e.message}"
        }
    }

    // Try NdefFormatable
    val formatable = NdefFormatable.get(tag)
    if (formatable != null) {
        return try {
            formatable.connect()
            formatable.format(message)
            formatable.close()
            "OK Formatted and written"
        } catch (e: Exception) {
            "Format error: ${e.message}"
        }
    }

    return "Tag doesn't support NDEF"
}

private fun formatSpeed(bytesPerSec: Long): String = when {
    bytesPerSec >= 1_000_000 -> "${"%.1f".format(bytesPerSec / 1_000_000f)} MB/s"
    bytesPerSec >= 1_000     -> "${"%.0f".format(bytesPerSec / 1_000f)} KB/s"
    else                     -> "$bytesPerSec B/s"
}

@Composable
private fun RadioCard(
    title: String, icon: ImageVector, enabled: Boolean,
    detail: String, onSettings: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
