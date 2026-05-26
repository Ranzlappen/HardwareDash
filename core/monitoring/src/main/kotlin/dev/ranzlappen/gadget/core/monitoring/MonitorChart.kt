package dev.ranzlappen.gadget.core.monitoring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.data.MonitorBucket

/**
 * Fixed-size design tokens for the monitor chart. Per the no-raw-dp rule,
 * fixed component dimensions live in a per-file `Defaults` object with a
 * rationale rather than as inline literals.
 */
private object MonitorChartDefaults {
    /** Chart body height — a deliberate fixed size, not a themed spacing. */
    val Height: Dp = 160.dp
    /** Hairline for grid lines. */
    val GridStroke: Dp = 1.dp
    /** Data line thickness. */
    val LineStroke: Dp = 2.dp
    /** Gap between an axis label and its tick / the plot edge. */
    val LabelGap: Dp = 4.dp
}

/**
 * Renders a metric's **downsampled** windowed history as a hand-drawn Compose
 * [Canvas] chart. Deliberately not a Vico chart: Vico's scroll/zoom + per-
 * entry axis labelling fought the sliding-window model (labels relative to
 * the oldest present sample, erratic auto-scroll, coarse duplicate ticks
 * cramming on zoom). Drawing it ourselves gives full control and matches the
 * widget sparkline.
 *
 * The x-axis is pinned to the **full window** anchored to *now*: a bucket's
 * index is `(timestamp − windowStart) / bucketMs`, so the right edge is the
 * present and the left edge is `windowMs` ago, regardless of how much data
 * exists yet. Data flows in from the right as it accumulates; the x labels
 * are "time-ago" marks at nice round intervals (`…3h, 2h, 1h, now`). Y is
 * pinned to `0..yMax` (capability-driven; 150 on the rooted torch).
 */
@Composable
fun MonitorChart(
    buckets: List<MonitorBucket>,
    bucketMs: Long,
    windowMs: Long,
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

    val colors = MaterialTheme.colorScheme
    val lineColor = colors.primary
    val fillColor = colors.primary.copy(alpha = 0.18f)
    val gridColor = colors.outlineVariant.copy(alpha = 0.5f)
    val labelColor = colors.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)

    val totalBuckets = (windowMs / bucketMs).coerceAtLeast(1L).toFloat()
    val safeYMax = yMax.takeIf { it > 0f } ?: 1f
    val xTicks = remember(windowMs) { timeAgoTicks(windowMs) }
    val yValues = remember(safeYMax) { listOf(0f, safeYMax / 2f, safeYMax) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(MonitorChartDefaults.Height),
    ) {
        val gridStroke = MonitorChartDefaults.GridStroke.toPx()
        val lineStroke = MonitorChartDefaults.LineStroke.toPx()
        val gap = MonitorChartDefaults.LabelGap.toPx()

        // Reserve room for the axis labels.
        val yLabel = textMeasurer.measure(formatY(safeYMax), labelStyle)
        val xLabel = textMeasurer.measure("now", labelStyle)
        val leftPad = yLabel.size.width + gap
        val bottomPad = xLabel.size.height + gap
        val topPad = yLabel.size.height / 2f
        val rightPad = gap
        val plotLeft = leftPad
        val plotTop = topPad
        val plotRight = size.width - rightPad
        val plotBottom = size.height - bottomPad
        val plotW = (plotRight - plotLeft).coerceAtLeast(1f)
        val plotH = (plotBottom - plotTop).coerceAtLeast(1f)

        fun xAt(bucketIndex: Float): Float =
            plotLeft + plotW * (bucketIndex / totalBuckets).coerceIn(0f, 1f)

        fun yAt(value: Float): Float =
            plotTop + plotH * (1f - (value / safeYMax).coerceIn(0f, 1f))

        // Horizontal grid + y labels.
        yValues.forEach { gv ->
            val gy = yAt(gv)
            drawLine(gridColor, Offset(plotLeft, gy), Offset(plotRight, gy), strokeWidth = gridStroke)
            val m = textMeasurer.measure(formatY(gv), labelStyle)
            drawText(
                m,
                topLeft = Offset(plotLeft - gap - m.size.width, gy - m.size.height / 2f),
            )
        }

        // Vertical grid + time-ago x labels (anchored to now at the right).
        xTicks.forEach { tick ->
            val frac = 1f - (tick.timeAgoMs.toFloat() / windowMs).coerceIn(0f, 1f)
            val tx = plotLeft + plotW * frac
            drawLine(gridColor, Offset(tx, plotTop), Offset(tx, plotBottom), strokeWidth = gridStroke)
            val m = textMeasurer.measure(tick.label, labelStyle)
            val lx = (tx - m.size.width / 2f).coerceIn(0f, size.width - m.size.width)
            drawText(m, topLeft = Offset(lx, plotBottom + gap))
        }

        // Data.
        val points = buckets.map { Offset(xAt(it.bucket.toFloat()), yAt(it.maxValue)) }
        when (layout) {
            MonitorChartLayout.Bars -> {
                val barW = (plotW / buckets.size).coerceAtLeast(1f) * BAR_WIDTH_FRACTION
                points.forEach { p ->
                    drawRect(
                        color = lineColor,
                        topLeft = Offset(p.x - barW / 2f, p.y),
                        size = androidx.compose.ui.geometry.Size(barW, plotBottom - p.y),
                    )
                }
            }
            else -> {
                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                if (layout == MonitorChartLayout.Area) {
                    val fill = Path().apply {
                        addPath(linePath)
                        lineTo(points.last().x, plotBottom)
                        lineTo(points.first().x, plotBottom)
                        close()
                    }
                    drawPath(fill, fillColor)
                }
                drawPath(linePath, lineColor, style = Stroke(width = lineStroke))
            }
        }
    }
}

private const val BAR_WIDTH_FRACTION = 0.7f

private fun formatY(value: Float): String = value.toInt().toString()

/** One x-axis tick: how long ago it is, and its rendered label. */
private data class TimeTick(val timeAgoMs: Long, val label: String)

/** Round tick intervals (ms): 1s → 12h. */
private val TICK_INTERVALS_MS = longArrayOf(
    1_000, 2_000, 5_000, 10_000, 15_000, 30_000,
    60_000, 120_000, 300_000, 600_000, 900_000, 1_800_000,
    3_600_000, 7_200_000, 10_800_000, 21_600_000, 43_200_000,
)
private const val TARGET_TICKS = 5

/**
 * "Time-ago" ticks across [windowMs], spaced at the smallest round interval
 * that keeps the count near [TARGET_TICKS]. Labels share one unit (derived
 * from the interval) so they read as a clean enumeration (`now, 1h, 2h, …`).
 */
private fun timeAgoTicks(windowMs: Long): List<TimeTick> {
    if (windowMs <= 0L) return emptyList()
    val interval = TICK_INTERVALS_MS.firstOrNull { windowMs / it <= TARGET_TICKS }
        ?: TICK_INTERVALS_MS.last()
    val ticks = ArrayList<TimeTick>()
    var t = 0L
    while (t <= windowMs) {
        ticks.add(TimeTick(t, formatTimeAgo(t, interval)))
        t += interval
    }
    return ticks
}

private fun formatTimeAgo(ms: Long, interval: Long): String = when {
    ms == 0L -> "now"
    interval >= 3_600_000L -> "${ms / 3_600_000L}h"
    interval >= 60_000L -> "${ms / 60_000L}m"
    else -> "${ms / 1_000L}s"
}
