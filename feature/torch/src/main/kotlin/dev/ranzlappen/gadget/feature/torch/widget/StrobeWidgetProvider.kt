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
import dev.ranzlappen.gadget.core.widgetkit.feedback.WidgetFeedbackDispatcher
import dev.ranzlappen.gadget.core.widgetkit.pin.PendingWidgetConfigs
import dev.ranzlappen.gadget.core.widgetkit.provider.BaseGadgetWidgetProvider
import dev.ranzlappen.gadget.core.widgetkit.render.WidgetAppearanceRenderer
import dev.ranzlappen.gadget.core.widgetkit.store.WidgetConfigStore
import dev.ranzlappen.gadget.feature.torch.R
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeRuntime
import dev.ranzlappen.gadget.feature.torch.strobe.StrobeService
import dev.ranzlappen.gadget.core.widgetkit.R as WidgetKitR
import kotlinx.coroutines.launch

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
 *
 * **Lifecycle skeleton inherited** from [BaseGadgetWidgetProvider]:
 *  - `onUpdate` → `renderAll` with the self-heal fallback to
 *    [defaultConfig].
 *  - `onDeleted` → per-id `WidgetConfigStore.delete`.
 *  - The post-action chain (feedback + repaint + press-frame) is the
 *    base's `handleTapAfterAction`; this class only owns the
 *    synchronous service-start path that has to run before
 *    `goAsync()` for FGS-safety.
 */
class StrobeWidgetProvider : BaseGadgetWidgetProvider<TorchWidgetConfig>() {

    override val logTag: String = TorchPinLog.TAG

    override val providerClass: Class<out AppWidgetProvider> =
        StrobeWidgetProvider::class.java

    override fun configStore(context: Context): WidgetConfigStore<TorchWidgetConfig> =
        entry(context).widgetRepository()

    override fun appearanceRenderer(context: Context): WidgetAppearanceRenderer =
        entry(context).appearanceRenderer()

    override fun feedbackDispatcher(context: Context): WidgetFeedbackDispatcher =
        entry(context).feedbackDispatcher()

    override fun defaultConfig(context: Context): TorchWidgetConfig = TorchWidgetConfig(
        type = WidgetType.Strobe,
        displayName = context.getString(R.string.torch_widget_default_name_strobe),
    )

    override suspend fun activeState(context: Context): Boolean =
        entry(context).strobeRuntime().running.value

    // Rescue a freshly-pinned strobe widget whose OS success callback never
    // landed: pull the sole unclaimed pending Strobe config (filtered by type
    // so it can't grab a pending Flashlight; sole-match so two strobes pinned
    // at once can't have their configs swapped). This is what makes a
    // first-pin Morse setting reliably apply on the very first tap.
    override suspend fun reconcilePendingConfig(context: Context): TorchWidgetConfig? =
        entry(context).pendingConfigs().claimSolePending { it.type == WidgetType.Strobe }

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
                Log.w(logTag, "Strobe FGS start refused", e)
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
                handleTapAfterAction(context, appWidgetId, willBeRunning)
            } catch (t: Throwable) {
                Log.e(logTag, "StrobeWidget onReceive failed", t)
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun buildRemoteViews(
        context: Context,
        appWidgetId: Int,
        active: Boolean,
        config: TorchWidgetConfig,
        pressed: Boolean,
    ): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_strobe).apply {
            val renderer = appearanceRenderer(context)
            renderer.apply(
                context = context,
                views = this,
                appearance = config.appearance,
                active = active,
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
        fun widgetRepository(): WidgetConfigStore<TorchWidgetConfig>
        fun appearanceRenderer(): WidgetAppearanceRenderer
        fun feedbackDispatcher(): WidgetFeedbackDispatcher
        fun strobeRuntime(): StrobeRuntime
        fun pendingConfigs(): PendingWidgetConfigs<TorchWidgetConfig>
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
