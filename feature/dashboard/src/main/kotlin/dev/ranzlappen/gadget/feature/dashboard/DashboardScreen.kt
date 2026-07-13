package dev.ranzlappen.gadget.feature.dashboard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.navigation.GadgetDestination
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSecondaryButton
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview

/**
 * App home screen (W9). Enumerates the [GadgetDestination.modules] catalog in
 * the user's saved [dev.ranzlappen.gadget.core.datastore.DashboardLayout]
 * (pinned first, hidden dropped) as tappable tiles, with an **Edit** affordance
 * that opens [DashboardEditorSheet] to reorder / hide / pin entries.
 *
 * [onNavigate] dispatches by route; the host decides whether to use
 * `navigateTopLevel` (top-level destinations) or `navigate(...)` (sub-routes).
 * The public signature is unchanged so the nav graph / host need no edit — the
 * ViewModel is Hilt-provided.
 */
@Composable
fun DashboardScreen(
    onNavigate: (GadgetDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showEditor by rememberSaveable { mutableStateOf(false) }

    DashboardScreenContent(
        entries = state.visible,
        onNavigate = onNavigate,
        onEdit = { showEditor = true },
        modifier = modifier,
    )

    if (showEditor) {
        DashboardEditorSheet(
            entries = state.entries,
            onMoveUp = viewModel::moveUp,
            onMoveDown = viewModel::moveDown,
            onSetHidden = viewModel::setHidden,
            onSetPinned = viewModel::setPinned,
            onReset = viewModel::resetLayout,
            onDismiss = { showEditor = false },
        )
    }
}

@Composable
internal fun DashboardScreenContent(
    entries: List<DashboardEntry>,
    onNavigate: (GadgetDestination) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModuleScreenScaffold(
        title = "Dashboard",
        modifier = modifier,
        functional = {
            GadgetSecondaryButton(
                onClick = onEdit,
                text = "Edit dashboard",
                leadingIcon = Icons.Outlined.Edit,
                modifier = Modifier.fillMaxWidth(),
            )
            if (entries.isEmpty()) {
                DashCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "Dashboard",
                ) {
                    Text(
                        text = "Every module is hidden. Tap “Edit dashboard” to show some.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                entries.forEach { entry ->
                    DashCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = entry.destination.label,
                        icon = entry.destination.iconOutlined,
                        onClick = { onNavigate(entry.destination) },
                    ) {}
                }
            }
        },
    )
}

@GadgetPreviewLightDark
@Composable
private fun DashboardScreenPreview() = GadgetThemedPreview {
    DashboardScreenContent(
        entries = GadgetDestination.modules.take(4).mapIndexed { i, d ->
            DashboardEntry(destination = d, hidden = false, pinned = i == 0)
        },
        onNavigate = {},
        onEdit = {},
    )
}
