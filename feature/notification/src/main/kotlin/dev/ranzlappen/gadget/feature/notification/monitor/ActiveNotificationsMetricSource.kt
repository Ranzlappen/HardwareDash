package dev.ranzlappen.gadget.feature.notification.monitor

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.notification.R
import dev.ranzlappen.gadget.feature.notification.listener.ActiveNotificationsBridge
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Count of currently active (posted, not yet cleared) notifications across
 * every app — the module's `active_notifications` readable signal, unlocked
 * once [GadgetNotificationListenerService][dev.ranzlappen.gadget.feature.notification.listener.GadgetNotificationListenerService]
 * is granted access (see that class's kdoc for the two grant paths).
 *
 * Push source: [ActiveNotificationsBridge.activeCount] only changes on a
 * real post/removal callback, so [stream] forwards it directly instead of
 * polling — an idle notification tray costs zero wakeups. [sample] still
 * reads the same last-known value as the one-shot / poll-fallback path the
 * [MetricSource] contract requires.
 *
 * Reports `0` — not an error — while the listener isn't connected; the
 * screen's capability row (driven by [ActiveNotificationsBridge.listenerConnected])
 * is the place that distinguishes "not granted" from "genuinely zero".
 */
@Singleton
class ActiveNotificationsMetricSource @Inject constructor(
    @ApplicationContext context: Context,
    private val bridge: ActiveNotificationsBridge,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.notification_metric_active_notifications_name),
        unit = "",
        min = 0f,
        max = DEFAULT_MAX_COUNT,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float = bridge.activeCount.value.toFloat()

    override fun stream(): Flow<Float> = bridge.activeCount.map { it.toFloat() }

    companion object {
        const val METRIC_KEY = "active_notifications"

        /** Soft chart ceiling — an ordinary device rarely holds more than a
         *  couple dozen simultaneously active notifications; the value can
         *  still exceed this (the chart just clips), it's not a hard cap. */
        private const val DEFAULT_MAX_COUNT = 30f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface NotificationMonitorModule {

    @Binds
    @IntoMap
    @StringKey(ActiveNotificationsMetricSource.METRIC_KEY)
    fun bindActiveNotificationsMetricSource(source: ActiveNotificationsMetricSource): MetricSource
}
