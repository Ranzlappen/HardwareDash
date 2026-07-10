package dev.ranzlappen.gadget.feature.radios.cell

import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Cellular signal strength, 0-4 "bars" — [android.telephony.SignalStrength.getLevel],
 * the same value the OS status bar renders. Wired `@IntoMap
 * @StringKey(METRIC_KEY)` via [dev.ranzlappen.gadget.feature.radios.cell.di.CellModule].
 *
 * Deliberately **not** raw dBm, unlike
 * [dev.ranzlappen.gadget.feature.radios.wifi.WifiSignalMetricSource]'s
 * precedent: WiFi's RSSI domain is a single, stable scale, but cellular dBm
 * varies wildly by radio access technology (GSM RSSI ≈ -113..-51 dBm vs.
 * LTE RSRP ≈ -140..-44 dBm vs. NR SS-RSRP ≈ -140..-44 dBm on a different
 * reference signal entirely). A raw-dBm automation threshold would silently
 * stop meaning the same thing across a routine LTE↔NR handover. The 0-4
 * bars scale is RAT-normalized by the platform itself and is exactly what
 * [CellScreen] already shows the user, so the automation threshold matches
 * what's on screen.
 *
 * Push source, per the module brief: backed by [CellTelephonyTracker.state],
 * which only changes on a genuine
 * [android.telephony.TelephonyCallback.SignalStrengthsListener] /
 * [android.telephony.PhoneStateListener] callback — never polled.
 */
@Singleton
class CellSignalMetricSource @Inject constructor(
    private val tracker: CellTelephonyTracker,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Cell signal",
        unit = "bars",
        min = 0f,
        max = 4f,
        category = MetricCategory.Network,
    )

    override suspend fun sample(): Float = tracker.state.value.signalLevel.toFloat()

    override fun stream(): Flow<Float> = tracker.state.map { it.signalLevel.toFloat() }

    companion object {
        const val METRIC_KEY = "cell_signal"
    }
}
