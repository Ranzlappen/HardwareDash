package com.gadget.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.gadget.gps.spoof.SpoofCapabilities
import com.gadget.gps.spoof.SpoofConfig
import com.gadget.gps.spoof.SpoofResult
import com.gadget.gps.spoof.SpoofState
import com.gadget.gps.spoof.Waypoint
import com.gadget.localization.S
import com.gadget.root.ui.rememberRootFeatures
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

/**
 * GPS spoofing screen — single source of UI for both flavors. The screen
 * self-degrades for the standard flavor: rooted-only buttons (LSPosed
 * install) only render when [SpoofCapabilities.rootGranted] is true.
 */
@Composable
fun GpsSpoofingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entryPoint = rememberRootFeatures()
    val controller = remember(entryPoint) { entryPoint.gpsSpoofController() }
    val scope = rememberCoroutineScope()

    // Pre-resolve every Spoof string we use in non-Composable callbacks. The
    // S.spoof.* accessors are @Composable; resolving them once at the top
    // of the composition and capturing the plain Strings keeps them usable
    // from coroutines and onClick handlers.
    val str = SpoofStrings.from(S.spoof)

    var capabilities by remember { mutableStateOf(SpoofCapabilities()) }
    val state by controller.state.collectAsStateLocal()

    var legalAck by remember { mutableStateOf(false) }
    var legalChecked by remember { mutableStateOf(false) }
    var legalModalVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        capabilities = controller.capabilities()
        legalAck = controller.isLegalAcknowledged()
    }

    var selectedTab by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }

    // Static-tab inputs.
    var staticLat by remember { mutableStateOf("37.7749") }
    var staticLon by remember { mutableStateOf("-122.4194") }
    var staticAlt by remember { mutableStateOf("0.0") }
    var staticAcc by remember { mutableStateOf("5") }
    var staticBearing by remember { mutableStateOf("0") }
    var staticSpeed by remember { mutableStateOf("0") }

    var gpxUri by remember { mutableStateOf<Uri?>(null) }
    var kmlUri by remember { mutableStateOf<Uri?>(null) }
    var playbackSpeed by remember { mutableStateOf(1f) }
    var loop by remember { mutableStateOf(false) }

    val gpxPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
            }
            gpxUri = it
        }
    }
    val kmlPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {
        if (it != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            } catch (_: SecurityException) {
            }
            kmlUri = it
        }
    }

    var routeText by remember {
        mutableStateOf("37.7749, -122.4194\n37.7849, -122.4094\n37.7949, -122.3994")
    }
    var routeDefaultSpeed by remember { mutableStateOf("5") }
    var routeInterpolation by remember { mutableStateOf(SpoofConfig.Route.Interpolation.Linear) }

    fun runWithStatus(label: String, action: suspend () -> SpoofResult) {
        scope.launch {
            status = "$label…"
            val r = action()
            status = describe(r, fallback = label, str = str)
            capabilities = controller.capabilities()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Spacer(Modifier.width(8.dp))
            Text(str.title, style = MaterialTheme.typography.headlineSmall)
        }

        CapabilityCard(capabilities, str)

        val s = state
        if (s is SpoofState.Running) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "${str.running} — ${s.sourceLabel}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("%.5f, %.5f".format(s.currentLat, s.currentLon))
                    Text("${str.activeMode}: ${s.activeModes.joinToString { it.name }}")
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { runWithStatus(str.stop) { controller.stop() } },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) { Text(str.stop) }
                }
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            listOf(str.tabStatic, str.tabGpx, str.tabKml, str.tabRoute)
                .forEachIndexed { i, label ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = { Text(label) },
                    )
                }
        }

        when (selectedTab) {
            0 -> StaticTab(
                str = str,
                lat = staticLat, onLat = { staticLat = it },
                lon = staticLon, onLon = { staticLon = it },
                alt = staticAlt, onAlt = { staticAlt = it },
                acc = staticAcc, onAcc = { staticAcc = it },
                bearing = staticBearing, onBearing = { staticBearing = it },
                speed = staticSpeed, onSpeed = { staticSpeed = it },
                onStart = onStart@{
                    val cfg = SpoofConfig.Static(
                        lat = staticLat.toDoubleOrNull() ?: return@onStart,
                        lon = staticLon.toDoubleOrNull() ?: return@onStart,
                        alt = staticAlt.toDoubleOrNull() ?: 0.0,
                        accuracy = staticAcc.toFloatOrNull() ?: 5f,
                        bearing = staticBearing.toFloatOrNull() ?: 0f,
                        speed = staticSpeed.toFloatOrNull() ?: 0f,
                    )
                    if (!legalAck) {
                        legalModalVisible = true
                        return@onStart
                    }
                    runWithStatus(str.start) { controller.start(cfg) }
                },
                onStop = { runWithStatus(str.stop) { controller.stop() } },
            )
            1 -> PlaybackTab(
                str = str,
                uri = gpxUri,
                pickFile = { gpxPicker.launch(arrayOf("application/gpx+xml", "*/*")) },
                playbackSpeed = playbackSpeed,
                onPlaybackSpeed = { playbackSpeed = it },
                loop = loop, onLoop = { loop = it },
                onStart = onStart@{
                    val u = gpxUri ?: return@onStart
                    val cfg = SpoofConfig.GpxPlayback(
                        source = u,
                        speedMultiplier = playbackSpeed,
                        loop = loop,
                    )
                    if (!legalAck) {
                        legalModalVisible = true
                        return@onStart
                    }
                    runWithStatus(str.start) { controller.start(cfg) }
                },
                onStop = { runWithStatus(str.stop) { controller.stop() } },
            )
            2 -> PlaybackTab(
                str = str,
                uri = kmlUri,
                pickFile = { kmlPicker.launch(arrayOf("application/vnd.google-earth.kml+xml", "*/*")) },
                playbackSpeed = playbackSpeed,
                onPlaybackSpeed = { playbackSpeed = it },
                loop = loop, onLoop = { loop = it },
                onStart = onStart@{
                    val u = kmlUri ?: return@onStart
                    val cfg = SpoofConfig.KmlPlayback(
                        source = u,
                        speedMultiplier = playbackSpeed,
                        loop = loop,
                    )
                    if (!legalAck) {
                        legalModalVisible = true
                        return@onStart
                    }
                    runWithStatus(str.start) { controller.start(cfg) }
                },
                onStop = { runWithStatus(str.stop) { controller.stop() } },
            )
            3 -> RouteTab(
                str = str,
                routeText = routeText, onRouteText = { routeText = it },
                defaultSpeed = routeDefaultSpeed, onDefaultSpeed = { routeDefaultSpeed = it },
                interpolation = routeInterpolation, onInterpolation = { routeInterpolation = it },
                loop = loop, onLoop = { loop = it },
                onStart = onStart@{
                    val pts = parseWaypoints(routeText)
                    if (pts.size < 2) {
                        status = str.needsAtLeastTwo
                        return@onStart
                    }
                    val cfg = SpoofConfig.Route(
                        waypoints = pts,
                        interpolation = routeInterpolation,
                        defaultSpeedMps = routeDefaultSpeed.toFloatOrNull() ?: 5f,
                        loop = loop,
                    )
                    if (!legalAck) {
                        legalModalVisible = true
                        return@onStart
                    }
                    runWithStatus(str.start) { controller.start(cfg) }
                },
                onStop = { runWithStatus(str.stop) { controller.stop() } },
            )
        }

        if (capabilities.rootGranted &&
            (capabilities.lsposedFrameworkActive || capabilities.lsposedModuleInstalled)
        ) {
            LsposedCard(
                str = str,
                caps = capabilities,
                onInstall = { runWithStatus(str.installLsposed) { controller.installLsposedModule() } },
                onUninstall = {
                    runWithStatus(str.uninstallLsposed) { controller.uninstallLsposedModule() }
                },
            )
        }

        DetectionMatrix(capabilities, str)

        status?.let {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(it, modifier = Modifier.padding(12.dp))
            }
        }

        if (capabilities.perApiCaveats.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    capabilities.perApiCaveats.forEach { Text("• $it") }
                }
            }
        }

        if (!capabilities.mockLocationAppSelected && !capabilities.rootGranted) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            ) { Text(str.openDevOptions) }
            Text(str.needsMockApp, style = MaterialTheme.typography.bodySmall)
        }
    }

    if (legalModalVisible) {
        AlertDialog(
            onDismissRequest = { legalModalVisible = false },
            title = { Text(str.legalTitle) },
            text = {
                Column {
                    Text(str.legalBody)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = legalChecked, onCheckedChange = { legalChecked = it })
                        Text(str.legalAccept)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = legalChecked,
                    onClick = {
                        scope.launch {
                            controller.acknowledgeLegal()
                            legalAck = true
                            legalModalVisible = false
                        }
                    },
                ) { Text(str.legalAccept) }
            },
            dismissButton = {
                OutlinedButton(onClick = { legalModalVisible = false }) { Text(str.legalCancel) }
            },
        )
    }
}

