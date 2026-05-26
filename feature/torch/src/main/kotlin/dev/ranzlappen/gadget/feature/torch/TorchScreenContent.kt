package dev.ranzlappen.gadget.feature.torch

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.GlassIntensity
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.module.CapabilityStatus
import dev.ranzlappen.gadget.core.ui.module.ModuleCapability
import dev.ranzlappen.gadget.core.ui.module.ModuleInfo
import dev.ranzlappen.gadget.core.ui.module.ModulePermission
import dev.ranzlappen.gadget.core.ui.module.OsCompatibility
import dev.ranzlappen.gadget.core.ui.module.OsNote
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetEmptyState
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.component.GadgetStatusKind
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.core.ui.component.GlassSurface
import dev.ranzlappen.gadget.feature.torch.ui.WidgetAppearancePreview
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetIconSource

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
    onMomentaryHold: (Boolean) -> Unit,
    onStrobeToggle: () -> Unit,
    onStrobeHold: (Boolean) -> Unit,
    onMorseToggle: () -> Unit,
    onMorseHold: (Boolean) -> Unit,
    onMorseTextChange: (String) -> Unit,
    onRateChange: (Float) -> Unit,
    onRateCommit: () -> Unit,
    onAddFlashlight: () -> Unit,
    onAddStrobe: () -> Unit,
    onEditWidget: (SavedTorchWidget) -> Unit,
    onDeleteWidget: (SavedTorchWidget) -> Unit,
    onResolveIcon: (String) -> WidgetIconSource,
    onRootBoostBrightness: () -> Unit,
    onRootDutyStrobe: () -> Unit,
    onRootMultiLed: () -> Unit,
    onRootThermal: () -> Unit,
    modifier: Modifier = Modifier,
    monitor: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.torch_screen_title),
        modifier = modifier,
        functional = {
            TorchToggleCard(
                torch = state.torch,
                strobeRunning = state.strobeRunning,
                morseText = state.morseText,
                onToggleClick = onToggleClick,
                onMomentaryHold = onMomentaryHold,
                onStrobeToggle = onStrobeToggle,
                onStrobeHold = onStrobeHold,
                onMorseToggle = onMorseToggle,
                onMorseHold = onMorseHold,
                onMorseTextChange = onMorseTextChange,
            )
            StrobeDefaultsCard(state.defaultStrobeRateHz, onRateChange, onRateCommit)
            // Monitoring tile (torch's instantiation of the reusable
            // MonitorContainer). Injected as a slot so the stateless content
            // stays Hilt-free for previews/tests; TorchScreen supplies it.
            monitor()
            WidgetsCard(
                widgets = state.widgets,
                onResolveIcon = onResolveIcon,
                onAddFlashlight = onAddFlashlight,
                onAddStrobe = onAddStrobe,
                onEditWidget = onEditWidget,
                onDeleteWidget = onDeleteWidget,
            )
            // Rooted-only privileged controls — shown only when the
            // rooted app version reports a usable root shell.
            if (state.rootAvailability.rootReady) {
                RootToolsCard(
                    onBoostBrightness = onRootBoostBrightness,
                    onDutyStrobe = onRootDutyStrobe,
                    onMultiLed = onRootMultiLed,
                    onThermal = onRootThermal,
                )
            }
        },
        moduleInfo = torchModuleInfo(state.torch, state.rootAvailability),
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
private fun torchModuleInfo(
    torch: TorchState,
    root: TorchRootAvailability,
): ModuleInfo = ModuleInfo(
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
    capabilities = torchCapabilities(torch, root),
)

/**
 * Per-function capability rows for Torch — green/amber/red status for each
 * button and action across both the standard and rooted app versions. The
 * standard functions key off live flash-hardware + OS-version checks; the
 * rooted functions key off the [TorchRootCapabilities] probe ([root]), so
 * on the standard build (or an un-rooted device) they read red with a
 * "requires the rooted app version" message.
 */
@Composable
private fun torchCapabilities(
    torch: TorchState,
    root: TorchRootAvailability,
): List<ModuleCapability> {
    val hasFlash = torch.isAvailable
    val noFlashMsg = stringResource(R.string.torch_cap_no_flash)
    val needsRootMsg = stringResource(R.string.torch_cap_needs_root)

    return listOf(
        ModuleCapability(
            name = stringResource(R.string.torch_cap_basic_name),
            detail = stringResource(R.string.torch_cap_basic_detail),
            status = {
                if (hasFlash) {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.torch_cap_basic_ok))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, noFlashMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_strobe_name),
            detail = stringResource(R.string.torch_cap_strobe_detail),
            status = {
                when {
                    !hasFlash -> CapabilityStatus(GadgetStatusKind.Error, noFlashMsg)
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
                        CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.torch_cap_strobe_caveat))
                    else -> CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.torch_cap_strobe_ok))
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_root_brightness_name),
            detail = stringResource(R.string.torch_cap_root_brightness_detail),
            status = {
                when {
                    root.brightnessReady ->
                        CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.torch_cap_root_ready))
                    root.rootReady ->
                        CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.torch_cap_root_no_led))
                    else -> CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_root_strobe_name),
            detail = stringResource(R.string.torch_cap_root_strobe_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Success, stringResource(R.string.torch_cap_root_ready))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_root_multiled_name),
            detail = stringResource(R.string.torch_cap_root_multiled_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.torch_cap_root_multiled_caveat))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
        ModuleCapability(
            name = stringResource(R.string.torch_cap_root_thermal_name),
            detail = stringResource(R.string.torch_cap_root_thermal_detail),
            status = {
                if (root.rootReady) {
                    CapabilityStatus(GadgetStatusKind.Warning, stringResource(R.string.torch_cap_root_thermal_caveat))
                } else {
                    CapabilityStatus(GadgetStatusKind.Error, needsRootMsg)
                }
            },
        ),
    )
}

