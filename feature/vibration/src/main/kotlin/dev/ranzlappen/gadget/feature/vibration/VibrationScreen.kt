package dev.ranzlappen.gadget.feature.vibration

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
import dev.ranzlappen.gadget.feature.vibration.monitor.VibrationMetricSource
import dev.ranzlappen.gadget.feature.vibration.ui.WidgetConfigurationSheet
import kotlinx.coroutines.launch

/**
 * Vibration screen — Hilt-wrapped stateful entry point. Thin shell over the
 * stateless [VibrationScreenContent]: injects [VibrationViewModel], owns the
 * snackbar host (pin-unsupported / cap-reached / widget-removed / rooted
 * results), supplies the monitor + live-monitor slots, and conditionally
 * renders the [WidgetConfigurationSheet]. Mirror of `TorchScreen`.
 *
 * [onNavigateToSettings] lets the rooted `OptedOut` snackbar deep-link to the
 * Settings screen where the rooted-feature opt-in lives.
 */
@Composable
fun VibrationScreen(
    modifier: Modifier = Modifier,
    viewModel: VibrationViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetTarget by viewModel.sheetTarget.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val pinUnsupportedMessage = stringResource(R.string.vibration_widget_pin_unsupported)
    val widgetRemovedMessage = stringResource(R.string.vibration_widget_removed_hint)
    val capReachedMessage = stringResource(R.string.vibration_widget_cap_reached, WidgetPinPolicy.MAX_WIDGETS_PER_KIND)
    val rootResultOk = stringResource(R.string.vibration_root_result_ok)
    val rootResultUnsupported = stringResource(R.string.vibration_root_result_unsupported)
    val rootResultOptedOut = stringResource(R.string.vibration_root_result_opted_out)
    val rootResultRateLimited = stringResource(R.string.vibration_root_result_rate_limited)
    val rootResultErrorFmt = stringResource(R.string.vibration_root_result_error)
    val rootOptedOutAction = stringResource(R.string.vibration_root_opted_out_action)

    LaunchedEffect(Unit) {
        viewModel.pinUnsupportedEvents.collect {
            snackbarHostState.showSnackbar(pinUnsupportedMessage, duration = SnackbarDuration.Short)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.widgetRemovedEvents.collect {
            snackbarHostState.showSnackbar(widgetRemovedMessage, duration = SnackbarDuration.Long)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.pinCapReachedEvents.collect {
            snackbarHostState.showSnackbar(capReachedMessage, duration = SnackbarDuration.Short)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.rootToolEvents.collect { result ->
            val message = when (result) {
                VibrationRootResult.Ok -> rootResultOk
                VibrationRootResult.Unsupported -> rootResultUnsupported
                VibrationRootResult.OptedOut -> rootResultOptedOut
                is VibrationRootResult.RateLimited -> rootResultRateLimited
                is VibrationRootResult.Error -> rootResultErrorFmt.format(result.message)
            }
            if (result == VibrationRootResult.OptedOut) {
                scope.launch {
                    val outcome = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = rootOptedOutAction,
                        duration = SnackbarDuration.Long,
                    )
                    if (outcome == SnackbarResult.ActionPerformed) onNavigateToSettings()
                }
            } else {
                snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
            }
        }
    }

    VibrationScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onResolveIcon = viewModel::resolveWidgetIcon,
        modifier = modifier,
        monitor = {
            MonitorContainer(
                metricKey = VibrationMetricSource.METRIC_KEY,
                title = stringResource(R.string.vibration_monitor_title),
                collapseId = VibrationSectionId.Monitor,
            )
        },
        liveMonitor = {
            LiveMonitorContainer(
                metricKey = VibrationMetricSource.METRIC_KEY,
                title = stringResource(R.string.vibration_live_monitor_title),
                collapseId = VibrationSectionId.LiveMonitor,
            )
        },
    )

    sheetTarget?.let { target ->
        WidgetConfigurationSheet(
            initial = target.config,
            isExisting = target is VibrationViewModel.SheetTarget.Existing,
            onDismiss = viewModel::onSheetDismissed,
            onConfirm = { viewModel.onEvent(VibrationUiEvent.SheetConfirmed(it)) },
            resolveIcon = viewModel::resolveWidgetIcon,
            onImportCustomIcon = viewModel::importCustomIcon,
            iconChoices = viewModel.iconChoices,
        )
    }

    SnackbarHost(hostState = snackbarHostState)
}
