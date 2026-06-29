package dev.ranzlappen.gadget.feature.battery.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.battery.BatteryMonitor

/**
 * Hilt entry point for the battery widget's `AppWidgetProvider`, which is
 * system-instantiated and can't receive `@Inject`. Reached via
 * `EntryPointAccessors.fromApplication(context, BatteryWidgetEntryPoint::class.java)`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface BatteryWidgetEntryPoint {
    fun batteryWidgetConfigStore(): WidgetConfigStore<BatteryWidgetConfig>
    fun batteryMonitor(): BatteryMonitor
    fun widgetAppearanceRenderer(): WidgetAppearanceRenderer
}
