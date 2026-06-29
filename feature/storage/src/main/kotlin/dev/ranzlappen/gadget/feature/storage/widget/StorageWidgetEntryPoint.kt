package dev.ranzlappen.gadget.feature.storage.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.storage.StorageMonitor

/**
 * Hilt entry point for the storage widget's `AppWidgetProvider`, which is
 * system-instantiated and can't receive `@Inject`. Reached via
 * `EntryPointAccessors.fromApplication(context, StorageWidgetEntryPoint::class.java)`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface StorageWidgetEntryPoint {
    fun storageWidgetConfigStore(): WidgetConfigStore<StorageWidgetConfig>
    fun storageMonitor(): StorageMonitor
    fun widgetAppearanceRenderer(): WidgetAppearanceRenderer
}
