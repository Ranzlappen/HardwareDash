package dev.ranzlappen.gadget.feature.logbook.monitor

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
import dev.ranzlappen.gadget.feature.logbook.LogbookRepository
import dev.ranzlappen.gadget.feature.logbook.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The `logbook_open_checkpoints` readable signal: the count of incomplete
 * checkpoints across every process, right now. It is an event-driven count
 * (it only changes on a checkpoint create/complete/delete) so it is exposed
 * as a **push** source over [LogbookRepository.openCheckpointCount] — an idle
 * logbook causes zero wakeups — with the required [sample] poll path reading
 * the same flow's current value.
 *
 * Charting/alerting the open-checkpoint count lets automation react to a
 * backlog (e.g. "notify me when I have more than 5 open checkpoints").
 */
@Singleton
class LogbookMetricSource @Inject constructor(
    @ApplicationContext context: Context,
    private val repository: LogbookRepository,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.logbook_monitor_metric_name),
        unit = "",
        min = 0f,
        max = DEFAULT_MAX_CHECKPOINTS,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float =
        repository.openCheckpointCount.first().toFloat()

    override fun stream(): Flow<Float> =
        repository.openCheckpointCount.map { it.toFloat() }

    companion object {
        const val METRIC_KEY = "logbook_open_checkpoints"
        private const val DEFAULT_MAX_CHECKPOINTS = 20f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface LogbookMonitorModule {

    @Binds
    @IntoMap
    @StringKey(LogbookMetricSource.METRIC_KEY)
    fun bindLogbookMetricSource(source: LogbookMetricSource): MetricSource
}
