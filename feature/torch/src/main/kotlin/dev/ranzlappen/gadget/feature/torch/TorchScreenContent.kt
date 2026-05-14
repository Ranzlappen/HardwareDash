package dev.ranzlappen.gadget.feature.torch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetFab
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType
import kotlin.math.roundToInt

/**
 * Stateless TorchScreen content — receives a single [TorchScreenState]
 * snapshot plus callbacks for every user-initiated event.
 *
 * Pulled out from [TorchScreen] (the Hilt-wrapped stateful entry point)
 * so the screen can be exercised in instrumented tests without standing
 * up the real Camera2 controller or DataStore. Tests inject a
 * deterministic [TorchScreenState] and assert that the expected text /
 * controls render and that taps invoke the right callbacks.
 *
 * Three sections render top-to-bottom inside [ModuleScreenScaffold]:
 *
 *   1. **Torch toggle** — hero FAB centred in a [DashCard], status
 *      message underneath.
 *   2. **Strobe defaults** — Hz slider feeding
 *      [UserPreferencesRepository.setDefaultStrobeRateHz]; the value
 *      is the default that gets captured into every new strobe widget
 *      at pin time. Existing widgets keep their per-instance rate.
 *   3. **Your widgets** — list of [TorchScreenState.widgets] rendered
 *      as [CompactCard] rows with edit / delete actions, plus two
 *      [GadgetSecondaryButton]s above the list for "Add flashlight"
 *      and "Add strobe". When the list is empty, a [GadgetEmptyState]
 *      tile replaces it.
 *
 * Rooted-flavor extras (brightness, multi-LED, thermal override) will
 * extend the toggle card with extra slots when the sibling
 * `:feature:torch-rooted` module ships — see issues #94 and #95.
 */
@Composable
fun TorchScreenContent(
    state: TorchScreenState,
    onToggleClick: () -> Unit,
    onRateChange: (Float) -> Unit,
    onAddFlashlight: () -> Unit,
    onAddStrobe: () -> Unit,
    onEditWidget: (SavedTorchWidget) -> Unit,
    onDeleteWidget: (SavedTorchWidget) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.torch_screen_title),
        modifier = modifier,
        functional = {
            TorchToggleCard(state.torch, onToggleClick)
            StrobeDefaultsCard(state.defaultStrobeRateHz, onRateChange)
            WidgetsCard(
                widgets = state.widgets,
                onAddFlashlight = onAddFlashlight,
                onAddStrobe = onAddStrobe,
                onEditWidget = onEditWidget,
                onDeleteWidget = onDeleteWidget,
            )
        },
    )
}

@Composable
private fun TorchToggleCard(
    torch: TorchState,
    onToggleClick: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(
            if (torch.isOn) R.string.torch_state_on else R.string.torch_state_off,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HeroBoxHeight),
                contentAlignment = Alignment.Center,
            ) {
                GadgetFab(
                    onClick = onToggleClick,
                    icon = if (torch.isOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    contentDescription = stringResource(
                        if (torch.isOn) R.string.torch_action_turn_off
                        else R.string.torch_action_turn_on,
                    ),
                    enabled = torch.isAvailable,
                )
            }
            Text(
                text = torch.statusMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun StrobeDefaultsCard(
    rateHz: Float,
    onRateChange: (Float) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.torch_section_defaults_title),
        icon = Icons.Outlined.FlashlightOn,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = stringResource(R.string.torch_section_defaults_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                Text(
                    text = stringResource(R.string.torch_strobe_rate_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = rateHz,
                    onValueChange = onRateChange,
                    valueRange = TorchWidgetConfig.MIN_RATE_HZ..TorchWidgetConfig.MAX_RATE_HZ,
                    // Steps are integers between min..max exclusive
                    // (M3 contract: stepCount = number of discrete
                    // values BETWEEN the two endpoints, not inclusive).
                    steps = (TorchWidgetConfig.MAX_RATE_HZ - TorchWidgetConfig.MIN_RATE_HZ).toInt() - 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(
                        R.string.torch_strobe_rate_value,
                        rateHz.roundToInt(),
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = spacing.tiny),
                )
            }
        }
    }
}

