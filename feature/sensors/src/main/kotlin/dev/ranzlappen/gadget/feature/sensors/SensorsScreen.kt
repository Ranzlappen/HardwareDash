package dev.ranzlappen.gadget.feature.sensors

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * Sensor explorer (v3 B5, MASTER-PLAN §5.2): every sensor the device
 * reports, grouped motion / position / environment / other. Tapping one
 * opens its live detail screen. Replaces v2's three-sensor screen and the
 * separate Ambient and Motion modules, and brings back v1's full list and
 * "copy readings".
 */
@Composable
fun SensorsScreen(
    onOpenSensor: (index: Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SensorsViewModel = hiltViewModel(),
) {
    val clipboard = LocalClipboardManager.current
    SensorsScreenContent(
        sensors = viewModel.sensors,
        onOpenSensor = onOpenSensor,
        onCopyList = { report -> clipboard.setText(AnnotatedString(report)) },
        modifier = modifier,
    )
}

@Composable
internal fun SensorsScreenContent(
    sensors: List<SensorInfo>,
    onOpenSensor: (index: Int) -> Unit,
    onCopyList: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    ModuleScreenScaffold(
        title = stringResource(R.string.sensors_title),
        modifier = modifier,
        functional = {
            if (sensors.isEmpty()) {
                Text(
                    text = stringResource(R.string.sensors_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@ModuleScreenScaffold
            }
            Text(
                text = stringResource(R.string.sensors_count, sensors.size),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SensorGroup.entries.forEach { group ->
                val members = sensors.filter { it.kind.group == group }
                if (members.isEmpty()) return@forEach
                Text(
                    text = stringResource(group.labelRes()),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Column(verticalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                    members.forEach { sensor ->
                        SensorRow(sensor = sensor, onClick = { onOpenSensor(sensor.index) })
                    }
                }
            }
            val report = sensors.joinToString(separator = "\n\n") { sensorReport(it) }
            GadgetSecondaryButton(
                onClick = { onCopyList(report) },
                text = stringResource(R.string.sensors_copy_list),
                leadingIcon = Icons.Outlined.ContentCopy,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    )
}

@Composable
private fun SensorRow(sensor: SensorInfo, onClick: () -> Unit) {
    val wakeUp = if (sensor.isWakeUp) " · " + stringResource(R.string.sensors_wake_up) else ""
    CompactCard(
        modifier = Modifier.fillMaxWidth(),
        title = sensor.typeLabel,
        subtitle = "${sensor.name} · ${sensor.vendor}$wakeUp",
        leadingIcon = sensor.kind.group.icon(),
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                // Decorative: the row title names the target.
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        onClick = onClick,
    )
}

internal fun SensorGroup.labelRes(): Int = when (this) {
    SensorGroup.Motion -> R.string.sensors_group_motion
    SensorGroup.Position -> R.string.sensors_group_position
    SensorGroup.Environment -> R.string.sensors_group_environment
    SensorGroup.Other -> R.string.sensors_group_other
}

internal fun SensorGroup.icon(): ImageVector = when (this) {
    SensorGroup.Motion -> Icons.Outlined.Vibration
    SensorGroup.Position -> Icons.Outlined.Explore
    SensorGroup.Environment -> Icons.Outlined.DeviceThermostat
    SensorGroup.Other -> Icons.Outlined.Sensors
}

internal fun previewSensor(index: Int, type: Int, label: String, group: SensorGroup, unit: String, axes: List<String>) =
    SensorInfo(
        index = index,
        type = type,
        typeLabel = label,
        name = "LSM6DSO $label",
        vendor = "STMicro",
        version = 1,
        maxRange = 78.4f,
        resolution = 0.0024f,
        powerMa = 0.15f,
        minDelayUs = 2_404,
        isWakeUp = false,
        reportingMode = 0,
        kind = SensorKind(unit, axes, group),
    )

@GadgetPreviewLightDark
@Composable
private fun SensorsScreenPreview() = GadgetThemedPreview {
    SensorsScreenContent(
        sensors = listOf(
            previewSensor(0, 1, "Accelerometer", SensorGroup.Motion, "m/s²", listOf("X", "Y", "Z")),
            previewSensor(1, 2, "Magnetic field", SensorGroup.Position, "µT", listOf("X", "Y", "Z")),
            previewSensor(2, 6, "Pressure", SensorGroup.Environment, "hPa", listOf("Pressure")),
        ),
        onOpenSensor = {},
        onCopyList = {},
    )
}
