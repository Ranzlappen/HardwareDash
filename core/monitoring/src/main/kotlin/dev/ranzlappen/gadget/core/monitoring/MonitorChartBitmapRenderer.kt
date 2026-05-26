package dev.ranzlappen.gadget.core.monitoring

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path

/**
 * Renders a downsampled metric history to a sparkline [Bitmap] for a
 * home-screen **chart** widget. RemoteViews can't host a Compose/Vico chart,
 * so the chart widget draws to a bitmap and ships it via
 * `setImageViewBitmap` (the same bitmap path the torch custom-icon widget
 * already uses).
 *
 * Pure `android.graphics` — no Vico, no Compose — so it's safe to call from a
 * widget provider's background thread. Feed it an already-downsampled value
 * list (peak-per-bucket) so the point count is bounded regardless of window
 * length. Reusable by any feature that adds a monitor chart widget.
 */
object MonitorChartBitmapRenderer {

    /**
     * Draw [values] (oldest-first, already downsampled) as a line/area/column
     * sparkline pinned to `0..yMax`, into a [widthPx] x [heightPx] bitmap.
     * Returns a transparent bitmap with no plot when fewer than two points
     * are available (the widget shows a "collecting" label over it).
     */
    fun render(
        values: List<Float>,
        yMax: Float,
        widthPx: Int,
        heightPx: Int,
        lineColor: Int,
        fillColor: Int,
        layout: MonitorChartLayout,
        strokeWidthPx: Float,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (values.size < 2) return bitmap

        val canvas = Canvas(bitmap)
        val safeMax = yMax.takeIf { it > 0f } ?: 1f
        val pad = strokeWidthPx
        val plotH = (height - 2 * pad).coerceAtLeast(1f)
        val plotW = (width - 2 * pad).coerceAtLeast(1f)
        val lastIndex = (values.size - 1).coerceAtLeast(1)

        fun xAt(i: Int): Float = pad + plotW * i / lastIndex
        fun yAt(v: Float): Float = pad + plotH * (1f - (v / safeMax).coerceIn(0f, 1f))

        when (layout) {
            MonitorChartLayout.Bars -> {
                val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = lineColor
                    style = Paint.Style.FILL
                }
                val barW = (plotW / values.size).coerceAtLeast(1f) * BAR_WIDTH_FRACTION
                values.forEachIndexed { i, v ->
                    val cx = xAt(i)
                    canvas.drawRect(cx - barW / 2f, yAt(v), cx + barW / 2f, pad + plotH, barPaint)
                }
            }
            else -> {
                val linePath = Path().apply {
                    moveTo(xAt(0), yAt(values[0]))
                    for (i in 1..lastIndex) lineTo(xAt(i), yAt(values[i]))
                }
                if (layout == MonitorChartLayout.Area) {
                    val fill = Path(linePath).apply {
                        lineTo(xAt(lastIndex), pad + plotH)
                        lineTo(xAt(0), pad + plotH)
                        close()
                    }
                    canvas.drawPath(fill, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = fillColor
                        style = Paint.Style.FILL
                    })
                }
                canvas.drawPath(linePath, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = lineColor
                    style = Paint.Style.STROKE
                    strokeWidth = strokeWidthPx
                    strokeCap = Paint.Cap.ROUND
                    strokeJoin = Paint.Join.ROUND
                })
            }
        }
        return bitmap
    }

    private const val BAR_WIDTH_FRACTION = 0.7f
}
