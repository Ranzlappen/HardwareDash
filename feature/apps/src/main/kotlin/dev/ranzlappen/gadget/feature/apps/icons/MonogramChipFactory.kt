package dev.ranzlappen.gadget.feature.apps.icons

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the universal "no real icon available" fallback: a tinted circle with
 * the first letter of the app's label centered. Color is derived from a stable
 * hash of the seed (typically the appKey) so the same app always lands on the
 * same monogram color across runs.
 */
@Singleton
class MonogramChipFactory @Inject constructor() {

    fun build(label: String, seed: String, sizePx: Int = DEFAULT_SIZE_PX): Bitmap {
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = colorForSeed(seed) }
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, bg)

        val initial = label.firstOrNull { it.isLetterOrDigit() }
            ?.uppercaseChar()
            ?.toString()
            ?: "?"
        val fg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = sizePx * 0.5f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        // Vertically center the text using ascent/descent metrics so the glyph
        // sits visually centered, not just on its baseline.
        val cy = sizePx / 2f - (fg.descent() + fg.ascent()) / 2f
        canvas.drawText(initial, sizePx / 2f, cy, fg)

        return bitmap
    }

    private fun colorForSeed(seed: String): Int {
        val hash = seed.hashCode()
        val hue = ((hash and 0xFFFF) % 360).toFloat()
        return Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.65f))
    }

    companion object {
        const val DEFAULT_SIZE_PX = 96
    }
}