@Composable
private fun TorchToggleCard(
    torch: TorchState,
    strobeRunning: Boolean,
    morseText: String,
    onToggleClick: () -> Unit,
    onMomentaryHold: (Boolean) -> Unit,
    onStrobeToggle: () -> Unit,
    onStrobeHold: (Boolean) -> Unit,
    onMorseToggle: () -> Unit,
    onMorseHold: (Boolean) -> Unit,
    onMorseTextChange: (String) -> Unit,
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
                .padding(top = spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.medium),
        ) {
            // Three explicit rows of paired controls — torch + hold,
            // strobe + strobe-hold, morse + morse-hold. Tap variants
            // toggle; hold variants run only while pressed. All drive the
            // one StrobeService. Fixed pairing keeps the layout stable
            // instead of reflowing with width.
            ControlRow {
                CircleControlButton(
                    icon = if (torch.isOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                    contentDescription = stringResource(
                        if (torch.isOn) R.string.torch_action_turn_off
                        else R.string.torch_action_turn_on,
                    ),
                    caption = stringResource(R.string.torch_action_toggle_caption),
                    enabled = torch.isAvailable,
                    active = torch.isOn,
                    hero = true,
                    onClick = onToggleClick,
                )
                CircleControlButton(
                    icon = Icons.Outlined.TouchApp,
                    contentDescription = stringResource(R.string.torch_action_hold),
                    caption = stringResource(R.string.torch_action_hold),
                    enabled = torch.isAvailable,
                    active = torch.isOn,
                    onHold = onMomentaryHold,
                )
            }
            ControlRow {
                CircleControlButton(
                    icon = Icons.Outlined.Bolt,
                    contentDescription = stringResource(R.string.torch_action_strobe_toggle),
                    caption = stringResource(R.string.torch_action_strobe),
                    enabled = torch.isAvailable,
                    active = strobeRunning,
                    onClick = onStrobeToggle,
                )
                CircleControlButton(
                    icon = Icons.Outlined.Bolt,
                    contentDescription = stringResource(R.string.torch_action_strobe_hold),
                    caption = stringResource(R.string.torch_action_strobe_hold),
                    enabled = torch.isAvailable,
                    onHold = onStrobeHold,
                )
            }
            ControlRow {
                CircleControlButton(
                    icon = Icons.Outlined.MoreHoriz,
                    contentDescription = stringResource(R.string.torch_action_morse_toggle),
                    caption = stringResource(R.string.torch_action_morse),
                    enabled = torch.isAvailable,
                    active = strobeRunning,
                    onClick = onMorseToggle,
                )
                CircleControlButton(
                    icon = Icons.Outlined.MoreHoriz,
                    contentDescription = stringResource(R.string.torch_action_morse_hold),
                    caption = stringResource(R.string.torch_action_morse_hold),
                    enabled = torch.isAvailable,
                    onHold = onMorseHold,
                )
            }
            MorseMessageField(
                persisted = morseText,
                onCommit = onMorseTextChange,
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

/** A horizontally-centred pair of [CircleControlButton]s. The torch
 *  screen's three control rows share this so spacing + alignment stay
 *  identical across them. */
@Composable
private fun ControlRow(content: @Composable RowScope.() -> Unit) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacing.medium, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.Top,
        content = content,
    )
}

/**
 * Morse message input for the in-app strobe. Typing updates a local
 * [rememberSaveable] buffer only (no per-keystroke persistence — that
 * round-trip through DataStore was the source of the typing lag), and a
 * trailing confirm (check) action commits the buffer via [onCommit].
 * Commit also fires on IME `Done` and on focus loss so edits aren't
 * silently lost. The check only appears while the buffer differs from
 * the persisted value.
 */
@Composable
private fun MorseMessageField(
    persisted: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var local by rememberSaveable(persisted) { mutableStateOf(persisted) }
    val dirty = local != persisted
    val focusManager = LocalFocusManager.current
    GadgetTextField(
        value = local,
        onValueChange = { local = it },
        label = stringResource(R.string.torch_morse_message_label),
        modifier = modifier.onFocusChanged { focus ->
            if (!focus.isFocused && local != persisted) onCommit(local)
        },
        trailingIcon = if (dirty) Icons.Outlined.Check else null,
        onTrailingIconClick = if (dirty) {
            {
                onCommit(local)
                focusManager.clearFocus()
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                if (local != persisted) onCommit(local)
                focusManager.clearFocus()
            },
        ),
    )
}

/**
 * Circular torch control — the single captioned control used by all six
 * buttons on the screen. Pass [onClick] for tap-to-toggle or [onHold]
 * for press-and-hold (called `true` on press, `false` on release **or**
 * cancel via try/finally so it can't get stuck on). [active] tints it
 * with the primary container to signal the "on" state.
 *
 * [hero] marks the primary action (the torch toggle): it reads as a
 * filled primary surface so it stands out from the secondary controls
 * while keeping the identical circle-over-caption shape, which is what
 * gives the row its consistency.
 */
@Composable
private fun CircleControlButton(
    icon: ImageVector,
    contentDescription: String,
    caption: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    hero: Boolean = false,
    onClick: (() -> Unit)? = null,
    onHold: ((Boolean) -> Unit)? = null,
) {
    val spacing = LocalGadgetTheme.current.spacing
    val container = when {
        !enabled -> MaterialTheme.colorScheme.surfaceVariant
        hero && active -> MaterialTheme.colorScheme.primary
        hero -> MaterialTheme.colorScheme.primaryContainer
        active -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val content = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
        hero && active -> MaterialTheme.colorScheme.onPrimary
        hero -> MaterialTheme.colorScheme.onPrimaryContainer
        active -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }
    val interaction = when {
        onHold != null -> Modifier.pointerInput(enabled) {
            if (!enabled) return@pointerInput
            detectTapGestures(
                onPress = {
                    onHold?.invoke(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        onHold?.invoke(false)
                    }
                },
            )
        }
        onClick != null -> Modifier.clickable(enabled = enabled, role = Role.Button) { onClick?.invoke() }
        else -> Modifier
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(MomentaryButtonDiameter, MomentaryButtonDiameter)
                .size(MomentaryButtonDiameter)
                .clip(CircleShape)
                .background(container)
                .then(interaction)
                .semantics { this.contentDescription = contentDescription; role = Role.Button },
            contentAlignment = Alignment.Center,
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = content)
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
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
    onResolveIcon: (String) -> WidgetIconSource,
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
                    WidgetListRow(
                        widget = widget,
                        onResolveIcon = onResolveIcon,
                        onEdit = { onEditWidget(widget) },
                        onDelete = { onDeleteWidget(widget) },
                    )
                }
            }
        }
    }
}

