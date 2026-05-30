package dev.ranzlappen.gadget.core.widgetkit.config

import androidx.annotation.DrawableRes

/**
 * Where an icon key resolves to. Built-in keys map to a bundled
 * [DrawableRes]; user-supplied keys (prefixed [WidgetIconKeys.CUSTOM_PREFIX])
 * map to a downscaled PNG copied into app-internal storage. The Compose
 * preview and the RemoteViews renderer both branch on this so a custom
 * image renders identically in-app and on the home screen.
 *
 * Feature-side icon catalogs return this from their resolver — kit-side
 * surfaces (renderer, preview) only know about the source, not where the
 * mapping came from.
 */
sealed interface WidgetIconSource {
    data class Resource(@DrawableRes val resId: Int) : WidgetIconSource
    data class CustomFile(val path: String) : WidgetIconSource
}
