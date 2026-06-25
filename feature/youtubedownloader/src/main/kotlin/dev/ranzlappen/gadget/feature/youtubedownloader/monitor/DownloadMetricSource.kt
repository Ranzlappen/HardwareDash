package dev.ranzlappen.gadget.feature.youtubedownloader.monitor

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
import dev.ranzlappen.gadget.feature.youtubedownloader.DownloadStatus
import dev.ranzlappen.gadget.feature.youtubedownloader.DownloadTask
import dev.ranzlappen.gadget.feature.youtubedownloader.R
import dev.ranzlappen.gadget.feature.youtubedownloader.YoutubeDlEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exposes the active download's progress (0–100 %) as a monitorable signal,
 * satisfying the Module Authoring Contract's monitoring requirement. When no
 * download is running the source reads 0.
 */
@Singleton
class DownloadMetricSource @Inject constructor(
    @ApplicationContext context: Context,
    private val engine: YoutubeDlEngine,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.ytdl_metric_name),
        unit = "%",
        min = 0f,
        max = 100f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float = activeProgress(engine.tasks.value)

    override fun stream(): Flow<Float> = engine.tasks.map(::activeProgress)

    private fun activeProgress(tasks: Map<String, DownloadTask>): Float =
        tasks.values
            .firstOrNull { it.status == DownloadStatus.Running }
            ?.progress
            ?: 0f

    companion object {
        const val METRIC_KEY = "youtube_download_progress"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DownloadMonitorModule {
    @Binds
    @IntoMap
    @StringKey(DownloadMetricSource.METRIC_KEY)
    fun bindDownloadMetricSource(source: DownloadMetricSource): MetricSource
}
