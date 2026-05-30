package dev.ranzlappen.gadget.feature.torch.widget.customization

import android.content.Context
import android.widget.RemoteViews
import dev.ranzlappen.gadget.core.widgetkit.config.BackgroundMode
import dev.ranzlappen.gadget.core.widgetkit.config.IconStyle
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetIconSource
import dev.ranzlappen.gadget.feature.torch.R
import javax.inject.Inject
import javax.inject.Singleton

/** Fully-transparent ARGB — a no-op colour filter under SRC_ATOP. */
private const val TRANSPARENT = 0

/** Fully-opaque image alpha (0..255). The resting icon alpha. */
private const val OPAQUE = 255

/** Resting icon padding (dp) — mirrors the value baked into the widget
 *  layouts. Re-applied on every normal render so a Scale press frame can
 *  be reverted on a recycled view. */
private const val DEFAULT_ICON_PADDING_DP = 12

/** Pressed-frame icon padding (dp) for [TapAnimation.Scale] — larger than
 *  the resting value so the icon visibly shrinks. */
private const val SCALE_PRESSED_PADDING_DP = 26

/** Pressed-frame icon alpha for [TapAnimation.Pulse]. */
private const val PULSE_ALPHA = 70

/** Pressed-frame icon tint for [TapAnimation.Flash] — bright white. */
private val FLASH_COLOR = 0xFFFFFFFF.toInt()

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
        val customBitmap = if (iconCatalog.isCustom(key)) iconCatalog.loadCustomBitmap(key) else null
        if (customBitmap != null) {
            views.setImageViewBitmap(R.id.widget_icon, customBitmap)
            // A user image carries its own colours — a tint would recolour
            // the whole bitmap, so clear any filter (TRANSPARENT is a SRC_ATOP
            // no-op) rather than applying the icon-style tint.
            views.setInt(R.id.widget_icon, "setColorFilter", TRANSPARENT)
        } else {
            views.setImageViewResource(R.id.widget_icon, iconCatalog.resolve(key))
            // Pre-31 fallback uses setColorFilter; API 31+ also supports
            // it for backwards compat. SRC_IN preserves the alpha channel
            // of the source drawable.
            views.setInt(R.id.widget_icon, "setColorFilter", iconTintArgb(context, appearance.iconStyle))
        }

        // Reset the two properties a tap-press frame mutates, so a
        // recycled host view always reverts cleanly to its resting look
        // (RemoteViews reuses views — state not re-set here would stick).
        views.setInt(R.id.widget_icon, "setImageAlpha", OPAQUE)
        val pad = dp(context, DEFAULT_ICON_PADDING_DP)
        views.setViewPadding(R.id.widget_icon, pad, pad, pad, pad)
    }

    /**
     * Overlay the transient "pressed" look for [TapBehavior.animation] on
     * top of an already-[apply]'d [views]. RemoteViews can't truly animate,
     * so each effect is a single mutated frame the provider holds for
     * ~150 ms before re-rendering the resting state:
     *  - [TapAnimation.Flash] — recolour the icon bright white.
     *  - [TapAnimation.Pulse] — drop the icon alpha.
     *  - [TapAnimation.Scale] — grow the icon padding so it shrinks.
     *  - [TapAnimation.None] / [TapAnimation.Ripple] — no frame
     *    (Ripple is the launcher's stock press ripple, applied as a
     *    button background by the provider).
     */
    fun applyPressedFrame(context: Context, views: RemoteViews, appearance: WidgetAppearance) {
        when (appearance.tap.animation) {
            TapAnimation.Flash -> views.setInt(R.id.widget_icon, "setColorFilter", FLASH_COLOR)
            TapAnimation.Pulse -> views.setInt(R.id.widget_icon, "setImageAlpha", PULSE_ALPHA)
            TapAnimation.Scale -> {
                val pad = dp(context, SCALE_PRESSED_PADDING_DP)
                views.setViewPadding(R.id.widget_icon, pad, pad, pad, pad)
            }
            TapAnimation.None, TapAnimation.Ripple -> Unit
        }
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
