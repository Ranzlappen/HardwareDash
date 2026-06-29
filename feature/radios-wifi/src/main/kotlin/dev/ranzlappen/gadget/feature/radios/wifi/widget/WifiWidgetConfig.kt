package dev.ranzlappen.gadget.feature.radios.wifi.widget

import dev.ranzlappen.gadget.core.widgetkit.WidgetKitConfig
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetAppearance
import dev.ranzlappen.gadget.core.widgetkit.config.WidgetSizePreset
import kotlinx.serialization.Serializable

/**
 * Per-`appWidgetId` config for the WiFi-signal widget. Display-only, so it
 * carries only the kit contract + a starting-size hint (the data is the live
 * WiFi connection, not a user-selected entity).
 */
@Serializable
data class WifiWidgetConfig(
    val sizePreset: WidgetSizePreset = WidgetSizePreset.Medium,
    override val displayName: String = "WiFi",
    override val removed: Boolean = false,
    override val schemaVersion: Int = 1,
    override val appearance: WidgetAppearance = WidgetAppearance(),
) : WidgetKitConfig
