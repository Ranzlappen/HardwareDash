package dev.ranzlappen.gadget.core.monitoring

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ranzlappen.gadget.core.designsystem.a11y.LocalReducedMotion
import dev.ranzlappen.gadget.core.designsystem.theme.LocalGadgetTheme
import kotlin.math.abs

/** Fixed-size design tokens for the live chart. */
private object LiveChartDefaults {
    val Height: Dp = 160.dp
    val LineStroke: Dp = 2.dp
    val DotRadius: Dp = 3.dp
    val LabelGap: Dp = 4.dp
    /** Min tap target for the "Auto" reset affordance (a11y minimum). */
    val TouchTarget: Dp = 48.dp
    /** Smallest auto Y span, so a flat/constant signal doesn't fill the
     *  whole height from rounding noise. */
    const val MinAutoSpan = 1f
    const val AutoPadFraction = 0.08f
    /** Manual-zoom span clamp (× the auto span) so pinch can't invert or
     *  zoom to absurd ranges. */
    const val MinManualSpan = 0.1f
    const val MaxManualSpan = 1_000_000f
}

/**
 * Live realtime line chart for a [LiveTrace]. Distinct from [MonitorChart]:
 * no buckets, no persistence — it draws the raw in-memory buffer.
 *
 * - **X** is the live time window anchored to *now* at the right edge
 *   ([LiveTrace.nowMs] / [LiveTrace.windowMs]); samples are placed by real
 *   timestamp so spacing is true and data flows in from the right.
 * - **Y is soft auto-scale**: the displayed bounds *ease* toward the visible
 *   buffer's padded min/max instead of snapping each frame (honors
 *   [LocalReducedMotion] → instant). **Pinch-zoom / vertical-drag** engages a
 *   manual Y viewport; the **Auto** chip (or a double-tap) returns to
 *   soft-auto.
 */
@Composable
fun LiveChart(
    trace: LiveTrace,
    modifier: Modifier = Modifier,
) {
    if (trace.samples.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(LiveChartDefaults.Height),
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

    val reducedMotion = LocalReducedMotion.current
    val spacing = LocalGadgetTheme.current.spacing
    val colors = MaterialTheme.colorScheme
    val lineTop = colors.primary
    val lineBottom = colors.tertiary
    val labelColor = colors.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor)

    val values = trace.samples.map { it.value }
    val dataMin = values.min()
    val dataMax = values.max()
    val (autoLow, autoHigh) = remember(dataMin, dataMax) { paddedBounds(dataMin, dataMax) }

    var auto by remember { mutableStateOf(true) }
    var manualLow by remember { mutableFloatStateOf(autoLow) }
    var manualHigh by remember { mutableFloatStateOf(autoHigh) }

    // While auto, keep the manual bounds shadowing the live auto bounds so a
    // later pinch starts from exactly what's on screen.
    LaunchedEffect(auto, autoLow, autoHigh) {
        if (auto) {
            manualLow = autoLow
            manualHigh = autoHigh
        }
    }

    val targetLow = if (auto) autoLow else manualLow
    val targetHigh = if (auto) autoHigh else manualHigh
    val softSpec = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
    val lowDisp = if (reducedMotion || !auto) targetLow else animateFloatAsState(targetLow, softSpec, label = "lowBound").value
    val highDisp = if (reducedMotion || !auto) targetHigh else animateFloatAsState(targetHigh, softSpec, label = "highBound").value

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(LiveChartDefaults.Height),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { auto = true })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        auto = false
                        val span = (manualHigh - manualLow).coerceAtLeast(LiveChartDefaults.MinManualSpan)
                        val center = (manualHigh + manualLow) / 2f
                        val newSpan = (span / zoom).coerceIn(
                            LiveChartDefaults.MinManualSpan,
                            LiveChartDefaults.MaxManualSpan,
                        )
                        // Drag down (pan.y > 0) shifts the view toward higher values.
                        val shift = if (size.height > 0) pan.y / size.height * newSpan else 0f
                        manualLow = center - newSpan / 2f + shift
                        manualHigh = center + newSpan / 2f + shift
                    }
                },
        ) {
            val lineStroke = LiveChartDefaults.LineStroke.toPx()
            val dotRadius = LiveChartDefaults.DotRadius.toPx()
            val gap = LiveChartDefaults.LabelGap.toPx()

            val highLabel = textMeasurer.measure(formatValue(highDisp), labelStyle)
            val lowLabel = textMeasurer.measure(formatValue(lowDisp), labelStyle)
            val leftPad = maxOf(highLabel.size.width, lowLabel.size.width) + gap
            val topPad = highLabel.size.height / 2f
            val bottomPad = lowLabel.size.height / 2f
            val plotLeft = leftPad
            val plotTop = topPad
            val plotRight = size.width
            val plotBottom = size.height - bottomPad
            val plotW = (plotRight - plotLeft).coerceAtLeast(1f)
            val plotH = (plotBottom - plotTop).coerceAtLeast(1f)

            val windowStart = trace.nowMs - trace.windowMs
            val span = (highDisp - lowDisp).takeIf { it > 0f } ?: 1f

            fun xAt(t: Long): Float =
                plotLeft + plotW * ((t - windowStart).toFloat() / trace.windowMs).coerceIn(0f, 1f)

            fun yAt(v: Float): Float =
                plotTop + plotH * (1f - ((v - lowDisp) / span).coerceIn(0f, 1f))

            // Y bound labels (top = high, bottom = low).
            drawText(highLabel, topLeft = Offset(plotLeft - gap - highLabel.size.width, plotTop - highLabel.size.height / 2f))
            drawText(lowLabel, topLeft = Offset(plotLeft - gap - lowLabel.size.width, plotBottom - lowLabel.size.height / 2f))

            // Smooth line via cubic-Bézier half-step handles (handles the
            // irregular x spacing of timestamped samples).
            val pts = trace.samples.map { Offset(xAt(it.t), yAt(it.value)) }
            val path = Path().apply {
                moveTo(pts.first().x, pts.first().y)
                for (i in 1 until pts.size) {
                    val prev = pts[i - 1]
                    val cur = pts[i]
                    val handleDx = (cur.x - prev.x) / 2f
                    cubicTo(prev.x + handleDx, prev.y, cur.x - handleDx, cur.y, cur.x, cur.y)
                }
            }
            drawPath(
                path = path,
                brush = Brush.verticalGradient(listOf(lineTop, lineBottom), startY = plotTop, endY = plotBottom),
                style = Stroke(width = lineStroke, cap = StrokeCap.Round),
            )
            // Current-value marker at the right (newest) sample.
            drawCircle(color = lineTop, radius = dotRadius, center = pts.last())
        }

        if (!auto) {
            Text(
                text = stringResource(R.string.live_monitor_auto),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .defaultMinSize(
                        minWidth = LiveChartDefaults.TouchTarget,
                        minHeight = LiveChartDefaults.TouchTarget,
                    )
                    .clickable { auto = true }
                    .padding(horizontal = spacing.tiny, vertical = spacing.micro),
            )
        }
    }
}

private fun paddedBounds(min: Float, max: Float): Pair<Float, Float> {
    val span = max - min
    if (span < LiveChartDefaults.MinAutoSpan) {
        val center = (max + min) / 2f
        val half = LiveChartDefaults.MinAutoSpan / 2f
        return (center - half) to (center + half)
    }
    val pad = span * LiveChartDefaults.AutoPadFraction
    return (min - pad) to (max + pad)
}

private fun formatValue(value: Float): String =
    if (abs(value) >= 10f) "${value.toInt()}" else String.format("%.1f", value)
