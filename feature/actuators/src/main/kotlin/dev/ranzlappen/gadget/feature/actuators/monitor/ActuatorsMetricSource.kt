package dev.ranzlappen.gadget.feature.actuators.monitor

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
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
 * Reports whether the device's vibrator actuator is present — `1` when
 * [Vibrator.hasVibrator] is true, `0` otherwise — the same
 * `VIBRATOR_MANAGER_SERVICE`/`VIBRATOR_SERVICE` lookup and availability
 * check [ActuatorsViewModel][dev.ranzlappen.gadget.feature.actuators.ActuatorsViewModel]
 * publishes as `ActuatorsState.vibratorAvailable` and
 * `ActuatorsActionHandler` guards its haptic actions on. Unlike the torch
 * (a continuously commandable brightness level) or vibration's modelled
 * amplitude, `:feature:actuators`'s haptic actions
 * (`ACTION_HAPTIC_CLICK`/`ACTION_HAPTIC_HEAVY`) are fire-and-forget one-shots
 * with no "currently vibrating at X" state to sample — the only state this
 * controller actually tracks is hardware presence, so that's the signal.
 *
 * Polled (not push): device vibrator presence is effectively static for a
 * running process, so a plain poll on the monitor cadence is sufficient —
 * no OS broadcast exists for "vibrator attached/detached" to push from.
 */
@Singleton
class ActuatorsMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
            ?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.actuators_monitor_metric_name),
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Actuator,
    )

    override suspend fun sample(): Float = if (vibrator?.hasVibrator() == true) 1f else 0f

    companion object {
        const val METRIC_KEY = "vibrator_available"
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
