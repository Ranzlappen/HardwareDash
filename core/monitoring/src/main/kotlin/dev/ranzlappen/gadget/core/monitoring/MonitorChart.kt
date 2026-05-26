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
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.scroll.AutoScrollCondition
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import dev.ranzlappen.gadget.core.data.MonitorBucket

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
 * Renders a metric's **downsampled** windowed history as a Vico line/area/
 * column chart, opened at the live (right) edge and pinch-zoomable.
 *
 * **X = fixed-time-bucket index** (the [MonitorBucket.bucket] integer). This
 * encoding is the deliberate fix for two Vico traps that are invisible to a
 * local compile and only blow up at runtime / at long windows:
 *  - Vico rejects x values with more than two decimal places ("The precision
 *    of the x values is too large"), so float-encoded elapsed-seconds crash.
 *    A bucket index is an exact integer.
 *  - Vico's scrollable content width is `(maxX - minX) / xStep` segments,
 *    where `xStep` is the GCD of the x deltas. Real elapsed-millis/centisecond
 *    timestamps produce a tiny GCD over a huge range (millions of segments)
 *    and hang the layout. Bucket indices are unit-stepped and bounded by the
 *    bucket count (a few hundred), so the content stays small.
 * Empty buckets are simply absent, so a monitoring gap shows as a
 * proportional horizontal gap rather than collapsing — real time is
 * preserved without the precision/range cost of timestamp-based x. The
 * bottom axis multiplies the index back to elapsed time via [bucketMs]. Y is
 * pinned to `0..yMax` so a flat signal doesn't autoscale into a misleading
 * full-height line.
 *
 * Scroll/zoom: [rememberChartScrollSpec] opens at [InitialScroll.End] (now)
 * and auto-follows new data ([AutoScrollCondition.OnModelSizeIncreased]);
 * `isZoomEnabled` lets the user pinch to inspect a sub-range of a long window.
 */
@Composable
fun MonitorChart(
    buckets: List<MonitorBucket>,
    bucketMs: Long,
    layout: MonitorChartLayout,
    yMax: Float,
    modifier: Modifier = Modifier,
) {
    if (buckets.size < 2) {
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

    // Synchronous model via entryModelOf + the `model` overload — NOT a
    // ChartEntryModelProducer. The producer builds its model on a background
    // executor, so the first frame draws against ChartValuesProvider.Empty
    // and throws at runtime ("…Empty#getChartValues shouldn't be used"). A
    // synchronous model exists before the first draw; we re-render on data
    // change instead of relying on the producer's diff animation.
    val model = remember(buckets) {
        entryModelOf(buckets.map { entryOf(it.bucket.toFloat(), it.maxValue) })
    }
    val firstBucket = remember(buckets) { buckets.first().bucket }
    val bottomAxisFormatter = remember(firstBucket, bucketMs) {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
            formatElapsed(((value.toLong() - firstBucket) * bucketMs).coerceAtLeast(0L))
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
        chartScrollSpec = rememberChartScrollSpec(
            isScrollEnabled = true,
            initialScroll = InitialScroll.End,
            autoScrollCondition = AutoScrollCondition.OnModelSizeIncreased,
        ),
        isZoomEnabled = true,
        modifier = modifier
            .fillMaxWidth()
            .height(MonitorChartDefaults.Height),
    )
}

/** Compact elapsed-time axis label: seconds under a minute, minutes under an
 *  hour, else whole hours. */
private fun formatElapsed(ms: Long): String = when {
    ms < 60_000L -> "${ms / 1_000L}s"
    ms < 3_600_000L -> "${ms / 60_000L}m"
    else -> "${ms / 3_600_000L}h"
}
