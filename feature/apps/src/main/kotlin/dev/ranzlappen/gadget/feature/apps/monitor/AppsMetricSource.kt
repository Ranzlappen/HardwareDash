package dev.ranzlappen.gadget.feature.apps.monitor

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.StringKey
import dev.ranzlappen.gadget.core.data.apps.AppsDao
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import dev.ranzlappen.gadget.feature.apps.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-Organizer's readable signal for the monitoring framework (and, later,
 * for automation triggers): how many folders the user currently has
 * configured.
 *
 * `apps_folder` row count is the most defensible already-modelled signal
 * here — the App-Organizer domain layer doesn't track any "apps launched"
 * or "widget taps" counters (there's no [dev.ranzlappen.gadget.core.data.apps.AppsDao]
 * write path for either), so this reuses [AppsDao.observeFolders] rather than
 * inventing a new counter. It's the exact same flow
 * [dev.ranzlappen.gadget.feature.apps.widget.FolderWidgetController] already
 * watches (combined with app-record and rule counts) to repaint placed
 * folder widgets, so it's proven to reflect real state changes.
 *
 * Push (not poll): folder creation/deletion is a discrete, already-observable
 * Room event, not a continuously sampled hardware signal — polling it would
 * just re-read an unchanged value on every cadence tick. [sample] still
 * works as a one-shot read (e.g. for the future automation engine) by taking
 * the first emission of the same flow.
 */
@Singleton
class AppsMetricSource @Inject constructor(
    @ApplicationContext context: Context,
    private val dao: AppsDao,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.apps_monitor_metric_folder_count_name),
        unit = context.getString(R.string.apps_monitor_metric_folder_count_unit),
        min = 0f,
        max = Float.MAX_VALUE,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float = dao.observeFolders().first().size.toFloat()

    override fun stream(): Flow<Float> =
        dao.observeFolders().map { it.size.toFloat() }.distinctUntilChanged()

    companion object {
        const val METRIC_KEY = "apps_folder_count"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface AppsMonitorModule {

    @Binds
    @IntoMap
    @StringKey(AppsMetricSource.METRIC_KEY)
    fun bindAppsMetricSource(source: AppsMetricSource): MetricSource
}
