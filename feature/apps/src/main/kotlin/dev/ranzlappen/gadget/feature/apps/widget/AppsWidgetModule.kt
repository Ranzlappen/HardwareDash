package dev.ranzlappen.gadget.feature.apps.widget

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingEntry
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import javax.inject.Singleton

/**
 * Hilt bindings for the folder widget's per-`appWidgetId` config store.
 *
 * Follows the repo convention (a top-level `object` module for `@Provides`,
 * never companion `@Provides` on an abstract class — see CLAUDE.md). Mirrors
 * torch's `TorchWidgetDataModule`.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppsWidgetModule {

    @Provides
    @Singleton
    fun provideFolderWidgetConfigStore(
        factory: FeaturePreferencesFactory,
    ): WidgetConfigStore<FolderWidgetConfig> {
        val prefs = factory.create(
            fileName = "apps_folder_widgets",
            keyPrefix = "folder_widget_",
            serializer = FolderWidgetConfig.serializer(),
        )
        return WidgetConfigStore(prefs)
    }

    /**
     * The folder widget's persistent pin-flow bridge — carries the user's
     * pre-pin [FolderWidgetConfig] across the `requestPinAppWidget` round-trip
     * so [FolderWidgetPinReceiver] (and the provider's `reconcilePendingConfig`
     * rescue) can recover it once the OS assigns an `appWidgetId`. Each entry is
     * a `PendingEntry<FolderWidgetConfig>`, hence the composed serializer.
     */
    @Provides
    @Singleton
    fun provideFolderPendingWidgetConfigs(
        factory: FeaturePreferencesFactory,
    ): PendingWidgetConfigs<FolderWidgetConfig> {
        val prefs = factory.create(
            fileName = "apps_pending_folder_widgets",
            keyPrefix = "pending_",
            serializer = PendingEntry.serializer(FolderWidgetConfig.serializer()),
        )
        return PendingWidgetConfigs(prefs, tag = PinFolderHelper.TAG)
    }
}
