package dev.ranzlappen.gadget.feature.gps

import android.Manifest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun GpsScreen(
    modifier: Modifier = Modifier,
    viewModel: GpsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) viewModel.onPermissionGranted()
        else viewModel.onPermissionRevoked()
    }

    GpsScreenContent(
        state = state,
        moduleInfo = gpsModuleInfo(
            locationGranted = permissionState.status.isGranted,
            hasLocation = state.hasLocation,
        ),
        onRequestPermission = { permissionState.launchPermissionRequest() },
        modifier = modifier,
        liveMonitors = {
            LiveMonitorContainer(
                metricKey = GpsSpeedMetricSource.METRIC_KEY,
                title = stringResource(R.string.gps_live_monitor_speed),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "gps_live_speed",
            )
            LiveMonitorContainer(
                metricKey = GpsAltitudeMetricSource.METRIC_KEY,
                title = stringResource(R.string.gps_live_monitor_altitude),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "gps_live_altitude",
            )
        },
        monitors = {
            MonitorContainer(
                metricKey = GpsSpeedMetricSource.METRIC_KEY,
                title = stringResource(R.string.gps_monitor_speed),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "gps_history_speed",
            )
            MonitorContainer(
                metricKey = GpsAltitudeMetricSource.METRIC_KEY,
                title = stringResource(R.string.gps_monitor_altitude),
                modifier = Modifier.fillMaxWidth(),
                collapseId = "gps_history_altitude",
            )
        },
    )
}

@Composable
private fun gpsModuleInfo(
    locationGranted: Boolean,
    hasLocation: Boolean,
): ModuleInfo = ModuleInfo(
    compatibility = OsCompatibility(minSdk = 21),
    permissions = listOf(
        ModulePermission(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            label = stringResource(R.string.gps_permission_label),
            rationale = stringResource(R.string.gps_permission_rationale),
        ),
    ),
    capabilities = listOf(
        ModuleCapability(
            name = stringResource(R.string.gps_cap_location_name),
            detail = stringResource(R.string.gps_cap_location_detail),
            status = {
                CapabilityStatus(
                    kind = when {
                        !locationGranted -> GadgetStatusKind.Warning
                        hasLocation -> GadgetStatusKind.Success
                        else -> GadgetStatusKind.Warning
                    },
                    message = when {
                        !locationGranted -> stringResource(R.string.gps_cap_no_permission)
                        hasLocation -> stringResource(R.string.gps_cap_available)
                        else -> stringResource(R.string.gps_cap_searching)
                    },
                )
            },
        ),
    ),
)

@Composable
internal fun GpsScreenContent(
    state: GpsState,
    moduleInfo: ModuleInfo?,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
    liveMonitors: @Composable () -> Unit = {},
    monitors: @Composable () -> Unit = {},
) {
    val spacing = LocalGadgetTheme.current.spacing
    ModuleScreenScaffold(
        title = stringResource(R.string.gps_screen_title),
        modifier = modifier,
        moduleInfo = moduleInfo,
        functional = {
            if (!state.permissionGranted) {
                GpsPermissionCard(onRequestPermission = onRequestPermission)
            } else {
                if (state.hasLocation) {
                    GpsMapCard(state = state)
                }
                GpsCoordinatesCard(state = state)
                liveMonitors()
                monitors()
            }
        },
    )
}

@Composable
private fun GpsPermissionCard(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.gps_permission_card_title),
    ) {
        Text(
            text = stringResource(R.string.gps_permission_card_body),
            style = MaterialTheme.typography.bodyMedium,
        )
        GadgetPrimaryButton(
            onClick = onRequestPermission,
            text = stringResource(R.string.gps_permission_grant_button),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun GpsMapCard(
    state: GpsState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.gps_card_map_title),
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            update = { view ->
                val center = GeoPoint(state.latitude, state.longitude)
                view.controller.setCenter(center)
                view.overlays.clear()
                val marker = Marker(view).apply {
                    position = center
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    title = "%.5f, %.5f".format(state.latitude, state.longitude)
                }
                view.overlays.add(marker)
                view.invalidate()
            },
        )
    }
}

@Composable
private fun GpsCoordinatesCard(
    state: GpsState,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.gps_card_coordinates_title),
    ) {
        if (!state.hasLocation) {
            Text(
                text = if (state.error != null)
                    state.error
                else
                    stringResource(R.string.gps_searching),
                style = MaterialTheme.typography.bodyMedium,
                color = if (state.error != null)
                    MaterialTheme.colorScheme.error
                else
                    MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                GpsReadingRow(
                    label = stringResource(R.string.gps_label_lat),
                    value = "%.6f°".format(state.latitude),
                )
                GpsReadingRow(
                    label = stringResource(R.string.gps_label_lon),
                    value = "%.6f°".format(state.longitude),
                )
                GpsReadingRow(
                    label = stringResource(R.string.gps_label_altitude),
                    value = "%.1f m".format(state.altitudeMeters),
                )
                GpsReadingRow(
                    label = stringResource(R.string.gps_label_speed),
                    value = "%.1f km/h".format(state.speedKmh),
                )
                GpsReadingRow(
                    label = stringResource(R.string.gps_label_bearing),
                    value = "%.1f°".format(state.bearingDegrees),
                )
                GpsReadingRow(
                    label = stringResource(R.string.gps_label_accuracy),
                    value = "±%.0f m".format(state.accuracyMeters),
                )
            }
        }
    }
}

@Composable
private fun GpsReadingRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@Composable
private fun GpsScreenWithLocationPreview() = GadgetThemedPreview {
    GpsScreenContent(
        state = GpsState(
            latitude = 48.8566,
            longitude = 2.3522,
            altitudeMeters = 35.0,
            speedKmh = 12.5f,
            bearingDegrees = 245f,
            accuracyMeters = 4f,
            hasLocation = true,
            permissionGranted = true,
        ),
        moduleInfo = null,
        onRequestPermission = {},
    )
}

@GadgetPreviewLightDark
@Composable
private fun GpsScreenNoPermissionPreview() = GadgetThemedPreview {
    GpsScreenContent(
        state = GpsState(permissionGranted = false),
        moduleInfo = null,
        onRequestPermission = {},
    )
}