// ─── Capability card ──────────────────────────────────────────────────────────

@Composable
private fun CapabilityCard(caps: SpoofCapabilities, str: SpoofStrings) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(str.capabilities, style = MaterialTheme.typography.titleMedium)
            CapabilityRow(str.rootRow, caps.rootGranted)
            CapabilityRow(str.mockLocationAppRow, caps.mockLocationAppSelected)
            if (caps.competingMockLocationAppActive) {
                Text(
                    "${str.competingMockApp}: ${caps.competingMockLocationAppPackage ?: "?"}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            CapabilityRow(str.lsposedFrameworkRow, caps.lsposedFrameworkActive)
            val moduleStateLabel = when {
                caps.lsposedModuleLoaded -> str.lsposedLoaded
                caps.lsposedModuleInstalled -> str.lsposedInstalledNotLoaded
                else -> str.lsposedNotInstalled
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(str.lsposedModuleRow, style = MaterialTheme.typography.bodyMedium)
                Text(moduleStateLabel, style = MaterialTheme.typography.bodyMedium)
            }
            if (caps.lsposedBundledVersion > 0 && caps.lsposedBundledVersion > caps.lsposedInstalledVersion) {
                Text(
                    "Update available: bundled v${caps.lsposedBundledVersion}, installed v${caps.lsposedInstalledVersion}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
    }
}

@Composable
private fun CapabilityRow(label: String, value: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(if (value) "✓" else "✗")
    }
}

// ─── Tabs ─────────────────────────────────────────────────────────────────────

@Composable
private fun StaticTab(
    str: SpoofStrings,
    lat: String, onLat: (String) -> Unit,
    lon: String, onLon: (String) -> Unit,
    alt: String, onAlt: (String) -> Unit,
    acc: String, onAcc: (String) -> Unit,
    bearing: String, onBearing: (String) -> Unit,
    speed: String, onSpeed: (String) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = lat, onValueChange = onLat, label = { Text(str.latitude) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = lon, onValueChange = onLon, label = { Text(str.longitude) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = alt, onValueChange = onAlt, label = { Text(str.altitude) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = acc, onValueChange = onAcc, label = { Text(str.accuracy) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bearing, onValueChange = onBearing, label = { Text(str.bearing) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = speed, onValueChange = onSpeed, label = { Text(str.speed) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text(str.start) }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text(str.stop) }
            }
        }
    }
}

@Composable
private fun PlaybackTab(
    str: SpoofStrings,
    uri: Uri?,
    pickFile: () -> Unit,
    playbackSpeed: Float,
    onPlaybackSpeed: (Float) -> Unit,
    loop: Boolean,
    onLoop: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = pickFile) { Text(str.pickFile) }
                Spacer(Modifier.width(8.dp))
                Text(uri?.lastPathSegment ?: "—")
            }
            Text("${str.speedMultiplier}: %.2fx".format(playbackSpeed))
            Slider(value = playbackSpeed, onValueChange = onPlaybackSpeed, valueRange = 0.25f..10f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = loop, onCheckedChange = onLoop)
                Spacer(Modifier.width(8.dp))
                Text(str.loop)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStart, enabled = uri != null, modifier = Modifier.weight(1f)) { Text(str.start) }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text(str.stop) }
            }
        }
    }
}

