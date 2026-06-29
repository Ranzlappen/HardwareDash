package dev.ranzlappen.gadget.feature.ambient.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.ambient.AmbientSensor

/**
 * Hilt entry point for the ambient widget's `AppWidgetProvider`, which is
 * system-instantiated and can't receive `@Inject`. Reached via
 * `EntryPointAccessors.fromApplication(context, AmbientWidgetEntryPoint::class.java)`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AmbientWidgetEntryPoint {
    fun ambientWidgetConfigStore(): WidgetConfigStore<AmbientWidgetConfig>
    fun ambientSensor(): AmbientSensor
    fun widgetAppearanceRenderer(): WidgetAppearanceRenderer
}
