package dev.ranzlappen.gadget.feature.torch.widget.customization

import android.content.Context
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import dev.ranzlappen.gadget.feature.torch.R
import javax.inject.Inject
import javax.inject.Singleton

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
        val drawableRes = when (appearance.background) {
            BackgroundMode.GlassSurface -> R.drawable.widget_background_glass
            BackgroundMode.Solid -> R.drawable.widget_background_solid
            BackgroundMode.Transparent -> android.R.color.transparent
        }
        views.setInt(R.id.widget_background, "setBackgroundResource", drawableRes)
        // Per-instance solid colour tinting is intentionally NOT
        // applied here. RemoteViews can't sensibly compose a shape
        // drawable + a background tint on a generic View, and using
        // `setBackgroundColor` would clobber the rounded corners.
        // A future batch can ship pre-tinted drawables or move to a
        // composed ImageView background to support arbitrary colours.
        // For now the schema field [WidgetAppearance.solidColor] is
        // captured + exported but not visually applied.
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
