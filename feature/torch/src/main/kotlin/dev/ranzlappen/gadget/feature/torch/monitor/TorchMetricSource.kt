package dev.ranzlappen.gadget.feature.torch.monitor

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
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import dev.ranzlappen.gadget.feature.torch.TorchRootCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torch's readable signal for the monitoring framework (and, later, for
 * automation triggers).
 *
 * Intensity is reported as a percent of the stock max: `0` when off, `100`
 * at a normal on, and up to the rooted boost ceiling (≈150) when the rooted
 * flavor has driven [TorchRootCapabilities.boostBrightness]. The descriptor's
 * `maxFlow` tracks the flavor's **live** ceiling
 * ([TorchRootCapabilities.maxBrightnessPercentFlow]) — a constant 100 on
 * standard (binary), rising to 150 on the rooted flavor only after a probe
 * confirms the boost path — so the chart y-axis and the progress widgets
 * scale to the real range instead of clipping a boost at 100.
 *
 * Polled (not push): a continuously-displayed chart needs a sample in every
 * downsample bucket, so [sample] is read on the monitor cadence rather than
 * emitting only on change.
 */
@Singleton
class TorchMetricSource @Inject constructor(
    @ApplicationContext context: Context,
    private val controller: TorchController,
    private val rootCapabilities: TorchRootCapabilities,
) : MetricSource {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.torch_monitor_metric_name),
        unit = "%",
        min = 0f,
        max = NORMAL_ON_PERCENT,
        maxFlow = rootCapabilities.maxBrightnessPercentFlow
            .map { it.toFloat() }
            .stateIn(scope, SharingStarted.Eagerly, NORMAL_ON_PERCENT),
        category = MetricCategory.Actuator,
    )

    override suspend fun sample(): Float {
        if (!controller.state.value.isOn) return 0f
        val commanded = rootCapabilities.commandedBrightnessPercent.value
        return if (commanded > 0) commanded.toFloat() else NORMAL_ON_PERCENT
    }

    companion object {
        const val METRIC_KEY = "torch_intensity"
        private const val NORMAL_ON_PERCENT = 100f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface TorchMonitorModule {

    @Binds
    @IntoMap
    @StringKey(TorchMetricSource.METRIC_KEY)
    fun bindTorchMetricSource(source: TorchMetricSource): MetricSource

    @Binds
    @IntoMap
    @StringKey(TorchMetricSource.METRIC_KEY)
    fun bindTorchMonitorWidgetNotifier(
        notifier: TorchMonitorWidgetNotifier,
    ): MonitorWidgetNotifier
}
