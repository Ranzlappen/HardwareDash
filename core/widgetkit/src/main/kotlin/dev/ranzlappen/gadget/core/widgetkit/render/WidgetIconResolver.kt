package dev.ranzlappen.gadget.core.widgetkit.render

import android.graphics.Bitmap
import androidx.annotation.DrawableRes

/**
 * Feature-side icon resolution surface the kit's [WidgetAppearanceRenderer]
 * depends on. Each widget-bearing feature provides one implementation
 * (typically named `<Feature>WidgetIconCatalog`) that knows about its
 * bundled drawables + the user-imported custom-icon path.
 *
 * Bind it from the feature's Hilt module so the kit's renderer (which
 * has an `@Inject` constructor) resolves the right per-feature catalog
 * at `:app` assembly:
 *
 * ```kotlin
 * @Module @InstallIn(SingletonComponent::class)
 * abstract class TorchModule {
 *   @Binds @Singleton
 *   abstract fun bindWidgetIconResolver(
 *       impl: WidgetIconCatalog,
 *   ): WidgetIconResolver
 * }
 * ```
 *
 * If an app has multiple widget-bearing features, multibindings or
 * qualifiers will be added in C5's provider registry batch.
 */
interface WidgetIconResolver {
    /**
     * Resolve a built-in icon [key] to its drawable resource id. For an
     * unknown / custom key, callers should fall back via [isCustom] +
     * [loadCustomBitmap]; if both fail the implementation typically
     * returns its default-active drawable so the widget never renders
     * blank.
     */
    @DrawableRes
    fun resolve(key: String): Int

    /** True iff [key] denotes a user-supplied custom icon (typically
     *  prefixed with [dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconKeys.CUSTOM_PREFIX]). */
    fun isCustom(key: String): Boolean

    /**
     * Decode the bitmap for a user-imported custom-icon [key], or null
     * if the file is missing / unreadable. Safe to call on a background
     * thread — the kit's renderer calls it from the provider's IO
     * coroutine.
     */
    fun loadCustomBitmap(key: String): Bitmap?
}
