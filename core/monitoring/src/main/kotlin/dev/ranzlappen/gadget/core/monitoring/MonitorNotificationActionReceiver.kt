package dev.ranzlappen.gadget.core.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles the "Stop monitoring" action tapped from a per-metric notification.
 * Disables the metric's [MonitorConfig] via [MonitorConfigRepository]; the
 * [MonitorService] reacts via `collectLatest` and stops sampling that metric.
 *
 * Uses [EntryPointAccessors] instead of `@AndroidEntryPoint` because Hilt's
 * ASM transform for receivers requires a generated base class in the javac
 * output dir, which a Kotlin-only module doesn't produce (same pattern as
 * `AutomationAlarmReceiver`).
 */
class MonitorNotificationActionReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun configRepo(): MonitorConfigRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DISABLE_METRIC) return
        val metricKey = intent.getStringExtra(EXTRA_METRIC_KEY) ?: return
        val entry = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ReceiverEntryPoint::class.java,
        )
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val config = entry.configRepo().get(metricKey)
                entry.configRepo().save(metricKey, config.copy(enabled = false))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISABLE_METRIC =
            "dev.ranzlappen.gadget.core.monitoring.ACTION_DISABLE_METRIC"
        const val EXTRA_METRIC_KEY = "metric_key"
    }
}
