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
class WifiEnabledMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "WiFi enabled",
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Network,
    )

    override suspend fun sample(): Float = if (wifiManager.isWifiEnabled) 1f else 0f

    override fun stream(): Flow<Float> = callbackFlow {
        trySend(sample())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val wifiState = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN,
                )
                trySend(if (wifiState == WifiManager.WIFI_STATE_ENABLED) 1f else 0f)
            }
        }
        context.registerReceiver(receiver, IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    companion object {
        const val METRIC_KEY = "wifi_enabled"
    }
}
