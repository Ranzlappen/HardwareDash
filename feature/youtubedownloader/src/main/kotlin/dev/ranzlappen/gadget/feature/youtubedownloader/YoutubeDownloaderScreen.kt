package dev.ranzlappen.gadget.feature.youtubedownloader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.ranzlappen.gadget.core.monitoring.LiveMonitorContainer
import dev.ranzlappen.gadget.core.monitoring.MonitorContainer
import dev.ranzlappen.gadget.feature.youtubedownloader.monitor.DownloadMetricSource

/**
 * Hilt-wrapped stateful entry point. Thin shell over the stateless
 * [YoutubeDownloaderScreenContent]: observes the view-state and supplies the
 * monitoring tiles bound to [DownloadMetricSource].
 *
 * [onNavigateToLogin] opens the in-app cookie-capture WebView route (see
 * [youtubeDownloaderScreen]).
 */
@Composable
fun YoutubeDownloaderScreen(
    modifier: Modifier = Modifier,
    viewModel: YoutubeDownloaderViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    YoutubeDownloaderScreenContent(
        state = state,
        onEvent = viewModel::onEvent,
        onSignIn = onNavigateToLogin,
        modifier = modifier,
        monitor = {
            MonitorContainer(
                metricKey = DownloadMetricSource.METRIC_KEY,
                title = stringResource(R.string.ytdl_monitor_title),
                collapseId = "ytdl_monitor",
            )
        },
        liveMonitor = {
            LiveMonitorContainer(
                metricKey = DownloadMetricSource.METRIC_KEY,
                title = stringResource(R.string.ytdl_live_monitor_title),
                collapseId = "ytdl_live_monitor",
            )
        },
    )
}
