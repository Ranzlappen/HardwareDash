package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.GadgetIconButton
import dev.ranzlappen.gadget.core.ui.component.GadgetTertiaryButton
import dev.ranzlappen.gadget.core.ui.component.GadgetBottomSheet

/**
 * The dashboard editor (W9): a bottom sheet listing every module entry in the
 * user's saved order with move-up / move-down, a pin toggle, and a show/hide
 * toggle. Fully state-hoisted — [DashboardViewModel] owns the persisted layout
 * and each callback maps to one of its mutations. A "Reset to default" action
 * clears the saved layout.
 *
 * Drag-reorder is deliberately avoided (no reorderable dependency in the repo);
 * move-up/move-down from the shared `GadgetIconButton` keeps the interaction on
 * design-system primitives.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardEditorSheet(
    entries: List<DashboardEntry>,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onSetHidden: (String, Boolean) -> Unit,
    onSetPinned: (String, Boolean) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    GadgetBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.dashboard_edit),
    ) {
        entries.forEachIndexed { index, entry ->
            DashboardEditorRow(
                entry = entry,
                isFirst = index == 0,
                isLast = index == entries.lastIndex,
                onMoveUp = { onMoveUp(entry.destination.route) },
                onMoveDown = { onMoveDown(entry.destination.route) },
                onSetHidden = { hidden -> onSetHidden(entry.destination.route, hidden) },
                onSetPinned = { pinned -> onSetPinned(entry.destination.route, pinned) },
            )
            if (index != entries.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
        GadgetTertiaryButton(
            onClick = onReset,
            text = stringResource(R.string.dashboard_reset),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.small),
        )
    }
}

@Composable
private fun DashboardEditorRow(
    entry: DashboardEntry,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onSetHidden: (Boolean) -> Unit,
    onSetPinned: (Boolean) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.tiny),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.tiny),
    ) {
        Text(
            text = entry.destination.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (entry.hidden) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        GadgetIconButton(
            onClick = onMoveUp,
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = stringResource(R.string.dashboard_move_up),
            enabled = !isFirst,
        )
        GadgetIconButton(
            onClick = onMoveDown,
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.dashboard_move_down),
            enabled = !isLast,
        )
        GadgetIconButton(
            onClick = { onSetPinned(!entry.pinned) },
            icon = if (entry.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
            contentDescription = stringResource(
                if (entry.pinned) R.string.dashboard_unpin else R.string.dashboard_pin,
            ),
            tint = if (entry.pinned) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Switch(
            checked = !entry.hidden,
            onCheckedChange = { show -> onSetHidden(!show) },
        )
    }
}
