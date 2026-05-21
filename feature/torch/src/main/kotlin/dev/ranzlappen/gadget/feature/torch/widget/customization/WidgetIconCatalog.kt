package dev.ranzlappen.gadget.feature.torch.widget.customization

import androidx.annotation.DrawableRes
import dev.ranzlappen.gadget.feature.torch.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Curated registry of icons available to widget configurations.
 *
 * Each entry pairs a stable [Entry.key] (persisted in [IconStyle])
 * with a [DrawableRes] reference. The catalog is the *single source
 * of truth* — configs never store raw resource IDs, only keys, so:
 *  - Renaming a drawable doesn't break on-disk configs (only the key
 *    has to stay stable; the resource ID can drift).
 *  - Export → JSON dumps human-readable strings rather than opaque
 *    Android resource integers.
 *  - Future "user-supplied icon" support slots into the same lookup
 *    surface (extend [resolve] to fall back to a file-Uri loader).
 *
 * The current set is intentionally compact — ~16 entries covering
 * the existing widget kinds (flashlight on/off, strobe on/off) plus
 * a handful of generic icons useful for Container slots
 * (battery, bolt, info, etc.). Future batches grow this catalog as
 * more widget kinds land.
 */
@Singleton
class WidgetIconCatalog @Inject constructor() {

    /**
     * One icon in the catalog.
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

    /** Public, ordered list — drives the picker grid's render order. */
    val entries: List<Entry> = listOf(
        Entry(DEFAULT_ACTIVE,   R.drawable.ic_flashlight_on,  "Flashlight on"),
        Entry(DEFAULT_INACTIVE, R.drawable.ic_flashlight_off, "Flashlight off"),
        Entry("strobe_on",      R.drawable.ic_strobe_on,      "Strobe active"),
        Entry("strobe_off",     R.drawable.ic_strobe,         "Strobe idle"),
    )

    /** Resolve a key to its drawable, falling back to the default
     *  active icon if the key is unknown (corrupted config, removed
     *  catalog entry across versions, etc.). Never returns 0 — callers
     *  can pass the result directly to
     *  [android.widget.RemoteViews.setImageViewResource]. */
    @DrawableRes
    fun resolve(key: String): Int =
        entries.firstOrNull { it.key == key }?.drawable
            ?: entries.first().drawable

    /** Convenience — true iff [key] resolves to a known entry. The
     *  picker grid uses this to mark the selected swatch. */
    fun isKnown(key: String): Boolean = entries.any { it.key == key }

    companion object {
        /** Default active-state icon key. Used in [IconStyle]'s
         *  default values so brand-new widgets render sensibly
         *  without the user picking anything. */
        const val DEFAULT_ACTIVE = "default_active"

        /** Default inactive-state icon key. */
        const val DEFAULT_INACTIVE = "default_inactive"
    }
}
