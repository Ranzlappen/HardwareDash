package dev.ranzlappen.gadget.feature.vibration.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.core.datastore.FeaturePreferencesFactory
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackConfig
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingEntry
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationPinLog
import dev.ranzlappen.gadget.feature.vibration.widget.VibrationWidgetConfig
import javax.inject.Singleton

/**
 * `@Provides`-style Hilt module for `:feature:vibration` (sibling to the
 * `@Binds`-only [VibrationModule]). Provides the kit's per-feature
 * [WidgetFeedbackConfig], [WidgetConfigStore], and [PendingWidgetConfigs] —
 * each backed by a vibration-namespaced DataStore so it never collides with
 * torch's.
 */
@Module
@InstallIn(SingletonComponent::class)
object VibrationProvidesModule {

    /**
     * Vibration's per-feature [WidgetFeedbackConfig]. The channel id is
     * **vibration-prefixed** (`"vibration_widget_feedback"`) so it never
     * collides with torch's legacy `"widget_feedback"` channel.
     */
    @Provides
    @Singleton
    fun provideWidgetFeedbackConfig(
        @ApplicationContext context: Context,
    ): WidgetFeedbackConfig = WidgetFeedbackConfig(
        channelId = "vibration_widget_feedback",
        channelName = context.getString(R.string.vibration_widget_feedback_channel_name),
        channelDescription = context.getString(R.string.vibration_widget_feedback_channel_description),
        smallIcon = R.drawable.ic_vibration_on,
        notificationIdBase = 0x56_42_00_00, // "VB"
    )

    @Provides
    @Singleton
    fun provideVibrationWidgetConfigStore(
        factory: FeaturePreferencesFactory,
    ): WidgetConfigStore<VibrationWidgetConfig> {
        val prefs = factory.create(
            fileName = "vibration_widgets",
            keyPrefix = "widget_",
            serializer = VibrationWidgetConfig.serializer(),
        )
        return WidgetConfigStore(prefs)
    }

    @Provides
    @Singleton
    fun provideVibrationPendingWidgetConfigs(
        factory: FeaturePreferencesFactory,
    ): PendingWidgetConfigs<VibrationWidgetConfig> {
        val prefs = factory.create(
            fileName = "vibration_pending_widgets",
            keyPrefix = "pending_",
            serializer = PendingEntry.serializer(VibrationWidgetConfig.serializer()),
        )
        return PendingWidgetConfigs(prefs, tag = VibrationPinLog.TAG)
    }
}