@Composable
private fun WidgetsCard(
    widgets: List<SavedTorchWidget>,
    onAddFlashlight: () -> Unit,
    onAddStrobe: () -> Unit,
    onEditWidget: (SavedTorchWidget) -> Unit,
    onDeleteWidget: (SavedTorchWidget) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.torch_section_widgets_title),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small),
            ) {
                GadgetSecondaryButton(
                    onClick = onAddFlashlight,
                    text = stringResource(R.string.torch_widget_add_flashlight),
                    modifier = Modifier.weight(1f),
                )
                GadgetSecondaryButton(
                    onClick = onAddStrobe,
                    text = stringResource(R.string.torch_widget_add_strobe),
                    modifier = Modifier.weight(1f),
                )
            }
            if (widgets.isEmpty()) {
                GadgetEmptyState(
                    title = stringResource(R.string.torch_widget_list_empty_title),
                    subtitle = stringResource(R.string.torch_widget_list_empty_subtitle),
                    icon = Icons.Outlined.FlashlightOn,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                widgets.forEach { widget ->
                    CompactCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = widget.config.displayName,
                        subtitle = widget.config.subtitleString(),
                        leadingIcon = when (widget.config.type) {
                            WidgetType.Flashlight -> Icons.Outlined.FlashlightOn
                            WidgetType.Strobe -> Icons.Outlined.FlashlightOn
                        },
                        trailingContent = {
                            Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                                GadgetIconButton(
                                    onClick = { onEditWidget(widget) },
                                    icon = Icons.Outlined.Edit,
                                    contentDescription = stringResource(
                                        R.string.torch_widget_list_action_edit,
                                    ),
                                )
                                GadgetIconButton(
                                    onClick = { onDeleteWidget(widget) },
                                    icon = Icons.Outlined.Delete,
                                    contentDescription = stringResource(
                                        R.string.torch_widget_list_action_delete,
                                    ),
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Stateless view-state container consumed by [TorchScreenContent].
 *
 * Produced by [TorchViewModel.state] from the three reactive sources
 * the screen depends on:
 * - [TorchController.state] (live torch hardware snapshot)
 * - [UserPreferencesRepository.flow.map { it.defaultStrobeRateHz }]
 * - [TorchWidgetConfigRepository.all] (saved widget configs)
 *
 * `@Immutable` so Compose skips recompositions when the structural
 * value is unchanged across emissions.
 */
@Immutable
data class TorchScreenState(
    val torch: TorchState,
    val defaultStrobeRateHz: Float,
    val widgets: List<SavedTorchWidget>,
) {
    companion object {
        /** First-emission placeholder used before the flows emit. */
        val Initial = TorchScreenState(
            torch = TorchState(),
            defaultStrobeRateHz = TorchWidgetConfig.DEFAULT_RATE_HZ,
            widgets = emptyList(),
        )
    }
}

/** A single persisted widget — `appWidgetId` keyed [TorchWidgetConfig]. */
@Immutable
data class SavedTorchWidget(
    val appWidgetId: Int,
    val config: TorchWidgetConfig,
)

@Composable
private fun TorchState.statusMessage(): String = when {
    !isAvailable && error == TorchError.NoFlashUnit ->
        stringResource(R.string.torch_status_no_flash)
    error == TorchError.HardwareError ->
        stringResource(R.string.torch_status_hardware_error)
    error == TorchError.PermissionDenied ->
        stringResource(R.string.torch_status_permission_denied)
    isOn -> stringResource(R.string.torch_status_on)
    else -> stringResource(R.string.torch_status_off)
}

@Composable
private fun TorchWidgetConfig.subtitleString(): String = when (type) {
    WidgetType.Flashlight ->
        stringResource(R.string.torch_widget_list_subtitle_flashlight)
    WidgetType.Strobe -> stringResource(
        if (sosMode) R.string.torch_widget_list_subtitle_strobe_sos
        else R.string.torch_widget_list_subtitle_strobe,
        rateHz.roundToInt(),
    )
}

/** Hero box height — generous tap target for the central FAB. */
private val HeroBoxHeight: Dp = 160.dp
