package dev.ranzlappen.gadget.feature.torch

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.core.widgetkit.WidgetPinPolicy
import dev.ranzlappen.gadget.feature.torch.monitor.TorchMetricSource
import dev.ranzlappen.gadget.feature.torch.ui.WidgetConfigurationSheet
import kotlinx.coroutines.launch

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
    onNavigateToSettings: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetTarget by viewModel.sheetTarget.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pinUnsupportedMessage = stringResource(R.string.torch_widget_pin_unsupported)
    val widgetRemovedMessage = stringResource(R.string.torch_widget_removed_hint)
    val capReachedMessage = stringResource(
        R.string.torch_widget_cap_reached,
        WidgetPinPolicy.MAX_WIDGETS_PER_KIND,
    )
    val rootResultOk = stringResource(R.string.torch_root_result_ok)
    val rootResultUnsupported = stringResource(R.string.torch_root_result_unsupported)
    val rootResultOptedOut = stringResource(R.string.torch_root_result_opted_out)
    val rootResultRateLimited = stringResource(R.string.torch_root_result_rate_limited)
    val rootResultErrorFmt = stringResource(R.string.torch_root_result_error)
    val rootOptedOutAction = stringResource(R.string.torch_root_opted_out_action)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.pinUnsupportedEvents.collect {
            snackbarHostState.showSnackbar(
                message = pinUnsupportedMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.widgetRemovedEvents.collect {
            snackbarHostState.showSnackbar(
                message = widgetRemovedMessage,
                duration = SnackbarDuration.Long,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.pinCapReachedEvents.collect {
            snackbarHostState.showSnackbar(
                message = capReachedMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.rootToolEvents.collect { result ->
            val message = when (result) {
                TorchRootResult.Ok -> rootResultOk
                TorchRootResult.Unsupported -> rootResultUnsupported
                TorchRootResult.OptedOut -> rootResultOptedOut
                is TorchRootResult.RateLimited -> rootResultRateLimited
                is TorchRootResult.Error -> rootResultErrorFmt.format(result.message)
            }
            // For the "turned off in settings" case, offer an action that
            // deep-links to the Settings screen where the per-feature opt-in
            // (and the safety-mode master switch) live — otherwise the toast
            // points at settings the user can't find their way to. Launch the
            // long-duration action snackbar in a separate coroutine so the
            // collector doesn't block (and drop) subsequent tool results while
            // it's shown.
            if (result == TorchRootResult.OptedOut) {
                scope.launch {
                    val outcome = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = rootOptedOutAction,
                        duration = SnackbarDuration.Long,
                    )
                    if (outcome == SnackbarResult.ActionPerformed) onNavigateToSettings()
                }
            } else {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            }
        }
    }

    TorchScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onResolveIcon = viewModel::resolveWidgetIcon,
        modifier = modifier,
        monitor = {
            MonitorContainer(
                metricKey = TorchMetricSource.METRIC_KEY,
                title = stringResource(R.string.torch_monitor_title),
                collapseId = TorchSectionId.Monitor,
            )
        },
        liveMonitor = {
            LiveMonitorContainer(
                metricKey = TorchMetricSource.METRIC_KEY,
                title = stringResource(R.string.torch_live_monitor_title),
                collapseId = TorchSectionId.LiveMonitor,
            )
        },
    )

    sheetTarget?.let { target ->
        WidgetConfigurationSheet(
            initial = target.config,
            isExisting = target is TorchViewModel.SheetTarget.Existing,
            onDismiss = viewModel::onSheetDismissed,
            onConfirm = viewModel::onSheetConfirmed,
            resolveIcon = viewModel::resolveWidgetIcon,
            onImportCustomIcon = viewModel::importCustomIcon,
            iconChoices = viewModel.iconChoices,
        )
    }

    SnackbarHost(hostState = snackbarHostState)
}
