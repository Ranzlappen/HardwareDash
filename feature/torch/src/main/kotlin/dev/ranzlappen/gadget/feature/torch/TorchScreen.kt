package dev.ranzlappen.gadget.feature.torch

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.feature.torch.ui.WidgetConfigurationSheet

/**
 * Torch / flashlight screen — Hilt-wrapped stateful entry point.
 *
 * Thin shell over the stateless [TorchScreenContent]. Responsibilities
 * kept here (not in [TorchScreenContent]) so the inner composable
 * remains pure and testable:
 *
 *  - Inject [TorchViewModel] via Hilt.
 *  - Observe the view-state flow + the transient `sheetTarget` flow.
 *  - Own the [SnackbarHostState] for the "launcher doesn't support
 *    pin" transient message.
 *  - Conditionally render [WidgetConfigurationSheet] when
 *    `sheetTarget != null`.
 *
 * All four torch surfaces (this screen, the QS tile, both home
 * widgets) converge on the same `@Singleton TorchController`, so
 * toggling from any path flows through `TorchCallback` into this
 * screen's `TorchScreenState.torch` snapshot.
 *
 * Rooted-flavor extras (brightness, multi-LED, thermal override)
 * land in a sibling `:feature:torch-rooted` module — see issues
 * https://github.com/Ranzlappen/HardwareDash/issues/94 and
 * https://github.com/Ranzlappen/HardwareDash/issues/95.
 */
@Composable
fun TorchScreen(
    modifier: Modifier = Modifier,
    viewModel: TorchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val sheetTarget by viewModel.sheetTarget.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val pinUnsupportedMessage = stringResource(R.string.torch_widget_pin_unsupported)

    LaunchedEffect(Unit) {
        viewModel.pinUnsupportedEvents.collect {
            snackbarHostState.showSnackbar(
                message = pinUnsupportedMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    TorchScreenContent(
        state = state,
        onToggleClick = viewModel::onToggleClick,
        onMomentaryHold = viewModel::onMomentaryHold,
        onStrobeToggle = viewModel::onStrobeToggle,
        onStrobeHold = viewModel::onStrobeHold,
        onMorseToggle = viewModel::onMorseToggle,
        onMorseHold = viewModel::onMorseHold,
        onMorseTextChange = viewModel::onMorseTextChange,
        onRateChange = viewModel::onRateChange,
        onRateCommit = viewModel::onRateCommit,
        onAddFlashlight = viewModel::onAddFlashlight,
        onAddStrobe = viewModel::onAddStrobeRequested,
        onEditWidget = viewModel::onEditWidget,
        onDeleteWidget = viewModel::onDeleteWidget,
        onResolveIcon = viewModel::resolveWidgetIcon,
        modifier = modifier,
    )

    sheetTarget?.let { target ->
        WidgetConfigurationSheet(
            initial = target.config,
            isExisting = target is TorchViewModel.SheetTarget.Existing,
            onDismiss = viewModel::onSheetDismissed,
            onConfirm = viewModel::onSheetConfirmed,
            resolveIconRes = viewModel::resolveWidgetIcon,
            iconChoices = viewModel.iconChoices,
        )
    }

    SnackbarHost(hostState = snackbarHostState)
}
