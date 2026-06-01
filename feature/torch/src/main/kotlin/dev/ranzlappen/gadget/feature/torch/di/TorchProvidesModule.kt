package dev.ranzlappen.gadget.feature.torch.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackConfig
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingEntry
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.monitor.TorchBootRearmHandler
import dev.ranzlappen.gadget.feature.torch.widget.TorchPinLog
import dev.ranzlappen.gadget.feature.torch.widget.TorchWidgetConfig
import javax.inject.Singleton

/**
 * Torch's `@Provides`-style Hilt module. Sibling to the `@Binds`-only
 * [TorchModule]; lives in its own top-level `object` so the codebase
 * convention (mirrors `core.data.di.DataModule`,
 * `core.datastore.di.DataStoreModule`) stays uniform and the wiring is
 * robust against companion-object-on-abstract-module pitfalls.
 */
@Module
@InstallIn(SingletonComponent::class)
object TorchProvidesModule {

    /**
     * Torch's per-feature [WidgetFeedbackConfig] consumed by the
     * kit-side `WidgetFeedbackDispatcher`.
     *
     * Channel id is **pinned to the legacy `"widget_feedback"`** so any
     * system-settings overrides a user already set on the channel
     * (sound, badge, importance) carry across the kit migration — a
     * renamed channel would silently lose them. Future widget-bearing
     * features must use their own feature-prefixed id to avoid colliding
     * with this one. Small icon + channel strings come from torch's res.
     * The notification-id base scopes hashed IDs into a torch-specific
     * integer range ("WF" prefix — matches the legacy
     * `WidgetFeedbackDispatcher`'s ID base so existing notifications
     * keep their ids on upgrade).
     *
     * Contributed into the kit's `Map<String, WidgetFeedbackConfig>`
     * multibinding under [TorchBootRearmHandler.FEATURE_ID] so the one
     * `WidgetFeedbackDispatcher` singleton serves every feature; the
     * provider passes that id to `dispatch(...)`.
     */
    @Provides
    @IntoMap
    @StringKey(TorchBootRearmHandler.FEATURE_ID)
    fun provideWidgetFeedbackConfig(
        @ApplicationContext context: Context,
    ): WidgetFeedbackConfig = WidgetFeedbackConfig(
        channelId = "widget_feedback",
        channelName = context.getString(R.string.widget_feedback_channel_name),
        channelDescription = context.getString(R.string.widget_feedback_channel_description),
        smallIcon = R.drawable.ic_strobe,
        notificationIdBase = 0x57_46_00_00, // "WF" — matches legacy WidgetFeedbackDispatcher
    )

    /**
     * Torch's persisted widget-config store. Wraps a [FeaturePreferences]
     * created off the kit's [FeaturePreferencesFactory] with the
     * filename + key prefix that the legacy `TorchWidgetConfigRepository`
     * used (`torch_widgets` / `widget_`) so existing on-disk configs
     * keep loading unchanged.
     *
     * No [Migrator] is bound yet — `TorchWidgetConfig.schemaVersion` is
     * 1 and no fields have shifted shape. When the first migration is
     * needed, bump `schemaVersion` and add a `Migrator<TorchWidgetConfig>`
     * provider here.
     */
    @Provides
    @Singleton
    fun provideTorchWidgetConfigStore(
        factory: FeaturePreferencesFactory,
    ): WidgetConfigStore<TorchWidgetConfig> {
        val prefs = factory.create(
            fileName = "torch_widgets",
            keyPrefix = "widget_",
            serializer = TorchWidgetConfig.serializer(),
        )
        return WidgetConfigStore(prefs)
    }

    /**
     * Torch's persistent pin-flow bridge. Backs the kit's generic
     * [PendingWidgetConfigs] with the legacy filename / key prefix
     * (`torch_pending_widgets` / `pending_`) so any in-flight pin
     * dialogs across the upgrade decode correctly.
     *
     * Note the serializer composition: each entry is a
     * `PendingEntry<TorchWidgetConfig>`, so the factory call passes
     * `PendingEntry.serializer(TorchWidgetConfig.serializer())`.
     */
    @Provides
    @Singleton
    fun provideTorchPendingWidgetConfigs(
        factory: FeaturePreferencesFactory,
    ): PendingWidgetConfigs<TorchWidgetConfig> {
        val prefs = factory.create(
            fileName = "torch_pending_widgets",
            keyPrefix = "pending_",
            serializer = PendingEntry.serializer(TorchWidgetConfig.serializer()),
        )
        return PendingWidgetConfigs(prefs, tag = TorchPinLog.TAG)
    }
}
