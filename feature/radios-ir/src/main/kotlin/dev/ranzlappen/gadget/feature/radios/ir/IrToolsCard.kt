package dev.ranzlappen.gadget.feature.radios.ir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.color
import dev.ranzlappen.gadget.core.ui.module.RootActionState
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import kotlin.math.roundToInt

/**
 * The config-entry (parameter-bearing) rooted IR tools (W6 write-tier): a
 * custom-carrier burst (carrier frequency + duration) and a raw on/off GPIO
 * pattern (on/off pulse widths + total duration). Mirrors
 * `MicrophoneToolsCard` — [GadgetSlider] inputs with tap-to-edit numeric
 * fields feeding the rooted `IrController` config methods, each row surfacing
 * its last [RootActionState] result. The no-arg reset lives in the sibling
 * `RootConfirmActionRow`; this card only carries the parameterized actions.
 *
 * Pending inputs are held locally (`rememberSaveable`) so the ViewModel stays
 * a thin launcher surface. Only rendered on the rooted flavor (the caller
 * gates on `isRootedFlavor`); the controller enforces the hard carrier
 * (20–100 kHz), 30 s burst, and ≤50 % duty ceilings regardless of input.
 */
@Composable
internal fun IrToolsCard(
    enabled: Boolean,
    customCarrier: RootActionState,
    rawPattern: RootActionState,
    onCustomCarrier: (carrierHz: Int, durationMillis: Long) -> Unit,
    onRawPattern: (onMillis: Long, offMillis: Long, totalDurationMillis: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    var carrierHz by rememberSaveable { mutableStateOf(DEFAULT_CARRIER_HZ) }
    var carrierDurationMs by rememberSaveable { mutableStateOf(DEFAULT_CARRIER_DURATION_MS) }
    var rawOnMs by rememberSaveable { mutableStateOf(DEFAULT_RAW_PULSE_MS) }
    var rawOffMs by rememberSaveable { mutableStateOf(DEFAULT_RAW_PULSE_MS) }
    var rawTotalMs by rememberSaveable { mutableStateOf(DEFAULT_RAW_TOTAL_MS) }

    DashCard(
        modifier = modifier.fillMaxWidth(),
        title = stringResource(R.string.ir_tools_title),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            Text(
                text = stringResource(R.string.ir_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // ─── Custom carrier ─────────────────────────────────────────
            ToolSection(label = stringResource(R.string.ir_tools_carrier_section)) {
                GadgetSlider(
                    value = carrierHz.toFloat(),
                    onValueChange = { carrierHz = it.roundToInt() },
                    valueRange = MIN_CARRIER_HZ.toFloat()..MAX_CARRIER_HZ.toFloat(),
                    label = stringResource(R.string.ir_tools_carrier_hz_label),
                    suffix = "kHz",
                    valueFormatter = { hz -> "%.1f".format(hz / 1000f) },
                    valueParser = { text -> text.trim().toFloatOrNull()?.times(1000f) },
                    enabled = enabled,
                )
                SecondsSlider(
                    valueMs = carrierDurationMs,
                    onChange = { carrierDurationMs = it },
                    maxMs = MAX_CARRIER_DURATION_MS,
                    enabled = enabled,
                )
                RunButton(
                    onClick = { onCustomCarrier(carrierHz, carrierDurationMs) },
                    textRes = R.string.ir_tools_carrier_run,
                    enabled = enabled,
                    loading = customCarrier.running,
                )
                StatusLine(customCarrier)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ─── Raw GPIO pattern ───────────────────────────────────────
            ToolSection(label = stringResource(R.string.ir_tools_raw_section)) {
                MillisSlider(
                    valueMs = rawOnMs,
                    onChange = { rawOnMs = it },
                    label = stringResource(R.string.ir_tools_raw_on_label),
                    maxMs = MAX_RAW_PULSE_MS,
                    enabled = enabled,
                )
                MillisSlider(
                    valueMs = rawOffMs,
                    onChange = { rawOffMs = it },
                    label = stringResource(R.string.ir_tools_raw_off_label),
                    maxMs = MAX_RAW_PULSE_MS,
                    enabled = enabled,
                )
                SecondsSlider(
                    valueMs = rawTotalMs,
                    onChange = { rawTotalMs = it },
                    maxMs = MAX_RAW_TOTAL_MS,
                    enabled = enabled,
                )
                RunButton(
                    onClick = { onRawPattern(rawOnMs, rawOffMs, rawTotalMs) },
                    textRes = R.string.ir_tools_raw_run,
                    enabled = enabled,
                    loading = rawPattern.running,
                )
                StatusLine(rawPattern)
            }
        }
    }
}

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

/** Duration slider rendered in **seconds**; the model stores milliseconds. */
@Composable
private fun SecondsSlider(valueMs: Long, onChange: (Long) -> Unit, maxMs: Long, enabled: Boolean) {
    GadgetSlider(
        value = valueMs.toFloat(),
        onValueChange = { onChange(it.roundToInt().toLong()) },
        valueRange = 0f..maxMs.toFloat(),
        label = stringResource(R.string.ir_tools_duration_label),
        suffix = "s",
        valueFormatter = { ms -> "%.1f".format(ms / 1000f) },
        valueParser = { text -> text.trim().toFloatOrNull()?.let { it * 1000f } },
        enabled = enabled,
    )
}

/** Pulse-width slider rendered in raw **milliseconds**. */
@Composable
private fun MillisSlider(
    valueMs: Long,
    onChange: (Long) -> Unit,
    label: String,
    maxMs: Long,
    enabled: Boolean,
) {
    GadgetSlider(
        value = valueMs.toFloat(),
        onValueChange = { onChange(it.roundToInt().toLong()) },
        valueRange = 1f..maxMs.toFloat(),
        label = label,
        suffix = "ms",
        enabled = enabled,
    )
}

private const val MIN_CARRIER_HZ = 20_000
private const val MAX_CARRIER_HZ = 100_000
private const val DEFAULT_CARRIER_HZ = 38_000
private const val MAX_CARRIER_DURATION_MS = 30_000L
private const val DEFAULT_CARRIER_DURATION_MS = 2_000L
private const val MAX_RAW_PULSE_MS = 50L
private const val DEFAULT_RAW_PULSE_MS = 9L
private const val MAX_RAW_TOTAL_MS = 5_000L
private const val DEFAULT_RAW_TOTAL_MS = 1_000L

@GadgetPreviewLightDark
@Composable
private fun IrToolsCardPreview() = GadgetThemedPreview {
    IrToolsCard(
        enabled = true,
        customCarrier = RootActionState(message = "Emitted 2.0 s @ 38 kHz"),
        rawPattern = RootActionState(),
        onCustomCarrier = { _, _ -> },
        onRawPattern = { _, _, _ -> },
    )
}
