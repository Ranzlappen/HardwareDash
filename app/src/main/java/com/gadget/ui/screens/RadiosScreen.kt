// CHANGE: Added NFC tag save/load, HCE emulation, and writer load-from-saved
// REASON: Persist scanned NFC tags, emulate them via HCE, populate writer from saved tags
// DATE: 2026-04-09

package com.gadget.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.nfc.*
import android.nfc.cardemulation.CardEmulation
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.content.ComponentName
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Base64
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
import androidx.compose.ui.viewinterop.AndroidView
import com.google.accompanist.permissions.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.gadget.localization.S
import com.gadget.ui.components.ScreenAnnouncement
import com.gadget.services.NfcEmulationService
import com.gadget.flipper.FlipperConnectionManager
import com.gadget.ir.IrCodecs
import com.gadget.ir.IrTransmitter
import com.gadget.subghz.SubGhzSignal
import com.gadget.subghz.buildFlipperSubFile
import dagger.hilt.android.EntryPointAccessors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipboardManager
import com.google.android.gms.location.*
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Saved NFC Tags: data model & SharedPreferences helpers ─────────────────

private data class SavedNfcTag(
    val name: String,
    val tagId: String,
    val techList: List<String>,
    val records: List<String>,
    val capacity: Int,
    val writable: Boolean?,
    val tagType: String?,
    val ndefBytes: String?,    // Base64-encoded raw NdefMessage bytes
    val writeType: String,     // "Text", "URI", or "MIME"
    val writeContent: String,
    val uriPrefix: String,
    val mimeType: String,
)

private const val NFC_PREFS_NAME = "nfc_saved_tags"
private const val NFC_PREFS_KEY = "saved_tags"
private const val NFC_MAX_SAVED = 50

