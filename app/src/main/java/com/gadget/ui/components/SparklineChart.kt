package com.gadget.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * A small composable that draws a mini line chart using Canvas.
 * Uses [AccessibleCanvas] for screen-reader support.
 *
 * @param data List of float values to plot
 * @param modifier Modifier for the canvas
 * @param lineColor Color of the sparkline stroke
 */
@Composable
fun SparklineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF6200EE),
) {
    if (data.isEmpty()) return

    val description = if (data.size >= 2) {
        val trend = if (data.last() >= data.first()) "trending up" else "trending down"
        "Sparkline chart with ${data.size} points, $trend"
    } else {
        "Sparkline chart with ${data.size} point"
    }

    AccessibleCanvas(
        modifier = modifier,
        contentDescription = description,
    ) {
        val width = size.width
        val height = size.height

        if (data.size < 2) {
            // Single point: draw a dot in the center
            drawCircle(
                color = lineColor,
                radius = 2f,
                center = Offset(width / 2f, height / 2f),
            )
            return@AccessibleCanvas
        }

        val minVal = data.min()
        val maxVal = data.max()
        val range = (maxVal - minVal).coerceAtLeast(0.001f)

        val stepX = width / (data.size - 1).toFloat()
        val paddingY = height * 0.1f
        val drawHeight = height - paddingY * 2

        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * stepX
                val normalised = (value - minVal) / range
                // Invert Y: top of canvas is high value
                val y = paddingY + drawHeight * (1f - normalised)

                if (index == 0) moveTo(x, y)
                else lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2f),
        )
    }
}
