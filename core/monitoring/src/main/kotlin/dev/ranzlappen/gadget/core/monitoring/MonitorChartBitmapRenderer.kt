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
 *
 * **Memory.** Bitmaps come from a size-keyed [BitmapPool] and are allocated
 * [Bitmap.Config.RGB_565]: a chart panel is opaque (the caller supplies an
 * opaque [render] `backgroundColor`), so `ARGB_8888`'s alpha channel was dead
 * weight — `RGB_565` halves both the heap footprint and the RemoteViews/Binder
 * transaction size. Pass each rendered bitmap back to [release] once it is no
 * longer referenced (for a widget, after `updateAppWidget` returns) so the next
 * repaint reuses it instead of allocating + GC-ing a fresh one every ~second.
 */
object MonitorChartBitmapRenderer {

    private val pool = BitmapPool()

    /**
     * Draw [values] (oldest-first, already downsampled) as a line/area/column
     * sparkline pinned to `0..yMax`, into a [widthPx] x [heightPx] bitmap
     * filled with the opaque [backgroundColor]. Returns a background-only
     * bitmap with no plot when fewer than two points are available (the widget
     * hides the image and shows a "collecting" label instead).
     *
     * The returned bitmap is pooled — hand it to [release] when done.
     */
    fun render(
        values: List<Float>,
        yMax: Float,
        widthPx: Int,
        heightPx: Int,
        lineColor: Int,
        fillColor: Int,
        backgroundColor: Int,
        layout: MonitorChartLayout,
        strokeWidthPx: Float,
    ): Bitmap {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        val bitmap = pool.obtain(width, height)
        // RGB_565 has no alpha, so fill an opaque base before plotting —
        // this also clears any stale pixels from a previous pooled render.
        bitmap.eraseColor(backgroundColor)
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

    /**
     * Return a [render]ed bitmap to the pool for reuse. Call this only once the
     * bitmap is no longer referenced — for a RemoteViews widget that means
     * **after** `AppWidgetManager.updateAppWidget` returns (it copies the
     * pixels into the Binder parcel synchronously, so reuse is then safe).
     */
    fun release(bitmap: Bitmap) = pool.release(bitmap)

    private const val BAR_WIDTH_FRACTION = 0.7f
}
