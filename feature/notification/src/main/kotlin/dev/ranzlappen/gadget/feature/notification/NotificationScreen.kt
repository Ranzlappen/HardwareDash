package dev.ranzlappen.gadget.feature.notification

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import dev.ranzlappen.gadget.core.ui.ModuleScreenScaffold
import dev.ranzlappen.gadget.feature.notification.components.ChannelInspectorCard
import dev.ranzlappen.gadget.feature.notification.components.ListenerAccessCard
import dev.ranzlappen.gadget.feature.notification.components.LockScreenOverlayCard
import dev.ranzlappen.gadget.feature.notification.components.NotificationBuilderCard
import dev.ranzlappen.gadget.feature.notification.components.ResetAllCard
import dev.ranzlappen.gadget.feature.notification.components.StickyOverrideCard
import dev.ranzlappen.gadget.feature.notification.components.notificationModuleInfo
import dev.ranzlappen.gadget.feature.notification.control.NotificationControllerResult
import dev.ranzlappen.gadget.feature.notification.monitor.ActiveNotificationsMetricSource
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import kotlinx.coroutines.launch

/**
 * Notification screen — Hilt-wrapped stateful entry point.
 *
 * Thin shell over the stateless [NotificationScreenContent], mirroring
 * `TorchScreen` / `VibrationScreen`:
 *  - Injects [NotificationViewModel] via Hilt.
 *  - Observes the state flow + the one-shot `resultEvents` flow, surfacing
 *    every rooted-controller result as a snackbar.
 *  - Calls [NotificationViewModel.onScreenExit] from a `DisposableEffect` so
 *    every rooted mutation this screen made reverts on navigate-away, per
 *    `NotificationController.revertOnScreenExit`'s contract.
 */
@Composable
fun NotificationScreen(
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resultOk = stringResource(R.string.notification_root_result_ok)
    val resultUnsupported = stringResource(R.string.notification_root_result_unsupported)
    val resultOptedOut = stringResource(R.string.notification_root_result_opted_out)
    val resultRateLimited = stringResource(R.string.notification_root_result_rate_limited)
    val resultErrorFmt = stringResource(R.string.notification_root_result_error)
    val resultResetFmt = stringResource(R.string.notification_root_result_reset)
    val resultSnapshotFmt = stringResource(R.string.notification_root_result_channel_snapshot)
    val optedOutAction = stringResource(R.string.notification_root_opted_out_action)
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose { viewModel.onScreenExit() }
    }

    LaunchedEffect(Unit) {
        viewModel.resultEvents.collect { result ->
            val message = when (result) {
                is NotificationControllerResult.Ok -> resultOk
                NotificationControllerResult.Unsupported -> resultUnsupported
                NotificationControllerResult.OptedOut -> resultOptedOut
                is NotificationControllerResult.RateLimited -> resultRateLimited
                is NotificationControllerResult.HardwareError -> resultErrorFmt.format(result.message)
                is NotificationControllerResult.ResetCompleted ->
                    resultResetFmt.format(result.restored, result.failed)
                is NotificationControllerResult.ChannelImportanceSnapshot -> resultSnapshotFmt.format(
                    result.channelId,
                    result.previousImportance,
                    result.newImportance,
                )
            }
            if (result == NotificationControllerResult.OptedOut) {
                scope.launch {
                    val outcome = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = optedOutAction,
                        duration = SnackbarDuration.Long,
                    )
                    if (outcome == SnackbarResult.ActionPerformed) onNavigateToSettings()
                }
            } else {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            }
        }
    }

    NotificationScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        modifier = modifier,
        monitor = {
            MonitorContainer(
                metricKey = ActiveNotificationsMetricSource.METRIC_KEY,
                title = stringResource(R.string.notification_active_notifications_monitor),
            )
        },
        liveMonitor = {
            LiveMonitorContainer(
                metricKey = ActiveNotificationsMetricSource.METRIC_KEY,
                title = stringResource(R.string.notification_active_notifications_live_monitor),
            )
        },
    )

    SnackbarHost(hostState = snackbarHostState)
}

