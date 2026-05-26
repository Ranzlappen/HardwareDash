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
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import dev.ranzlappen.gadget.core.data.MonitorSample
import kotlin.math.roundToInt

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
 * X is the integer **sample index** (0, 1, 2, …). Vico rejects x values
 * with more than two decimal places ("The precision of the x values is
 * too large") and float-encoded elapsed-seconds (millisecond resolution)
 * trips that limit; an integer index is exact, strictly increasing (so no
 * duplicate-x), and survives sub-second poll intervals. The bottom axis
 * maps each index back to its real elapsed-seconds label. Y is pinned to
 * `0..yMax` so a flat signal doesn't autoscale into a misleading
 * full-height line.
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
    // x = sample index (see KDoc — Vico caps x at two decimal places).
    val model = remember(samples) {
        entryModelOf(samples.mapIndexed { index, sample -> entryOf(index.toFloat(), sample.value) })
    }
    val elapsedSeconds = remember(samples) {
        val t0 = samples.first().timestampMs
        samples.map { (it.timestampMs - t0) / 1000f }
    }
    val bottomAxisFormatter = remember(elapsedSeconds) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            val index = value.roundToInt()
            if (index in elapsedSeconds.indices) "${elapsedSeconds[index].roundToInt()}s" else ""
        }
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
        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisFormatter),
        modifier = modifier
            .fillMaxWidth()
            .height(MonitorChartDefaults.Height),
    )
}
