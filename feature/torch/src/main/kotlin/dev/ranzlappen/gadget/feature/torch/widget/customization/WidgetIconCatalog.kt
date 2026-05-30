package dev.ranzlappen.gadget.feature.torch.widget.customization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconKeys
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetIconResolver
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.widget.PendingTorchWidgetConfigs
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
 * Curated registry of icons available to widget configurations, plus the
 * import/resolve surface for user-supplied custom icons.
 *
 * Built-in entries pair a stable [Entry.key] (persisted in IconStyle)
 * with a [DrawableRes]. Custom icons are persisted as keys of the form
 * `custom:<uuid>.png` pointing at a downscaled copy in
 * `filesDir/widget_icons/` — configs never store a raw content Uri (those
 * are revocable), so a picked image keeps working across reboots and
 * permission changes. The catalog is the single resolution surface for
 * both kinds via [resolveSource].
 */
@Singleton
class WidgetIconCatalog @Inject constructor(
    @ApplicationContext private val context: Context,
) : WidgetIconResolver {

    /**
     * One built-in icon in the catalog.
     *
     * @property key stable identifier persisted in configs.
     * @property drawable the resource backing the key today.
     * @property displayName user-facing label shown in the picker grid.
     */
    data class Entry(
        val key: String,
        @DrawableRes val drawable: Int,
        val displayName: String,
    )

    /** Public, ordered list of built-ins — drives the picker grid order. */
    val entries: List<Entry> = listOf(
        Entry(WidgetIconKeys.DEFAULT_ACTIVE,   R.drawable.ic_flashlight_on,  "Flashlight on"),
        Entry(WidgetIconKeys.DEFAULT_INACTIVE, R.drawable.ic_flashlight_off, "Flashlight off"),
        Entry("strobe_on",                     R.drawable.ic_strobe_on,      "Strobe active"),
        Entry("strobe_off",                    R.drawable.ic_strobe,         "Strobe idle"),
    )

    private val customDir: File by lazy { File(context.filesDir, CUSTOM_DIR_NAME) }

    /** True iff [key] denotes a user-supplied custom icon. */
    override fun isCustom(key: String): Boolean = key.startsWith(WidgetIconKeys.CUSTOM_PREFIX)

    /**
     * Resolve a key to its [WidgetIconSource]. Unknown built-in keys fall
     * back to the default active icon; a custom key always resolves to its
     * file path (missing-file handling is the caller's concern — it falls
     * back to the default drawable when the bitmap can't be decoded).
     */
    fun resolveSource(key: String): WidgetIconSource =
        if (isCustom(key)) {
            WidgetIconSource.CustomFile(File(customDir, key.removePrefix(WidgetIconKeys.CUSTOM_PREFIX)).absolutePath)
        } else {
            WidgetIconSource.Resource(resolve(key))
        }

    /** Resolve a built-in key to its drawable, falling back to the default
     *  active icon for unknown / custom keys. */
    @DrawableRes
    override fun resolve(key: String): Int =
        entries.firstOrNull { it.key == key }?.drawable
            ?: entries.first().drawable

    /** Convenience — true iff [key] resolves to a known built-in entry. */
    fun isKnown(key: String): Boolean = entries.any { it.key == key }

    /**
     * Decode a custom-icon file to a [Bitmap] for the RemoteViews
     * renderer, or `null` if it's missing / unreadable (caller falls back
     * to the default drawable). The stored file is already downscaled, so
     * this is a cheap decode safe to call on the provider's IO coroutine.
     */
    override fun loadCustomBitmap(key: String): Bitmap? {
        if (!isCustom(key)) return null
        val path = File(customDir, key.removePrefix(WidgetIconKeys.CUSTOM_PREFIX)).absolutePath
        return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    /**
     * Copy + downscale the image at [uri] into app-internal storage and
     * return its stable custom key (`custom:<uuid>.png`), or `null` on
     * failure. Downscaling keeps the PNG small enough for the RemoteViews
     * Binder transaction and bounds memory use for large source images.
     */
    suspend fun importCustomIcon(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            // Read the URI ONCE into memory — re-opening a gallery/Photos
            // content:// stream (the old two-pass decode) is fragile and
            // was silently failing on real devices.
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                Log.w(PendingTorchWidgetConfigs.TAG, "importCustomIcon: empty/unreadable stream for $uri")
                return@withContext null
            }
            val decoded = decodeDownscaled(bytes, MAX_ICON_PX)
            if (decoded == null) {
                Log.w(PendingTorchWidgetConfigs.TAG, "importCustomIcon: undecodable image for $uri")
                return@withContext null
            }
            // Gallery photos carry their rotation in EXIF, which the
            // decoder ignores — apply it so the icon isn't sideways.
            val bitmap = applyExifOrientation(decoded, bytes)
            customDir.mkdirs()
            val file = File(customDir, "${UUID.randomUUID()}.png")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out) }
            bitmap.recycle()
            Log.d(PendingTorchWidgetConfigs.TAG, "importCustomIcon: wrote ${file.name}")
            WidgetIconKeys.CUSTOM_PREFIX + file.name
        } catch (t: Throwable) {
            Log.w(PendingTorchWidgetConfigs.TAG, "importCustomIcon failed for $uri", t)
            null
        }
    }

    /** Two-pass decode of an already-read [bytes] buffer: read bounds,
     *  pick an `inSampleSize` so the source never loads at full
     *  resolution, then exact-scale to fit [maxPx]. Decoding from bytes
     *  (not the URI stream) means we open the content URI only once. */
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

    /** Rotate / flip [bitmap] to honour the source image's EXIF
     *  orientation (read from the same [bytes]). Returns the input
     *  unchanged for normal/undefined orientation or if the transform
     *  can't be allocated. */
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
            Log.w(PendingTorchWidgetConfigs.TAG, "applyExifOrientation OOM — using unrotated icon", e)
            bitmap
        }
    }

    companion object {
        private const val CUSTOM_DIR_NAME = "widget_icons"

        /** Longest-edge cap (px) for an imported icon — small enough for
         *  the RemoteViews Binder limit, large enough to stay crisp on a
         *  home-screen cell. */
        private const val MAX_ICON_PX = 192

        private const val PNG_QUALITY = 100
    }
}