/**
 * Stateless NotificationScreen content — a single [NotificationScreenState]
 * snapshot plus a flat [NotificationUiEvent] dispatcher. The monitor /
 * live-monitor tiles are injected as slots (supplied by the Hilt route) so
 * this stays Hilt-free for previews/tests.
 *
 * Card order:
 *  1. **Notification builder** (standard) — title/body/importance + post/cancel.
 *  2. **Channel inspector** (standard) — every owned channel + live importance.
 *  3. Monitoring tiles for `active_notifications` (both flavors — the metric
 *     just reports 0 until the listener is granted).
 *  4. **Sticky channel importance override** (rooted only).
 *  5. **Programmatic listener access** (rooted only).
 *  6. **Lock-screen overlay test** (rooted only).
 *  7. **Reset all** (rooted only).
 */
@Composable
fun NotificationScreenContent(
    state: NotificationScreenState,
    onEvent: (NotificationUiEvent) -> Unit,
    modifier: Modifier = Modifier,
    monitor: @Composable () -> Unit = {},
    liveMonitor: @Composable () -> Unit = {},
) {
    ModuleScreenScaffold(
        title = stringResource(R.string.notification_screen_title),
        modifier = modifier,
        functional = {
            NotificationBuilderCard(
                title = state.builderTitle,
                body = state.builderBody,
                importance = state.builderImportance,
                hasPostedNotification = state.lastPostedNotificationId != null,
                onTitleChange = { onEvent(NotificationUiEvent.BuilderTitleChange(it)) },
                onBodyChange = { onEvent(NotificationUiEvent.BuilderBodyChange(it)) },
                onImportanceChange = { onEvent(NotificationUiEvent.BuilderImportanceChange(it)) },
                onPost = { onEvent(NotificationUiEvent.PostTestNotification) },
                onCancel = { onEvent(NotificationUiEvent.CancelTestNotification) },
            )
            ChannelInspectorCard(
                channels = state.channels,
                onRefresh = { onEvent(NotificationUiEvent.RefreshChannels) },
            )
            liveMonitor()
            monitor()
            if (state.isRootedFlavor) {
                StickyOverrideCard(
                    channelId = state.stickyChannelId,
                    onChannelIdChange = { onEvent(NotificationUiEvent.StickyChannelIdChange(it)) },
                    onOverrideRequest = { onEvent(NotificationUiEvent.StickyOverrideRequest) },
                )
                ListenerAccessCard(
                    listenerConnected = state.listenerConnected,
                    onGrantRequest = { onEvent(NotificationUiEvent.GrantListenerAccessRequest) },
                )
                LockScreenOverlayCard(
                    message = state.overlayMessage,
                    durationMillis = state.overlayDurationMillis,
                    onMessageChange = { onEvent(NotificationUiEvent.OverlayMessageChange(it)) },
                    onDurationChange = { onEvent(NotificationUiEvent.OverlayDurationChange(it)) },
                    onShowOverlayRequest = { onEvent(NotificationUiEvent.ShowOverlayRequest) },
                )
                ResetAllCard(
                    onResetAllRequest = { onEvent(NotificationUiEvent.ResetAllRequest) },
                )
            }
        },
        moduleInfo = notificationModuleInfo(
            state = state,
            onOpenListenerSettings = { onEvent(NotificationUiEvent.OpenListenerSettings) },
        ),
    )
}

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun NotificationScreenStandardPreview() = GadgetThemedPreview {
    NotificationScreenContent(
        state = NotificationScreenState.Initial.copy(
            isRootedFlavor = false,
            channels = listOf(
                NotificationChannelSummary("notification_builder_default", "Test — Default importance", 3),
            ),
        ),
        onEvent = {},
    )
}

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun NotificationScreenRootedPreview() = GadgetThemedPreview {
    NotificationScreenContent(
        state = NotificationScreenState.Initial.copy(
            isRootedFlavor = true,
            listenerConnected = true,
            activeNotificationCount = 3,
            channels = listOf(
                NotificationChannelSummary("notification_builder_default", "Test — Default importance", 3),
                NotificationChannelSummary("notification_builder_high", "Test — High importance", 4),
            ),
            stickyChannelId = "notification_builder_default",
        ),
        onEvent = {},
    )
}
