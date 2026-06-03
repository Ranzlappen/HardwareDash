package dev.ranzlappen.gadget.core.widgetkit.provider

import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset

/**
 * The render density the generic provider resolves for one paint of a widget
 * instance — derived from the launcher-reported size when available, else
 * seeded from the config's [WidgetSizePreset].
 *
 * Drives the adaptive RemoteViews paint: [Expanded] shows the widget's name
 * label beneath the icon; [Compact] / [Regular] keep it icon-only. Kept coarse
 * deliberately — RemoteViews scaling is approximate, so a three-step ladder is
 * all the granularity that reads cleanly across launchers.
 */
enum class WidgetRenderDensity {
    Compact,
    Regular,
    Expanded;

    /** Whether the widget's name label should paint at this density. */
    val showLabel: Boolean get() = this == Expanded

    companion object {
        /** The fallback density for a cold paint before the launcher reports a
         *  size, mapped from the user's chosen starting [WidgetSizePreset]. */
        fun fromPreset(preset: WidgetSizePreset): WidgetRenderDensity = when (preset) {
            WidgetSizePreset.Small -> Compact
            WidgetSizePreset.Medium -> Regular
            WidgetSizePreset.Large -> Expanded
        }
    }
}
