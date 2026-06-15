package dev.ranzlappen.gadget.feature.radios.bt

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
class BtEnabledMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val adapter: BluetoothAdapterWrapper,
) : MetricSource {

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Bluetooth enabled",
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Network,
    )

    override suspend fun sample(): Float = if (adapter.isEnabled()) 1f else 0f

    override fun stream(): Flow<Float> = callbackFlow {
        trySend(sample())
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                trySend(if (state == BluetoothAdapter.STATE_ON) 1f else 0f)
            }
        }
        context.registerReceiver(receiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        awaitClose { context.unregisterReceiver(receiver) }
    }

    companion object {
        const val METRIC_KEY = "bt_enabled"
    }
}
