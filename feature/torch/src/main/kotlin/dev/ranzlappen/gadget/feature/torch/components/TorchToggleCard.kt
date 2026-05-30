package dev.ranzlappen.gadget.feature.torch.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetCircleControl
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetTextField
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchState
import dev.ranzlappen.gadget.feature.torch.statusMessage

@Composable
internal fun TorchToggleCard(
    torch: TorchState,
    strobeRunning: Boolean,
    morseText: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onToggleClick: () -> Unit,
    onMomentaryHold: (Boolean) -> Unit,
    onStrobeToggle: () -> Unit,
    onStrobeHold: (Boolean) -> Unit,
    onMorseToggle: () -> Unit,
    onMorseHold: (Boolean) -> Unit,
    onMorseTextChange: (String) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetExpandableCard(
        title = stringResource(
            if (torch.isOn) R.string.torch_state_on else R.string.torch_state_off,
        ),
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth(),
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
                GadgetCircleControl(
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
                GadgetCircleControl(
                    icon = Icons.Outlined.TouchApp,
                    contentDescription = stringResource(R.string.torch_action_hold),
                    caption = stringResource(R.string.torch_action_hold),
                    enabled = torch.isAvailable,
                    active = torch.isOn,
                    onHold = onMomentaryHold,
                )
            }
            ControlRow {
                GadgetCircleControl(
                    icon = Icons.Outlined.Bolt,
                    contentDescription = stringResource(R.string.torch_action_strobe_toggle),
                    caption = stringResource(R.string.torch_action_strobe),
                    enabled = torch.isAvailable,
                    active = strobeRunning,
                    onClick = onStrobeToggle,
                )
                GadgetCircleControl(
                    icon = Icons.Outlined.Bolt,
                    contentDescription = stringResource(R.string.torch_action_strobe_hold),
                    caption = stringResource(R.string.torch_action_strobe_hold),
                    enabled = torch.isAvailable,
                    onHold = onStrobeHold,
                )
            }
            ControlRow {
                GadgetCircleControl(
                    icon = Icons.Outlined.MoreHoriz,
                    contentDescription = stringResource(R.string.torch_action_morse_toggle),
                    caption = stringResource(R.string.torch_action_morse),
                    enabled = torch.isAvailable,
                    active = strobeRunning,
                    onClick = onMorseToggle,
                )
                GadgetCircleControl(
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

/** A horizontally-centred pair of [GadgetCircleControl]s. The torch
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
