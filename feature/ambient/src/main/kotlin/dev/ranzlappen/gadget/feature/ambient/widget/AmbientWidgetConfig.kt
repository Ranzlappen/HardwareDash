package dev.ranzlappen.gadget.feature.ambient.widget

import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.Serializable

/**
 * Per-`appWidgetId` config for the ambient-light widget. Display-only, so it
 * carries only the kit contract + a starting-size hint (the data is the live
 * light-sensor reading, not a user-selected entity).
 */
@Serializable
data class AmbientWidgetConfig(
    val sizePreset: WidgetSizePreset = WidgetSizePreset.Medium,
    override val displayName: String = "Ambient light",
    override val removed: Boolean = false,
    override val schemaVersion: Int = 1,
    override val appearance: WidgetAppearance = WidgetAppearance(),
) : WidgetKitConfig
