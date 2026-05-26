package dev.ranzlappen.gadget.core.monitoring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import dev.ranzlappen.gadget.core.data.MonitorSample

/**
 * Fixed-size design tokens for the monitor chart. Per the no-raw-dp rule,
 * fixed component dimensions live in a per-file `Defaults` object with a
 * rationale rather than as inline literals.
 */
private object MonitorChartDefaults {
    /** Chart body height — a deliberate fixed size, not a themed spacing. */
    val Height: Dp = 160.dp
}

/**
 * Renders a metric's windowed history as a Vico line/area/column chart.
 *
 * X is seconds elapsed since the oldest sample in the window (raw epoch
 * millis can't be a chart `Float` — the 24-bit mantissa collapses
 * sub-minute differences). Y is pinned to `0..yMax` so a flat signal
 * doesn't autoscale into a misleading full-height line.
 */
@Composable
fun MonitorChart(
    samples: List<MonitorSample>,
    layout: MonitorChartLayout,
    yMax: Float,
    modifier: Modifier = Modifier,
) {
    if (samples.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(MonitorChartDefaults.Height),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.monitor_chart_collecting),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Build the model SYNCHRONOUSLY via entryModelOf and pass it through
    // the `model` overload — NOT a ChartEntryModelProducer. The producer
    // generates its model on a background executor, so on the first frame
    // the Chart draws against ChartValuesProvider.Empty and throws
    // "ChartValuesProvider.Empty#getChartValues shouldn't be used". A
    // synchronous model exists before the first draw; we re-render on
    // `samples` change, so the producer's diff animation isn't needed.
    val model = remember(samples) {
        val t0 = samples.first().timestampMs
        entryModelOf(samples.map { entryOf((it.timestampMs - t0) / 1000f, it.value) })
    }

    val overrider = remember(yMax) { AxisValuesOverrider.fixed(minY = 0f, maxY = yMax) }
    val chart = when (layout) {
        MonitorChartLayout.Bars -> columnChart(axisValuesOverrider = overrider)
        else -> lineChart(axisValuesOverrider = overrider)
    }

    Chart(
        chart = chart,
        model = model,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
        modifier = modifier
            .fillMaxWidth()
            .height(MonitorChartDefaults.Height),
    )
}
