package dev.ranzlappen.gadget.feature.storage.widget

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import javax.inject.Singleton

/**
 * Hilt bindings for the storage widget's per-`appWidgetId` config store.
 * Top-level `object` module for `@Provides` (repo convention; mirrors
 * `AppsWidgetModule` / `BatteryWidgetModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object StorageWidgetModule {

    @Provides
    @Singleton
    fun provideStorageWidgetConfigStore(
        factory: FeaturePreferencesFactory,
    ): WidgetConfigStore<StorageWidgetConfig> {
        val prefs = factory.create(
            fileName = "storage_widgets",
            keyPrefix = "storage_widget_",
            serializer = StorageWidgetConfig.serializer(),
        )
        return WidgetConfigStore(prefs)
    }
}
