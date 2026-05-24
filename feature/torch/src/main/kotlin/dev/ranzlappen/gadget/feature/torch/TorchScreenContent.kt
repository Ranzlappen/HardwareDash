package dev.ranzlappen.gadget.feature.torch

import android.Manifest
import android.os.Build
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
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material3.MaterialTheme
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
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.OsNote
import dev.ranzlappen.gadget.core.ui.component.CompactCard
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetFab
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.WidgetType
import kotlin.math.roundToInt

/**
 * Stateless TorchScreen content — receives a single [TorchScreenState]
 * snapshot plus callbacks for every user-initiated event.
 *
 * Three sections render top-to-bottom inside the screen scaffold:
 *
 *   1. **Torch toggle** — hero FAB centred in a [DashCard] plus an
 *      in-app strobe toggle button right below for immediate testing
 *      without pinning a widget.
 *   2. **Strobe defaults** — [GadgetSlider] feeding
 *      [UserPreferencesRepository.setDefaultStrobeRateHz]; the value
 *      is the default that gets captured into every new strobe widget
 *      at pin time. Existing widgets keep their per-instance rate.
 *      The slider is fully draggable AND the trailing "5 Hz" label
 *      is tap-to-edit (numeric input).
 *   3. **Your widgets** — list of [TorchScreenState.widgets] rendered
 *      as [CompactCard] rows with edit / delete actions, plus two
 *      stacked full-width [GadgetSecondaryButton]s for "Add flashlight"
 *      and "Add strobe". Vertical stacking + tightened button padding
 *      avoids the label-truncation bug from the first pass.
 */
@Composable
fun TorchScreenContent(
    state: TorchScreenState,
    onToggleClick: () -> Unit,
    onStrobeToggle: () -> Unit,
    onRateChange: (Float) -> Unit,
    onRateCommit: () -> Unit,
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
            TorchToggleCard(state.torch, state.strobeRunning, onToggleClick, onStrobeToggle)
            StrobeDefaultsCard(state.defaultStrobeRateHz, onRateChange, onRateCommit)
            WidgetsCard(
                widgets = state.widgets,
                onAddFlashlight = onAddFlashlight,
                onAddStrobe = onAddStrobe,
                onEditWidget = onEditWidget,
                onDeleteWidget = onDeleteWidget,
            )
        },
        moduleInfo = torchModuleInfo(),
    )
}

/**
 * Torch's [ModuleInfo] — the reference implementation of the module
 * blueprint. Torch toggles on-device hardware via `CameraManager`, so:
 *  - the only runtime permission is the optional `POST_NOTIFICATIONS`
 *    (Android 13+) that lets the strobe foreground-service chrome and
 *    the widget toggle confirmations show — the feature works without
 *    it, so it's marked `optional`,
 *  - it works on every supported OS (minSdk 29) with two foreground-
 *    service behaviour notes,
 *  - it has **no firmware** requirement (the firmware section is
 *    omitted).
 */
@Composable
private fun torchModuleInfo(): ModuleInfo = ModuleInfo(
    permissions = buildList {
        // POST_NOTIFICATIONS only exists as a runtime permission on
        // API 33+; below that it's auto-granted, so don't surface it as
        // a perpetually-missing row.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(
                ModulePermission(
                    permission = Manifest.permission.POST_NOTIFICATIONS,
                    label = stringResource(R.string.torch_module_perm_notifications_label),
                    rationale = stringResource(R.string.torch_module_perm_notifications_rationale),
                    optional = true,
                ),
            )
        }
    },
    compatibility = OsCompatibility(
        minSdk = Build.VERSION_CODES.Q,
        notes = listOf(
            OsNote(
                sinceSdk = Build.VERSION_CODES.Q,
                text = stringResource(R.string.torch_module_compat_note_fgs_active),
            ),
            OsNote(
                sinceSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE,
                text = stringResource(R.string.torch_module_compat_note_fgs_short),
            ),
        ),
    ),
    firmware = null,
)

@Composable
private fun TorchToggleCard(
    torch: TorchState,
    strobeRunning: Boolean,
    onToggleClick: () -> Unit,
    onStrobeToggle: () -> Unit,
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
            // In-app strobe toggle. Mirrors widget behaviour — tap once to
            // start at the current default rate, tap again to stop. Doubles
            // as a diagnostic for the strobe service path independent of
            // widget wiring.
            GadgetSecondaryButton(
                onClick = onStrobeToggle,
                text = stringResource(
                    if (strobeRunning) R.string.torch_action_strobe_stop
                    else R.string.torch_action_strobe_start,
                ),
                leadingIcon = Icons.Outlined.Bolt,
                enabled = torch.isAvailable,
                modifier = Modifier.fillMaxWidth(),
            )
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
    onRateCommit: () -> Unit,
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
            GadgetSlider(
                value = rateHz,
                onValueChange = onRateChange,
                onValueChangeFinished = onRateCommit,
                valueRange = TorchWidgetConfig.MIN_RATE_HZ..TorchWidgetConfig.MAX_RATE_HZ,
                steps = (TorchWidgetConfig.MAX_RATE_HZ - TorchWidgetConfig.MIN_RATE_HZ).toInt() - 1,
                label = stringResource(R.string.torch_strobe_rate_label),
                suffix = "Hz",
            )
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
            // Stacked vertical layout — full-width buttons avoid the
            // label-truncation issue that a side-by-side Row exhibited
            // when paired with the design system's default button
            // padding.
            GadgetSecondaryButton(
                onClick = onAddFlashlight,
                text = stringResource(R.string.torch_widget_add_flashlight),
                leadingIcon = Icons.Outlined.FlashlightOn,
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = onAddStrobe,
                text = stringResource(R.string.torch_widget_add_strobe),
                leadingIcon = Icons.Outlined.Bolt,
                modifier = Modifier.fillMaxWidth(),
            )
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
                            WidgetType.Strobe -> Icons.Outlined.Bolt
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
 * Produced by [TorchViewModel.state] from the four reactive sources
 * the screen depends on:
 * - [TorchController.state] (live torch hardware snapshot)
 * - [UserPreferencesRepository.flow.map { it.defaultStrobeRateHz }]
 * - [TorchWidgetConfigRepository.all] (saved widget configs)
 * - [StrobeService.isRunning] (polled cheaply via a Volatile read)
 *
 * `@Immutable` so Compose skips recompositions when the structural
 * value is unchanged across emissions.
 */
@Immutable
data class TorchScreenState(
    val torch: TorchState,
    val defaultStrobeRateHz: Float,
    val widgets: List<SavedTorchWidget>,
    val strobeRunning: Boolean = false,
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