@Composable
private fun RouteTab(
    str: SpoofStrings,
    routeText: String, onRouteText: (String) -> Unit,
    defaultSpeed: String, onDefaultSpeed: (String) -> Unit,
    interpolation: SpoofConfig.Route.Interpolation,
    onInterpolation: (SpoofConfig.Route.Interpolation) -> Unit,
    loop: Boolean, onLoop: (Boolean) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val waypoints = remember(routeText) { parseWaypoints(routeText) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (waypoints.size >= 2) {
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            Configuration.getInstance().userAgentValue = ctx.packageName
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                controller.setZoom(13.0)
                                renderRoute(this, waypoints)
                            }
                        },
                        update = { renderRoute(it, waypoints) },
                    )
                }
            }
            OutlinedTextField(
                value = routeText,
                onValueChange = onRouteText,
                label = { Text(str.routeWaypointsLabel) },
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
            OutlinedTextField(
                value = defaultSpeed,
                onValueChange = onDefaultSpeed,
                label = { Text(str.speed) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onInterpolation(SpoofConfig.Route.Interpolation.Linear) },
                    enabled = interpolation != SpoofConfig.Route.Interpolation.Linear,
                ) { Text("Linear") }
                OutlinedButton(
                    onClick = { onInterpolation(SpoofConfig.Route.Interpolation.Cubic) },
                    enabled = interpolation != SpoofConfig.Route.Interpolation.Cubic,
                ) { Text("Cubic") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = loop, onCheckedChange = onLoop)
                Spacer(Modifier.width(8.dp))
                Text(str.loop)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onStart, modifier = Modifier.weight(1f)) { Text(str.start) }
                OutlinedButton(onClick = onStop, modifier = Modifier.weight(1f)) { Text(str.stop) }
            }
        }
    }
}

