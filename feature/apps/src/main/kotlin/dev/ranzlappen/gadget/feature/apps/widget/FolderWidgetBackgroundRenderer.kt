package dev.ranzlappen.gadget.feature.apps.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import dev.ranzlappen.gadget.core.widgetkit.config.BackgroundMode

/**
 * Generates a Canvas-drawn background [Bitmap] for folder widgets when
 * any folder-specific style (non-default shape, gradient, or stroke) is
 * requested. The bitmap is set on `@id/widget_background` via
 * `setImageViewBitmap`, overriding the kit's standard shape-drawable path.
 *
 * This is a plain stateless object — no Hilt injection needed.
 */
object FolderWidgetBackgroundRenderer {

    private const val SIZE = 512

    fun buildBackground(config: FolderWidgetConfig, context: Context): Bitmap? {
        val needsBitmap = config.folderShape != FolderShape.RoundedSquare
            || config.gradientEndArgb != null
            || config.strokeWidthDp > 0f
            || config.cornerRadiusFraction != null
        if (!needsBitmap) return null

        val density = context.resources.displayMetrics.density
        val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val cornerRadius: Float = when {
            config.cornerRadiusFraction != null ->
                SIZE * config.cornerRadiusFraction.coerceIn(0f, 0.5f)
            else -> when (config.folderShape) {
                FolderShape.Circle -> SIZE / 2f
                FolderShape.RoundedSquare -> SIZE * 0.22f
                FolderShape.Square -> 0f
            }
        }

        val strokePx = config.strokeWidthDp * density
        val inset = strokePx / 2f
        val rect = RectF(inset, inset, SIZE - inset, SIZE - inset)

        when (config.appearance.background) {
            BackgroundMode.GlassSurface -> {
                paint.color = 0x33FFFFFF
                paint.shader = null
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            }
            BackgroundMode.Solid -> {
                val startColor = config.appearance.solidColor.toInt()
                val endColor = config.gradientEndArgb?.toInt() ?: startColor
                paint.shader = if (endColor != startColor) {
                    LinearGradient(
                        0f, 0f, SIZE.toFloat(), SIZE.toFloat(),
                        startColor, endColor,
                        Shader.TileMode.CLAMP,
                    )
                } else {
                    null
                }
                paint.color = startColor
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            }
            BackgroundMode.Transparent -> { /* clear bitmap = transparent */ }
        }

        if (config.strokeWidthDp > 0f) {
            paint.shader = null
            paint.color = config.strokeArgb.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokePx
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
            paint.style = Paint.Style.FILL
        }

        return bitmap
    }
}
