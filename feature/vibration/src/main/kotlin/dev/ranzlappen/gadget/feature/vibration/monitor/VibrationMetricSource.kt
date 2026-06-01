package dev.ranzlappen.gadget.feature.vibration.monitor

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
import dev.ranzlappen.gadget.core.monitoring.MonitorWidgetNotifier
import dev.ranzlappen.gadget.feature.vibration.R
import dev.ranzlappen.gadget.feature.vibration.VibrationRootCapabilities
import dev.ranzlappen.gadget.feature.vibration.VibrationRuntime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vibration's readable signal for the monitoring framework (and, later, for
 * automation triggers).
 *
 * Amplitude is reported as a percent of full scale: `0` while idle, up to the
 * commanded amplitude during a command, decaying back to 0 when a timed
 * command expires. Because the OS exposes **no** "currently vibrating at X"
 * query, the value comes from the modelled [VibrationRuntime] rather than the
 * hardware — the deliberate stress-test of the poll contract for a
 * non-pollable actuator.
 *
 * Polled (not push): a continuously-displayed chart needs a sample in every
 * downsample bucket, so [sample] reads the runtime on the monitor cadence and
 * produces a filled plateau that drops to 0 at command end.
 *
 * The descriptor's `maxFlow` tracks the rooted flavor's **live** ceiling
 * ([VibrationRootCapabilities.maxAmplitudePercentFlow]) — a constant 100 on
 * standard, potentially higher on the rooted extreme-amplitude path — so the
 * chart y-axis / progress widgets scale to the real range.
 */
@Singleton
class VibrationMetricSource @Inject constructor(
    @ApplicationContext context: Context,
    private val runtime: VibrationRuntime,
    private val rootCapabilities: VibrationRootCapabilities,
) : MetricSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.vibration_monitor_metric_name),
        unit = "%",
        min = 0f,
        max = FULL_SCALE_PERCENT,
        maxFlow = rootCapabilities.maxAmplitudePercentFlow
            .map { it.toFloat() }
            .stateIn(scope, SharingStarted.Eagerly, FULL_SCALE_PERCENT),
        category = MetricCategory.Actuator,
    )

    override suspend fun sample(): Float {
        // The rooted extreme-tier path publishes its own commanded amplitude
        // (which can exceed the modelled standard 0..100); prefer it when
        // active, else fall back to the standard runtime model.
        val rootCommanded = rootCapabilities.commandedAmplitudePercent.value
        if (rootCommanded > 0) return rootCommanded.toFloat()
        return runtime.state.value.amplitudePercent.toFloat()
    }

    companion object {
        const val METRIC_KEY = "vibration_amplitude"
        private const val FULL_SCALE_PERCENT = 100f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface VibrationMonitorModule {

    @Binds
    @IntoMap
    @StringKey(VibrationMetricSource.METRIC_KEY)
    fun bindVibrationMetricSource(source: VibrationMetricSource): MetricSource

    @Binds
    @IntoMap
    @StringKey(VibrationMetricSource.METRIC_KEY)
    fun bindVibrationMonitorWidgetNotifier(
        notifier: VibrationMonitorWidgetNotifier,
    ): MonitorWidgetNotifier
}
