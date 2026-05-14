package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Home-screen strobe widget — 1×1 cell, single ImageButton.
 *
 * Tapping the widget toggles [StrobeService]:
 * - If the service isn't running ([StrobeService.isRunning] == false),
 *   start it via `startForegroundService` carrying the widget's
 *   per-instance [TorchWidgetConfig] as Intent extras (rate Hz +
 *   SOS flag).
 * - If the service IS running, fire an [StrobeService.ACTION_STOP]
 *   intent so the service shuts down cleanly (cancels the loop,
 *   turns the torch off, dismisses the foreground notification).
 *
 * The widget icon flips between `ic_strobe` (idle) and `ic_strobe_on`
 * (running) in [onUpdate].
 *
 * Per-instance config: each widget owns a [TorchWidgetConfig] entry
 * in [TorchWidgetConfigRepository] keyed by `appWidgetId`. Config is
 * created at pin time (see [TorchWidgetCreator] +
 * [WidgetPinSuccessReceiver]) and deleted by [onDeleted] when the
 * widget leaves the home screen.
 *
 * The SOS-mode flag in the config is plumbed all the way to the
 * service but its playback pattern is deferred — tracked at
 * https://github.com/Ranzlappen/HardwareDash/issues/96.
 */
class StrobeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val running = StrobeService.isRunning
        appWidgetIds.forEach { id ->
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, id, running))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_STROBE_TOGGLE) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        if (StrobeService.isRunning) {
            val stopIntent = Intent(context, StrobeService::class.java)
                .setAction(StrobeService.ACTION_STOP)
            context.startService(stopIntent)
        } else {
            // Lookup is fast (a Preferences DataStore one-shot read)
            // and we need the rate/SOS before launching the service.
            // `runBlocking` is safe here — BroadcastReceiver.onReceive
            // already runs on a binder-pool thread and the read
            // completes in single-digit ms.
            val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                runBlocking { entry(context).widgetRepository().get(appWidgetId) }
            } else null
            val rateHz = config?.rateHz ?: TorchWidgetConfig.DEFAULT_RATE_HZ
            val sosMode = config?.sosMode ?: false

            val startIntent = Intent(context, StrobeService::class.java).apply {
                putExtra(StrobeService.EXTRA_RATE_HZ, rateHz)
                putExtra(StrobeService.EXTRA_SOS_MODE, sosMode)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(startIntent)
            } else {
                context.startService(startIntent)
            }
        }
        // Refresh icon state on all instances. We do it after the
        // start/stop request so the next render reflects the new
        // isRunning value.
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, StrobeWidgetProvider::class.java)
        val ids = appWidgetManager.getAppWidgetIds(componentName)
        if (ids.isNotEmpty()) onUpdate(context, appWidgetManager, ids)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repo = entry(context).widgetRepository()
        val purgeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        purgeScope.launch {
            appWidgetIds.forEach { repo.delete(it) }
        }
    }

    private fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        running: Boolean,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_strobe).apply {
            setImageViewResource(
                R.id.widget_strobe_button,
                if (running) R.drawable.ic_strobe_on else R.drawable.ic_strobe,
            )
            setOnClickPendingIntent(
                R.id.widget_strobe_button,
                togglePendingIntent(context, appWidgetId),
            )
        }

    private fun entry(context: Context): StrobeWidgetEntryPoint =
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            StrobeWidgetEntryPoint::class.java,
        )

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StrobeWidgetEntryPoint {
        fun widgetRepository(): TorchWidgetConfigRepository
    }

    companion object {
        const val ACTION_STROBE_TOGGLE =
            "dev.ranzlappen.gadget.feature.torch.ACTION_STROBE_TOGGLE"

        /**
         * Build the per-widget toggle PendingIntent. Each
         * `appWidgetId` gets a distinct PendingIntent (the
         * `requestCode` is the ID itself) so the OS doesn't
         * coalesce taps from multiple instances into a single
         * intent.
         */
        fun togglePendingIntent(context: Context, appWidgetId: Int): PendingIntent {
            val intent = Intent(context, StrobeWidgetProvider::class.java).apply {
                action = ACTION_STROBE_TOGGLE
                component = ComponentName(context, StrobeWidgetProvider::class.java)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                /* requestCode = */ appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
    }
}