/**
 * Privileged flashlight controls for the rooted app version. Only placed
 * in the tree when [TorchRootAvailability.rootReady] is true; each button
 * routes through the rooted implementation's `RootSafetyGate` and reports
 * back via a snackbar. One-tap presets — the parameter surface lives in
 * the ViewModel constants.
 */
@Composable
private fun RootToolsCard(
    onBoostBrightness: () -> Unit,
    onDutyStrobe: () -> Unit,
    onMultiLed: () -> Unit,
    onThermal: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(
        modifier = Modifier.fillMaxWidth(),
        title = stringResource(R.string.torch_root_tools_title),
        icon = Icons.Outlined.Bolt,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
            verticalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = stringResource(R.string.torch_root_tools_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GadgetSecondaryButton(
                onClick = onBoostBrightness,
                text = stringResource(R.string.torch_root_action_brightness),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = onDutyStrobe,
                text = stringResource(R.string.torch_root_action_strobe),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = onMultiLed,
                text = stringResource(R.string.torch_root_action_multiled),
                modifier = Modifier.fillMaxWidth(),
            )
            GadgetSecondaryButton(
                onClick = onThermal,
                text = stringResource(R.string.torch_root_action_thermal),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * One row in the in-app widget list. Shows a live [WidgetAppearancePreview]
 * — the exact thumbnail the home-screen widget renders — followed by edit
 * and delete actions. There's deliberately no title/subtitle text: the
 * preview is the identifier, which removes the old (unhelpful) label
 * column entirely.
 */
@Composable
private fun WidgetListRow(
    widget: SavedTorchWidget,
    onResolveIcon: (String) -> WidgetIconSource,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        intensity = GlassIntensity.Subtle,
        contentPadding = PaddingValues(spacing.small),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            WidgetAppearancePreview(
                appearance = widget.config.appearance,
                icon = onResolveIcon(widget.config.appearance.iconStyle.activeKey),
                modifier = Modifier.semantics {
                    contentDescription = widget.config.displayName
                },
            )
            Spacer(modifier = Modifier.weight(1f))
            GadgetIconButton(
                onClick = onEdit,
                icon = Icons.Outlined.Edit,
                contentDescription = stringResource(R.string.torch_widget_list_action_edit),
            )
            GadgetIconButton(
                onClick = onDelete,
                icon = Icons.Outlined.Delete,
                contentDescription = stringResource(R.string.torch_widget_list_action_delete),
            )
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
    val morseText: String = "",
    val rootAvailability: TorchRootAvailability = TorchRootAvailability.Unavailable,
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

/** Circular-control diameter — all six buttons share it so they read as
 *  a consistent grid, and it clears the 48 dp accessibility touch-target
 *  minimum. */
private val MomentaryButtonDiameter: Dp = 56.dp
