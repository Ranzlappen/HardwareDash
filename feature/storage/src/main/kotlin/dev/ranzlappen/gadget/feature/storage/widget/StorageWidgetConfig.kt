package dev.ranzlappen.gadget.feature.storage.widget

import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.Serializable

/**
 * Per-`appWidgetId` config for the internal-storage widget. Display-only, so it
 * carries only the kit contract + a starting-size hint (the data is the live
 * internal volume, not a user-selected entity).
 */
@Serializable
data class StorageWidgetConfig(
    val sizePreset: WidgetSizePreset = WidgetSizePreset.Medium,
    override val displayName: String = "Storage",
    override val removed: Boolean = false,
    override val schemaVersion: Int = 1,
    override val appearance: WidgetAppearance = WidgetAppearance(),
) : WidgetKitConfig
