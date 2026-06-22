package dev.ranzlappen.gadget.feature.radios.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.ranzlappen.gadget.core.model.MetricCategory
import dev.ranzlappen.gadget.core.model.MetricDescriptor
import dev.ranzlappen.gadget.core.model.MetricSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class WifiSignalMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "WiFi signal",
        unit = "dBm",
        min = -100f,
        max = -30f,
        category = MetricCategory.Network,
    )

    @Suppress("DEPRECATION")
    override suspend fun sample(): Float = wifiManager.connectionInfo?.rssi?.toFloat() ?: -100f

    override fun stream(): Flow<Float> = callbackFlow {
        trySend(sample())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, Int.MIN_VALUE)
                if (rssi != Int.MIN_VALUE) trySend(rssi.toFloat())
            }
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    companion object {
        const val METRIC_KEY = "wifi_signal"
    }
}
