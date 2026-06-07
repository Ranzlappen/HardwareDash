package dev.ranzlappen.gadget.feature.apps.widget

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.apps.icons.AppIconLoader

/**
 * Hilt entry point for the folder widget's `AppWidgetProvider` / receivers,
 * which can't receive `@Inject`. Reached via `EntryPointAccessors
 * .fromApplication(context, FolderWidgetEntryPoint::class.java)`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface FolderWidgetEntryPoint {
    fun folderWidgetConfigStore(): WidgetConfigStore<FolderWidgetConfig>
    fun folderPendingConfigs(): PendingWidgetConfigs<FolderWidgetConfig>
    fun appsDao(): AppsDao
    fun appIconLoader(): AppIconLoader
}
