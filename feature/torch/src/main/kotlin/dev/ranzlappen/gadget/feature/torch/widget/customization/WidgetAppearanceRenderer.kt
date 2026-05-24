package dev.ranzlappen.gadget.feature.torch.widget.customization

import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import dev.ranzlappen.gadget.feature.torch.R
import javax.inject.Inject
import javax.inject.Singleton

/** Fully-transparent ARGB — a no-op colour filter under SRC_ATOP. */
private const val TRANSPARENT = 0

/**
 * Applies a [WidgetAppearance] to a [RemoteViews] tree.
 *
 * Centralises the RemoteViews paint logic so the two existing widget
 * providers + future Container slot rendering all share one
 * implementation. RemoteViews has a narrow programmatic surface
 * (`setInt`, `setImageViewResource`, `setColorFilter`, …), so this
 * helper holds the mapping from semantic appearance fields to those
 * primitive calls.
 *
 * Layout contract: the host layout MUST contain
 *  - a background view with id `@+id/widget_background`
 *  - an icon view with id `@+id/widget_icon`
 *
 * Layouts ship with both views; providers that want to opt out of a
 * field can simply not include the corresponding id (RemoteViews
 * silently no-ops calls against missing ids).
 *
 * Tap-animation primitive is split into a `prepare` call that
 * primes the visual into its "pressed" state and a `revert` call
 * the provider schedules ~150 ms later. The simple animations
 * (Pulse/Scale/Flash) are approximations — see
 * [TapAnimation]'s KDoc.
 */
@Singleton
class WidgetAppearanceRenderer @Inject constructor(
    private val iconCatalog: WidgetIconCatalog,
) {

    /**
     * Apply [appearance] to [views] for a widget in the given
     * `active` state. The icon swaps between
     * `appearance.iconStyle.activeKey` and `inactiveKey` based on
     * the boolean.
     */
    fun apply(
        context: Context,
        views: RemoteViews,
        appearance: WidgetAppearance,
        active: Boolean,
    ) {
        applyBackground(views, appearance)
        applyIcon(context, views, appearance, active)
    }

    private fun applyBackground(views: RemoteViews, appearance: WidgetAppearance) {
        // Paint the surface through the @id/widget_background ImageView's
        // *image* (not its View background) so Solid mode can recolour a
        // rounded-rect shape via setColorFilter while keeping the corners.
        // setColorFilter uses PorterDuff.SRC_ATOP, so a colour with alpha 0
        // is a no-op — we set it on every render to clear any tint left on
        // a recycled host view (RemoteViews reuses views across updates).
        when (appearance.background) {
            BackgroundMode.GlassSurface -> {
                views.setImageViewResource(R.id.widget_background, R.drawable.widget_background_glass)
                views.setInt(R.id.widget_background, "setColorFilter", TRANSPARENT)
            }
            BackgroundMode.Solid -> {
                views.setImageViewResource(R.id.widget_background, R.drawable.widget_background_solid)
                views.setInt(R.id.widget_background, "setColorFilter", appearance.solidColor.toInt())
            }
            BackgroundMode.Transparent -> {
                views.setImageViewResource(R.id.widget_background, 0)
                views.setInt(R.id.widget_background, "setColorFilter", TRANSPARENT)
            }
        }
    }

    private fun applyIcon(
        context: Context,
        views: RemoteViews,
        appearance: WidgetAppearance,
        active: Boolean,
    ) {
        val key = if (active) appearance.iconStyle.activeKey else appearance.iconStyle.inactiveKey
        val drawable = iconCatalog.resolve(key)
        views.setImageViewResource(R.id.widget_icon, drawable)

        val tintArgb = when (appearance.iconStyle.tint) {
            IconTint.ThemeAccent -> ContextCompat.getColor(context, R.color.widget_tint_accent)
            IconTint.ThemeOnSurface -> ContextCompat.getColor(context, R.color.widget_tint_on_surface)
            IconTint.MonochromeWhite -> 0xFFFFFFFF.toInt()
            IconTint.MonochromeBlack -> 0xFF000000.toInt()
            IconTint.Custom -> appearance.iconStyle.customTintArgb.toInt()
        }
        // Pre-31 fallback uses setColorFilter; API 31+ also supports
        // it for backwards compat. SRC_IN preserves the alpha channel
        // of the source drawable.
        views.setInt(R.id.widget_icon, "setColorFilter", tintArgb)
    }
}
