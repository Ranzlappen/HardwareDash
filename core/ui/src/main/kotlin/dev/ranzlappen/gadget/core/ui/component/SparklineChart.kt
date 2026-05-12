package dev.ranzlappen.gadget.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Lightweight inline sparkline chart.
 *
 * Custom Canvas implementation rather than a third-party chart
 * library because:
 *   * One-line API at the call site — `SparklineChart(samples)`.
 *   * Matches the glass aesthetic — gradient-stroked smooth line,
 *     no axis labels, no tick marks, no grid.
 *   * Zero additional dependencies on the dashboard's hot path.
 *
 * The chart auto-scales to the min/max of the data and stretches
 * across the full width. Empty and single-point lists render nothing
 * (no divide-by-zero on the x-axis step).
 *
 * Line stroking uses a vertical gradient: [topColor] at the canvas'
 * top, [bottomColor] at the bottom. Defaults resolve to
 * `colorScheme.primary` (top) and `colorScheme.tertiary` (bottom) so
 * on the canonical dark theme the line reads as "teal at the peak,
 * amber in the valley", which reinforces the readout's directional
 * context without an explicit legend.
 *
 * Smoothing uses cubic Bézier interpolation with horizontal control
 * handles — softer than straight segments, lighter than full spline
 * fitting, and visually indistinguishable from a cubic spline at
 * sparkline scale.
 */
@Composable
fun SparklineChart(
    samples: List<Float>,
    modifier: Modifier = Modifier,
    topColor: Color = MaterialTheme.colorScheme.primary,
    bottomColor: Color = MaterialTheme.colorScheme.tertiary,
    strokeWidth: Dp = 2.dp,
) {
    if (samples.size < 2) {
        // Not enough data to draw — render empty. Also avoids a
        // divide-by-zero when computing the x-axis step.
        Canvas(modifier = modifier) {}
        return
    }
    val min = samples.min()
    val max = samples.max()
    val range = (max - min).takeIf { it > 0f } ?: 1f

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val xStep = width / (samples.size - 1)

        fun y(value: Float): Float = height - ((value - min) / range) * height

        val path = Path().apply {
            moveTo(0f, y(samples[0]))
            for (i in 1 until samples.size) {
                val prevX = (i - 1) * xStep
                val prevY = y(samples[i - 1])
                val currX = i * xStep
                val currY = y(samples[i])
                // Horizontal control handles smooth the curve without
                // overshoot. Half-step distance keeps the handles
                // proportional to the data density.
                val handleDx = xStep / 2f
                cubicTo(
                    x1 = prevX + handleDx, y1 = prevY,
                    x2 = currX - handleDx, y2 = currY,
                    x3 = currX, y3 = currY,
                )
            }
        }

        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                colors = listOf(topColor, bottomColor),
                startY = 0f,
                endY = height,
            ),
            style = Stroke(
                width = strokeWidth.toPx(),
                cap = StrokeCap.Round,
            ),
        )
    }
}