private fun saveNfcTags(context: Context, tags: List<SavedNfcTag>) {
    val arr = JSONArray()
    tags.take(NFC_MAX_SAVED).forEach { t ->
        arr.put(JSONObject().apply {
            put("name", t.name)
            put("tagId", t.tagId)
            put("techList", JSONArray(t.techList))
            put("records", JSONArray(t.records))
            put("capacity", t.capacity)
            put("writable", t.writable ?: JSONObject.NULL)
            put("tagType", t.tagType ?: JSONObject.NULL)
            put("ndefBytes", t.ndefBytes ?: JSONObject.NULL)
            put("writeType", t.writeType)
            put("writeContent", t.writeContent)
            put("uriPrefix", t.uriPrefix)
            put("mimeType", t.mimeType)
        })
    }
    context.getSharedPreferences(NFC_PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(NFC_PREFS_KEY, arr.toString()).apply()
}

private fun loadNfcTags(context: Context): List<SavedNfcTag> {
    val json = context.getSharedPreferences(NFC_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(NFC_PREFS_KEY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            val techArr = obj.getJSONArray("techList")
            val recArr = obj.getJSONArray("records")
            SavedNfcTag(
                name = obj.getString("name"),
                tagId = obj.getString("tagId"),
                techList = (0 until techArr.length()).map { techArr.getString(it) },
                records = (0 until recArr.length()).map { recArr.getString(it) },
                capacity = obj.optInt("capacity", 0),
                writable = if (obj.isNull("writable")) null else obj.getBoolean("writable"),
                tagType = if (obj.isNull("tagType")) null else obj.getString("tagType"),
                ndefBytes = if (obj.isNull("ndefBytes")) null else obj.getString("ndefBytes"),
                writeType = obj.optString("writeType", "Text"),
                writeContent = obj.optString("writeContent", ""),
                uriPrefix = obj.optString("uriPrefix", "https://"),
                mimeType = obj.optString("mimeType", "text/plain"),
            )
        }
    } catch (_: Exception) { emptyList() }
}

/** Extract write parameters from the first NDEF record of raw bytes. */
private fun extractWriteParams(ndefBytes: ByteArray): Triple<String, String, String>? {
    return try {
        val msg = NdefMessage(ndefBytes)
        val rec = msg.records.firstOrNull() ?: return null
        val payload = rec.payload
        when {
            rec.tnf == NdefRecord.TNF_WELL_KNOWN && String(rec.type) == "T" -> {
                if (payload.isNotEmpty()) {
                    val langLen = payload[0].toInt() and 0x3F
                    val text = if (payload.size > 1 + langLen)
                        String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
                    else String(payload, Charsets.UTF_8)
                    Triple("Text", text, "")
                } else null
            }
            rec.tnf == NdefRecord.TNF_WELL_KNOWN && String(rec.type) == "U" -> {
                if (payload.isNotEmpty()) {
                    val prefixByte = payload[0].toInt()
                    val uriPrefixes = arrayOf("", "http://www.", "https://www.", "http://", "https://", "tel:", "mailto:")
                    val prefix = if (prefixByte < uriPrefixes.size) uriPrefixes[prefixByte] else ""
                    val path = String(payload, 1, payload.size - 1, Charsets.UTF_8)
                    Triple("URI", path, prefix)
                } else null
            }
            rec.tnf == NdefRecord.TNF_MIME_MEDIA -> {
                val mime = String(rec.type, Charsets.UTF_8)
                val content = String(payload, Charsets.UTF_8)
                Triple("MIME", content, mime)
            }
            else -> null
        }
    } catch (_: Exception) { null }
}

// ─── Saved IR Codes: data model & SharedPreferences helpers ────────────────

private data class SavedIrCode(
    val name: String,
    val protocol: String,   // "NEC", "Pronto", "Raw"
    val carrierHz: Int,
    val payload: String,
    val repeats: Int,
)

private const val IR_PREFS_NAME = "ir_saved_codes"
private const val IR_PREFS_KEY = "saved_codes"
private const val IR_MAX_SAVED = 50

private fun saveIrCodes(context: Context, codes: List<SavedIrCode>) {
    val arr = JSONArray()
    codes.take(IR_MAX_SAVED).forEach { c ->
        arr.put(JSONObject().apply {
            put("name", c.name)
            put("protocol", c.protocol)
            put("carrierHz", c.carrierHz)
            put("payload", c.payload)
            put("repeats", c.repeats)
        })
    }
    context.getSharedPreferences(IR_PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(IR_PREFS_KEY, arr.toString()).apply()
}

private fun loadIrCodes(context: Context): List<SavedIrCode> {
    val json = context.getSharedPreferences(IR_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(IR_PREFS_KEY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SavedIrCode(
                name = o.getString("name"),
                protocol = o.optString("protocol", "NEC"),
                carrierHz = o.optInt("carrierHz", 38000),
                payload = o.optString("payload", ""),
                repeats = o.optInt("repeats", 1),
            )
        }
    } catch (_: Exception) { emptyList() }
}

// ─── Saved Sub-GHz Signals: data model & SharedPreferences helpers ─────────

private data class SavedSubGhzSignal(
    val name: String,
    val frequencyHz: Long,
    val modulation: String,
    val protocol: String,
    val bitLength: Int,
    val keyHex: String,
    val rawData: String,
    val te: Int,
    val sourceFile: String?,
)

private const val SUBGHZ_PREFS_NAME = "subghz_saved_signals"
private const val SUBGHZ_PREFS_KEY = "saved_signals"
private const val SUBGHZ_MAX_SAVED = 50

private fun saveSubGhzSignals(context: Context, signals: List<SavedSubGhzSignal>) {
    val arr = JSONArray()
    signals.take(SUBGHZ_MAX_SAVED).forEach { s ->
        arr.put(JSONObject().apply {
            put("name", s.name)
            put("frequencyHz", s.frequencyHz)
            put("modulation", s.modulation)
            put("protocol", s.protocol)
            put("bitLength", s.bitLength)
            put("keyHex", s.keyHex)
            put("rawData", s.rawData)
            put("te", s.te)
            put("sourceFile", s.sourceFile ?: JSONObject.NULL)
        })
    }
    context.getSharedPreferences(SUBGHZ_PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(SUBGHZ_PREFS_KEY, arr.toString()).apply()
}

private fun loadSubGhzSignals(context: Context): List<SavedSubGhzSignal> {
    val json = context.getSharedPreferences(SUBGHZ_PREFS_NAME, Context.MODE_PRIVATE)
        .getString(SUBGHZ_PREFS_KEY, null) ?: return emptyList()
    return try {
        val arr = JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            SavedSubGhzSignal(
                name = o.getString("name"),
                frequencyHz = o.optLong("frequencyHz", 0L),
                modulation = o.optString("modulation", ""),
                protocol = o.optString("protocol", ""),
                bitLength = o.optInt("bitLength", 0),
                keyHex = o.optString("keyHex", ""),
                rawData = o.optString("rawData", ""),
                te = o.optInt("te", 0),
                sourceFile = if (o.isNull("sourceFile")) null else o.optString("sourceFile", null),
            )
        }
    } catch (_: Exception) { emptyList() }
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RadiosScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Flipper Zero connection (used for Sub-GHz / IR "Send to Flipper" buttons).
    val flipperManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            FlipperManagerEntryPoint::class.java,
        ).flipperManager()
    }
    val flipperState by flipperManager.state.collectAsState()
    val flipperConnected = flipperState is FlipperConnectionManager.State.Connected
    val flipperStrings = S.flipper

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
    // Write protection analysis state
    var nfcProtectionInfo    by remember { mutableStateOf<List<String>>(emptyList()) }
    var nfcProtectionType    by remember { mutableStateOf<String?>(null) }
    var nfcBypassPossible    by remember { mutableStateOf<String?>(null) }
    var nfcTagType           by remember { mutableStateOf<String?>(null) }
    var nfcBypassLog         by remember { mutableStateOf<List<String>>(emptyList()) }
    var nfcBypassInProgress  by remember { mutableStateOf(false) }
    var showProtectionDetails by remember { mutableStateOf(false) }
    var nfcRawNdefBase64 by remember { mutableStateOf<String?>(null) }
    // Saved NFC tags state
    var savedNfcTags by remember { mutableStateOf(loadNfcTags(context)) }
    var showNfcSaveDialog by remember { mutableStateOf(false) }
    var nfcSaveName by remember { mutableStateOf("") }
    var selectedSavedTagIndex by remember { mutableIntStateOf(-1) }
    var nfcEmulating by remember { mutableStateOf(false) }
    var savedTagDropdownExpanded by remember { mutableStateOf(false) }
    var writerSavedTagDropdownExpanded by remember { mutableStateOf(false) }

    // IR state
    val irHasEmitter = remember { IrTransmitter.hasEmitter(context) }
    val irCarriers = remember { IrTransmitter.carrierFrequencies(context) }
    var irProtocol by remember { mutableStateOf("NEC") }
    var irCarrierText by remember { mutableStateOf("38000") }
    var irPayload by remember { mutableStateOf("") }
    var irRepeatsText by remember { mutableStateOf("1") }
    var irStatus by remember { mutableStateOf("") }
    var savedIrCodes by remember { mutableStateOf(loadIrCodes(context)) }
    var selectedIrIndex by remember { mutableIntStateOf(-1) }
    var irDropdownExpanded by remember { mutableStateOf(false) }
    var showIrSaveDialog by remember { mutableStateOf(false) }
    var irSaveName by remember { mutableStateOf("") }

    // Sub-GHz state
    var subGhzFreqText by remember { mutableStateOf("433920000") }
    var subGhzModulation by remember { mutableStateOf("AM650") }
    var subGhzProtocol by remember { mutableStateOf("Princeton") }
    var subGhzBitText by remember { mutableStateOf("24") }
    var subGhzKey by remember { mutableStateOf("") }
    var subGhzRaw by remember { mutableStateOf("") }
    var subGhzTeText by remember { mutableStateOf("") }
    var subGhzSourceFile by remember { mutableStateOf<String?>(null) }
    var subGhzStatus by remember { mutableStateOf("") }
    var savedSubGhzSignals by remember { mutableStateOf(loadSubGhzSignals(context)) }
    var selectedSubGhzIndex by remember { mutableIntStateOf(-1) }
    var subGhzDropdownExpanded by remember { mutableStateOf(false) }
    var showSubGhzSaveDialog by remember { mutableStateOf(false) }
    var subGhzSaveName by remember { mutableStateOf("") }

    val parseFailedMsg = S.radios.parseFailed
    val transmittingMsg = S.radios.transmitting
    val externalHwMsg = S.radios.externalHardwareRequired
    val subFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                val parsed = text?.let { SubGhzSignal.parseFlipperSub(it) }
                if (parsed != null) {
                    subGhzFreqText = parsed.frequencyHz.toString()
                    subGhzModulation = SubGhzSignal.modulationFromPreset(parsed.preset).ifBlank { subGhzModulation }
                    subGhzProtocol = parsed.protocol.ifBlank { subGhzProtocol }
                    subGhzBitText = parsed.bitLength.takeIf { it > 0 }?.toString() ?: subGhzBitText
                    subGhzKey = parsed.keyHex
                    subGhzRaw = parsed.rawData
                    subGhzTeText = parsed.te.takeIf { it > 0 }?.toString() ?: ""
                    subGhzSourceFile = uri.lastPathSegment
                    subGhzStatus = ""
                } else {
                    subGhzStatus = parseFailedMsg
                }
            } catch (e: Exception) {
                subGhzStatus = "$parseFailedMsg: ${e.message}"
            }
        }
    }

    // GPS state
    var gpsActive     by remember { mutableStateOf(false) }
    var gpsLat        by remember { mutableStateOf("--") }
    var gpsLon        by remember { mutableStateOf("--") }
    var gpsAlt        by remember { mutableStateOf("--") }
    var gpsSpeed      by remember { mutableStateOf("--") }
    var gpsAccuracy   by remember { mutableStateOf("--") }
    var gpsBearing    by remember { mutableStateOf("--") }
    var gpsProvider   by remember { mutableStateOf("--") }
    var gpsLog        by remember { mutableStateOf<List<String>>(emptyList()) }
    var showGpsLog    by remember { mutableStateOf(false) }
    var gpsLocation   by remember { mutableStateOf<Location?>(null) }

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
                    nfcRawNdefBase64 = try {
                        msg?.toByteArray()?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
                    } catch (_: Exception) { null }
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
                nfcRawNdefBase64 = null
                records.add("No NDEF data (tag may be unformatted)")
            }
            nfcRecords = records

            // Detect write protection details
            val protResult = detectWriteProtection(tag)
            nfcTagType = protResult.tagType
            nfcProtectionType = protResult.protectionType
            nfcBypassPossible = protResult.bypassPossible
            nfcProtectionInfo = protResult.details
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

    // GPS location updates
    val locationPermState = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
    )
    DisposableEffect(gpsActive) {
        if (!gpsActive || !locationPermState.allPermissionsGranted) return@DisposableEffect onDispose { }
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                gpsLocation = loc
                gpsLat = "%.6f".format(loc.latitude)
                gpsLon = "%.6f".format(loc.longitude)
                gpsAlt = if (loc.hasAltitude()) "%.1f m".format(loc.altitude) else "--"
                gpsSpeed = if (loc.hasSpeed()) "%.1f km/h".format(loc.speed * 3.6f) else "--"
                gpsAccuracy = if (loc.hasAccuracy()) "%.1f m".format(loc.accuracy) else "--"
                gpsBearing = if (loc.hasBearing()) "%.0f°".format(loc.bearing) else "--"
                gpsProvider = loc.provider ?: "unknown"
                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(loc.time))
                val entry = "$ts  ${gpsLat}, ${gpsLon}  alt=$gpsAlt  spd=$gpsSpeed  acc=$gpsAccuracy"
                gpsLog = (listOf(entry) + gpsLog).take(100)
            }
        }
        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (_: SecurityException) { }
        onDispose {
            fusedClient.removeLocationUpdates(callback)
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

    ScreenAnnouncement(S.accessibility.radiosScreen)

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
            modifier = Modifier.semantics { heading() },
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
                    @Suppress("DEPRECATION")
                    val signalPercent = WifiManager.calculateSignalLevel(wifiRssi, 100) / 100f
                    LinearProgressIndicator(
                        progress = { signalPercent },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                }
            }
        }

        // WiFi quick actions: Hotspot & Wireless Projection
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent("android.settings.TETHERING_SETTINGS").also {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.radios.hotspot, maxLines = 1, softWrap = false)
            }
            OutlinedButton(
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS).also {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(S.radios.wirelessProjection, maxLines = 1, softWrap = false)
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
                                nfcRawNdefBase64 = null
                                nfcTagCapacity = 0
                                nfcTagWritable = null
                                currentTag = null
                                nfcProtectionInfo = emptyList()
                                nfcProtectionType = null
                                nfcBypassPossible = null
                                nfcTagType = null
                                nfcBypassLog = emptyList()
                                nfcBypassInProgress = false
                                showProtectionDetails = false
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
                            OutlinedButton(
                                onClick = { nfcSaveName = ""; showNfcSaveDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(S.radios.saveTag)
                            }
                        } else {
                            Text("Hold an NFC tag near the device...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                }
            }

            // Write Protection Analysis
            if (nfcReaderActive && nfcTagId != null && nfcProtectionType != null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Write Protection Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        if (nfcTagType != null) {
                            Text("Tag Type: $nfcTagType", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Protection: ${nfcProtectionType ?: "Unknown"}", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Bypassable:", style = MaterialTheme.typography.bodyMedium)
                            val bypassColor = when (nfcBypassPossible) {
                                "Yes" -> Color(0xFF4CAF50)
                                "No" -> MaterialTheme.colorScheme.error
                                "Maybe" -> Color(0xFFFFA000)
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                            Text(nfcBypassPossible ?: "N/A", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = bypassColor)
                        }
                        if (nfcProtectionInfo.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Details", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = { showProtectionDetails = !showProtectionDetails }) {
                                    Icon(
                                        if (showProtectionDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        S.accessibility.toggleDetails,
                                    )
                                }
                            }
                            if (showProtectionDetails) {
                                nfcProtectionInfo.forEach { line ->
                                    Text(line, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // ── Saved NFC Tags ───────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(S.radios.savedNfcTags, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                    if (savedNfcTags.isEmpty()) {
                        Text(S.radios.noSavedTags, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    } else {
                        // Dropdown to select a saved tag
                        ExposedDropdownMenuBox(
                            expanded = savedTagDropdownExpanded,
                            onExpandedChange = { savedTagDropdownExpanded = it },
                        ) {
                            OutlinedTextField(
                                value = if (selectedSavedTagIndex in savedNfcTags.indices) savedNfcTags[selectedSavedTagIndex].name else S.radios.selectTag,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = savedTagDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                singleLine = true,
                            )
                            ExposedDropdownMenu(
                                expanded = savedTagDropdownExpanded,
                                onDismissRequest = { savedTagDropdownExpanded = false },
                            ) {
                                savedNfcTags.forEachIndexed { idx, tag ->
                                    DropdownMenuItem(
                                        text = { Text(tag.name) },
                                        onClick = {
                                            selectedSavedTagIndex = idx
                                            savedTagDropdownExpanded = false
                                        },
                                    )
                                }
                            }
                        }

                        // Display selected tag info
                        if (selectedSavedTagIndex in savedNfcTags.indices) {
                            val sel = savedNfcTags[selectedSavedTagIndex]
                            Text("Tag ID: ${sel.tagId}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Technologies: ${sel.techList.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
                            if (sel.capacity > 0) {
                                Text("Capacity: ${sel.capacity} bytes  |  Writable: ${sel.writable ?: "N/A"}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            if (sel.tagType != null) {
                                Text("Tag Type: ${sel.tagType}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (sel.records.isNotEmpty()) {
                                Text("NDEF Records:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                sel.records.forEach { r ->
                                    Text(r, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                // Emulate toggle
                                Button(
                                    onClick = {
                                        if (nfcEmulating) {
                                            NfcEmulationService.ndefMessageBytes = null
                                            nfcEmulating = false
                                        } else {
                                            val bytes = sel.ndefBytes?.let { Base64.decode(it, Base64.NO_WRAP) }
                                            if (bytes != null) {
                                                NfcEmulationService.ndefMessageBytes = bytes
                                                // Set as preferred service
                                                val adapter = NfcAdapter.getDefaultAdapter(context)
                                                if (adapter != null) {
                                                    val cardEmu = CardEmulation.getInstance(adapter)
                                                    val component = ComponentName(context, NfcEmulationService::class.java)
                                                    val activity = context as? Activity
                                                    if (activity != null) {
                                                        cardEmu.setPreferredService(activity, component)
                                                    }
                                                }
                                                nfcEmulating = true
                                            }
                                        }
                                    },
                                    enabled = sel.ndefBytes != null,
                                    modifier = Modifier.weight(1f),
                                    colors = if (nfcEmulating) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                             else ButtonDefaults.buttonColors(),
                                ) {
                                    Icon(
                                        if (nfcEmulating) Icons.Default.StopCircle else Icons.Default.Nfc,
                                        null, modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (nfcEmulating) S.radios.stopEmulating else S.radios.emulate)
                                }

                                // Delete button
                                OutlinedButton(
                                    onClick = {
                                        if (nfcEmulating) {
                                            NfcEmulationService.ndefMessageBytes = null
                                            nfcEmulating = false
                                        }
                                        savedNfcTags = savedNfcTags.toMutableList().also { it.removeAt(selectedSavedTagIndex) }
                                        saveNfcTags(context, savedNfcTags)
                                        selectedSavedTagIndex = -1
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Icon(Icons.Default.Delete, S.radios.deleteTag, modifier = Modifier.size(18.dp))
                                }
                            }

                            if (nfcEmulating) {
                                Text(S.radios.emulating, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Stop emulation when leaving NFC section
            DisposableEffect(Unit) {
                onDispose {
                    if (nfcEmulating) {
                        NfcEmulationService.ndefMessageBytes = null
                        val adapter = NfcAdapter.getDefaultAdapter(context)
                        if (adapter != null) {
                            val cardEmu = CardEmulation.getInstance(adapter)
                            val activity = context as? Activity
                            if (activity != null) {
                                cardEmu.unsetPreferredService(activity)
                            }
                        }
                    }
                }
            }

            // NFC Writer — supports Text, URI, MIME
            if (nfcReaderActive && currentTag != null) {
                Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Write to Tag", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        // Load from saved tag dropdown
                        if (savedNfcTags.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = writerSavedTagDropdownExpanded,
                                onExpandedChange = { writerSavedTagDropdownExpanded = it },
                            ) {
                                OutlinedTextField(
                                    value = S.radios.loadFromSaved,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = writerSavedTagDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall,
                                )
                                ExposedDropdownMenu(
                                    expanded = writerSavedTagDropdownExpanded,
                                    onDismissRequest = { writerSavedTagDropdownExpanded = false },
                                ) {
                                    savedNfcTags.forEach { saved ->
                                        DropdownMenuItem(
                                            text = { Text(saved.name) },
                                            onClick = {
                                                nfcWriteType = saved.writeType
                                                nfcWriteMsg = saved.writeContent
                                                nfcUriPrefix = saved.uriPrefix
                                                nfcMimeType = saved.mimeType
                                                writerSavedTagDropdownExpanded = false
                                            },
                                        )
                                    }
                                }
                            }
                        }

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
                                    placeholder = { Text(S.radios.exampleDomain) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                )
                            }
                            "MIME" -> {
                                OutlinedTextField(
                                    value = nfcMimeType,
                                    onValueChange = { nfcMimeType = it },
                                    label = { Text("MIME type") },
                                    placeholder = { Text(S.radios.textPlain) },
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
                                val record = when (nfcWriteType) {
                                    "URI" -> NdefRecord.createUri(nfcUriPrefix + nfcWriteMsg)
                                    "MIME" -> NdefRecord.createMime(nfcMimeType, nfcWriteMsg.toByteArray())
                                    else -> NdefRecord.createTextRecord("en", nfcWriteMsg)
                                }
                                nfcBypassLog = emptyList()
                                nfcBypassInProgress = true
                                nfcWriteStatus = ""
                                coroutineScope.launch(Dispatchers.IO) {
                                    val result = writeNdefRecord(tag, record) { logLine ->
                                        nfcBypassLog = nfcBypassLog + logLine
                                    }
                                    nfcWriteStatus = result
                                    nfcBypassInProgress = false
                                }
                            },
                            enabled = nfcWriteMsg.isNotBlank() && !nfcBypassInProgress,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (nfcBypassInProgress) "Writing..." else "Write NDEF $nfcWriteType") }

                        if (nfcBypassInProgress) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        if (nfcWriteStatus.isNotEmpty()) {
                            Text(nfcWriteStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (nfcWriteStatus.startsWith("OK")) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.error)
                        }

                        if (nfcBypassLog.isNotEmpty()) {
                            Text("Bypass log:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            nfcBypassLog.forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                        IconButton(onClick = { showNfcInfo = !showNfcInfo }) {
                            Icon(
                                if (showNfcInfo) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                S.accessibility.toggleInfo,
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

            // ── Save NFC Tag Dialog ──────────────────────────────────
            if (showNfcSaveDialog && nfcTagId != null) {
                AlertDialog(
                    onDismissRequest = { showNfcSaveDialog = false },
                    title = { Text(S.radios.saveTag) },
                    text = {
                        OutlinedTextField(
                            value = nfcSaveName,
                            onValueChange = { nfcSaveName = it },
                            label = { Text(S.radios.tagName) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val rawBytes = nfcRawNdefBase64?.let { Base64.decode(it, Base64.NO_WRAP) }
                                val writeParams = rawBytes?.let { extractWriteParams(it) }
                                val saved = SavedNfcTag(
                                    name = nfcSaveName.trim(),
                                    tagId = nfcTagId ?: "",
                                    techList = nfcTechList,
                                    records = nfcRecords,
                                    capacity = nfcTagCapacity,
                                    writable = nfcTagWritable,
                                    tagType = nfcTagType,
                                    ndefBytes = nfcRawNdefBase64,
                                    writeType = writeParams?.first ?: "Text",
                                    writeContent = writeParams?.second ?: "",
                                    uriPrefix = if (writeParams?.first == "URI") writeParams.third else "https://",
                                    mimeType = if (writeParams?.first == "MIME") writeParams.third else "text/plain",
                                )
                                savedNfcTags = (listOf(saved) + savedNfcTags).take(NFC_MAX_SAVED)
                                saveNfcTags(context, savedNfcTags)
                                showNfcSaveDialog = false
                            },
                            enabled = nfcSaveName.isNotBlank(),
                        ) { Text(S.radios.saveTag) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNfcSaveDialog = false }) {
                            Text(S.radios.cancel)
                        }
                    },
                )
            }
        }

        HorizontalDivider()

        // ── Infrared section ─────────────────────────────────────────────────
        Text(S.radios.infrared, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!irHasEmitter) {
                    Text(
                        S.radios.irNotSupported,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (irCarriers.isNotEmpty()) {
                    Text(
                        "Supported carriers: " + irCarriers.joinToString(", ") { "${it.first}-${it.last} Hz" },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(S.radios.protocol, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("NEC", "Pronto", "Raw").forEach { p ->
                        FilterChip(
                            selected = irProtocol == p,
                            onClick = { irProtocol = p },
                            label = { Text(p) },
                        )
                    }
                }

                if (irProtocol != "Pronto") {
                    OutlinedTextField(
                        value = irCarrierText,
                        onValueChange = { irCarrierText = it.filter { c -> c.isDigit() } },
                        label = { Text(S.radios.carrierHz) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }

                OutlinedTextField(
                    value = irPayload,
                    onValueChange = { irPayload = it },
                    label = { Text(S.radios.payload) },
                    placeholder = {
                        Text(
                            when (irProtocol) {
                                "NEC" -> "0xE13DC03F"
                                "Pronto" -> "0000 006D 0022 0000 …"
                                else -> "9000, 4500, 560, 1690, …"
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = irRepeatsText,
                    onValueChange = { irRepeatsText = it.filter { c -> c.isDigit() }.take(2) },
                    label = { Text(S.radios.repeats) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = {
                            val carrier = irCarrierText.toIntOrNull() ?: 38000
                            val repeats = irRepeatsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            when (val r = IrCodecs.encode(irProtocol, irPayload, carrier, repeats)) {
                                is IrCodecs.Result.Error -> irStatus = r.message
                                is IrCodecs.Result.Ok -> {
                                    irStatus = transmittingMsg
                                    val err = IrTransmitter.transmit(context, r.encoded.carrierHz, r.encoded.pattern)
                                    irStatus = err ?: "OK (${r.encoded.pattern.size} steps @ ${r.encoded.carrierHz} Hz)"
                                }
                            }
                        },
                        enabled = irHasEmitter && irPayload.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(S.radios.transmit)
                    }
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val txt = cm?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
                            if (!txt.isNullOrBlank()) irPayload = txt
                        },
                    ) {
                        Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(S.radios.pasteFromClipboard)
                    }
                }

                if (flipperConnected) {
                    OutlinedButton(
                        onClick = {
                            val carrier = irCarrierText.toIntOrNull() ?: 38000
                            val repeats = irRepeatsText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            val encoded = when (val r = IrCodecs.encode(irProtocol, irPayload, carrier, repeats)) {
                                is IrCodecs.Result.Error -> { irStatus = r.message; null }
                                is IrCodecs.Result.Ok -> r.encoded
                            }
                            val cmds = flipperManager.infrared
                            if (encoded != null && cmds != null) {
                                val irFile = cmds.buildRawIrFile(
                                    name = "hd_${irProtocol.lowercase()}",
                                    frequencyHz = encoded.carrierHz,
                                    dutyCycle = 0.33,
                                    timingsMicros = encoded.pattern,
                                )
                                irStatus = transmittingMsg
                                coroutineScope.launch(Dispatchers.IO) {
                                    try {
                                        cmds.transmitIrFile(irFile)
                                        irStatus = flipperStrings.transmittedViaFlipper
                                    } catch (e: Exception) {
                                        irStatus = "${flipperStrings.sendFailed}: ${e.message ?: e::class.java.simpleName}"
                                    }
                                }
                            } else if (encoded != null) {
                                irStatus = flipperStrings.disconnected
                            }
                        },
                        enabled = irPayload.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(flipperStrings.sendToFlipper)
                    }
                }

                OutlinedButton(
                    onClick = { irSaveName = ""; showIrSaveDialog = true },
                    enabled = irPayload.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(S.radios.saveCode)
                }

                if (irStatus.isNotBlank()) {
                    Text(irStatus, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Saved IR codes
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(S.radios.savedIrCodes, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (savedIrCodes.isEmpty()) {
                    Text(S.radios.noSavedIrCodes, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                } else {
                    ExposedDropdownMenuBox(
                        expanded = irDropdownExpanded,
                        onExpandedChange = { irDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = if (selectedIrIndex >= 0) savedIrCodes[selectedIrIndex].name else S.radios.selectTag,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(S.radios.loadFromSaved) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = irDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = irDropdownExpanded,
                            onDismissRequest = { irDropdownExpanded = false },
                        ) {
                            savedIrCodes.forEachIndexed { i, c ->
                                DropdownMenuItem(
                                    text = { Text("${c.name} — ${c.protocol}") },
                                    onClick = {
                                        selectedIrIndex = i
                                        irProtocol = c.protocol
                                        irCarrierText = c.carrierHz.toString()
                                        irPayload = c.payload
                                        irRepeatsText = c.repeats.toString()
                                        irDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (selectedIrIndex >= 0) {
                        val sel = savedIrCodes[selectedIrIndex]
                        Text("${sel.protocol}  ${sel.carrierHz} Hz  x${sel.repeats}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    when (val r = IrCodecs.encode(sel.protocol, sel.payload, sel.carrierHz, sel.repeats)) {
                                        is IrCodecs.Result.Error -> irStatus = r.message
                                        is IrCodecs.Result.Ok -> {
                                            val err = IrTransmitter.transmit(context, r.encoded.carrierHz, r.encoded.pattern)
                                            irStatus = err ?: "OK"
                                        }
                                    }
                                },
                                enabled = irHasEmitter,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(S.radios.transmit)
                            }
                            OutlinedButton(
                                onClick = {
                                    savedIrCodes = savedIrCodes.toMutableList().also { it.removeAt(selectedIrIndex) }
                                    saveIrCodes(context, savedIrCodes)
                                    selectedIrIndex = -1
                                },
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(S.radios.deleteTag)
                            }
                        }
                    }
                }
            }
        }

        if (showIrSaveDialog) {
            AlertDialog(
                onDismissRequest = { showIrSaveDialog = false },
                title = { Text(S.radios.saveCode) },
                text = {
                    OutlinedTextField(
                        value = irSaveName,
                        onValueChange = { irSaveName = it },
                        label = { Text(S.radios.codeName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val saved = SavedIrCode(
                                name = irSaveName.trim(),
                                protocol = irProtocol,
                                carrierHz = irCarrierText.toIntOrNull() ?: 38000,
                                payload = irPayload,
                                repeats = irRepeatsText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            )
                            savedIrCodes = (listOf(saved) + savedIrCodes).take(IR_MAX_SAVED)
                            saveIrCodes(context, savedIrCodes)
                            showIrSaveDialog = false
                        },
                        enabled = irSaveName.isNotBlank(),
                    ) { Text(S.radios.saveCode) }
                },
                dismissButton = {
                    TextButton(onClick = { showIrSaveDialog = false }) { Text(S.radios.cancel) }
                },
            )
        }

        HorizontalDivider()

        // ── Sub-GHz section ──────────────────────────────────────────────────
        Text(S.radios.subGhz, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    S.radios.subGhzNotSupported,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedButton(
                    onClick = { subFileLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.FileOpen, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(S.radios.importSubFile)
                }

                OutlinedTextField(
                    value = subGhzFreqText,
                    onValueChange = { subGhzFreqText = it.filter { c -> c.isDigit() } },
                    label = { Text(S.radios.frequency) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Text(S.radios.modulation, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("AM650", "AM270", "FM238", "FM476").forEach { m ->
                        FilterChip(
                            selected = subGhzModulation == m,
                            onClick = { subGhzModulation = m },
                            label = { Text(m) },
                        )
                    }
                }

                OutlinedTextField(
                    value = subGhzProtocol,
                    onValueChange = { subGhzProtocol = it },
                    label = { Text(S.radios.protocol) },
                    placeholder = { Text("Princeton, CAME, NICE FLO, Keeloq, RAW…", style = MaterialTheme.typography.bodySmall) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subGhzBitText,
                        onValueChange = { subGhzBitText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text(S.radios.bitLength) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = subGhzTeText,
                        onValueChange = { subGhzTeText = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("TE (µs)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                }

                OutlinedTextField(
                    value = subGhzKey,
                    onValueChange = { subGhzKey = it },
                    label = { Text(S.radios.key) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = subGhzRaw,
                    onValueChange = { subGhzRaw = it },
                    label = { Text(S.radios.rawData) },
                    modifier = Modifier.fillMaxWidth(),
                )

                if (subGhzSourceFile != null) {
                    Text("Source: ${subGhzSourceFile}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val freq = subGhzFreqText.toLongOrNull()
                            if (freq == null) {
                                subGhzStatus = parseFailedMsg
                            } else {
                                val subFile = buildFlipperSubFile(
                                    frequencyHz = freq,
                                    preset = subGhzModulation,
                                    protocol = subGhzProtocol,
                                    bitLength = subGhzBitText.toIntOrNull() ?: 0,
                                    keyHex = subGhzKey,
                                    rawData = subGhzRaw,
                                    te = subGhzTeText.toIntOrNull() ?: 0,
                                )
                                coroutineScope.launch(Dispatchers.IO) {
                                    val cmds = flipperManager.subGhz
                                    if (cmds == null) {
                                        subGhzStatus = flipperStrings.disconnected
                                    } else {
                                        try {
                                            cmds.transmitSubFile(subFile)
                                            subGhzStatus = flipperStrings.transmittedViaFlipper
                                        } catch (e: Exception) {
                                            subGhzStatus = "${flipperStrings.sendFailed}: ${e.message ?: e::class.java.simpleName}"
                                        }
                                    }
                                }
                            }
                        },
                        enabled = flipperConnected && subGhzFreqText.isNotBlank(),
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (flipperConnected) flipperStrings.sendToFlipper else S.radios.transmit)
                    }
                    OutlinedButton(
                        onClick = { subGhzSaveName = ""; showSubGhzSaveDialog = true },
                        enabled = subGhzFreqText.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(S.radios.saveSignal)
                    }
                }

                if (!flipperConnected) {
                    Text(
                        S.radios.externalHardwareRequired,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (subGhzStatus.isNotBlank()) {
                    Text(subGhzStatus, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Saved Sub-GHz signals
        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(S.radios.savedSubGhzSignals, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (savedSubGhzSignals.isEmpty()) {
                    Text(S.radios.noSavedSubGhzSignals, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                } else {
                    ExposedDropdownMenuBox(
                        expanded = subGhzDropdownExpanded,
                        onExpandedChange = { subGhzDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = if (selectedSubGhzIndex >= 0) savedSubGhzSignals[selectedSubGhzIndex].name else S.radios.selectTag,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(S.radios.loadFromSaved) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subGhzDropdownExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = subGhzDropdownExpanded,
                            onDismissRequest = { subGhzDropdownExpanded = false },
                        ) {
                            savedSubGhzSignals.forEachIndexed { i, s ->
                                DropdownMenuItem(
                                    text = { Text("${s.name} — ${s.frequencyHz} Hz") },
                                    onClick = {
                                        selectedSubGhzIndex = i
                                        subGhzFreqText = s.frequencyHz.toString()
                                        subGhzModulation = s.modulation.ifBlank { subGhzModulation }
                                        subGhzProtocol = s.protocol
                                        subGhzBitText = s.bitLength.toString()
                                        subGhzKey = s.keyHex
                                        subGhzRaw = s.rawData
                                        subGhzTeText = s.te.takeIf { it > 0 }?.toString() ?: ""
                                        subGhzSourceFile = s.sourceFile
                                        subGhzDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (selectedSubGhzIndex >= 0) {
                        val sel = savedSubGhzSignals[selectedSubGhzIndex]
                        Text("${sel.protocol}  ${sel.modulation}  ${sel.bitLength} bit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary)
                        OutlinedButton(
                            onClick = {
                                savedSubGhzSignals = savedSubGhzSignals.toMutableList().also { it.removeAt(selectedSubGhzIndex) }
                                saveSubGhzSignals(context, savedSubGhzSignals)
                                selectedSubGhzIndex = -1
                            },
                        ) {
                            Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(S.radios.deleteTag)
                        }
                    }
                }
            }
        }

        if (showSubGhzSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSubGhzSaveDialog = false },
                title = { Text(S.radios.saveSignal) },
                text = {
                    OutlinedTextField(
                        value = subGhzSaveName,
                        onValueChange = { subGhzSaveName = it },
                        label = { Text(S.radios.signalName) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val saved = SavedSubGhzSignal(
                                name = subGhzSaveName.trim(),
                                frequencyHz = subGhzFreqText.toLongOrNull() ?: 0L,
                                modulation = subGhzModulation,
                                protocol = subGhzProtocol,
                                bitLength = subGhzBitText.toIntOrNull() ?: 0,
                                keyHex = subGhzKey,
                                rawData = subGhzRaw,
                                te = subGhzTeText.toIntOrNull() ?: 0,
                                sourceFile = subGhzSourceFile,
                            )
                            savedSubGhzSignals = (listOf(saved) + savedSubGhzSignals).take(SUBGHZ_MAX_SAVED)
                            saveSubGhzSignals(context, savedSubGhzSignals)
                            showSubGhzSaveDialog = false
                        },
                        enabled = subGhzSaveName.isNotBlank(),
                    ) { Text(S.radios.saveSignal) }
                },
                dismissButton = {
                    TextButton(onClick = { showSubGhzSaveDialog = false }) { Text(S.radios.cancel) }
                },
            )
        }

        HorizontalDivider()

        // ── GPS section ──────────────────────────────────────────────────────
        Text(S.radios.gpsLocation, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (!locationPermState.allPermissionsGranted) {
            Card(shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(S.radios.locationPermRequired, color = MaterialTheme.colorScheme.onErrorContainer)
                    Button(onClick = { locationPermState.launchMultiplePermissionRequest() }) {
                        Text(S.radios.grantLocationPerm)
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(S.radios.gpsTracking, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Switch(
                        checked = gpsActive,
                        onCheckedChange = {
                            if (it && !locationPermState.allPermissionsGranted) {
                                locationPermState.launchMultiplePermissionRequest()
                            } else {
                                gpsActive = it
                                if (!it) {
                                    gpsLocation = null
                                }
                            }
                        },
                    )
                }

                if (gpsActive && locationPermState.allPermissionsGranted) {
                    // Metrics display
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(S.radios.latitude, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(gpsLat, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Column {
                            Text(S.radios.longitude, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(gpsLon, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(S.radios.altitude, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(gpsAlt, style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            Text(S.radios.speed, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(gpsSpeed, style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            Text(S.radios.accuracy, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(gpsAccuracy, style = MaterialTheme.typography.bodySmall)
                        }
                        Column {
                            Text(S.radios.bearing, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(gpsBearing, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text("Provider: $gpsProvider", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }

        // Live map
        if (gpsActive && gpsLocation != null) {
            Card(modifier = Modifier.fillMaxWidth().height(250.dp), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                val loc = gpsLocation
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        Configuration.getInstance().userAgentValue = ctx.packageName
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(17.0)
                            if (loc != null) {
                                val point = GeoPoint(loc.latitude, loc.longitude)
                                controller.setCenter(point)
                                val marker = Marker(this)
                                marker.position = point
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                marker.title = "Current Location"
                                overlays.add(marker)
                            }
                        }
                    },
                    update = { mapView ->
                        if (loc != null) {
                            val point = GeoPoint(loc.latitude, loc.longitude)
                            mapView.controller.animateTo(point)
                            // Update marker
                            mapView.overlays.clear()
                            val marker = Marker(mapView)
                            marker.position = point
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = "Current Location"
                            mapView.overlays.add(marker)
                            mapView.invalidate()
                        }
                    },
                )
            }
        }

        // GPS Log
        if (gpsActive && gpsLog.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("GPS Log (${gpsLog.size} entries)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                        Row {
                            IconButton(onClick = { showGpsLog = !showGpsLog }) {
                                Icon(if (showGpsLog) Icons.Default.ExpandLess else Icons.Default.ExpandMore, S.accessibility.toggleLog)
                            }
                            IconButton(onClick = { gpsLog = emptyList() }) {
                                Icon(Icons.Default.Delete, S.accessibility.clearLog, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    if (showGpsLog) {
                        gpsLog.take(20).forEach { entry ->
                            Text(entry, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (gpsLog.size > 20) {
                            Text("... and ${gpsLog.size - 20} more", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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

private fun writeNdefRecord(tag: Tag, record: NdefRecord, onLog: (String) -> Unit): String {
    val message = NdefMessage(arrayOf(record))

    // Try NDEF first
    val ndef = Ndef.get(tag)
    if (ndef != null) {
        try {
            ndef.connect()
            if (!ndef.isWritable) {
                ndef.close()
                onLog("Tag reports read-only, attempting bypass...")
                return attemptBypassAndWrite(tag, record, onLog)
            }
            if (message.toByteArray().size > ndef.maxSize) {
                ndef.close()
                return "Message too large (${message.toByteArray().size}B > ${ndef.maxSize}B max)"
            }
            ndef.writeNdefMessage(message)
            ndef.close()
            return "OK Written successfully (${message.toByteArray().size} bytes)"
        } catch (e: Exception) {
            try { ndef.close() } catch (_: Exception) {}
            return "Write error: ${e.message}"
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

// ── NFC Write Protection Detection & Bypass ──────────────────────────────────

private data class WriteProtectionResult(
    val tagType: String?,
    val protectionType: String?,
    val bypassPossible: String?,  // "Yes", "No", "Maybe"
    val details: List<String>,
)

private val NTAG_DEFAULT_PASSWORDS = listOf(
    byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
    byteArrayOf(0x00, 0x00, 0x00, 0x00),
    byteArrayOf(0x01, 0x02, 0x03, 0x04),
    byteArrayOf(0x11, 0x22, 0x33, 0x44),
    byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte()),
)

private val MIFARE_DEFAULT_KEYS = listOf(
    byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()),
    byteArrayOf(0xA0.toByte(), 0xA1.toByte(), 0xA2.toByte(), 0xA3.toByte(), 0xA4.toByte(), 0xA5.toByte()),
    byteArrayOf(0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte(), 0xD3.toByte(), 0xF7.toByte()),
    byteArrayOf(0x00, 0x00, 0x00, 0x00, 0x00, 0x00),
    byteArrayOf(0xB0.toByte(), 0xB1.toByte(), 0xB2.toByte(), 0xB3.toByte(), 0xB4.toByte(), 0xB5.toByte()),
    byteArrayOf(0x4D.toByte(), 0x3A.toByte(), 0x99.toByte(), 0xC3.toByte(), 0x51.toByte(), 0xDD.toByte()),
    byteArrayOf(0x1A.toByte(), 0x98.toByte(), 0x2C.toByte(), 0x7E.toByte(), 0x45.toByte(), 0x9A.toByte()),
    byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte()),
)

/** Identifies NTAG type from GET_VERSION response. Returns (name, configPage, totalPages) or null. */
private fun identifyNtagType(version: ByteArray): Triple<String, Int, Int>? {
    if (version.size < 8) return null
    val storageSize = version[6].toInt() and 0xFF
    return when (storageSize) {
        0x0F -> Triple("NTAG213", 41, 45)
        0x11 -> Triple("NTAG215", 131, 135)
        0x13 -> Triple("NTAG216", 227, 231)
        0x0E -> Triple("NTAG210", 16, 20)
        0x06 -> Triple("Mifare Ultralight", -1, 20)
        else -> Triple("NfcA (unknown, storage=0x${"%02X".format(storageSize)})", -1, -1)
    }
}

/** Detects write protection details for the current tag. */
private fun detectWriteProtection(tag: Tag): WriteProtectionResult {
    val techList = tag.techList.map { it.substringAfterLast('.') }
    val details = mutableListOf<String>()
    var tagType: String? = null
    var protectionType: String? = null
    var bypassPossible: String? = null

    // ── NTAG / Mifare Ultralight (NfcA) ──
    if ("NfcA" in techList) {
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            try {
                nfcA.connect()
                nfcA.timeout = 1000

                // GET_VERSION to identify chip
                var configPage = -1
                var totalPages = -1
                try {
                    val version = nfcA.transceive(byteArrayOf(0x60))
                    val info = identifyNtagType(version)
                    if (info != null) {
                        tagType = info.first
                        configPage = info.second
                        totalPages = info.third
                        details.add("Tag type: $tagType")
                    }
                } catch (_: Exception) {
                    tagType = if ("MifareUltralight" in techList) "Mifare Ultralight" else "NfcA (unknown)"
                    details.add("Tag type: $tagType (GET_VERSION not supported)")
                }

                // Read page 2 (static lock bits in bytes 2-3) and page 3 (CC bytes)
                try {
                    val pages2to5 = nfcA.transceive(byteArrayOf(0x30, 0x02))
                    if (pages2to5.size >= 8) {
                        val lockByte0 = pages2to5[2].toInt() and 0xFF
                        val lockByte1 = pages2to5[3].toInt() and 0xFF
                        val staticLockBits = (lockByte0 shl 8) or lockByte1
                        val ccByte3 = pages2to5[7].toInt() and 0xFF

                        details.add("Static lock bits: 0x${"%04X".format(staticLockBits)}${if (staticLockBits != 0) " (SET — irreversible)" else " (clear)"}")
                        details.add("CC byte 3 (access): 0x${"%02X".format(ccByte3)}${if (ccByte3 and 0x0F != 0) " (read-only flag set)" else " (read-write)"}")

                        val hasStaticLocks = staticLockBits != 0
                        val hasCCReadOnly = ccByte3 and 0x0F != 0

                        // Read dynamic lock bits if tag type known
                        var hasDynamicLocks = false
                        if (configPage > 0) {
                            val dynLockPage = when (tagType) {
                                "NTAG213" -> 40
                                "NTAG215" -> 130
                                "NTAG216" -> 226
                                else -> -1
                            }
                            if (dynLockPage > 0) {
                                try {
                                    val dynPages = nfcA.transceive(byteArrayOf(0x30, dynLockPage.toByte()))
                                    if (dynPages.size >= 3) {
                                        val dyn = ((dynPages[0].toInt() and 0xFF) shl 16) or
                                                  ((dynPages[1].toInt() and 0xFF) shl 8) or
                                                   (dynPages[2].toInt() and 0xFF)
                                        hasDynamicLocks = dyn != 0
                                        details.add("Dynamic lock bits: 0x${"%06X".format(dyn)}${if (hasDynamicLocks) " (SET — irreversible)" else " (clear)"}")
                                    }
                                } catch (_: Exception) {
                                    details.add("Dynamic lock bits: could not read")
                                }
                            }
                        }

                        // Read password / auth config if NTAG21x
                        var hasPasswordProtection = false
                        if (configPage > 0) {
                            try {
                                val cfgPages = nfcA.transceive(byteArrayOf(0x30, configPage.toByte()))
                                if (cfgPages.size >= 8) {
                                    val auth0 = cfgPages[3].toInt() and 0xFF
                                    val access = cfgPages[4].toInt() and 0xFF
                                    val protBit = (access shr 7) and 1
                                    val cfglck = (access shr 6) and 1
                                    val authLim = access and 0x07

                                    hasPasswordProtection = auth0 < (totalPages and 0xFF)
                                    details.add("AUTH0: 0x${"%02X".format(auth0)} (password required from page $auth0)${if (hasPasswordProtection) " — ACTIVE" else " — not active"}")
                                    details.add("ACCESS: PROT=${if (protBit == 1) "read+write" else "write-only"}, CFGLCK=${if (cfglck == 1) "locked" else "unlocked"}, AUTHLIM=$authLim")
                                }
                            } catch (_: Exception) {
                                details.add("Auth config: could not read (may be protected)")
                                hasPasswordProtection = true
                            }
                        }

                        // Determine bypass possibility
                        when {
                            hasStaticLocks || hasDynamicLocks -> {
                                protectionType = if (hasStaticLocks) "Static lock bits (irreversible)" else "Dynamic lock bits (irreversible)"
                                bypassPossible = "No"
                                if (hasCCReadOnly) protectionType += " + CC read-only"
                                if (hasPasswordProtection) protectionType += " + Password"
                            }
                            hasCCReadOnly && !hasPasswordProtection -> {
                                protectionType = "CC byte read-only"
                                bypassPossible = "Yes"
                            }
                            hasPasswordProtection && !hasCCReadOnly -> {
                                protectionType = "Password protected"
                                bypassPossible = "Maybe"
                            }
                            hasPasswordProtection && hasCCReadOnly -> {
                                protectionType = "CC read-only + Password protected"
                                bypassPossible = "Maybe"
                            }
                            else -> {
                                protectionType = "No protection detected"
                                bypassPossible = "N/A"
                            }
                        }
                    }
                } catch (_: Exception) {
                    details.add("Could not read lock/CC bytes")
                    protectionType = "Unknown (read error)"
                    bypassPossible = "Maybe"
                }
            } catch (e: TagLostException) {
                details.add("Tag removed during scan")
                protectionType = "Unknown (tag lost)"
                bypassPossible = "Maybe"
            } catch (e: Exception) {
                details.add("NfcA error: ${e.message}")
                protectionType = "Unknown (error)"
                bypassPossible = "Maybe"
            } finally {
                try { nfcA.close() } catch (_: Exception) {}
            }
            return WriteProtectionResult(tagType, protectionType, bypassPossible, details)
        }
    }

    // ── Mifare Classic ──
    if ("MifareClassic" in techList) {
        val mfc = MifareClassic.get(tag)
        if (mfc != null) {
            try {
                mfc.connect()
                tagType = "Mifare Classic ${mfc.size / 1024}K"
                details.add("Tag type: $tagType (${mfc.sectorCount} sectors)")
                var accessibleSectors = 0
                var testedSectors = 0
                for (sector in 0 until minOf(mfc.sectorCount, 16)) {
                    testedSectors++
                    val accessible = MIFARE_DEFAULT_KEYS.any { key ->
                        try { mfc.authenticateSectorWithKeyA(sector, key) } catch (_: Exception) { false } ||
                        try { mfc.authenticateSectorWithKeyB(sector, key) } catch (_: Exception) { false }
                    }
                    if (accessible) accessibleSectors++
                }
                details.add("Sectors accessible with default keys: $accessibleSectors/$testedSectors")
                when {
                    accessibleSectors == testedSectors -> {
                        protectionType = "Default keys (all sectors accessible)"
                        bypassPossible = "Yes"
                    }
                    accessibleSectors > 0 -> {
                        protectionType = "Partial key protection"
                        bypassPossible = "Maybe"
                    }
                    else -> {
                        protectionType = "Custom keys on all sectors"
                        bypassPossible = "No"
                    }
                }
            } catch (e: TagLostException) {
                details.add("Tag removed during scan")
                protectionType = "Unknown (tag lost)"
                bypassPossible = "Maybe"
            } catch (e: Exception) {
                details.add("Mifare Classic error: ${e.message}")
                protectionType = "Unknown (error)"
                bypassPossible = "Maybe"
            } finally {
                try { mfc.close() } catch (_: Exception) {}
            }
            return WriteProtectionResult(tagType, protectionType, bypassPossible, details)
        }
    }

    // ── ISO-DEP / Type 4 ──
    if ("IsoDep" in techList) {
        val isoDep = IsoDep.get(tag)
        if (isoDep != null) {
            try {
                isoDep.connect()
                isoDep.timeout = 2000
                tagType = "ISO-DEP (Type 4)"
                details.add("Tag type: $tagType")
                // Select NDEF application
                val selectApp = isoDep.transceive(byteArrayOf(
                    0x00, 0xA4.toByte(), 0x04, 0x00, 0x07,
                    0xD2.toByte(), 0x76, 0x00, 0x00, 0x85.toByte(), 0x01, 0x01, 0x00
                ))
                if (selectApp.size >= 2 && selectApp[selectApp.size - 2] == 0x90.toByte()) {
                    // Select CC file
                    val selectCC = isoDep.transceive(byteArrayOf(0x00, 0xA4.toByte(), 0x00, 0x0C, 0x02, 0xE1.toByte(), 0x03))
                    if (selectCC.size >= 2 && selectCC[selectCC.size - 2] == 0x90.toByte()) {
                        val readCC = isoDep.transceive(byteArrayOf(0x00, 0xB0.toByte(), 0x00, 0x00, 0x0F))
                        if (readCC.size >= 16) {
                            val writeAccess = readCC[7].toInt() and 0xFF
                            details.add("CC write access byte: 0x${"%02X".format(writeAccess)}${if (writeAccess == 0xFF) " (read-only)" else if (writeAccess == 0x00) " (writable)" else " (proprietary)"}")
                            when (writeAccess) {
                                0xFF -> {
                                    protectionType = "Type 4 CC read-only"
                                    bypassPossible = "No"
                                }
                                0x00 -> {
                                    protectionType = "No protection detected"
                                    bypassPossible = "N/A"
                                }
                                else -> {
                                    protectionType = "Type 4 proprietary access control"
                                    bypassPossible = "No"
                                }
                            }
                        }
                    }
                } else {
                    details.add("NDEF application not found")
                    protectionType = "No NDEF application"
                    bypassPossible = "No"
                }
            } catch (e: Exception) {
                details.add("ISO-DEP error: ${e.message}")
                protectionType = "Unknown (error)"
                bypassPossible = "Maybe"
            } finally {
                try { isoDep.close() } catch (_: Exception) {}
            }
            return WriteProtectionResult(tagType, protectionType, bypassPossible, details)
        }
    }

    return WriteProtectionResult(null, "Unknown tag type", "No", listOf("No supported low-level technology detected"))
}

/** Attempts to bypass write protection and write the NDEF record. */
private fun attemptBypassAndWrite(tag: Tag, record: NdefRecord, onLog: (String) -> Unit): String {
    val message = NdefMessage(arrayOf(record))
    val techList = tag.techList.map { it.substringAfterLast('.') }

    // ── Attempt 1: CC byte 3 reset (NTAG/Ultralight) ──
    if ("NfcA" in techList) {
        onLog("Attempt 1: CC byte reset...")
        val ccResult = attemptCCByteReset(tag, onLog)
        if (ccResult) {
            // Try NDEF write after CC reset
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                try {
                    ndef.connect()
                    ndef.writeNdefMessage(message)
                    ndef.close()
                    onLog("NDEF write succeeded after CC byte reset")
                    return "OK Bypass successful (CC byte reset) — ${message.toByteArray().size} bytes written"
                } catch (e: Exception) {
                    onLog("NDEF write still failed after CC reset: ${e.message}")
                    try { ndef.close() } catch (_: Exception) {}
                }
            }
        }

        // ── Attempt 2: Password authentication (NTAG21x) ──
        onLog("Attempt 2: Password authentication...")
        val pwdResult = attemptPasswordBypass(tag, message, onLog)
        if (pwdResult != null) return pwdResult
    }

    // ── Attempt 3: Mifare Classic default keys ──
    if ("MifareClassic" in techList) {
        onLog("Attempt 3: Mifare Classic default keys...")
        val mfcResult = attemptMifareClassicWrite(tag, message, onLog)
        if (mfcResult != null) return mfcResult
    }

    // ── Attempt 4: Check lock bits and report ──
    if ("NfcA" in techList) {
        onLog("Checking lock bit state...")
        val nfcA = NfcA.get(tag)
        if (nfcA != null) {
            try {
                nfcA.connect()
                nfcA.timeout = 1000
                val pages2to5 = nfcA.transceive(byteArrayOf(0x30, 0x02))
                if (pages2to5.size >= 4) {
                    val lock0 = pages2to5[2].toInt() and 0xFF
                    val lock1 = pages2to5[3].toInt() and 0xFF
                    if (lock0 != 0 || lock1 != 0) {
                        onLog("Static lock bits set (0x${"%02X".format(lock0)}${"%02X".format(lock1)}) — IRREVERSIBLE")
                    }
                }
            } catch (e: Exception) {
                onLog("Lock bit read error: ${e.message}")
            } finally {
                try { nfcA.close() } catch (_: Exception) {}
            }
        }
    }

    onLog("All bypass attempts exhausted")
    return "Write protection could not be bypassed"
}

/** Attempts to clear CC byte 3 to remove NDEF read-only flag. Returns true if the write succeeded. */
private fun attemptCCByteReset(tag: Tag, onLog: (String) -> Unit): Boolean {
    val nfcA = NfcA.get(tag) ?: return false
    try {
        nfcA.connect()
        nfcA.timeout = 1000
        // Read page 3 (CC bytes) — READ command returns pages 3-6
        val pages = nfcA.transceive(byteArrayOf(0x30, 0x03))
        if (pages.size < 4) {
            onLog("  CC read returned too few bytes")
            return false
        }
        val cc0 = pages[0]
        val cc1 = pages[1]
        val cc2 = pages[2]
        val cc3 = pages[3].toInt() and 0xFF
        if (cc3 and 0x0F == 0) {
            onLog("  CC byte 3 already clear (0x${"%02X".format(cc3)}), skipping")
            return false
        }
        onLog("  CC byte 3 = 0x${"%02X".format(cc3)}, clearing read-only bits...")
        val newCc3 = (cc3 and 0xF0).toByte()  // Clear lower nibble (access bits)
        nfcA.transceive(byteArrayOf(0xA2.toByte(), 0x03, cc0, cc1, cc2, newCc3))
        onLog("  CC byte 3 reset to 0x${"%02X".format(newCc3.toInt() and 0xFF)}")
        return true
    } catch (e: TagLostException) {
        onLog("  Tag removed during CC byte reset")
        return false
    } catch (e: Exception) {
        onLog("  CC byte reset failed: ${e.message}")
        return false
    } finally {
        try { nfcA.close() } catch (_: Exception) {}
    }
}

/** Tries default passwords on NTAG21x, then attempts write. Returns status string on success, null on failure. */
private fun attemptPasswordBypass(tag: Tag, message: NdefMessage, onLog: (String) -> Unit): String? {
    val nfcA = NfcA.get(tag) ?: return null
    try {
        nfcA.connect()
        nfcA.timeout = 1000

        // Identify tag to find config page
        val configPage: Int
        val totalPages: Int
        try {
            val version = nfcA.transceive(byteArrayOf(0x60))
            val info = identifyNtagType(version)
            if (info == null || info.second < 0) {
                onLog("  Not an NTAG21x, skipping password auth")
                return null
            }
            configPage = info.second
            totalPages = info.third
        } catch (_: Exception) {
            onLog("  GET_VERSION failed, skipping password auth")
            return null
        }

        // Check if password protection is active
        try {
            val cfgPages = nfcA.transceive(byteArrayOf(0x30, configPage.toByte()))
            if (cfgPages.size >= 4) {
                val auth0 = cfgPages[3].toInt() and 0xFF
                if (auth0 >= totalPages) {
                    onLog("  AUTH0=0x${"%02X".format(auth0)} (not active), skipping")
                    return null
                }
                onLog("  AUTH0=0x${"%02X".format(auth0)}, password protection active from page $auth0")
            }
        } catch (_: Exception) {
            onLog("  Config read failed, trying passwords anyway...")
        }

        // Try each default password
        for (pwd in NTAG_DEFAULT_PASSWORDS) {
            try {
                // Need to reconnect for each attempt since failed auth may close connection
                if (!nfcA.isConnected) {
                    nfcA.connect()
                    nfcA.timeout = 1000
                }
                val authCmd = byteArrayOf(0x1B, pwd[0], pwd[1], pwd[2], pwd[3])
                val response = nfcA.transceive(authCmd)
                if (response.size >= 2) {
                    // PACK received — authentication succeeded
                    val pwdHex = pwd.joinToString("") { "%02X".format(it) }
                    onLog("  Password 0x$pwdHex accepted (PACK: ${response.joinToString("") { "%02X".format(it) }})")

                    // Try to disable password protection by setting AUTH0 to max
                    try {
                        val cfgPages = nfcA.transceive(byteArrayOf(0x30, configPage.toByte()))
                        if (cfgPages.size >= 4) {
                            nfcA.transceive(byteArrayOf(0xA2.toByte(), configPage.toByte(), cfgPages[0], cfgPages[1], cfgPages[2], 0xFF.toByte()))
                            onLog("  AUTH0 reset to 0xFF (password protection disabled)")
                        }
                    } catch (e: Exception) {
                        onLog("  Could not disable password protection: ${e.message}")
                    }
                    nfcA.close()

                    // Now try NDEF write
                    val ndef = Ndef.get(tag)
                    if (ndef != null) {
                        try {
                            ndef.connect()
                            ndef.writeNdefMessage(message)
                            ndef.close()
                            onLog("  NDEF write succeeded after password auth")
                            return "OK Bypass successful (password auth) — ${message.toByteArray().size} bytes written"
                        } catch (e: Exception) {
                            onLog("  NDEF write failed after auth: ${e.message}")
                            try { ndef.close() } catch (_: Exception) {}
                        }
                    }
                    return null  // Auth worked but write still failed
                }
            } catch (_: Exception) {
                // Password rejected or connection error, try next
                try { if (nfcA.isConnected) nfcA.close() } catch (_: Exception) {}
                try {
                    val freshNfcA = NfcA.get(tag)
                    if (freshNfcA != null) {
                        // Re-get won't help since nfcA is the same object; just reconnect
                    }
                } catch (_: Exception) {}
            }
        }
        onLog("  No default password worked")
        return null
    } catch (e: TagLostException) {
        onLog("  Tag removed during password auth")
        return null
    } catch (e: Exception) {
        onLog("  Password auth error: ${e.message}")
        return null
    } finally {
        try { nfcA.close() } catch (_: Exception) {}
    }
}

/** Tries default keys on Mifare Classic sectors and attempts NDEF write. Returns status or null. */
private fun attemptMifareClassicWrite(tag: Tag, message: NdefMessage, onLog: (String) -> Unit): String? {
    // First try the normal NDEF approach with a fresh connection
    // (Mifare Classic NDEF write works if sector keys are defaults)
    val ndef = Ndef.get(tag)
    if (ndef != null) {
        try {
            ndef.connect()
            ndef.writeNdefMessage(message)
            ndef.close()
            onLog("  NDEF write succeeded (default key access)")
            return "OK Written via Mifare Classic NDEF — ${message.toByteArray().size} bytes"
        } catch (e: Exception) {
            onLog("  NDEF write failed: ${e.message}")
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    // Try authenticating individual sectors with default keys
    val mfc = MifareClassic.get(tag) ?: return null
    try {
        mfc.connect()
        var anyKeyWorked = false
        for (sector in 0 until minOf(mfc.sectorCount, 16)) {
            for (key in MIFARE_DEFAULT_KEYS) {
                val authed = try { mfc.authenticateSectorWithKeyA(sector, key) } catch (_: Exception) { false } ||
                             try { mfc.authenticateSectorWithKeyB(sector, key) } catch (_: Exception) { false }
                if (authed) {
                    anyKeyWorked = true
                    break
                }
            }
        }
        if (!anyKeyWorked) {
            onLog("  No default key worked on any sector")
            return null
        }
        onLog("  Default keys work but low-level Mifare Classic NDEF write not supported in bypass")
        return null
    } catch (e: Exception) {
        onLog("  Mifare Classic error: ${e.message}")
        return null
    } finally {
        try { mfc.close() } catch (_: Exception) {}
    }
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
