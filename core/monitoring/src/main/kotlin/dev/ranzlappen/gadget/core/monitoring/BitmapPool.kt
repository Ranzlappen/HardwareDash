package dev.ranzlappen.gadget.core.monitoring

import android.graphics.Bitmap

/**
 * Tiny size-keyed [Bitmap] pool for surfaces that are re-rendered on a tight
 * cadence — chart **widgets** above all. A monitor chart widget repaints up to
 * ~1 Hz per placed instance; without reuse every repaint allocated a fresh
 * `widthPx × heightPx` bitmap and orphaned the last one for the GC. On a
 * low-RAM device with two chart widgets that is a steady drip of multi-hundred-
 * KB allocations and the matching collection pressure.
 *
 * This pool lets callers [obtain] a reusable bitmap of a given size and
 * [release] it back once it is no longer referenced (for a RemoteViews widget,
 * *after* `AppWidgetManager.updateAppWidget` returns — that call copies the
 * pixels into the Binder parcel synchronously, so the bitmap is free to reuse
 * the moment it completes). Reuse is keyed by `(width, height)`; a resized
 * widget simply starts a new size bucket and the old one ages out.
 *
 * **Config.** Bitmaps are allocated [Bitmap.Config.RGB_565] — chart panels are
 * opaque (callers paint a solid background first), so the alpha channel of
 * `ARGB_8888` is dead weight and `RGB_565` halves both the heap footprint and
 * the RemoteViews/Binder transaction size.
 *
 * **Thread-safety.** Widget repaints can overlap (a framework `onUpdate` and a
 * notifier-driven repaint can land on the shared widget scope at once), so
 * every pool mutation is guarded. [obtain] *removes* a bitmap from the free
 * list, so two concurrent callers never hand out the same instance — the pool
 * just grows to hold both (bounded per size by [maxPerKey]; overflow is
 * recycled rather than retained).
 *
 * Reusable by any feature that ships a bitmap-backed widget — it lives in
 * `:core:monitoring` only because that is where [MonitorChartBitmapRenderer],
 * its first consumer, lives.
 */
internal class BitmapPool(private val maxPerKey: Int = MAX_PER_KEY) {

    private val lock = Any()
    private val free = HashMap<Long, ArrayDeque<Bitmap>>()

    /**
     * Return a reusable [Bitmap.Config.RGB_565] bitmap of [width] × [height],
     * reusing a free same-size instance when one is available. Callers must
     * fully repaint it (the contents are undefined) and eventually [release]
     * it back.
     */
    fun obtain(width: Int, height: Int): Bitmap {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        val key = key(w, h)
        synchronized(lock) {
            val bucket = free[key]
            while (bucket != null && bucket.isNotEmpty()) {
                val candidate = bucket.removeLast()
                if (!candidate.isRecycled) return candidate
            }
        }
        return Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
    }

    /**
     * Return [bitmap] to the pool for reuse. Drop (recycle) it if the matching
     * size bucket is already at [maxPerKey] so the pool can't grow unbounded.
     * No-op for an already-recycled bitmap.
     */
    fun release(bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        val key = key(bitmap.width, bitmap.height)
        synchronized(lock) {
            val bucket = free.getOrPut(key) { ArrayDeque() }
            if (bucket.size >= maxPerKey) {
                bitmap.recycle()
            } else {
                bucket.addLast(bitmap)
            }
        }
    }

    private fun key(width: Int, height: Int): Long =
        (width.toLong() shl 32) or (height.toLong() and 0xFFFF_FFFFL)

    companion object {
        /** A single placed widget never needs more than one live bitmap per
         *  size at a time; two absorbs an overlapping repaint without churn. */
        private const val MAX_PER_KEY = 2
    }
}
