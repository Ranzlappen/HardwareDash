package dev.ranzlappen.gadget.feature.lock

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
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
class LockStateMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    private val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    override val descriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = "Lock state",
        unit = "",
        min = 0f,
        max = 1f,
        category = MetricCategory.Device,
    )

    override suspend fun sample(): Float = if (keyguard?.isKeyguardLocked == true) 1f else 0f

    override fun stream(): Flow<Float> = callbackFlow {
        trySend(sample())
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(if (keyguard?.isKeyguardLocked == true) 1f else 0f)
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { context.unregisterReceiver(receiver) }
    }

    companion object {
        const val METRIC_KEY = "lock_state"
    }
}
