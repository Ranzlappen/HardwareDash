package dev.ranzlappen.gadget.feature.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.component.color
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import kotlin.math.roundToInt

/**
 * The config-entry (parameter-bearing) rooted camera tools (W6 write-tier):
 * high-FPS capture, manual ISO/exposure/focus override, raw multi-frame
 * capture, simultaneous multi-camera capture, and the shutter-sound toggle.
 * Mirrors `MicrophoneToolsCard` — [GadgetSlider] / [GadgetTextField] inputs
 * feeding the rooted `CameraController` config methods, each row surfacing its
 * last [RootActionState]. The no-arg HAL-bypass frame lives in the sibling
 * `RootConfirmActionRow`; this card only carries the parameterized actions.
 *
 * Pending inputs are held locally (`rememberSaveable`). A single camera-id
 * field feeds the single-camera actions; the multi-camera row takes a
 * comma-separated id list. Only rendered on the rooted flavor; the controller
 * enforces every hard ceiling (240 fps, 30 s, 15 s multi-cam, stream caps).
 */
@Composable
internal fun CameraToolsCard(
    enabled: Boolean,
    rootTools: CameraRootToolsState,
    onHighFps: (cameraId: String, fps: Int, durationMillis: Long) -> Unit,
    onManualExposure: (
        cameraId: String,
        iso: Int,
        exposureTimeNanos: Long,
        focusDiopter: Float,
        durationMillis: Long,
    ) -> Unit,
    onRawCapture: (cameraId: String, frameCount: Int) -> Unit,
    onMultiCamera: (cameraIds: List<String>, durationMillis: Long) -> Unit,
    onShutterSound: (enabled: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var cameraId by rememberSaveable { mutableStateOf(DEFAULT_CAMERA_ID) }
    var fps by rememberSaveable { mutableStateOf(DEFAULT_FPS) }
    var highFpsDurationMs by rememberSaveable { mutableStateOf(DEFAULT_CAPTURE_DURATION_MS) }
    var iso by rememberSaveable { mutableStateOf(DEFAULT_ISO) }
    var exposureMs by rememberSaveable { mutableStateOf(DEFAULT_EXPOSURE_MS) }
    var focusDiopter by rememberSaveable { mutableStateOf(DEFAULT_FOCUS_DIOPTER) }
    var manualDurationMs by rememberSaveable { mutableStateOf(DEFAULT_CAPTURE_DURATION_MS) }
    var frameCount by rememberSaveable { mutableStateOf(DEFAULT_FRAME_COUNT) }
    var multiCameraIds by rememberSaveable { mutableStateOf(DEFAULT_MULTI_CAMERA_IDS) }
    var multiDurationMs by rememberSaveable { mutableStateOf(DEFAULT_MULTI_DURATION_MS) }
    var shutterSoundOn by rememberSaveable { mutableStateOf(true) }

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.camera_tools_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = stringResource(R.string.camera_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetTextField(
                value = cameraId,
                onValueChange = { cameraId = it },
                label = stringResource(R.string.camera_tools_camera_id_label),
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ─── High-FPS capture ───────────────────────────────────────
            ToolSection(label = stringResource(R.string.camera_tools_high_fps_section)) {
                IntSlider(
                    value = fps,
                    onChange = { fps = it },
                    label = stringResource(R.string.camera_tools_fps_label),
                    range = MIN_FPS..MAX_FPS,
                    suffix = "fps",
                    enabled = enabled,
                )
                SecondsSlider(highFpsDurationMs, { highFpsDurationMs = it }, MAX_CAPTURE_DURATION_MS, enabled)
                RunButton(
                    onClick = { onHighFps(cameraId, fps, highFpsDurationMs) },
                    textRes = R.string.camera_tools_high_fps_run,
                    enabled = enabled,
                    loading = rootTools.highFps.running,
                )
                StatusLine(rootTools.highFps)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ─── Manual exposure ────────────────────────────────────────
            ToolSection(label = stringResource(R.string.camera_tools_manual_section)) {
                IntSlider(
                    value = iso,
                    onChange = { iso = it },
                    label = stringResource(R.string.camera_tools_iso_label),
                    range = MIN_ISO..MAX_ISO,
                    suffix = "ISO",
                    enabled = enabled,
                )
                IntSlider(
                    value = exposureMs.toInt(),
                    onChange = { exposureMs = it.toLong() },
                    label = stringResource(R.string.camera_tools_exposure_label),
                    range = MIN_EXPOSURE_MS.toInt()..MAX_EXPOSURE_MS.toInt(),
                    suffix = "ms",
                    enabled = enabled,
                )
                GadgetSlider(
                    value = focusDiopter,
                    onValueChange = { focusDiopter = it },
                    valueRange = MIN_FOCUS_DIOPTER..MAX_FOCUS_DIOPTER,
                    label = stringResource(R.string.camera_tools_focus_label),
                    suffix = "dpt",
                    valueFormatter = { "%.1f".format(it) },
                    enabled = enabled,
                )
                SecondsSlider(manualDurationMs, { manualDurationMs = it }, MAX_CAPTURE_DURATION_MS, enabled)
                RunButton(
                    onClick = {
                        onManualExposure(
                            cameraId,
                            iso,
                            exposureMs * NANOS_PER_MILLI,
                            focusDiopter,
                            manualDurationMs,
                        )
                    },
                    textRes = R.string.camera_tools_manual_run,
                    enabled = enabled,
                    loading = rootTools.manualExposure.running,
                )
                StatusLine(rootTools.manualExposure)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ─── Raw capture ────────────────────────────────────────────
            ToolSection(label = stringResource(R.string.camera_tools_raw_section)) {
                IntSlider(
                    value = frameCount,
                    onChange = { frameCount = it },
                    label = stringResource(R.string.camera_tools_frame_count_label),
                    range = MIN_FRAME_COUNT..MAX_FRAME_COUNT,
                    suffix = "",
                    enabled = enabled,
                )
                RunButton(
                    onClick = { onRawCapture(cameraId, frameCount) },
                    textRes = R.string.camera_tools_raw_run,
                    enabled = enabled,
                    loading = rootTools.rawCapture.running,
                )
                StatusLine(rootTools.rawCapture)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ─── Multi-camera ───────────────────────────────────────────
            ToolSection(label = stringResource(R.string.camera_tools_multi_section)) {
                GadgetTextField(
                    value = multiCameraIds,
                    onValueChange = { multiCameraIds = it },
                    label = stringResource(R.string.camera_tools_multi_ids_label),
                    placeholder = "0, 1",
                    enabled = enabled,
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondsSlider(multiDurationMs, { multiDurationMs = it }, MAX_MULTI_DURATION_MS, enabled)
                RunButton(
                    onClick = { onMultiCamera(parseCameraIds(multiCameraIds), multiDurationMs) },
                    textRes = R.string.camera_tools_multi_run,
                    enabled = enabled && parseCameraIds(multiCameraIds).size >= 2,
                    loading = rootTools.multiCamera.running,
                )
                StatusLine(rootTools.multiCamera)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ─── Shutter sound ──────────────────────────────────────────
            ToolSection(label = stringResource(R.string.camera_tools_shutter_section)) {
                Text(
                    text = stringResource(R.string.camera_tools_shutter_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ToggleRow(
                    label = stringResource(R.string.camera_tools_shutter_toggle_label),
                    checked = shutterSoundOn,
                    onCheckedChange = { checked ->
                        shutterSoundOn = checked
                        onShutterSound(checked)
                    },
                    enabled = enabled && !rootTools.shutterSound.running,
                )
                StatusLine(rootTools.shutterSound)
            }
        }
    }
}

private fun parseCameraIds(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }

@Composable
private fun ToolSection(label: String, content: @Composable () -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(spacing.small),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        content()
    }
}

@Composable
private fun RunButton(onClick: () -> Unit, textRes: Int, enabled: Boolean, loading: Boolean) {
    GadgetSecondaryButton(
        onClick = onClick,
        text = stringResource(textRes),
        enabled = enabled && !loading,
        loading = loading,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun StatusLine(state: RootActionState) {
    val message = state.message ?: return
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = state.statusKind.color(),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun IntSlider(
    value: Int,
    onChange: (Int) -> Unit,
    label: String,
    range: IntRange,
    suffix: String,
    enabled: Boolean,
) {
    GadgetSlider(
        value = value.toFloat(),
        onValueChange = { onChange(it.roundToInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat(),
        label = label,
        suffix = suffix,
        enabled = enabled,
    )
}

/** Duration slider rendered in **seconds**; the model stores milliseconds. */
@Composable
private fun SecondsSlider(valueMs: Long, onChange: (Long) -> Unit, maxMs: Long, enabled: Boolean) {
    GadgetSlider(
        value = valueMs.toFloat(),
        onValueChange = { onChange(it.roundToInt().toLong()) },
        valueRange = 0f..maxMs.toFloat(),
        label = stringResource(R.string.camera_tools_duration_label),
        suffix = "s",
        valueFormatter = { ms -> "%.1f".format(ms / 1000f) },
        valueParser = { text -> text.trim().toFloatOrNull()?.let { it * 1000f } },
        enabled = enabled,
    )
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
) {
    val spacing = LocalGadgetTheme.current.spacing
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f).padding(end = spacing.small),
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

private const val DEFAULT_CAMERA_ID = "0"
private const val DEFAULT_MULTI_CAMERA_IDS = "0, 1"
private const val MIN_FPS = 30
private const val MAX_FPS = 240
private const val DEFAULT_FPS = 120
private const val MIN_ISO = 50
private const val MAX_ISO = 3200
private const val DEFAULT_ISO = 400
private const val MIN_EXPOSURE_MS = 1L
private const val MAX_EXPOSURE_MS = 1_000L
private const val DEFAULT_EXPOSURE_MS = 20L
private const val MIN_FOCUS_DIOPTER = 0f
private const val MAX_FOCUS_DIOPTER = 10f
private const val DEFAULT_FOCUS_DIOPTER = 2f
private const val MIN_FRAME_COUNT = 1
private const val MAX_FRAME_COUNT = 30
private const val DEFAULT_FRAME_COUNT = 5
private const val MAX_CAPTURE_DURATION_MS = 30_000L
private const val MAX_MULTI_DURATION_MS = 15_000L
private const val DEFAULT_CAPTURE_DURATION_MS = 2_000L
private const val DEFAULT_MULTI_DURATION_MS = 3_000L
private const val NANOS_PER_MILLI = 1_000_000L

@GadgetPreviewLightDark
@Composable
private fun CameraToolsCardPreview() = GadgetThemedPreview {
    CameraToolsCard(
        enabled = true,
        rootTools = CameraRootToolsState(highFps = RootActionState(message = "Captured 120 fps for 2.0 s")),
        onHighFps = { _, _, _ -> },
        onManualExposure = { _, _, _, _, _ -> },
        onRawCapture = { _, _ -> },
        onMultiCamera = { _, _ -> },
        onShutterSound = {},
    )
}
