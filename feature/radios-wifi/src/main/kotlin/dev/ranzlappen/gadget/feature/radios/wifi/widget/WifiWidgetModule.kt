package dev.ranzlappen.gadget.feature.radios.wifi.widget

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import javax.inject.Singleton

/**
 * Hilt bindings for the WiFi widget's per-`appWidgetId` config store.
 * Top-level `object` module for `@Provides` (repo convention; mirrors
 * `BatteryWidgetModule` / `StorageWidgetModule`).
 */
@Module
@InstallIn(SingletonComponent::class)
object WifiWidgetModule {

    @Provides
    @Singleton
    fun provideWifiWidgetConfigStore(
        factory: FeaturePreferencesFactory,
    ): WidgetConfigStore<WifiWidgetConfig> {
        val prefs = factory.create(
            fileName = "wifi_widgets",
            keyPrefix = "wifi_widget_",
            serializer = WifiWidgetConfig.serializer(),
        )
        return WidgetConfigStore(prefs)
    }
}
