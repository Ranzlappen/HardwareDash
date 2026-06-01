package dev.ranzlappen.gadget.core.widgetkit.render

import android.graphics.Bitmap
import androidx.annotation.DrawableRes

/**
 * Feature-side icon resolution surface the kit's [WidgetAppearanceRenderer]
 * depends on. Each widget-bearing feature provides one implementation
 * (typically named `<Feature>WidgetIconCatalog`) that knows about its
 * bundled drawables + the user-imported custom-icon path.
 *
 * Each feature contributes its impl into a
 * `Map<String, WidgetIconResolver>` multibinding keyed by its stable
 * feature id, so the kit's renderer (one app-wide `@Inject` singleton)
 * resolves the right per-feature catalog at `:app` assembly. The
 * provider passes that same id to [WidgetAppearanceRenderer.apply], which
 * selects the entry — icon keys (e.g.
 * [dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconKeys.DEFAULT_ACTIVE])
 * are shared across features, so the feature id, not the key, picks the
 * catalog:
 *
 * ```kotlin
 * @Module @InstallIn(SingletonComponent::class)
 * abstract class TorchModule {
 *   @Binds @IntoMap @StringKey(TorchBootRearmHandler.FEATURE_ID)
 *   abstract fun bindWidgetIconResolver(
 *       impl: WidgetIconCatalog,
 *   ): WidgetIconResolver
 * }
 * ```
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
