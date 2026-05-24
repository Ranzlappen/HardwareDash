package dev.ranzlappen.gadget.feature.torch.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService
import dev.ranzlappen.gadget.feature.torch.widget.customization.TapAnimation
import dev.ranzlappen.gadget.feature.torch.widget.customization.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.feature.torch.widget.feedback.WidgetFeedbackDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home-screen strobe widget — 1×1 cell.
 *
 * Tapping the widget toggles [StrobeService]:
 * - If the service isn't running ([StrobeService.isRunning] == false),
 *   start it via `startForegroundService` carrying the widget's
 *   per-instance [TorchWidgetConfig] as Intent extras (rate Hz +
 *   SOS flag).
 * - If the service IS running, fire an [StrobeService.ACTION_STOP]
 *   intent so the service shuts down cleanly.
 *
 * Each pinned instance owns a [TorchWidgetConfig] keyed by
 * `appWidgetId`. The config drives rate, SOS, and the visual
 * appearance (background mode, icon style, tap animation, toggle
 * feedback). Self-heal applies if a config goes missing — see
 * [FlashlightWidgetProvider] for the canonical write-up.
 */
class StrobeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                renderAll(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Render every [appWidgetIds] instance from its persisted config,
     * read via the DataStore-backed [TorchWidgetConfigRepository.getAll]
     * (not the hot `all.value` cache) so a cold process still paints the
     * saved appearance instead of self-healing a default over it.
     */
    private suspend fun renderAll(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val repo = entry(context).widgetRepository()
        val configs = repo.getAll()
        val running = StrobeService.isRunning
        appWidgetIds.forEach { id ->
            val config = configs[id] ?: run {
                val default = TorchWidgetConfig(
                    type = WidgetType.Strobe,
                    displayName = context.getString(R.string.torch_widget_default_name_strobe),
                )
                Log.w(PendingTorchWidgetConfigs.TAG, "StrobeWidget self-heal id=$id")
                repo.save(id, default)
                default
            }
            appWidgetManager.updateAppWidget(id, buildRemoteViews(context, id, running, config))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_STROBE_TOGGLE) return
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val ep = entry(context)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // DataStore-backed read so a cold process sees the real
                // rate / SOS / feedback / animation config — the hot cache
                // is empty until its first async emission.
                val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ep.widgetRepository().get(appWidgetId)
                } else {
                    null
                }

                val willBeRunning: Boolean
                if (StrobeService.isRunning) {
                    context.startService(
                        Intent(context, StrobeService::class.java).setAction(StrobeService.ACTION_STOP),
                    )
                    willBeRunning = false
                } else {
                    val startIntent = Intent(context, StrobeService::class.java).apply {
                        putExtra(StrobeService.EXTRA_RATE_HZ, config?.rateHz ?: TorchWidgetConfig.DEFAULT_RATE_HZ)
                        putExtra(StrobeService.EXTRA_SOS_MODE, config?.sosMode ?: false)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(startIntent)
                    } else {
                        context.startService(startIntent)
                    }
                    willBeRunning = true
                }

                if (config != null) {
                    withContext(Dispatchers.Main) {
                        ep.feedbackDispatcher().dispatch(
                            displayName = config.displayName,
                            newState = willBeRunning,
                            feedback = config.appearance.feedback,
                        )
                    }
                }

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val componentName = ComponentName(context, StrobeWidgetProvider::class.java)
                val ids = appWidgetManager.getAppWidgetIds(componentName)
                if (ids.isNotEmpty()) renderAll(context, appWidgetManager, ids)

                if (config != null &&
                    appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
                    config.appearance.tap.enabled &&
                    config.appearance.tap.animation.hasPressFrame()
                ) {
                    playTapPressFrame(
                        manager = appWidgetManager,
                        appWidgetId = appWidgetId,
                        pressedViews = buildRemoteViews(context, appWidgetId, willBeRunning, config, pressed = true),
                        restingViews = buildRemoteViews(context, appWidgetId, willBeRunning, config, pressed = false),
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
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
        config: TorchWidgetConfig,
        pressed: Boolean = false,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_strobe).apply {
            val renderer = entry(context).appearanceRenderer()
            renderer.apply(
                context = context,
                views = this,
                appearance = config.appearance,
                active = running,
            )
            if (pressed) renderer.applyPressedFrame(context, this, config.appearance)
            // Ripple is the launcher's stock press effect — set it as the
            // click target's background only when selected, transparent
            // otherwise (set every render so a recycled view reverts).
            val rippleOn = config.appearance.tap.enabled &&
                config.appearance.tap.animation == TapAnimation.Ripple
            setInt(
                R.id.widget_strobe_button,
                "setBackgroundResource",
                if (rippleOn) R.drawable.widget_tap_ripple else android.R.color.transparent,
            )
            if (config.appearance.tap.enabled) {
                setOnClickPendingIntent(
                    R.id.widget_strobe_button,
                    togglePendingIntent(context, appWidgetId),
                )
            } else {
                setOnClickPendingIntent(R.id.widget_strobe_button, null)
            }
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
        fun appearanceRenderer(): WidgetAppearanceRenderer
        fun feedbackDispatcher(): WidgetFeedbackDispatcher
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
