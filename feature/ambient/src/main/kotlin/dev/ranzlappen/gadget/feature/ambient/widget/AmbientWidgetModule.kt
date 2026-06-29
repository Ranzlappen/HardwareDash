package dev.ranzlappen.gadget.feature.ambient.widget

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import javax.inject.Singleton

/**
 * Hilt bindings for the ambient widget's per-`appWidgetId` config store.
 * Top-level `object` module for `@Provides` (repo convention; mirrors
 * `BatteryWidgetModule` / `StorageWidgetModule` / `WifiWidgetModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object AmbientWidgetModule {

    @Provides
    @Singleton
    fun provideAmbientWidgetConfigStore(
        factory: FeaturePreferencesFactory,
    ): WidgetConfigStore<AmbientWidgetConfig> {
        val prefs = factory.create(
            fileName = "ambient_widgets",
            keyPrefix = "ambient_widget_",
            serializer = AmbientWidgetConfig.serializer(),
        )
        return WidgetConfigStore(prefs)
    }
}
