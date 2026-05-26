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
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.TorchController
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Torch's readable signal for the monitoring framework (and, later, for
 * automation triggers). Standard intensity is binary — `100` when the
 * flash is on, `0` otherwise; rooted brightness reporting is a later
 * enhancement once `boostBrightness` lands.
 */
@Singleton
class TorchMetricSource @Inject constructor(
    @ApplicationContext context: Context,
    private val controller: TorchController,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.torch_monitor_metric_name),
        unit = "%",
        min = 0f,
        max = 100f,
        category = MetricCategory.Actuator,
    )

    override suspend fun sample(): Float = if (controller.state.value.isOn) 100f else 0f

    companion object {
        const val METRIC_KEY = "torch_intensity"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface TorchMonitorModule {

    @Binds
    @IntoMap
    @StringKey(TorchMetricSource.METRIC_KEY)
    fun bindTorchMetricSource(source: TorchMetricSource): MetricSource
}
