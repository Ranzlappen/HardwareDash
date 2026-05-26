package dev.ranzlappen.gadget.core.monitoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.data.MonitorBucket
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import kotlin.math.roundToInt

/**
 * Drop-in monitoring tile for any feature: a glass [DashCard] with a live
 * chart, an on/off toggle, and a persistent settings block (sample
 * interval, time window, chart style, "show as widget", "show as
 * notification").
 *
 * Self-contained — supply a `metricKey` (matching a contributed
 * [dev.ranzlappen.gadget.core.model.MetricSource]) and a [title]; the
 * config + history are read from the framework's repos via Hilt. Embed it
 * straight into a feature screen's content slot.
 */
@Composable
fun MonitorContainer(
    metricKey: String,
    title: String,
    modifier: Modifier = Modifier,
    viewModel: MonitorViewModel = hiltViewModel(key = metricKey),
) {
    val configFlow = remember(metricKey, viewModel) { viewModel.config(metricKey) }
    val historyFlow = remember(metricKey, viewModel) { viewModel.history(metricKey) }
    val config by configFlow.collectAsState(initial = MonitorConfig())
    val history by historyFlow.collectAsState(initial = EmptyHistory)
    val yMax = remember(metricKey, viewModel) { viewModel.maxValue(metricKey) }

    MonitorContent(
        title = title,
        config = config,
        history = history,
        yMax = yMax,
        onConfigChange = { viewModel.update(metricKey, it) },
        modifier = modifier,
    )
}

/**
 * Stateless monitoring tile. Hoisted from [MonitorContainer] so it stays
 * preview- and test-friendly (no Hilt). Feature screens embed the
 * stateful [MonitorContainer]; this is the rendering layer.
 */
@Composable
fun MonitorContent(
    title: String,
    config: MonitorConfig,
    history: MonitorHistory,
    yMax: Float,
    onConfigChange: (MonitorConfig) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalGadgetTheme.current.spacing
    DashCard(modifier = modifier, title = title, icon = Icons.Outlined.ShowChart) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
            ToggleRow(
                label = stringResource(R.string.monitor_toggle_label),
                checked = config.enabled,
                onCheckedChange = { onConfigChange(config.copy(enabled = it)) },
            )

            MonitorChart(
                buckets = history.buckets,
                bucketMs = history.bucketMs,
                windowMs = history.windowMs,
                layout = config.chartLayout,
                yMax = yMax,
            )

            if (config.enabled) {
                // Local draft so dragging doesn't hammer DataStore at 60 Hz;
                // persist once on release via onValueChangeFinished. Re-seeds
                // when the stored value changes externally.
                var pollDraftSeconds by remember(config.pollIntervalMs) {
                    mutableStateOf(config.pollIntervalMs / 1_000f)
                }
                GadgetSlider(
                    value = pollDraftSeconds,
                    onValueChange = { pollDraftSeconds = it },
                    valueRange = POLL_MIN_S..POLL_MAX_S,
                    label = stringResource(R.string.monitor_poll_interval_label),
                    suffix = stringResource(R.string.monitor_poll_interval_suffix),
                    valueFormatter = { formatSeconds(it) },
                    onValueChangeFinished = {
                        onConfigChange(
                            config.copy(
                                pollIntervalMs = (pollDraftSeconds * 1_000f).toLong()
                                    .coerceAtLeast(250L),
                            ),
                        )
                    },
                )

                // Time window in minutes (1m..24h). One value drives both the
                // in-app chart and the chart widget.
                var windowDraftMinutes by remember(config.windowSeconds) {
                    mutableStateOf(config.windowSeconds / 60f)
                }
                GadgetSlider(
                    value = windowDraftMinutes,
                    onValueChange = { windowDraftMinutes = it },
                    valueRange = WINDOW_MIN_MIN..WINDOW_MAX_MIN,
                    label = stringResource(R.string.monitor_window_label),
                    valueFormatter = { formatWindowMinutes(it) },
                    onValueChangeFinished = {
                        onConfigChange(
                            config.copy(
                                windowSeconds = (windowDraftMinutes * 60f).roundToInt()
                                    .coerceIn(
                                        MonitorConfig.MIN_WINDOW_SECONDS,
                                        MonitorConfig.MAX_WINDOW_SECONDS,
                                    ),
                            ),
                        )
                    },
                )

                Text(
                    text = stringResource(R.string.monitor_layout_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.tiny)) {
                    MonitorChartLayout.entries.forEach { layout ->
                        GadgetChip(
                            selected = config.chartLayout == layout,
                            onClick = { onConfigChange(config.copy(chartLayout = layout)) },
                            label = layoutLabel(layout),
                        )
                    }
                }

                ToggleRow(
                    label = stringResource(R.string.monitor_show_widget),
                    checked = config.widgetEnabled,
                    onCheckedChange = { onConfigChange(config.copy(widgetEnabled = it)) },
                )
                ToggleRow(
                    label = stringResource(R.string.monitor_show_notification),
                    checked = config.notificationEnabled,
                    onCheckedChange = { onConfigChange(config.copy(notificationEnabled = it)) },
                )
            }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun layoutLabel(layout: MonitorChartLayout): String = stringResource(
    when (layout) {
        MonitorChartLayout.Line -> R.string.monitor_layout_line
        MonitorChartLayout.Area -> R.string.monitor_layout_area
        MonitorChartLayout.Bars -> R.string.monitor_layout_bars
    },
)

private fun formatSeconds(seconds: Float): String =
    if (seconds >= 1f) "${seconds.toInt()}" else String.format("%.2f", seconds)

/** "Xm" under an hour, otherwise whole/half hours ("2h", "1.5h"). */
private fun formatWindowMinutes(minutes: Float): String {
    val m = minutes.roundToInt()
    if (m < 60) return "${m}m"
    val hours = m / 60f
    return if (hours % 1f == 0f) "${hours.toInt()}h" else String.format("%.1fh", hours)
}

private const val POLL_MIN_S = 0.25f
private const val POLL_MAX_S = 10f
private val WINDOW_MIN_MIN = MonitorConfig.MIN_WINDOW_SECONDS / 60f
private val WINDOW_MAX_MIN = MonitorConfig.MAX_WINDOW_SECONDS / 60f

private val EmptyHistory = MonitorHistory(
    buckets = emptyList(),
    bucketMs = 1_000L,
    windowMs = MonitorConfig.DEFAULT_WINDOW_SECONDS.toLong() * 1_000L,
)

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun MonitorContentPreview() = GadgetThemedPreview {
    val buckets = List(30) { i ->
        MonitorBucket(bucket = i.toLong(), maxValue = if (i % 4 == 0) 100f else 0f)
    }
    MonitorContent(
        title = "Torch activity",
        config = MonitorConfig(enabled = true),
        history = MonitorHistory(buckets = buckets, bucketMs = 1_000L, windowMs = 30_000L),
        yMax = 100f,
        onConfigChange = {},
    )
}
