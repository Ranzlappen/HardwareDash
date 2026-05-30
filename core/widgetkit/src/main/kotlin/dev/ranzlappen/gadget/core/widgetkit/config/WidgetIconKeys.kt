package dev.ranzlappen.gadget.core.widgetkit.config

/**
 * Stable string keys shared across features' icon catalogs.
 *
 * Built-in keys persisted in [IconStyle] use these constants so the kit-
 * level [IconStyle] defaults are self-contained — features map the keys
 * to their own bundled drawables. Custom (user-imported) icons carry a
 * `custom:<file>` prefix so the resolver can branch.
 */
object WidgetIconKeys {
    /** Default active-state icon key. */
    const val DEFAULT_ACTIVE: String = "default_active"

    /** Default inactive-state icon key. */
    const val DEFAULT_INACTIVE: String = "default_inactive"

    /** Key prefix marking a user-supplied custom icon. The remainder is
     *  a file name under the feature's custom-icons directory. */
    const val CUSTOM_PREFIX: String = "custom:"
}