private fun renderRoute(map: MapView, waypoints: List<Waypoint>) {
    map.overlays.clear()
    if (waypoints.isEmpty()) return
    val pts = waypoints.map { GeoPoint(it.lat, it.lon) }
    val poly = Polyline().apply { setPoints(pts) }
    map.overlays.add(poly)
    pts.forEachIndexed { i, p ->
        val m = Marker(map)
        m.position = p
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        m.title = "${i + 1}"
        map.overlays.add(m)
    }
    map.controller.setCenter(pts.first())
    map.invalidate()
}

private fun parseWaypoints(text: String): List<Waypoint> =
    text.lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .mapNotNull { line ->
            val parts = line.split(',', ';').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size < 2) return@mapNotNull null
            val lat = parts[0].toDoubleOrNull() ?: return@mapNotNull null
            val lon = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val alt = if (parts.size >= 3) parts[2].toDoubleOrNull() else null
            Waypoint(lat = lat, lon = lon, alt = alt)
        }
        .toList()

// ─── LSPosed management ───────────────────────────────────────────────────────

@Composable
private fun LsposedCard(
    str: SpoofStrings,
    caps: SpoofCapabilities,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(str.lsposedModuleRow, style = MaterialTheme.typography.titleMedium)
            if (caps.lsposedModuleInstalled) {
                if (!caps.lsposedModuleLoaded) {
                    Text(str.lsposedAfterInstall, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) {
                    Text(str.uninstallLsposed)
                }
            }
            if (!caps.lsposedModuleInstalled || caps.lsposedBundledVersion > caps.lsposedInstalledVersion) {
                Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) {
                    Text(str.installLsposed)
                }
            }
        }
    }
}

// ─── Detection matrix ─────────────────────────────────────────────────────────

@Composable
private fun DetectionMatrix(caps: SpoofCapabilities, str: SpoofStrings) {
    val lspBypass = caps.lsposedModuleLoaded
    val testProvider = caps.mockLocationAppSelected
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(str.matrixHeader, style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            MatrixRow(str.matrixIsFromMock, lspBypass, str)
            MatrixRow(str.matrixIsMock, lspBypass, str)
            MatrixRow(str.matrixAppOps, lspBypass, str)
            MatrixRow(str.matrixSettingsSecure, lspBypass, str)
            MatrixRow(str.matrixGnssStatus, lspBypass, str)
            MatrixRow(str.matrixFused, lspBypass, str)
            MatrixRow(str.matrixSensorFusion, false, str)
            MatrixRow(str.matrixWifiBssid, false, str)
            MatrixRow(str.matrixCellTower, false, str)
            if (!testProvider) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "(rows above are best-effort; until the Mock Location grant is in place, no test-provider value is emitted)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MatrixRow(label: String, bypassed: Boolean, str: SpoofStrings) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            if (bypassed) str.matrixBypassed else str.matrixDetected,
            style = MaterialTheme.typography.bodySmall,
            color = if (bypassed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
        )
    }
}

// ─── Helpers (non-Composable) ────────────────────────────────────────────────

private fun describe(result: SpoofResult, fallback: String, str: SpoofStrings): String =
    when (result) {
        SpoofResult.Ok -> "$fallback ✓"
        is SpoofResult.Unsupported -> "${str.statusFailed}: ${result.reason}"
        is SpoofResult.Blocked -> str.statusBlocked
        is SpoofResult.Failed -> "${str.statusFailed}: ${result.message}"
        SpoofResult.LegalNotAcknowledged -> str.statusLegalNotAcknowledged
    }

