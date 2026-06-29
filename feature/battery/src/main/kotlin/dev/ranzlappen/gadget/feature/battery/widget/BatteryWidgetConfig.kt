package dev.ranzlappen.gadget.feature.battery.widget

import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.Serializable

/**
 * Per-`appWidgetId` config for the battery status widget. A display-only
 * widget, so it carries only the kit contract + a starting-size hint — no
 * feature-specific binding (the data is the live system battery, not a
 * user-selected entity).
 */
@Serializable
data class BatteryWidgetConfig(
    val sizePreset: WidgetSizePreset = WidgetSizePreset.Medium,
    override val displayName: String = "Battery",
    override val removed: Boolean = false,
    override val schemaVersion: Int = 1,
    override val appearance: WidgetAppearance = WidgetAppearance(),
) : WidgetKitConfig
