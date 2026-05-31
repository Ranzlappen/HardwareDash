package dev.ranzlappen.gadget.feature.vibration.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet
import dev.ranzlappen.gadget.core.ui.component.GadgetPrimaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconChoice
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.ui.WidgetAppearanceSection
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig
import dev.ranzlappen.gadget.feature.vibration.widget.WidgetType
import kotlin.math.roundToInt

/**
 * Modal sheet for creating or editing a [VibrationWidgetConfig]. Captures the
 * vibration-specific surface (name + the Vibrate variant's amplitude/duration)
 * and delegates appearance / tap / feedback / preview to the kit's
 * [WidgetAppearanceSection]. Mirror of torch's `WidgetConfigurationSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WidgetConfigurationSheet(
    initial: VibrationWidgetConfig,
    isExisting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (VibrationWidgetConfig) -> Unit,
    resolveIcon: (String) -> WidgetIconSource,
    onImportCustomIcon: suspend (Uri) -> String?,
    iconChoices: List<WidgetIconChoice>,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember(initial) { mutableStateOf(initial.displayName) }
    var amplitude by remember(initial) { mutableIntStateOf(initial.amplitudePercent) }
    var durationMs by remember(initial) { mutableLongStateOf(initial.durationMillis) }
    var appearance by remember(initial) { mutableStateOf(initial.appearance) }

    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        title = stringResource(
            if (isExisting) R.string.vibration_widget_config_title_edit
            else R.string.vibration_widget_config_title_new,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            if (initial.type == WidgetType.Vibrate) {
                GadgetSlider(
                    value = amplitude.toFloat(),
                    onValueChange = { amplitude = it.roundToInt() },
                    valueRange = VibrationWidgetConfig.MIN_AMPLITUDE_PERCENT.toFloat()..
                        VibrationWidgetConfig.MAX_AMPLITUDE_PERCENT.toFloat(),
                    label = stringResource(R.string.vibration_controls_amplitude),
                    suffix = "%",
                )
                GadgetSlider(
                    value = durationMs.toFloat(),
                    onValueChange = { durationMs = it.roundToInt().toLong() },
                    valueRange = VibrationWidgetConfig.MIN_DURATION_MS.toFloat()..
                        VibrationWidgetConfig.MAX_DURATION_MS.toFloat(),
                    label = stringResource(R.string.vibration_controls_duration),
                    suffix = "ms",
                )
            } else {
                Text(
                    text = stringResource(R.string.vibration_widget_config_pattern_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            WidgetAppearanceSection(
                appearance = appearance,
                onAppearanceChange = { appearance = it },
                iconChoices = iconChoices,
                resolveIcon = resolveIcon,
                onImportCustomIcon = onImportCustomIcon,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetTertiaryButton(
                    onClick = onDismiss,
                    text = stringResource(R.string.vibration_widget_config_cancel),
                    modifier = Modifier.weight(1f),
                )
                GadgetPrimaryButton(
                    onClick = {
                        onConfirm(
                            initial.copy(
                                displayName = name.ifBlank { initial.displayName },
                                amplitudePercent = amplitude,
                                durationMillis = durationMs,
                                appearance = appearance,
                            ),
                        )
                    },
                    text = stringResource(
                        if (isExisting) R.string.vibration_widget_config_save_existing
                        else R.string.vibration_widget_config_save_new,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
