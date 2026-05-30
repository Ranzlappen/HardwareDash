package dev.ranzlappen.gadget.feature.torch.widget.customization

import android.content.Context
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import dev.ranzlappen.gadget.core.widgetkit.config.IconStyle
import dev.ranzlappen.gadget.core.widgetkit.config.IconTint
import dev.ranzlappen.gadget.feature.torch.R

/**
 * Resolve an [IconStyle]'s [IconTint] to a concrete ARGB int.
 *
 * Single source of truth shared by the RemoteViews renderer
 * ([WidgetAppearanceRenderer]) and the in-app Compose preview so the
 * placed widget and its preview can never drift on tint.
 *
 * Stays torch-side for the moment — the two theme colors live in
 * `feature/torch/src/main/res/values/colors.xml`. Move to the kit
 * follows in C2 alongside the resources + the RemoteViews renderer.
 */
@ColorInt
internal fun iconTintArgb(context: Context, iconStyle: IconStyle): Int = when (iconStyle.tint) {
    IconTint.ThemeAccent -> ContextCompat.getColor(context, R.color.widget_tint_accent)
    IconTint.ThemeOnSurface -> ContextCompat.getColor(context, R.color.widget_tint_on_surface)
    IconTint.MonochromeWhite -> 0xFFFFFFFF.toInt()
    IconTint.MonochromeBlack -> 0xFF000000.toInt()
    IconTint.Custom -> iconStyle.customTintArgb.toInt()
}
