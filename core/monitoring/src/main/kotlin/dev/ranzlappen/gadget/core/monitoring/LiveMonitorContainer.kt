package dev.ranzlappen.gadget.core.monitoring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import dev.ranzlappen.gadget.core.ui.component.DashCard
import dev.ranzlappen.gadget.core.ui.component.GadgetChip
import dev.ranzlappen.gadget.core.ui.component.GadgetExpandableCard
import dev.ranzlappen.gadget.core.ui.component.GadgetSlider
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLargeFont
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewLightDark
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewRtl
import dev.ranzlappen.gadget.core.ui.preview.GadgetPreviewSizeClasses
import dev.ranzlappen.gadget.core.ui.preview.GadgetThemedPreview
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Drop-in **live** monitoring tile: the in-memory, realtime companion to
 * [MonitorContainer]. The two are independent — this one bypasses Room /
 * the foreground service and reads the [dev.ranzlappen.gadget.core.model.MetricSource]
 * directly into a fast-updating, auto-scaling chart for live analysis.
 *
 * Supply a `metricKey` (matching a contributed `MetricSource`) and a [title];
 * pass [collapseId] to make the card collapsible with persisted state.
 * Sampling is bound to composition (via a `DisposableEffect`) so it stops
 * when the card leaves the screen, and gated by the in-card "Live stream"
 * toggle.
 */
@Composable
fun LiveMonitorContainer(
    metricKey: String,
    title: String,
    modifier: Modifier = Modifier,
    collapseId: String? = null,
    viewModel: LiveMonitorViewModel = hiltViewModel(key = "$metricKey#live"),
) {
    DisposableEffect(metricKey, viewModel) {
        viewModel.start(metricKey)
        onDispose { viewModel.stop() }
    }
    val enabled by viewModel.enabled.collectAsState()
    val frozen by viewModel.frozen.collectAsState()
    val trace by viewModel.trace.collectAsState()
    val intervalMs by viewModel.intervalMs.collectAsState()
    val windowMs by viewModel.windowMs.collectAsState()
    val unit = remember(metricKey, viewModel) { viewModel.unit(metricKey) }

    val body: @Composable () -> Unit = {
        LiveMonitorBody(
            enabled = enabled,
            frozen = frozen,
            trace = trace,
            unit = unit,
            intervalMs = intervalMs,
            windowMs = windowMs,
            onEnabledChange = viewModel::setEnabled,
            onToggleFreeze = viewModel::toggleFreeze,
            onIntervalChange = viewModel::setIntervalMs,
            onWindowChange = viewModel::setWindowMs,
        )
    }

    if (collapseId == null) {
        DashCard(modifier = modifier, title = title, icon = Icons.Outlined.Timeline, content = body)
    } else {
        val expandedFlow = remember(collapseId, viewModel) { viewModel.expanded(collapseId) }
        val expanded by expandedFlow.collectAsState(initial = true)
        GadgetExpandableCard(
            title = title,
            expanded = expanded,
            onExpandedChange = { viewModel.setExpanded(collapseId, it) },
            modifier = modifier,
            icon = Icons.Outlined.Timeline,
            content = body,
        )
    }
}

/**
 * Stateless live tile rendered inside a [DashCard] — preview/test entry
 * point. The stateful [LiveMonitorContainer] renders [LiveMonitorBody]
 * directly so it can swap in a collapsible card; this wrapper keeps the
 * non-collapsible rendering exercised Hilt-free.
 */
@Composable
fun LiveMonitorContent(
    title: String,
    enabled: Boolean,
    frozen: Boolean,
    trace: LiveTrace,
    unit: String,
    intervalMs: Long,
    windowMs: Long,
    onEnabledChange: (Boolean) -> Unit,
    onToggleFreeze: () -> Unit,
    onIntervalChange: (Long) -> Unit,
    onWindowChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    DashCard(modifier = modifier, title = title, icon = Icons.Outlined.Timeline) {
        LiveMonitorBody(
            enabled = enabled,
            frozen = frozen,
            trace = trace,
            unit = unit,
            intervalMs = intervalMs,
            windowMs = windowMs,
            onEnabledChange = onEnabledChange,
            onToggleFreeze = onToggleFreeze,
            onIntervalChange = onIntervalChange,
            onWindowChange = onWindowChange,
        )
    }
}

