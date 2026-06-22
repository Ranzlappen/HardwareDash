package dev.ranzlappen.gadget.feature.radios.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class WifiMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _state = MutableStateFlow(buildCurrentState())
    val state: StateFlow<WifiState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            _state.value = buildCurrentState()
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.RSSI_CHANGED_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(receiver, filter)
    }

    @Suppress("DEPRECATION")
    private fun buildCurrentState(): WifiState {
        val enabled = wifiManager.isWifiEnabled
        if (!enabled) return WifiState(enabled = false)
        val info = wifiManager.connectionInfo
        val connected = info != null && info.networkId != -1
        return WifiState(
            enabled = true,
            connected = connected,
            ssid = if (connected) {
                val raw = info!!.ssid
                when {
                    raw == null || raw == WifiManager.UNKNOWN_SSID -> null
                    raw.startsWith("\"") && raw.endsWith("\"") -> raw.drop(1).dropLast(1)
                    else -> raw
                }
            } else null,
            rssiDbm = if (connected) info!!.rssi.takeIf { it > Int.MIN_VALUE } else null,
            linkSpeedMbps = if (connected) info!!.linkSpeed.takeIf { it > 0 } else null,
            frequencyMhz = if (connected) info!!.frequency.takeIf { it > 0 } else null,
            bssid = if (connected) {
                info!!.bssid?.takeIf { it != "02:00:00:00:00:00" }
            } else null,
        )
    }
}
