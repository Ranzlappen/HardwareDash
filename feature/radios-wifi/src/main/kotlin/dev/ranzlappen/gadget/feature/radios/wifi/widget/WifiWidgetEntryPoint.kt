package dev.ranzlappen.gadget.feature.radios.wifi.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.radios.wifi.WifiMonitor

/**
 * Hilt entry point for the WiFi widget's `AppWidgetProvider`, which is
 * system-instantiated and can't receive `@Inject`. Reached via
 * `EntryPointAccessors.fromApplication(context, WifiWidgetEntryPoint::class.java)`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WifiWidgetEntryPoint {
    fun wifiWidgetConfigStore(): WidgetConfigStore<WifiWidgetConfig>
    fun wifiMonitor(): WifiMonitor
    fun widgetAppearanceRenderer(): WidgetAppearanceRenderer
}