@Composable
private fun LiveMonitorBody(
    enabled: Boolean,
    frozen: Boolean,
    trace: LiveTrace,
    unit: String,
    intervalMs: Long,
    windowMs: Long,
    onEnabledChange: (Boolean) -> Unit,
    onToggleFreeze: () -> Unit,
    onIntervalChange: (Long) -> Unit,
    onWindowChange: (Long) -> Unit,
) {
    val spacing = LocalGadgetTheme.current.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.medium)) {
        ToggleRow(
            label = stringResource(R.string.live_monitor_toggle_label),
            checked = enabled,
            onCheckedChange = onEnabledChange,
        )

        LiveChart(trace = trace)

        if (enabled) {
            LiveStatsRow(trace = trace, unit = unit)

            GadgetChip(
                selected = frozen,
                onClick = onToggleFreeze,
                label = stringResource(R.string.live_monitor_freeze),
            )

            GadgetSlider(
                value = intervalMs.toFloat(),
                onValueChange = { onIntervalChange(it.roundToInt().toLong()) },
                valueRange = MIN_LIVE_INTERVAL_MS.toFloat()..MAX_LIVE_INTERVAL_MS.toFloat(),
                label = stringResource(R.string.live_monitor_interval_label),
                suffix = stringResource(R.string.live_monitor_interval_suffix),
            )
            GadgetSlider(
                value = (windowMs / 1_000L).toFloat(),
                onValueChange = { onWindowChange((it * 1_000f).roundToInt().toLong()) },
                valueRange = (MIN_LIVE_WINDOW_MS / 1_000L).toFloat()..(MAX_LIVE_WINDOW_MS / 1_000L).toFloat(),
                label = stringResource(R.string.live_monitor_window_label),
                suffix = stringResource(R.string.live_monitor_window_suffix),
            )
        }
    }
}

@Composable
private fun LiveStatsRow(trace: LiveTrace, unit: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatItem(stringResource(R.string.live_monitor_stat_now), trace.current, unit)
        StatItem(stringResource(R.string.live_monitor_stat_min), trace.min, unit)
        StatItem(stringResource(R.string.live_monitor_stat_max), trace.max, unit)
        StatItem(stringResource(R.string.live_monitor_stat_avg), trace.avg, unit)
    }
}

@Composable
private fun StatItem(label: String, value: Float?, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value?.let { formatStat(it) + unit } ?: stringResource(R.string.live_monitor_stat_empty),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatStat(value: Float): String =
    if (abs(value) >= 10f) "${value.toInt()}" else String.format("%.1f", value)

// ─── Previews ───────────────────────────────────────────────────────

@GadgetPreviewLightDark
@GadgetPreviewLargeFont
@GadgetPreviewRtl
@GadgetPreviewSizeClasses
@Composable
private fun LiveMonitorContentPreview() = GadgetThemedPreview {
    val now = 1_000_000L
    val window = DEFAULT_LIVE_WINDOW_MS
    val samples = List(60) { i ->
        val t = now - window + (window * i / 59)
        TimedSample(t = t, value = 50f + 40f * sin(i / 6f))
    }
    LiveMonitorContent(
        title = "Live torch intensity",
        enabled = true,
        frozen = false,
        trace = LiveTrace(
            samples = samples,
            windowMs = window,
            nowMs = now,
            current = samples.last().value,
            min = samples.minOf { it.value },
            max = samples.maxOf { it.value },
            avg = samples.map { it.value }.average().toFloat(),
        ),
        unit = "%",
        intervalMs = DEFAULT_LIVE_INTERVAL_MS,
        windowMs = window,
        onEnabledChange = {},
        onToggleFreeze = {},
        onIntervalChange = {},
        onWindowChange = {},
    )
}
