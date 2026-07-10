package dev.ranzlappen.gadget.feature.display.monitor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.ContextCompat
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
import dev.ranzlappen.gadget.feature.display.R
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Readable signal for the monitoring framework: screen brightness as a
 * percent of the framework max (`Settings.System.SCREEN_BRIGHTNESS`,
 * `0..255`, mapped to `0..100`).
 *
 * Push, not poll: brightness only changes on a user/automation write, so a
 * [ContentObserver] on the settings URI drives [stream] and an idle screen
 * costs zero wakeups. [sample] still backs the one-shot poll path (e.g. an
 * automation trigger evaluating current state).
 */
@Singleton
class DisplayBrightnessMetricSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : MetricSource {

    override val descriptor: MetricDescriptor = MetricDescriptor(
        metricKey = METRIC_KEY,
        displayName = context.getString(R.string.display_monitor_metric_name),
        unit = "%",
        min = 0f,
        max = 100f,
        category = MetricCategory.Actuator,
    )

    override suspend fun sample(): Float = readBrightnessPercent()

    override fun stream(): Flow<Float> = callbackFlow {
        trySend(readBrightnessPercent())
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(readBrightnessPercent())
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            observer,
        )
        // Screen-off/on can coincide with brightness-mode changes on some
        // OEM skins without firing the content observer; re-sample on
        // ACTION_SCREEN_ON as a cheap safety net (mirrors LockStateMetricSource's
        // broadcast-driven push pattern).
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                trySend(readBrightnessPercent())
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
            context.unregisterReceiver(receiver)
        }
    }

    private fun readBrightnessPercent(): Float {
        val raw = try {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        return (raw * 100f / MAX_RAW_BRIGHTNESS).coerceIn(0f, 100f)
    }

    companion object {
        const val METRIC_KEY = "screen_brightness"
        private const val MAX_RAW_BRIGHTNESS = 255
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DisplayMonitorModule {

    @Binds
    @IntoMap
    @StringKey(DisplayBrightnessMetricSource.METRIC_KEY)
    fun bindDisplayBrightnessMetricSource(source: DisplayBrightnessMetricSource): MetricSource
}
