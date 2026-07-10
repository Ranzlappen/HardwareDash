package dev.ranzlappen.gadget.feature.usbdebug.monitor

import android.content.Context
import android.provider.Settings
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
import dev.ranzlappen.gadget.feature.usbdebug.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * USB-debugging enabled/disabled signal for monitoring and (later)
 * automation triggers — `0`/`1` read straight from
 * `Settings.Global.ADB_ENABLED`.
 *
 * Android exposes no separate "USB debugging" setting distinct from ADB
 * access — flipping the Developer-options "USB debugging" switch and
 * `adb`'s own enablement are the exact same underlying flag.
 * `:feature:adbdebug`'s own metric (once it ships one) will read this same
 * setting under its own `adb_enabled` key; that's expected duplication —
 * the two features model different feature-facing concepts even though
 * the OS conflates them into one setting.
 *
 * Polled (not push): no cheap observable seam is wired for this setting
 * here, so [sample] is read on the monitor's normal cadence rather than
 * emitting only on change (a `ContentObserver` on
 * `Settings.Global.getUriFor(ADB_ENABLED)` would let a future revision
 * switch this to push).
 */
@Singleton
class UsbDebuggingMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.usbdebug_monitor_metric_name),
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float =
        if (Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1) 1f else 0f

    companion object {
        const val METRIC_KEY = "usb_debugging"
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface UsbDebugMonitorModule {

    @Binds
    @IntoMap
    @StringKey(UsbDebuggingMetricSource.METRIC_KEY)
    fun bindUsbDebuggingMetricSource(source: UsbDebuggingMetricSource): MetricSource
}
