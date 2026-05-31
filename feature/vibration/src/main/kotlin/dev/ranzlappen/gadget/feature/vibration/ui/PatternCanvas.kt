package dev.ranzlappen.gadget.feature.vibration.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable

/**
 * Freehand vibration-pattern draw surface — the modular successor to the
 * legacy draw-canvas. The user drags a finger across the canvas; X maps to
 * time (0 → the fixed window) and Y maps to intensity (top = 100%, bottom =
 * 0%). The drawn curve is sampled into [PATTERN_SAMPLE_COUNT] evenly-spaced
 * intensity values (0..1) which the ViewModel converts into a
 * `VibrationEffect` waveform.
 *
 * Stateless: [samples] is the rendered curve (empty = nothing drawn);
 * [onSamplesChange] fires with a fresh 0..1 sample list as the finger moves.
 *
 * `CANVAS_HEIGHT` is a per-file fixed design dimension (the only allowed raw
 * `dp` per CLAUDE.md — a sizing token documented here, not a call-site literal).
 */
internal val CANVAS_HEIGHT: Dp = 160.dp

/** Number of evenly-spaced intensity samples the drawn curve is reduced to. */
internal const val PATTERN_SAMPLE_COUNT = 40

@Composable
internal fun PatternCanvas(
    samples: List<Float>,
    onSamplesChange: (List<Float>) -> Unit,
    lineColor: Color,
    fillColor: Color,
    gridColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(CANVAS_HEIGHT)
            .pointerInput(Unit) {
                // Each gesture rebuilds the sample buffer from scratch: the
                // working list tracks the highest intensity touched per X
                // bucket so a left→right drag paints a curve.
                val working = FloatArray(PATTERN_SAMPLE_COUNT) { 0f }

                fun record(pos: Offset) {
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    val bucket = ((pos.x / w) * (PATTERN_SAMPLE_COUNT - 1))
                        .toInt().coerceIn(0, PATTERN_SAMPLE_COUNT - 1)
                    val intensity = (1f - (pos.y / h)).coerceIn(0f, 1f)
                    working[bucket] = intensity
                    onSamplesChange(working.toList())
                }

                detectDragGestures(
                    onDragStart = { working.fill(0f); record(it) },
                    onDrag = { change, _ -> record(change.position) },
                )
            },
    ) {
        val w = size.width
        val h = size.height

        // Baseline grid: a horizontal midline + the zero baseline.
        drawLine(gridColor, Offset(0f, h), Offset(w, h), strokeWidth = 2f)
        drawLine(gridColor, Offset(0f, h / 2f), Offset(w, h / 2f), strokeWidth = 1f)

        if (samples.size < 2) return@Canvas

        val stepX = w / (samples.size - 1)
        val linePath = Path()
        val fillPath = Path().apply { moveTo(0f, h) }
        samples.forEachIndexed { i, value ->
            val x = i * stepX
            val y = h - (value.coerceIn(0f, 1f) * h)
            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            fillPath.lineTo(x, y)
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(fillPath, fillColor)
        drawPath(linePath, lineColor, style = Stroke(width = 4f))
    }
}
