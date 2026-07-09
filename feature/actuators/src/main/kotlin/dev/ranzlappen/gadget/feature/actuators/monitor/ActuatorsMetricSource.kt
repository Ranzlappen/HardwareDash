package dev.ranzlappen.gadget.feature.actuators.monitor

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
import dev.ranzlappen.gadget.feature.actuators.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Actuators' readable signal for the monitoring framework (and, later, for
 * automation triggers).
 *
 * Pulse strength is reported as a percent of full scale: `0` while idle,
 * `100` for the duration of a dispatched haptic action
 * (`ActuatorsActionHandler`'s click/heavy-click triggers), decaying back to
 * 0 once that pulse's approximate duration elapses. Because the OS exposes
 * **no** "currently vibrating" query — same constraint as
 * `:feature:vibration` — the value comes from the modelled [ActuatorsRuntime]
 * rather than the hardware.
 *
 * Polled (not push): a continuously-displayed chart needs a sample in every
 * downsample bucket, so [sample] reads the runtime on the monitor cadence and
 * produces a filled plateau that drops to 0 at pulse end.
 */
@Singleton
class ActuatorsMetricSource @Inject constructor(
    @ApplicationContext context: Context,
    private val runtime: ActuatorsRuntime,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.actuators_monitor_metric_name),
        unit = "%",
        min = 0f,
        max = FULL_SCALE_PERCENT,
        category = MetricCategory.Actuator,
    )

    override suspend fun sample(): Float = runtime.pulsePercent.value

    companion object {
        const val METRIC_KEY = "actuators_pulse"
        private const val FULL_SCALE_PERCENT = 100f
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface ActuatorsMonitorModule {

    @Binds
    @IntoMap
    @StringKey(ActuatorsMetricSource.METRIC_KEY)
    fun bindActuatorsMetricSource(source: ActuatorsMetricSource): MetricSource
}
