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
import dev.ranzlappen.gadget.core.widgetkit.WidgetReceiverScope
import dev.ranzlappen.gadget.core.widgetkit.config.TapAnimation
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.render.hasPressFrame
import dev.ranzlappen.gadget.core.widgetkit.render.playTapPressFrame
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeRuntime
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackDispatcher
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Home-screen strobe widget — 1×1 cell.
 *
 * Tapping the widget toggles [StrobeService]:
 * - If the service isn't running ([StrobeRuntime.running] == false),
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
        WidgetReceiverScope.scope.launch {
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
        val ep = entry(context)
        val repo = ep.widgetRepository()
        val configs = repo.getAll()
        val running = ep.strobeRuntime().running.value
        appWidgetIds.forEach { id ->
            val config = configs[id] ?: run {
                val default = TorchWidgetConfig(
                    type = WidgetType.Strobe,
                    displayName = context.getString(R.string.torch_widget_default_name_strobe),
                )
                Log.w(PendingTorchWidgetConfigs.TAG, "StrobeWidget self-heal id=$id")
                // saveIfAbsent (not save) so a concurrent pin-success
                // write of the real config — including its Morse text —
                // is never clobbered by this default. This is the fix
                // for "Morse only works after editing a new widget".
                repo.saveIfAbsent(id, default)
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

        // Start / stop the service SYNCHRONOUSLY in the foreground broadcast
        // context. Deferring the start into the goAsync coroutine below
        // risks ForegroundServiceStartNotAllowedException on Android 12+.
        // We pass only the appWidgetId — the service reads that widget's
        // rate / SOS config itself.
        val willBeRunning: Boolean
        if (ep.strobeRuntime().running.value) {
            context.startService(
                Intent(context, StrobeService::class.java).setAction(StrobeService.ACTION_STOP),
            )
            willBeRunning = false
        } else {
            val startIntent = Intent(context, StrobeService::class.java).apply {
                putExtra(StrobeService.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            willBeRunning = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(startIntent)
                } else {
                    context.startService(startIntent)
                }
                true
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException (API 31+) is an
                // IllegalStateException subtype thrown when the broadcast
                // lands outside an allowed FGS-start window. Degrade
                // gracefully — a stray launcher tap must never crash the
                // home-screen process.
                Log.w(PendingTorchWidgetConfigs.TAG, "Strobe FGS start refused", e)
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.strobe_widget_start_failed),
                    android.widget.Toast.LENGTH_SHORT,
                ).show()
                false
            }
        }

        // Feedback + animation + repaint run off the FGS path (no service
        // start here), so an FGS exception can never abort them.
        val pendingResult = goAsync()
        WidgetReceiverScope.scope.launch {
            try {
                val config = if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    ep.widgetRepository().get(appWidgetId)
                } else {
                    null
                }
                Log.d(
                    PendingTorchWidgetConfigs.TAG,
                    "StrobeWidget tap id=$appWidgetId running->$willBeRunning " +
                        "config=${config != null} morse=${config?.morseMode} " +
                        "fb=${config?.appearance?.feedback?.let { it::class.simpleName }} " +
                        "anim=${config?.appearance?.tap?.animation}",
                )

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
            } catch (t: Throwable) {
                Log.e(PendingTorchWidgetConfigs.TAG, "StrobeWidget onReceive failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val repo = entry(context).widgetRepository()
        WidgetReceiverScope.scope.launch {
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
            if (config.removed) {
                // Deleted in-app but still hosted by the launcher: dim it
                // and drop the tap target so it reads as defunct until
                // the user drags it off the home screen.
                setInt(WidgetKitR.id.widget_icon, "setImageAlpha", REMOVED_WIDGET_ICON_ALPHA)
                setInt(R.id.widget_strobe_button, "setBackgroundResource", android.R.color.transparent)
                setOnClickPendingIntent(R.id.widget_strobe_button, null)
                return@apply
            }
            if (pressed) renderer.applyPressedFrame(context, this, config.appearance)
            // Ripple is the launcher's stock press effect — set it as the
            // click target's background only when selected, transparent
            // otherwise (set every render so a recycled view reverts).
            val rippleOn = config.appearance.tap.enabled &&
                config.appearance.tap.animation == TapAnimation.Ripple
            setInt(
                R.id.widget_strobe_button,
                "setBackgroundResource",
                if (rippleOn) WidgetKitR.drawable.widget_tap_ripple else android.R.color.transparent,
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
        fun strobeRuntime(): StrobeRuntime
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
