package dev.ranzlappen.gadget.feature.torch.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackConfig
import dev.ranzlappen.gadget.feature.torch.R
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
     * As the second widget-bearing feature lands, this will be
     * promoted to a `Map<FeatureId, WidgetFeedbackConfig>`
     * multibinding so a single dispatcher serves both.
     */
    @Provides
    @Singleton
    fun provideWidgetFeedbackConfig(
        @ApplicationContext context: Context,
    ): WidgetFeedbackConfig = WidgetFeedbackConfig(
        channelId = "widget_feedback",
        channelName = context.getString(R.string.widget_feedback_channel_name),
        channelDescription = context.getString(R.string.widget_feedback_channel_description),
        smallIcon = R.drawable.ic_strobe,
        notificationIdBase = 0x57_46_00_00, // "WF" — matches legacy WidgetFeedbackDispatcher
    )
}
