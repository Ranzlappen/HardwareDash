package dev.ranzlappen.gadget.feature.sensors

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.SparklineChart
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/** Fixed structural sizes for this screen (the documented per-file Defaults exception). */
private object SensorDetailDefaults {
    val SparklineHeight = 40.dp
}

/**
 * One sensor: live values per axis with a sparkline each, accuracy and event
 * rate, its specs, a copy button, and — for sensors that back a persisted
 * metric — the history chart.
 */
@Composable
fun SensorDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: SensorDetailViewModel = hiltViewModel(),
) {
    val info = viewModel.info
    if (info == null) {
        ModuleScreenScaffold(
            title = stringResource(R.string.sensors_title),
            modifier = modifier,
            functional = { Text(stringResource(R.string.sensors_not_found)) },
        )
        return
    }

    val context = LocalContext.current
    val needsPermission = needsActivityRecognition(info.type)
    var permissionGranted by remember {
        mutableStateOf(
            !needsPermission ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permissionGranted = it
    }
    val clipboard = LocalClipboardManager.current

    // Only collect (= register the listener) once the sensor can deliver.
    val live = if (permissionGranted) viewModel.live.collectAsState().value else SensorLive()

    SensorDetailContent(
        info = info,
        live = live,
        permissionGranted = permissionGranted,
        onRequestPermission = { launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION) },
        onCopy = { clipboard.setText(AnnotatedString(sensorReport(info, live.values.toFloatArray().takeIf { it.isNotEmpty() }))) },
        modifier = modifier,
        history = {
            metricKeyFor(info.type)?.let { key ->
                MonitorContainer(
                    metricKey = key,
                    title = stringResource(R.string.sensors_history_title, info.typeLabel),
                    modifier = Modifier.fillMaxWidth(),
                    collapseId = "sensors_history_$key",
                )
            }
        },
    )
}

@Composable
internal fun SensorDetailContent(
    info: SensorInfo,
    live: SensorLive,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
    history: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = info.typeLabel,
        modifier = modifier,
        functional = {
            Text(
                text = "${info.name} · ${info.vendor}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LiveCard(info, live, permissionGranted, onRequestPermission)
            SpecsCard(info)
            GadgetSecondaryButton(
                onClick = onCopy,
                text = stringResource(R.string.sensors_copy_readings),
                leadingIcon = Icons.Outlined.ContentCopy,
                modifier = Modifier.fillMaxWidth(),
            )
            history()
        },
    )
}

@Composable
private fun LiveCard(
    info: SensorInfo,
    live: SensorLive,
    permissionGranted: Boolean,
    onRequestPermission: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(modifier = Modifier.fillMaxWidth(), title = stringResource(R.string.sensors_detail_live)) {
        when {
            info.isOneShot -> Text(
                text = stringResource(R.string.sensors_one_shot_note),
                style = MaterialTheme.typography.bodyMedium,
            )
            !permissionGranted -> Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                Text(
                    text = stringResource(R.string.sensors_permission_body),
                    style = MaterialTheme.typography.bodyMedium,
                )
                GadgetPrimaryButton(
                    onClick = onRequestPermission,
                    text = stringResource(R.string.sensors_permission_grant),
                )
            }
            live.values.isEmpty() -> Text(
                text = stringResource(R.string.sensors_value_pending),
                style = MaterialTheme.typography.bodyMedium,
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
                val unit = info.kind.unit.let { if (it.isEmpty()) "" else " $it" }
                live.values.forEachIndexed { i, value ->
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = axisLabel(info.kind, i),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = formatValue(value) + unit,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                            )
                        }
                        SparklineChart(
                            samples = live.history.getOrElse(i) { emptyList() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(SensorDetailDefaults.SparklineHeight),
                        )
                    }
                }
                val accuracy = live.accuracy?.let { stringResource(accuracyLabelRes(it)) }
                val rate = live.rateHz?.let { stringResource(R.string.sensors_rate, it) }
                Text(
                    text = listOfNotNull(accuracy, rate).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SpecsCard(info: SensorInfo) {
    val spacing = LocalGadgetTheme.current.spacing
    val unit = info.kind.unit.let { if (it.isEmpty()) "" else " $it" }
    DashCard(modifier = Modifier.fillMaxWidth(), title = stringResource(R.string.sensors_detail_specs)) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
            SpecRow(R.string.sensors_spec_name, info.name)
            SpecRow(R.string.sensors_spec_vendor, info.vendor)
            SpecRow(R.string.sensors_spec_version, info.version.toString())
            SpecRow(R.string.sensors_spec_range, formatValue(info.maxRange) + unit)
            SpecRow(R.string.sensors_spec_resolution, formatValue(info.resolution) + unit)
            SpecRow(R.string.sensors_spec_power, stringResource(R.string.sensors_power_value, info.powerMa))
            SpecRow(R.string.sensors_spec_min_delay, stringResource(R.string.sensors_delay_value, info.minDelayUs))
            SpecRow(R.string.sensors_spec_mode, stringResource(reportingModeLabelRes(info.reportingMode)))
            SpecRow(
                R.string.sensors_spec_wake_up,
                stringResource(if (info.isWakeUp) R.string.sensors_yes else R.string.sensors_no),
            )
        }
    }
}

@Composable
private fun SpecRow(labelRes: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun accuracyLabelRes(accuracy: Int): Int = when (accuracy) {
    SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> R.string.sensors_accuracy_high
    SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> R.string.sensors_accuracy_medium
    SensorManager.SENSOR_STATUS_ACCURACY_LOW -> R.string.sensors_accuracy_low
    SensorManager.SENSOR_STATUS_NO_CONTACT -> R.string.sensors_accuracy_no_contact
    else -> R.string.sensors_accuracy_unreliable
}

internal fun reportingModeLabelRes(mode: Int): Int = when (mode) {
    Sensor.REPORTING_MODE_CONTINUOUS -> R.string.sensors_mode_continuous
    Sensor.REPORTING_MODE_ON_CHANGE -> R.string.sensors_mode_on_change
    Sensor.REPORTING_MODE_ONE_SHOT -> R.string.sensors_mode_one_shot
    else -> R.string.sensors_mode_special
}

@GadgetPreviewLightDark
@Composable
private fun SensorDetailPreview() = GadgetThemedPreview {
    var live = SensorLive()
    repeat(30) { t ->
        live = live.next(floatArrayOf(0.1f * t, 9.8f - 0.05f * t, 0.3f), SensorManager.SENSOR_STATUS_ACCURACY_HIGH, (t + 1) * 20_000_000L)
    }
    SensorDetailContent(
        info = previewSensor(0, Sensor.TYPE_ACCELEROMETER, "Accelerometer", SensorGroup.Motion, "m/s²", listOf("X", "Y", "Z")),
        live = live,
        permissionGranted = true,
        onRequestPermission = {},
        onCopy = {},
    )
}
