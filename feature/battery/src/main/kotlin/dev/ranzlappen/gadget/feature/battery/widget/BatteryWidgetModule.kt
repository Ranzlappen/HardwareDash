package dev.ranzlappen.gadget.feature.battery.widget

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import javax.inject.Singleton

/**
 * Hilt bindings for the battery widget's per-`appWidgetId` config store.
 * Top-level `object` module for `@Provides` (repo convention; mirrors
 * `AppsWidgetModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object BatteryWidgetModule {

    @Provides
    @Singleton
    fun provideBatteryWidgetConfigStore(
        factory: FeaturePreferencesFactory,
    ): WidgetConfigStore<BatteryWidgetConfig> {
        val prefs = factory.create(
            fileName = "battery_widgets",
            keyPrefix = "battery_widget_",
            serializer = BatteryWidgetConfig.serializer(),
        )
        return WidgetConfigStore(prefs)
    }
}
