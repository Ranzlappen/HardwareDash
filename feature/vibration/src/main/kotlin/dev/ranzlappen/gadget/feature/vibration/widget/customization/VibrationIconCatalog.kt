package dev.ranzlappen.gadget.feature.vibration.widget.customization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconKeys
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetIconResolver
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationPinLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Curated registry of icons available to vibration widget configurations, plus
 * the import/resolve surface for user-supplied custom icons. Mirror of torch's
 * `WidgetIconCatalog` — built-ins pair a stable key with a drawable; custom
 * icons are downscaled WEBP copies in `filesDir/widget_icons/` keyed
 * `custom:<uuid>.webp`. The single resolution surface for both kinds.
 */
@Singleton
class VibrationIconCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetIconResolver {

    data class Entry(
        val key: String,
        @DrawableRes val drawable: Int,
        val displayName: String,
    )

    val entries: List<Entry> = listOf(
        Entry(WidgetIconKeys.DEFAULT_ACTIVE, R.drawable.ic_vibration_on, "Vibrate active"),
        Entry(WidgetIconKeys.DEFAULT_INACTIVE, R.drawable.ic_vibration_off, "Vibrate idle"),
        Entry("pattern", R.drawable.ic_vibration_pattern, "Pattern"),
    )

    private val customDir: File by lazy { File(context.filesDir, CUSTOM_DIR_NAME) }

    override fun isCustom(key: String): Boolean = key.startsWith(WidgetIconKeys.CUSTOM_PREFIX)

    fun resolveSource(key: String): WidgetIconSource =
        if (isCustom(key)) {
            WidgetIconSource.CustomFile(File(customDir, key.removePrefix(WidgetIconKeys.CUSTOM_PREFIX)).absolutePath)
        } else {
            WidgetIconSource.Resource(resolve(key))
        }

    @DrawableRes
    override fun resolve(key: String): Int =
        entries.firstOrNull { it.key == key }?.drawable ?: entries.first().drawable

    fun isKnown(key: String): Boolean = entries.any { it.key == key }

    override fun loadCustomBitmap(key: String): Bitmap? {
        if (!isCustom(key)) return null
        val path = File(customDir, key.removePrefix(WidgetIconKeys.CUSTOM_PREFIX)).absolutePath
        return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    suspend fun importCustomIcon(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Log.w(VibrationPinLog.TAG, "importCustomIcon: empty/unreadable stream for $uri")
                return@withContext null
            }
            val decoded = decodeDownscaled(bytes, MAX_ICON_PX)
            if (decoded == null) {
                Log.w(VibrationPinLog.TAG, "importCustomIcon: undecodable image for $uri")
                return@withContext null
            }
            val bitmap = applyExifOrientation(decoded, bytes)
            customDir.mkdirs()
            val file = File(customDir, "${UUID.randomUUID()}.webp")
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            FileOutputStream(file).use { out -> bitmap.compress(format, ICON_QUALITY, out) }
            bitmap.recycle()
            Log.d(VibrationPinLog.TAG, "importCustomIcon: wrote ${file.name}")
            WidgetIconKeys.CUSTOM_PREFIX + file.name
        } catch (t: Throwable) {
            Log.w(VibrationPinLog.TAG, "importCustomIcon failed for $uri", t)
            null
        }
    }

    private fun decodeDownscaled(bytes: ByteArray, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxPx * 2 || bounds.outHeight / sample > maxPx * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts) ?: return null

        val longest = max(decoded.width, decoded.height)
        if (longest <= maxPx) return decoded
        val scale = maxPx.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun applyExifOrientation(bitmap: Bitmap, bytes: ByteArray): Bitmap {
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.postScale(-1f, 1f)
            }
            else -> return bitmap
        }
        return try {
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            rotated
        } catch (e: OutOfMemoryError) {
            Log.w(VibrationPinLog.TAG, "applyExifOrientation OOM — using unrotated icon", e)
            bitmap
        }
    }

    companion object {
        private const val CUSTOM_DIR_NAME = "widget_icons"
        private const val MAX_ICON_PX = 192
        private const val ICON_QUALITY = 80
    }
}