@Composable
private fun <T> StateFlow<T>.collectAsStateLocal(): State<T> {
    val state = remember { mutableStateOf(this.value) }
    LaunchedEffect(this) {
        this@collectAsStateLocal.collectLatest { state.value = it }
    }
    return state
}

/**
 * Snapshot of every Spoof string we use, captured once at the top of the
 * screen so non-@Composable lambdas (button onClick handlers, coroutines)
 * can reference plain Strings without re-entering Composable scope.
 */
private data class SpoofStrings(
    val title: String, val capabilities: String,
    val rootRow: String, val mockLocationAppRow: String, val competingMockApp: String,
    val lsposedFrameworkRow: String, val lsposedModuleRow: String,
    val lsposedNotInstalled: String, val lsposedInstalledNotLoaded: String, val lsposedLoaded: String,
    val installLsposed: String, val uninstallLsposed: String, val lsposedAfterInstall: String,
    val tabStatic: String, val tabGpx: String, val tabKml: String, val tabRoute: String,
    val latitude: String, val longitude: String, val altitude: String, val accuracy: String,
    val bearing: String, val speed: String, val pickFile: String, val speedMultiplier: String,
    val loop: String, val start: String, val stop: String, val running: String, val activeMode: String,
    val openDevOptions: String, val needsMockApp: String,
    val matrixHeader: String, val matrixDetected: String, val matrixBypassed: String,
    val matrixIsFromMock: String, val matrixIsMock: String, val matrixAppOps: String,
    val matrixSettingsSecure: String, val matrixGnssStatus: String, val matrixFused: String,
    val matrixSensorFusion: String, val matrixWifiBssid: String, val matrixCellTower: String,
    val legalTitle: String, val legalBody: String, val legalAccept: String, val legalCancel: String,
    val routeWaypointsLabel: String, val needsAtLeastTwo: String,
    val statusLegalNotAcknowledged: String, val statusBlocked: String, val statusFailed: String,
) {
    companion object {
        fun from(s: S.Spoof): SpoofStrings = SpoofStrings(
            title = s.title, capabilities = s.capabilities,
            rootRow = s.rootRow, mockLocationAppRow = s.mockLocationAppRow,
            competingMockApp = s.competingMockApp,
            lsposedFrameworkRow = s.lsposedFrameworkRow, lsposedModuleRow = s.lsposedModuleRow,
            lsposedNotInstalled = s.lsposedNotInstalled,
            lsposedInstalledNotLoaded = s.lsposedInstalledNotLoaded, lsposedLoaded = s.lsposedLoaded,
            installLsposed = s.installLsposed, uninstallLsposed = s.uninstallLsposed,
            lsposedAfterInstall = s.lsposedAfterInstall,
            tabStatic = s.tabStatic, tabGpx = s.tabGpx, tabKml = s.tabKml, tabRoute = s.tabRoute,
            latitude = s.latitude, longitude = s.longitude, altitude = s.altitude,
            accuracy = s.accuracy, bearing = s.bearing, speed = s.speed,
            pickFile = s.pickFile, speedMultiplier = s.speedMultiplier, loop = s.loop,
            start = s.start, stop = s.stop, running = s.running, activeMode = s.activeMode,
            openDevOptions = s.openDevOptions, needsMockApp = s.needsMockApp,
            matrixHeader = s.matrixHeader, matrixDetected = s.matrixDetected,
            matrixBypassed = s.matrixBypassed,
            matrixIsFromMock = s.matrixIsFromMock, matrixIsMock = s.matrixIsMock,
            matrixAppOps = s.matrixAppOps, matrixSettingsSecure = s.matrixSettingsSecure,
            matrixGnssStatus = s.matrixGnssStatus, matrixFused = s.matrixFused,
            matrixSensorFusion = s.matrixSensorFusion, matrixWifiBssid = s.matrixWifiBssid,
            matrixCellTower = s.matrixCellTower,
            legalTitle = s.legalTitle, legalBody = s.legalBody, legalAccept = s.legalAccept,
            legalCancel = s.legalCancel,
            routeWaypointsLabel = s.routeWaypointsLabel, needsAtLeastTwo = s.needsAtLeastTwo,
            statusLegalNotAcknowledged = s.statusLegalNotAcknowledged,
            statusBlocked = s.statusBlocked, statusFailed = s.statusFailed,
        )
    }
}
