package dev.ranzlappen.gadget.feature.torch.widget.customization

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.annotation.DrawableRes
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.feature.torch.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

/**
 * Where an icon key resolves to. Built-in keys map to a bundled
 * [DrawableRes]; user-supplied keys (prefixed [WidgetIconCatalog.CUSTOM_PREFIX])
 * map to a downscaled PNG copied into app-internal storage. Both the
 * Compose preview and the RemoteViews renderer branch on this so a custom
 * image renders identically in-app and on the home screen.
 */
sealed interface WidgetIconSource {
    data class Resource(@DrawableRes val resId: Int) : WidgetIconSource
    data class CustomFile(val path: String) : WidgetIconSource
}

/**
 * Curated registry of icons available to widget configurations, plus the
 * import/resolve surface for user-supplied custom icons.
 *
 * Built-in entries pair a stable [Entry.key] (persisted in [IconStyle])
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
) {

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
        Entry(DEFAULT_ACTIVE,   R.drawable.ic_flashlight_on,  "Flashlight on"),
        Entry(DEFAULT_INACTIVE, R.drawable.ic_flashlight_off, "Flashlight off"),
        Entry("strobe_on",      R.drawable.ic_strobe_on,      "Strobe active"),
        Entry("strobe_off",     R.drawable.ic_strobe,         "Strobe idle"),
    )

    private val customDir: File by lazy { File(context.filesDir, CUSTOM_DIR_NAME) }

    /** True iff [key] denotes a user-supplied custom icon. */
    fun isCustom(key: String): Boolean = key.startsWith(CUSTOM_PREFIX)

    /**
     * Resolve a key to its [WidgetIconSource]. Unknown built-in keys fall
     * back to the default active icon; a custom key always resolves to its
     * file path (missing-file handling is the caller's concern — it falls
     * back to the default drawable when the bitmap can't be decoded).
     */
    fun resolveSource(key: String): WidgetIconSource =
        if (isCustom(key)) {
            WidgetIconSource.CustomFile(File(customDir, key.removePrefix(CUSTOM_PREFIX)).absolutePath)
        } else {
            WidgetIconSource.Resource(resolve(key))
        }

    /** Resolve a built-in key to its drawable, falling back to the default
     *  active icon for unknown / custom keys. */
    @DrawableRes
    fun resolve(key: String): Int =
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
    fun loadCustomBitmap(key: String): Bitmap? {
        if (!isCustom(key)) return null
        val path = File(customDir, key.removePrefix(CUSTOM_PREFIX)).absolutePath
        return runCatching { BitmapFactory.decodeFile(path) }.getOrNull()
    }

    /**
     * Copy + downscale the image at [uri] into app-internal storage and
     * return its stable custom key (`custom:<uuid>.png`), or `null` on
     * failure. Downscaling keeps the PNG small enough for the RemoteViews
     * Binder transaction and bounds memory use for large source images.
     */
    suspend fun importCustomIcon(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            customDir.mkdirs()
            val bitmap = decodeDownscaled({ context.contentResolver.openInputStream(uri) }, MAX_ICON_PX)
                ?: return@runCatching null
            val file = File(customDir, "${UUID.randomUUID()}.png")
            FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out) }
            bitmap.recycle()
            CUSTOM_PREFIX + file.name
        }.getOrNull()
    }

    /** Two-pass decode: read bounds, pick an `inSampleSize` so the source
     *  never loads at full resolution, then exact-scale to fit [maxPx]. */
    private fun decodeDownscaled(openStream: () -> InputStream?, maxPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / sample > maxPx * 2 || bounds.outHeight / sample > maxPx * 2) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = openStream()?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null

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

    companion object {
        /** Default active-state icon key. */
        const val DEFAULT_ACTIVE = "default_active"

        /** Default inactive-state icon key. */
        const val DEFAULT_INACTIVE = "default_inactive"

        /** Key prefix marking a user-supplied custom icon. The remainder
         *  is the file name inside [CUSTOM_DIR_NAME]. */
        const val CUSTOM_PREFIX = "custom:"

        private const val CUSTOM_DIR_NAME = "widget_icons"

        /** Longest-edge cap (px) for an imported icon — small enough for
         *  the RemoteViews Binder limit, large enough to stay crisp on a
         *  home-screen cell. */
        private const val MAX_ICON_PX = 192

        private const val PNG_QUALITY = 100
    }
}
